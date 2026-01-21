(ns org-mode.parser.blocks.core
  "Block parser for org-mode documents.
   Parses keywords, comments, tables, lists, drawers, code blocks, etc."
  (:require
   [clojure.string :as str]
   [org-mode.parser.inline.core :as inline]))

;; Tags ------------------------------------------------------------------------

(def ^:const tag-newlines :newlines)
(def ^:const tag-text :text)
(def ^:const tag-comment :comment)
(def ^:const tag-keyword :keyword)
(def ^:const tag-block :block)
(def ^:const tag-drawer-start :drawer-start)
(def ^:const tag-drawer-end :drawer-end)
(def ^:const tag-property :property)
(def ^:const tag-horizontal-rule :horizontal-rule)
(def ^:const tag-list :list)
(def ^:const tag-table-row :table-row)
(def ^:const tag-table-separator :table-separator)
(def ^:const tag-column :column)
(def ^:const tag-scheduled :scheduled)
(def ^:const tag-deadline :deadline)
(def ^:const tag-closed :closed)
(def ^:const tag-clock :clock)

;; Helpers ---------------------------------------------------------------------

(defn- starts-with-ic?
  "Case-insensitive starts-with check."
  [^String s ^String prefix]
  (let [slen (.length s)
        plen (.length prefix)]
    (and (>= slen plen)
         (loop [i 0]
           (if (>= i plen)
             true
             (let [sc (Character/toLowerCase (.charAt s i))
                   pc (Character/toLowerCase (.charAt prefix i))]
               (if (= sc pc)
                 (recur (inc i))
                 false)))))))

(defn- count-leading-spaces
  "Count leading spaces/tabs."
  [^String s]
  (let [len (.length s)]
    (loop [i 0]
      (if (>= i len)
        i
        (let [ch (.charAt s i)]
          (if (or (= ch \space) (= ch \tab))
            (recur (inc i))
            i))))))

;; Block Parsers ---------------------------------------------------------------

