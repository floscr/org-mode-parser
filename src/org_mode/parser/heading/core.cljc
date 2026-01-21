(ns org-mode.parser.heading.core
  "Document parser for org-mode files.
   Splits documents by headings and parses each section."
  (:require
   [org-mode.chunker.core :as chunker]
   [org-mode.parser.blocks.core :as blocks]
   [org-mode.parser.inline.core :as inline]))

(defn- heading-line-info
  "Returns {:level n :title-str \"...\"} when the line is a valid heading."
  [^String line]
  (when line
    (let [len (.length line)]
      (loop [idx 0]
        (when (< idx len)
          (let [ch (.charAt line idx)]
            (cond
              (= ch \*) (recur (inc idx))
              (and (> idx 0)
                   (or (= ch \space) (= ch \tab)))
              {:level idx
               :title-str (subs line (inc idx))}
              :else nil)))))))

(defn- heading-line? [line]
  (boolean (heading-line-info line)))

(defn- parse-heading-line [line]
  (let [{:keys [level title-str] :as info} (heading-line-info line)]
    (when-not info
      (throw (ex-info "Invalid heading line" {:line line})))
    {:level level
     :title (inline/parse (or title-str ""))}))

(defn- ends-with-blank-line?
  "Check if string ends with a blank line (two or more newlines at end)."
  [^String s]
  (let [len (.length s)]
    (and (>= len 2)
         (= (.charAt s (dec len)) \newline)
         (= (.charAt s (- len 2)) \newline))))

(defn- chunk->heading
  "Takes a chunk and turns it into a heading map."
  [^String chunk]
  (let [nl-idx (.indexOf chunk "\n")
        [headline-line body-str]
        (if (neg? nl-idx)
          [chunk ""]
          [(subs chunk 0 nl-idx) (subs chunk (inc nl-idx))])
        {:keys [level title]} (parse-heading-line headline-line)
        body (blocks/parse body-str)
        ;; Track if there's a blank line after this section (before next heading)
        trailing-blank? (ends-with-blank-line? chunk)]
    {:level level
     :title title
     :body body
     :trailing-blank? trailing-blank?}))

(defn parse-document
  "Parse a full org-mode document into {:body ... :headers [...]}.

   The body contains any content before the first heading.
   Headers is a flat vector of heading maps, each with :level, :title, and :body.
   Both body and headers track :trailing-blank? to preserve blank lines."
  [s & {:keys [parallel?]}]
  (let [chunks (let [c (chunker/chunk-headings (or s ""))]
                 (if (seq c) c [""]))
        first-chunk (first chunks)
        starts-with-heading? (heading-line? first-chunk)
        [body-chunk header-chunks]
        (if starts-with-heading?
          ["" chunks]
          [(or first-chunk "") (rest chunks)])
        body (blocks/parse body-chunk)
        ;; Track if body ends with blank line (before first heading)
        body-trailing-blank? (ends-with-blank-line? (str body-chunk))
        headers #?(:clj (if (and parallel? (> (count header-chunks) 1))
                          (into [] (pmap chunk->heading header-chunks))
                          (mapv chunk->heading header-chunks))
                   :cljs (mapv chunk->heading header-chunks))]
    {:body body
     :body-trailing-blank? body-trailing-blank?
     :headers headers}))
