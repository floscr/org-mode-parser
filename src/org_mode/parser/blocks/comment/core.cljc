(ns org-mode.parser.blocks.comment.core
  (:require
   [org-mode.parser.blocks.comment.tags :as tags]
   [org-mode.parser.inline.core :as inline]
   [org-mode.parser.tokenizer.core :refer [token]]
   [strojure.parsesso.parser :as p]))

(def comment-line
  (p/for [_ (p/word "# ")
          comment-text inline/raw-parser]
    (p/result
     (token tags/comment-line comment-text))))

(def parser
  (p/maybe comment-line))
