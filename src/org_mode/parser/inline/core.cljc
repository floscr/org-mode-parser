(ns org-mode.parser.inline.core
  "Inline parser for org-mode text formatting.
   Parses bold, italic, links, timestamps, footnotes, etc."
  (:require
   [clojure.string :as str]
   [org-mode.parser.inline.tags :as tags]))

(defn- find-closing
  "Find closing delimiter starting from idx. Returns index of closing char or -1."
  [^String s ^long idx ^char close-char ^long len]
  (loop [i idx]
    (if (>= i len)
      -1
      (if (= (.charAt s i) close-char)
        i
        (recur (inc i))))))

(defn- find-closing-str
  "Find closing string delimiter starting from idx. Returns index or -1."
  [^String s ^long idx ^String close-str ^long len]
  (let [close-idx (.indexOf s close-str idx)]
    (if (or (< close-idx 0) (>= close-idx len))
      -1
      close-idx)))

(defn- parse-styled
  "Parse styled text like *bold*, /italic/, etc.
   Returns [token next-idx] or nil if not a valid styled span."
  [^String s ^long idx ^char delim tag ^long len]
  (when (and (< (inc idx) len)
             (not= (.charAt s (inc idx)) \space))
    (let [close-idx (find-closing s (inc idx) delim len)]
      (when (and (pos? close-idx)
                 (> close-idx (inc idx))  ;; Must have non-empty content
                 (not= (.charAt s (dec close-idx)) \space))
        (let [content (subs s (inc idx) close-idx)]
          [[tag content] (inc close-idx)])))))

