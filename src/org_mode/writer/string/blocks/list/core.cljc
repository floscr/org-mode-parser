(ns org-mode.writer.string.blocks.list.core
  (:require
   [org-mode.writer.string.inline.core :as inline]))

(defn- write-list-marker [[marker-type payload]]
  (case marker-type
    :unordered (str payload " ")
    :ordered (let [{:keys [marker delimiter]} payload
                   m (apply str marker)]
               (str m delimiter " "))
    (str payload)))

(defn token->string [[_ {:keys [indent marker checkbox tokens]}]]
  (str (apply str (repeat indent " "))
       (write-list-marker marker)
       (when checkbox (str "[" (char checkbox) "] "))
       (when (seq tokens) (inline/tokens->string tokens))
       "\n"))
