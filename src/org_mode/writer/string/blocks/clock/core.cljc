(ns org-mode.writer.string.blocks.clock.core
  (:require
   [org-mode.writer.string.inline.core :as inline]))

(defn token->string [[_ tokens]]
  (str "CLOCK: " (inline/tokens->string tokens) "\n"))

