(ns org-mode.parser.blocks.table.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [org-mode.parser.blocks.table.core :as sut]
   [strojure.parsesso.parser :as p]))

(deftest table-row-test
  (testing "simple table row with plain text cells"
    (is (= (p/parse sut/parser "| cell1 | cell2 | cell3 |\n")
           [:table-row
            [[:column [" " "cell1" " "]]
             [:column [" " "cell2" " "]]
             [:column [" " "cell3" " "]]]])))

  (testing "table row with empty cells"
    (is (= (p/parse sut/parser "|  | |  |\n")
           [:table-row
            [[:column ["  "]]
             [:column [" "]]
             [:column ["  "]]]])))

  (testing "table row with mixed content"
    (is (= (p/parse sut/parser "| *bold* | /italic/ | plain |\n")
           [:table-row
            [[:column [" " [:bold "bold"] " "]]
             [:column [" " [:italic "italic"] " "]]
             [:column [" " "plain" " "]]]])))

  (testing "table row with single cell"
    (is (= (p/parse sut/parser "| single |\n")
           [:table-row
            [[:column [" " "single" " "]]]])))

  (testing "table row with EOF instead of newline"
    (is (= (p/parse sut/parser "| cell1 | cell2 |")
           [:table-row
            [[:column [" " "cell1" " "]]
             [:column [" " "cell2" " "]]]]))))

(deftest table-separator-test
  (testing "simple separator line"
    (is (= (p/parse sut/table-separator "|---+---|\n")
           [:table-separator
            [[:column [\- \- \-]]
             [:column [\- \- \-]]]]))))
