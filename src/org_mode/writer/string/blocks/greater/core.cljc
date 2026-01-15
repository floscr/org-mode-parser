(ns org-mode.writer.string.blocks.greater.core
  (:require
   [org-mode.writer.string.inline.core :as inline]))

(defn token->string [[_ {:keys [begin end args content]}]]
  (let [args-str (apply str (remove nil? args))
        content-str (if (string? content)
                      content
                      (inline/tokens->string content))]
    (str begin args-str "\n" content-str "\n" end "\n")))

