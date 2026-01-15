(ns org-mode.writer.string.blocks.table.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [org-mode.parser.blocks.table.tags :as tags]
   [org-mode.writer.string.blocks.table.core :as w]))

(deftest table-writer
  (testing "writes table separator"
    (is (= "|---+--|\n"
           (w/token->string [tags/table-separator [[:column [\- \- \-]] [:column [\- \-]]]]))))
  (testing "writes table row"
    (is (= "| a|b |\n"
           (w/token->string [tags/table-row [[:column [" a"]] [:column ["b "]]]])))))

