(ns org-mode.writer.string.core
  (:require
   [org-mode.writer.string.blocks.core :as blocks]
   [org-mode.writer.engine :as engine]
   [org-mode.writer.string.inline.core :as inline]))

(def inline->string inline/tokens->string)

(defn blocks->string
  "Render the result of org-mode parser blocks.core/parse back to text.
  Accepts a vector where each element is either a block token vector
  or a vector of inline tokens representing a plain line."
  [parsed]
  (engine/render-blocks {:tokens parsed
                         :inline-renderer (inline/renderer)
                         :handlers blocks/default-block-writers
                         :default-handler (fn [_ tok] (str tok))
                         :line-handler (fn [{:keys [render-inline]} line]
                                         (str (render-inline line) "\n"))

                         :init ""
                         :step (fn [acc node] (str acc node))}))

(defn- make-headline-line [{:keys [level title]}]
  (let [stars (apply str (repeat level "*"))
        inline-renderer (inline/renderer)
        title-str (inline-renderer title)]
    (str stars " " title-str "\n")))

(defn document->string
  "Render a full document map {:body :headers} back to text.
  Because the chunker splits on a newline before each heading, we reinsert
  a single newline before every heading (except when the document starts
  with a heading)."
  [{:keys [body headers]}]
  (let [body-str (blocks->string body)]
    (reduce
     (fn [acc {:keys [body] :as h}]
       (str acc "\n"
            (make-headline-line h)
            (blocks->string body)))
     (or body-str "") headers)))
