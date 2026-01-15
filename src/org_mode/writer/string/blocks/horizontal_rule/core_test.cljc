(ns org-mode.writer.string.blocks.horizontal-rule.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [org-mode.parser.blocks.horizontal-rule.tags :as tags]
   [org-mode.writer.string.blocks.horizontal-rule.core :as w]))

(deftest hr-writer
  (testing "writes horizontal rule"
    (is (= "-----\n"
           (w/token->string [tags/horizontal-rule 5])))))

