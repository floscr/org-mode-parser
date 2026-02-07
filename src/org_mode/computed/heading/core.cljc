(ns org-mode.computed.heading.core
  "Computed properties for org-mode headings.

   Headings from the parser have the shape:
     {:level n :title <inline-tokens> :body <block-tokens> :trailing-blank? bool}

   This module provides derived accessors: TODO state, tags, plain-text
   conversions, search, and tag aggregation."
  (:require
   [clojure.string :as str]
   [org-mode.writer.string.core :as w.string]
   [org-mode.writer.string.inline.core :as w.inline]))

;; Default TODO keywords ------------------------------------------------------

(def default-todo-keywords
  "Default set of org-mode TODO keywords."
  #{"TODO" "DONE"})

;; Basic accessors ------------------------------------------------------------

(defn title
  "Return the inline tokens of the heading title."
  [heading]
  (:title heading))

(defn body
  "Return the block tokens of the heading body."
  [heading]
  (:body heading))

(defn level
  "Return the heading level (number of stars)."
  [heading]
  (:level heading))

;; TODO extraction ------------------------------------------------------------

(defn todo
  "Extract the TODO keyword from the heading title.
   Returns the keyword string (e.g. \"TODO\", \"DONE\") or nil.

   `todo-keywords` defaults to `default-todo-keywords`."
  [heading & {:keys [todo-keywords]
              :or {todo-keywords default-todo-keywords}}]
  (let [tokens (title heading)]
    (when (seq tokens)
      (let [first-token (first tokens)]
        (when (and (string? first-token)
                   (contains? todo-keywords first-token))
          first-token)))))

(defn todo?
  "Returns true if the heading has a TODO keyword."
  [heading & {:as opts}]
  (some? (todo heading opts)))

(defn todo-eq?
  "Returns true if the heading's TODO keyword equals `kw`."
  [kw heading & {:as opts}]
  (= (todo heading opts) kw))

(defn done?
  "Returns true if the heading's TODO keyword is \"DONE\"."
  [heading & {:as opts}]
  (todo-eq? "DONE" heading opts))

;; Tags extraction ------------------------------------------------------------

(defn tags
  "Extract tags from the heading title.
   Tags appear at the end of the title line as \":tag1:tag2:\".
   Returns a vector of tag strings, or nil if none."
  [heading]
  (let [tokens (title heading)]
    (when (seq tokens)
      (let [last-token (last tokens)]
        (when (and (string? last-token)
                   (str/starts-with? last-token ":")
                   (str/ends-with? last-token ":"))
          (let [inner (subs last-token 1 (dec (count last-token)))]
            (when (seq inner)
              (vec (str/split inner #":")))))))))

(defn heading-tags
  "Aggregate tag counts across multiple headings.
   Returns a map of {tag-string count}."
  [headings]
  (reduce
   (fn [acc heading]
     (reduce
      (fn [acc tag]
        (update acc tag (fnil inc 0)))
      acc (or (tags heading) [])))
   {} headings))

(defn normalize-tag
  "Normalize a tag string: uppercase, replace delimiter characters with _."
  [tag]
  (when-let [trimmed (not-empty (str/trim (or tag "")))]
    (-> trimmed
        (str/upper-case)
        (str/replace #"[: ]" "_")
        (str/replace #"_+" "_"))))

;; Text conversion ------------------------------------------------------------

(defn title-str
  "Return the heading title as a plain string."
  [heading]
  (w.inline/tokens->string (or (title heading) [])))

(defn body-str
  "Return the heading body as a plain string."
  [heading]
  (w.string/blocks->string (or (body heading) [])))

(defn title-stripped-str
  "Return the heading title as plain text with all inline markup stripped."
  [heading]
  (let [tokens (or (title heading) [])]
    (apply str
           (map (fn [token]
                  (if (string? token)
                    token
                    ;; For markup tokens like [:bold "text"], extract the content
                    (let [[_ content] token]
                      (if (string? content) content ""))))
                tokens))))

;; Search ---------------------------------------------------------------------

(defn raw-text-str
  "Return the raw text content (title + body) for full-text searching."
  [heading]
  (str (title-str heading) " " (body-str heading)))

(defn matches-query?
  "Returns true if the heading matches the search query (case-insensitive)."
  [query heading]
  (if (str/blank? query)
    true
    (let [raw-text (str/lower-case (raw-text-str heading))
          search-query (str/lower-case (str/trim query))]
      (str/includes? raw-text search-query))))
