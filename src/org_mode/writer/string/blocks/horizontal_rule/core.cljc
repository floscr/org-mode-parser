(ns org-mode.writer.string.blocks.horizontal-rule.core)

(defn token->string [[_ n]]
  (str (apply str (repeat n "-")) "\n"))

