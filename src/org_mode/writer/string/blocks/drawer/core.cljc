(ns org-mode.writer.string.blocks.drawer.core
  (:require
   [org-mode.writer.string.inline.core :as inline]))

(defn token->string [[tag payload]]
  (case tag
    :property (let [{:keys [key value]} payload]
                ;; Preserve original spacing in value by not forcing an extra space
                (str ":" key ":" (inline/tokens->string value) "\n"))
    :drawer-start (str ":" payload ":\n")
    :drawer-end (str ":" payload ":\n")
    (str tag payload)))
