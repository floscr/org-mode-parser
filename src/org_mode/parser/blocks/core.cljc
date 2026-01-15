(ns org-mode.parser.blocks.core
  (:require
   [org-mode.parser.blocks.clock.core :as clock]
   [org-mode.parser.blocks.comment.core :as comment]
   [org-mode.parser.blocks.drawer.core :as drawer]
   [org-mode.parser.blocks.greater.core :as greater]
   [org-mode.parser.blocks.horizontal-rule.core :as horizontal-rule]
   [org-mode.parser.blocks.keyword.core :as keyword]
   [org-mode.parser.blocks.list.core :as list]
   [org-mode.parser.blocks.planning.core :as planning]
   [org-mode.parser.blocks.table.core :as table]
   [org-mode.parser.blocks.tags :as tags]
   [org-mode.parser.inline.core :as inline]
   [org-mode.parser.tokenizer.core :refer [token]]
   [strojure.parsesso.char :as char]
   [strojure.parsesso.parser :as p]))

(def newlines
  (-> (p/+many char/newline)
      (p/value #(token tags/newlines (count %)))))

(defn- inline->text-token [tokens]
  (token tags/text tokens))

(def text-block
  (p/value inline/parser inline->text-token))

(def block
  (p/alt
   (p/maybe newlines)
   (p/maybe comment/parser)
   (p/maybe keyword/parser)
   (p/maybe planning/parser)
   (p/maybe clock/parser)
   (p/maybe horizontal-rule/parser)
   (p/maybe greater/parser)
   (p/maybe drawer/parser)
   (p/maybe table/parser)
   (p/maybe list/parser)
   text-block))

(def parser
  (p/*many-till block p/eof))

(defn parse [s]
  (p/parse parser s))
