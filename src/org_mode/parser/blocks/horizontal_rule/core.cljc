(ns org-mode.parser.blocks.horizontal-rule.core
  (:require
   [org-mode.parser.blocks.horizontal-rule.tags :as tags]
   [org-mode.parser.tokenizer.core :refer [token]]
   [org-mode.parser.utils.core :refer [end]]
   [strojure.parsesso.char :as char]
   [strojure.parsesso.parser :as p]))

(def horizontal-rule
  "Parse horizontal rule: 5 or more dashes (-----)"
  (p/for [_ (p/word "-----")
          dashes (p/*many-till (char/is "-") end)]
    (p/result
     (token tags/horizontal-rule (+ 5 (count dashes))))))

(def parser
  (p/maybe horizontal-rule))
