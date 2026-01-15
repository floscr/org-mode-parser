(ns org-mode.parser.blocks.keyword-line
  (:require
   [org-mode.parser.inline.core :as inline]
   [org-mode.parser.tokenizer.core :refer [token]]
   [strojure.parsesso.parser :as p]))

(defn parser
  "Generic parser for keyword lines: KEYWORD: inline-content
  Returns a token with the given tag and inline-parsed content"
  [keyword-str tag]
  (p/for [_ (p/word (str keyword-str ": ") :ic)
          content inline/parser]
    (p/result (token tag content))))
