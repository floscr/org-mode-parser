(ns org-mode.computed.heading.links
  "Link extraction from org-mode headings.

   Works with the inline link tokens produced by the parser:
     [:link \"url\"]
     [:link-with-title {:link \"url\" :title \"text\"}]"
  (:require
   [org-mode.parser.blocks.core :as blocks]
   [org-mode.parser.inline.tags :as inline-tags]))

;; Token predicates -----------------------------------------------------------

(defn link-token?
  "Returns true if the inline token is a link (plain or with title)."
  [token]
  (boolean
   (and (vector? token)
        (#{inline-tags/link inline-tags/link-with-title} (first token)))))

;; Link accessors -------------------------------------------------------------

(defn link-href
  "Return the URL/target of a link token."
  [token]
  (let [[tag content] token]
    (case tag
      :link content
      :link-with-title (:link content)
      nil)))

(defn link-title
  "Return the display title of a link token, falling back to the href."
  [token]
  (let [[tag content] token]
    (case tag
      :link content
      :link-with-title (or (:title content) (:link content))
      nil)))

;; Extraction -----------------------------------------------------------------

(defn title-links
  "Extract all link tokens from the heading title."
  [heading]
  (filterv link-token? (:title heading)))

(defn body-links
  "Extract all link tokens from the heading body.
   Searches through :text blocks which contain inline tokens."
  [heading]
  (reduce
   (fn [acc block-token]
     (case (first block-token)
       :text
       (into acc (filter link-token? (second block-token)))

       ;; Also check list items for links
       :list
       (let [{:keys [tokens]} (second block-token)]
         (into acc (filter link-token? tokens)))

       ;; default
       acc))
   [] (:body heading)))

(defn heading-links
  "Extract all link tokens from both title and body of a heading."
  [heading]
  (into (title-links heading) (body-links heading)))