(defn- parse-comment-line
  "Parse '# comment text'. Returns token or nil."
  [^String line]
  (when (and (>= (.length line) 2)
             (= (.charAt line 0) \#)
             (= (.charAt line 1) \space))
    [tag-comment (inline/parse (subs line 2))]))

(defn- parse-keyword-line
  "Parse '#+KEY: value'. Returns token or nil."
  [^String line]
  (when (and (>= (.length line) 3)
             (= (.charAt line 0) \#)
             (= (.charAt line 1) \+)
             (not (starts-with-ic? line "#+BEGIN_"))
             (not (starts-with-ic? line "#+END_")))
    (let [colon-idx (.indexOf line ":")]
      (when (> colon-idx 2)
        (let [key (str/upper-case (subs line 2 colon-idx))
              value (str/trim (subs line (inc colon-idx)))]
          [tag-keyword {:key key :value value}])))))

(defn- parse-greater-block-start
  "Parse '#+BEGIN_TYPE args'. Returns [type args-str] or nil."
  [^String line]
  (when (starts-with-ic? line "#+BEGIN_")
    (let [rest-str (subs line 8)
          space-idx (.indexOf rest-str " ")
          [block-type args-str] (if (neg? space-idx)
                                   [(str/lower-case rest-str) ""]
                                   [(str/lower-case (subs rest-str 0 space-idx))
                                    ;; Keep the space + args for roundtrip compatibility
                                    (subs rest-str space-idx)])]
      [block-type args-str])))

(defn- parse-drawer-or-property
  "Parse ':NAME:' (drawer) or ':KEY: value' (property). Returns token or nil."
  [^String line]
  (when (and (pos? (.length line))
             (= (.charAt line 0) \:))
    (let [second-colon (.indexOf line ":" 1)]
      (when (pos? second-colon)
        (let [name (subs line 1 second-colon)
              rest-str (subs line (inc second-colon))]
          (if (str/blank? rest-str)
            ;; Drawer start or end
            (if (= (str/lower-case name) "end")
              [tag-drawer-end name]
              [tag-drawer-start name])
            ;; Property - preserve the space before value for roundtrip
            [tag-property {:key name
                           :value (inline/parse rest-str)}]))))))

(defn- parse-horizontal-rule
  "Parse '-----' (5+ dashes). Returns token or nil."
  [^String line]
  (let [trimmed (str/trim line)]
    (when (and (>= (.length trimmed) 5)
               (every? #(= % \-) trimmed))
      [tag-horizontal-rule (.length trimmed)])))

(defn- parse-planning-line
  "Parse 'SCHEDULED:', 'DEADLINE:', 'CLOSED:'. Returns token or nil."
  [^String line]
  (cond
    (starts-with-ic? line "SCHEDULED: ")
    [tag-scheduled (inline/parse (subs line 11))]

    (starts-with-ic? line "DEADLINE: ")
    [tag-deadline (inline/parse (subs line 10))]

    (starts-with-ic? line "CLOSED: ")
    [tag-closed (inline/parse (subs line 8))]

    :else nil))

(defn- parse-clock-line
  "Parse 'CLOCK: timestamp'. Returns token or nil."
  [^String line]
  (when (starts-with-ic? line "CLOCK: ")
    [tag-clock (inline/parse (subs line 7))]))

(defn- parse-table-line
  "Parse '| cell | cell |' or '|---+---|'. Returns token or nil."
  [^String line]
  (when (and (pos? (.length line))
             (= (.charAt line 0) \|))
    (let [trimmed (str/trim line)
          ;; Check if it's a separator (contains only |, -, +)
          is-separator? (every? #(or (= % \|) (= % \-) (= % \+)) trimmed)]
      (if is-separator?
        ;; Table separator - split on | or + and parse dash sequences
        (let [content (if (str/ends-with? trimmed "|")
                        (subs trimmed 1 (dec (.length trimmed)))
                        (subs trimmed 1))
              cells (str/split content #"[|+]" -1)
              columns (mapv (fn [cell]
                              [tag-column (seq cell)])
                            cells)]
          [tag-table-separator columns])
        ;; Table row - preserve cell content including spaces
        (let [content (if (str/ends-with? trimmed "|")
                        (subs trimmed 1 (dec (.length trimmed)))
                        (subs trimmed 1))
              cells (str/split content #"\|" -1)
              columns (mapv (fn [cell]
                              [tag-column (inline/parse cell)])
                            cells)]
          [tag-table-row columns])))))

(defn- parse-checkbox
  "Parse '[ ]', '[X]', '[x]', '[-]' at position. Returns [state remaining] or nil."
  [^String s ^long idx]
  (let [len (.length s)]
    (when (and (<= (+ idx 4) len)
               (= (.charAt s idx) \[)
               (= (.charAt s (+ idx 2)) \])
               (= (.charAt s (+ idx 3)) \space))
      (let [state (.charAt s (+ idx 1))]
        (when (or (= state \space) (= state \X) (= state \x) (= state \-))
          [state (+ idx 4)])))))

(defn- parse-list-item
  "Parse list item: '- item', '+ item', '* item', '1. item', '1) item'.
   Returns token or nil."
  [^String line]
  (let [indent (count-leading-spaces line)
        rest-line (subs line indent)]
    (when (pos? (.length rest-line))
      (let [first-char (.charAt rest-line 0)]
        (cond
          ;; Unordered: -, +, * followed by space
          (and (or (= first-char \-) (= first-char \+) (= first-char \*))
               (> (.length rest-line) 1)
               (= (.charAt rest-line 1) \space))
          (let [content-start 2
                [checkbox content-idx] (or (parse-checkbox rest-line content-start)
                                           [nil content-start])
                content (subs rest-line content-idx)]
            [tag-list {:indent indent
                       :marker [:unordered first-char]
                       :checkbox checkbox
                       :tokens (inline/parse content)}])

          ;; Ordered: digit(s) followed by . or ) and space
          (Character/isDigit first-char)
          (let [len (.length rest-line)]
            (loop [i 1]
              (if (>= i len)
                nil
                (let [ch (.charAt rest-line i)]
                  (cond
                    (Character/isDigit ch) (recur (inc i))
                    (and (or (= ch \.) (= ch \)))
                         (< (inc i) len)
                         (= (.charAt rest-line (inc i)) \space))
                    (let [marker (subs rest-line 0 i)
                          content-start (+ i 2)
                          [checkbox content-idx] (or (parse-checkbox rest-line content-start)
                                                     [nil content-start])
                          content (subs rest-line content-idx)]
                      [tag-list {:indent indent
                                 :marker [:ordered {:marker (seq marker)
                                                    :delimiter ch}]
                                 :checkbox checkbox
                                 :tokens (inline/parse content)}])
                    :else nil)))))

          :else nil)))))

(defn- parse-text-line
  "Parse as plain text line with inline content."
  [^String line]
  (inline/parse line))

;; Greater Block Handling ------------------------------------------------------

(def ^:private raw-content-blocks
  "Blocks that should preserve raw content without parsing inline elements."
  #{"verse" "comment" "example" "export" "src"})

(defn- collect-greater-block
  "Collect lines until #+END_type. Returns [block-token remaining-lines]."
  [block-type args-str lines]
  (let [end-marker (str "#+END_" block-type)
        ;; Parse args as inline tokens
        parsed-args (when (seq args-str) (inline/parse args-str))]
    (loop [content-lines []
           [line & rest-lines] lines]
      (cond
        (nil? line)
        ;; Unterminated block - return what we have
        (let [content (str/join "\n" content-lines)]
          [[tag-block {:type block-type
                       :begin (str "#+BEGIN_" (str/upper-case block-type))
                       :end nil
                       :args parsed-args
                       :content content}]
           nil])

        (starts-with-ic? (str/trim line) end-marker)
        ;; Found end
        (let [content (str/join "\n" content-lines)
              raw? (contains? raw-content-blocks block-type)
              parsed-content (if raw?
                               content
                               (when (seq content) (inline/parse content)))]
          [[tag-block {:type block-type
                       :begin (str "#+BEGIN_" (str/upper-case block-type))
                       :end (str/trim line)
                       :args parsed-args
                       :content parsed-content}]
           rest-lines])

        :else
        (recur (conj content-lines line) rest-lines)))))

;; Main Parser -----------------------------------------------------------------

(defn- try-parse-line
  "Try various parsers on a line. Returns [token remaining-lines] or nil."
  [^String line rest-lines]
  (or
   ;; Empty line
   (when (str/blank? line)
     [[tag-newlines 1] rest-lines])

   ;; Greater block start
   (when-let [[block-type args] (parse-greater-block-start line)]
     (collect-greater-block block-type args rest-lines))

   ;; Comment line (# ...)
   (when-let [token (parse-comment-line line)]
     [token rest-lines])

   ;; Keyword line (#+KEY: value)
   (when-let [token (parse-keyword-line line)]
     [token rest-lines])

   ;; Drawer/Property (:NAME: or :KEY: value)
   (when-let [token (parse-drawer-or-property line)]
     [token rest-lines])

   ;; Planning lines
   (when-let [token (parse-planning-line line)]
     [token rest-lines])

   ;; Clock line
   (when-let [token (parse-clock-line line)]
     [token rest-lines])

   ;; Horizontal rule (-----)
   (when-let [token (parse-horizontal-rule line)]
     [token rest-lines])

   ;; Table line
   (when-let [token (parse-table-line line)]
     [token rest-lines])

   ;; List item
   (when-let [token (parse-list-item line)]
     [token rest-lines])))

(defn parse
  "Parse block content into tokens. Returns a vector of tokens."
  [^String s]
  (when (and s (pos? (.length s)))
    (let [lines (str/split-lines s)]
      (loop [[line & rest-lines] lines
             acc (transient [])]
        (if (nil? line)
          (persistent! acc)
          (if-let [[token remaining] (try-parse-line line rest-lines)]
            (recur remaining (conj! acc token))
            ;; Plain text (fallback)
            (recur rest-lines (conj! acc [tag-text (parse-text-line line)]))))))))
