(ns org-mode.writer.hiccup.core
  (:require
   [hiccup2.core :as h]
   [org-mode.writer.hiccup.blocks.core :as blocks]
   [org-mode.writer.hiccup.inline.core :as inline]))

(defn inline->hiccup
  "Render inline tokens to hiccup. Optional opts map accepts :writers overrides."
  ([tokens] (inline->hiccup tokens {}))
  ([tokens opts]
   (inline/tokens->hiccup tokens opts)))

(defn blocks->hiccup
  "Render block tokens to hiccup. Supports :block-writers, :inline-writers,
  and :conjoin? overrides."
  ([tokens] (blocks->hiccup tokens {}))
  ([tokens opts]
   (blocks/tokens->hiccup (or tokens []) opts)))

(defn- heading->hiccup [opts {:keys [level title body]}]
  (let [heading-tag (keyword (str "h" (-> level (max 1) (min 6))))
        title-nodes (inline->hiccup title (select-keys opts [:inline-writers]))
        body-nodes (blocks->hiccup body opts)
        section (into [:section {:data-level level}]
                      [(into [heading-tag] title-nodes)])]
    (into section body-nodes)))

(defn document->hiccup
  "Render a full document map {:body .. :headers [...] } to hiccup.
  Accepts the same opts as `blocks->hiccup`."
  ([doc] (document->hiccup doc {}))
  ([{:keys [body headers]} opts]
   (let [body-nodes (blocks->hiccup body opts)
         header-nodes (map #(heading->hiccup opts %) (or headers []))]
     (into [:article] (concat body-nodes header-nodes)))))

(defn document->html
  ([doc] (document->html doc {}))
  ([doc opts]
   (-> (document->hiccup doc opts)
       (h/html))))
