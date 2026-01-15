
(ns org-mode.parser.heading.core
  (:require
   [org-mode.chunker.core :as chunker]
   [org-mode.parser.blocks.core :as blocks]
   [org-mode.parser.inline.core :as inline]))

(defn- heading-line-info
  "Returns {:level n :title-str \"...\"} when the line is a valid heading."
  [line]
  (when line
    (let [^String line line
          len (.length line)]
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

(defn- chunk->heading
  "Takes a chunk returned by chunker/chunk-headings and turns it into a heading map."
  [chunk]
  (let [nl-idx (.indexOf ^String chunk "\n")
        [headline-line body-str]
        (if (neg? nl-idx)
          [chunk ""]
          [(subs chunk 0 nl-idx) (subs chunk (inc nl-idx))])
        {:keys [level title]} (parse-heading-line headline-line)
        body (blocks/parse body-str)]
    {:level level
     :title title
     :body body}))

(defn- parse-header-chunks
  [header-chunks parallel?]
  #?(:clj (if (and parallel? (> (count header-chunks) 1))
            (into [] (pmap chunk->heading header-chunks))
            (mapv chunk->heading header-chunks))
     :cljs (mapv chunk->heading header-chunks)))

(defn parse-document
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
        headers (parse-header-chunks header-chunks parallel?)]
    {:body body
     :headers headers}))
