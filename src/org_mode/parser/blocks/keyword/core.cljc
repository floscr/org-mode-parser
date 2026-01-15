(ns org-mode.parser.blocks.keyword.core
  (:require
   [clojure.string :as str]
   [org-mode.parser.blocks.keyword.tags :as tags]
   [org-mode.parser.inline.core :as inline]
   [org-mode.parser.tokenizer.core :refer [token]]
   [org-mode.parser.utils.core :refer [end]]
   [strojure.parsesso.char :as char]
   [strojure.parsesso.parser :as p]
   [strojure.parsesso.unicode :as unicode]))

(def keyword-line
  "#+KEYWORD: value
  Parse org-mode keyword lines like #+TITLE:, #+AUTHOR:, etc."
  (p/for [_ (p/word "#+" :ic)
          keyword-name (p/+many (p/alt unicode/letter-or-digit? (char/is "_") (char/is "-")))
          _ (char/is ":")
          _ (p/*many unicode/space?)
          value inline/raw-parser]
    (p/result
     (token tags/keyword-line {:key (str/upper-case (char/str* keyword-name))
                               :value (str/trim (char/str* value))}))))

(def parser
  (p/maybe keyword-line))
