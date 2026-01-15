(ns org-mode.parser.blocks.horizontal-rule.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [org-mode.parser.blocks.horizontal-rule.core :as horizontal-rule]
   [strojure.parsesso.parser :as p])
  (:import
   [java.lang Exception]))

(deftest horizontal-rule-test
  (testing "parses exactly 5 dashes"
    (is (= (p/parse horizontal-rule/parser "-----")
           [:horizontal-rule 5])))

  (testing "parses more than 5 dashes"
    (is (= (p/parse horizontal-rule/parser "----------")
           [:horizontal-rule 10])))

  (testing "fails on less than 5 dashes"
    (is (thrown? Exception (p/parse horizontal-rule/parser "----"))))

  (testing "fails on 3 dashes"
    (is (thrown? Exception (p/parse horizontal-rule/parser "---"))))

  (testing "fails on empty string"
    (is (thrown? Exception (p/parse horizontal-rule/parser "")))))
