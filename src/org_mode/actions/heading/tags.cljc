(ns org-mode.actions.heading.tags
  "Tag manipulation actions for headings."
  (:require
   [clojure.string :as str]
   [org-mode.computed.heading.core :as c.heading]))

(defn- tags->token
  "Convert a seq of tag strings to the org-mode tag token string.
   e.g. [\"foo\" \"bar\"] -> \":foo:bar:\""
  [tags]
  (str ":" (str/join ":" tags) ":"))

(defn- has-tag-token?
  "Check if the title ends with a tag token."
  [heading]
  (some? (c.heading/tags heading)))

(defn- title-without-tag-token
  "Return title tokens with the trailing tag token (and preceding space) removed."
  [heading]
  (let [title (:title heading)]
    (if (has-tag-token? heading)
      ;; Remove last token (tag) and preceding space if present
      (let [without-last (vec (butlast title))]
        (if (and (seq without-last)
                 (= " " (last without-last)))
          (vec (butlast without-last))
          without-last))
      title)))

(defn set-tags
  "Set all tags on a heading. Pass empty vector to remove all tags."
  [tags heading]
  (let [base-title (title-without-tag-token heading)]
    (if (seq tags)
      (assoc heading :title (conj (conj base-title " ") (tags->token tags)))
      (assoc heading :title base-title))))

(defn add-tag
  "Add a tag to the heading. No-op if the tag already exists."
  [tag heading]
  (let [current-tags (or (c.heading/tags heading) [])]
    (if (some #(= % tag) current-tags)
      heading
      (set-tags (conj current-tags tag) heading))))

(defn remove-tag
  "Remove a tag from the heading."
  [tag heading]
  (let [current-tags (or (c.heading/tags heading) [])]
    (set-tags (vec (remove #(= % tag) current-tags)) heading)))

(defn toggle-tag
  "Add if absent, remove if present."
  [tag heading]
  (let [current-tags (or (c.heading/tags heading) [])]
    (if (some #(= % tag) current-tags)
      (remove-tag tag heading)
      (add-tag tag heading))))

(defn remove-all-tags
  "Remove all tags from the heading."
  [heading]
  (set-tags [] heading))
