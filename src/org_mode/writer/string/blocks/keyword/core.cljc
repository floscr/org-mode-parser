(ns org-mode.writer.string.blocks.keyword.core)

(defn token->string [[_ {:keys [key value]}]]
  (str "#+" key ": " value "\n"))

