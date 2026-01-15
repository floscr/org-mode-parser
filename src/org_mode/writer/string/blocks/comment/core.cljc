(ns org-mode.writer.string.blocks.comment.core
  (:require
   [org-mode.writer.string.inline.core :as inline]))

(defn token->string [[_ raw]]
  (str "# " (apply str raw) "\n"))