(defn- parse-link
  "Parse [[link]] or [[link][title]]. Returns [token next-idx] or nil."
  [^String s ^long idx ^long len]
  (when (and (< (+ idx 3) len)
             (= (.charAt s idx) \[)
             (= (.charAt s (inc idx)) \[))
    (let [close-idx (find-closing-str s (+ idx 2) "]]" len)]
      (when (pos? close-idx)
        (let [content (subs s (+ idx 2) close-idx)
              sep-idx (.indexOf content "][")]
          (if (pos? sep-idx)
            [[tags/link-with-title {:link (subs content 0 sep-idx)
                                    :title (subs content (+ sep-idx 2))}]
             (+ close-idx 2)]
            [[tags/link content] (+ close-idx 2)]))))))

(defn- parse-target
  "Parse <<target>>. Returns [token next-idx] or nil."
  [^String s ^long idx ^long len]
  (when (and (< (+ idx 3) len)
             (= (.charAt s idx) \<)
             (= (.charAt s (inc idx)) \<))
    (let [close-idx (find-closing-str s (+ idx 2) ">>" len)]
      (when (pos? close-idx)
        [[tags/target (subs s (+ idx 2) close-idx)] (+ close-idx 2)]))))

(defn- parse-timestamp
  "Parse <timestamp> or <ts>--<ts> range. Returns [token next-idx] or nil."
  [^String s ^long idx ^long len]
  (when (and (< (inc idx) len)
             (= (.charAt s idx) \<))
    (let [close-idx (find-closing s (inc idx) \> len)]
      (when (pos? close-idx)
        (let [content (subs s (inc idx) close-idx)
              next-idx (inc close-idx)]
          ;; Check for range: >--<
          (if (and (< (+ next-idx 3) len)
                   (= (.charAt s next-idx) \-)
                   (= (.charAt s (inc next-idx)) \-)
                   (= (.charAt s (+ next-idx 2)) \<))
            (let [second-close (find-closing s (+ next-idx 3) \> len)]
              (if (pos? second-close)
                [[tags/timestamp-range {:start content
                                        :end (subs s (+ next-idx 3) second-close)}]
                 (inc second-close)]
                [[tags/timestamp content] next-idx]))
            [[tags/timestamp content] next-idx]))))))

(defn- parse-inactive-timestamp
  "Parse [timestamp] (inactive). Returns [token next-idx] or nil.
   Must not be a link, footnote, or stats cookie."
  [^String s ^long idx ^long len]
  (when (and (< (inc idx) len)
             (= (.charAt s idx) \[)
             ;; Not a link [[
             (not= (.charAt s (inc idx)) \[)
             ;; Not a footnote [fn:
             (not (and (< (+ idx 3) len)
                       (= (.charAt s (inc idx)) \f)
                       (= (.charAt s (+ idx 2)) \n)
                       (= (.charAt s (+ idx 3)) \:))))
    (let [close-idx (find-closing s (inc idx) \] len)]
      (when (pos? close-idx)
        (let [content (subs s (inc idx) close-idx)]
          ;; Check if it's a stats cookie [n/m] or [n%]
          (cond
            ;; Stats percent [33%] or [%]
            (str/ends-with? content "%")
            (let [num-str (subs content 0 (dec (count content)))
                  n (when (seq num-str) (parse-long num-str))]
              [[tags/stats-percent-cookie n] (inc close-idx)])

            ;; Stats range [2/5]
            (str/includes? content "/")
            (let [[from-str to-str] (str/split content #"/" 2)]
              (when (and from-str to-str)
                [[tags/stats-range-cookie {:from (parse-long from-str)
                                           :to (parse-long to-str)}]
                 (inc close-idx)]))

            ;; Inactive timestamp - must look like a date
            (or (str/includes? content "-")
                (re-matches #"\d{4}.*" content))
            (let [next-idx (inc close-idx)]
              ;; Check for range: ]--[
              (if (and (< (+ next-idx 3) len)
                       (= (.charAt s next-idx) \-)
                       (= (.charAt s (inc next-idx)) \-)
                       (= (.charAt s (+ next-idx 2)) \[))
                (let [second-close (find-closing s (+ next-idx 3) \] len)]
                  (if (pos? second-close)
                    [[tags/inactive-timestamp-range
                      {:start content
                       :end (subs s (+ next-idx 3) second-close)}]
                     (inc second-close)]
                    [[tags/inactive-timestamp content] next-idx]))
                [[tags/inactive-timestamp content] next-idx]))

            :else nil))))))

(defn- parse-footnote
  "Parse [fn:name] or [fn:name:definition]. Returns [token next-idx] or nil."
  [^String s ^long idx ^long len]
  (when (and (< (+ idx 4) len)
             (= (.charAt s idx) \[)
             (= (.charAt s (inc idx)) \f)
             (= (.charAt s (+ idx 2)) \n)
             (= (.charAt s (+ idx 3)) \:))
    (let [close-idx (find-closing s (+ idx 4) \] len)]
      (when (pos? close-idx)
        (let [content (subs s (+ idx 4) close-idx)
              sep-idx (.indexOf content ":")]
          (if (pos? sep-idx)
            [[tags/footnote-def {:name (subs content 0 sep-idx)
                                 :definition (subs content (inc sep-idx))}]
             (inc close-idx)]
            [[tags/footnote-ref content] (inc close-idx)]))))))

(defn- parse-macro
  "Parse {{{macro(args)}}}. Returns [token next-idx] or nil."
  [^String s ^long idx ^long len]
  (when (and (< (+ idx 5) len)
             (= (.charAt s idx) \{)
             (= (.charAt s (inc idx)) \{)
             (= (.charAt s (+ idx 2)) \{))
    (let [close-idx (find-closing-str s (+ idx 3) "}}}" len)]
      (when (pos? close-idx)
        (let [content (subs s (+ idx 3) close-idx)
              paren-idx (.indexOf content "(")]
          (if (and (pos? paren-idx)
                   (str/ends-with? content ")"))
            (let [name (subs content 0 paren-idx)
                  args-str (subs content (inc paren-idx) (dec (count content)))
                  args (when (not (str/blank? args-str))
                         (mapv str/trim (str/split args-str #",")))]
              [[tags/macro {:name name :args args}] (+ close-idx 3)])
            [[tags/macro {:name content}] (+ close-idx 3)]))))))

(defn- special-char?
  "Check if character is a special inline markup character."
  [ch]
  (or (= ch \[) (= ch \<) (= ch \{)
      (= ch \*) (= ch \/) (= ch \_)
      (= ch \=) (= ch \~) (= ch \+)))

(defn- collect-word
  "Collect characters until whitespace or special char. Returns [word next-idx]."
  [^String s ^long idx ^long len]
  (loop [i idx]
    (if (>= i len)
      [(subs s idx i) i]
      (let [ch (.charAt s i)]
        (if (or (= ch \space) (= ch \tab) (= ch \newline)
                ;; Stop at special chars only if not at start position
                (and (> i idx) (special-char? ch)))
          [(subs s idx i) i]
          (recur (inc i)))))))

(defn- collect-spaces
  "Collect whitespace characters. Returns [spaces next-idx]."
  [^String s ^long idx ^long len]
  (loop [i idx]
    (if (>= i len)
      [(subs s idx i) i]
      (let [ch (.charAt s i)]
        (if (or (= ch \space) (= ch \tab))
          (recur (inc i))
          [(subs s idx i) i])))))

(defn- word-boundary?
  "Check if position is at a word boundary (start of string or after whitespace)."
  [^String s ^long idx]
  (or (zero? idx)
      (let [prev-ch (.charAt s (dec idx))]
        (or (= prev-ch \space) (= prev-ch \tab) (= prev-ch \newline)))))

(defn parse
  "Parse inline content into tokens. Returns a vector of tokens."
  [^String s]
  (let [len (.length s)]
    (loop [idx 0
           acc (transient [])]
      (if (>= idx len)
        (persistent! acc)
        (let [ch (.charAt s idx)
              result
              (case ch
                \[ (or (parse-link s idx len)
                       ;; Footnotes require word boundary before [
                       (when (word-boundary? s idx)
                         (parse-footnote s idx len))
                       (parse-inactive-timestamp s idx len))
                \< (or (parse-target s idx len)
                       (parse-timestamp s idx len))
                \{ (parse-macro s idx len)
                \* (parse-styled s idx \* tags/bold len)
                \/ (parse-styled s idx \/ tags/italic len)
                \_ (parse-styled s idx \_ tags/underline len)
                \= (parse-styled s idx \= tags/verbatim len)
                \~ (parse-styled s idx \~ tags/code len)
                \+ (parse-styled s idx \+ tags/strike-through len)
                (\space \tab) :space
                nil)]
          (cond
            ;; Matched a token
            (vector? result)
            (let [[token next-idx] result]
              (recur (long next-idx) (conj! acc token)))

            ;; Whitespace
            (= result :space)
            (let [[spaces next-idx] (collect-spaces s idx len)]
              (recur (long next-idx) (conj! acc spaces)))

            ;; No match - collect as word
            :else
            (let [[word next-idx] (collect-word s idx len)]
              (if (pos? (count word))
                (recur (long next-idx) (conj! acc word))
                ;; Single unmatched char
                (recur (inc idx) (conj! acc (subs s idx (inc idx))))))))))))
