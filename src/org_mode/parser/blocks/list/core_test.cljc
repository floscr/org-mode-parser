(ns org-mode.parser.blocks.list.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [org-mode.parser.blocks.list.core :as sut]
   [strojure.parsesso.parser :as p]))

(deftest unordered-list-test
  (testing "simple unordered list with dash"
    (is (= (p/parse sut/parser "- item text\n")
           [:list {:indent 0, :marker [:unordered \-], :tokens ["item" " " "text"]}])))

  (testing "unordered list with plus"
    (is (= (p/parse sut/parser "+ item text\n")
           [:list {:indent 0
                   :marker [:unordered \+]
                   :tokens ["item" " " "text"]}])))

  (testing "unordered list with asterisk"
    (is (= (p/parse sut/parser "* item text\n")
           [:list {:indent 0
                   :marker [:unordered \*]
                   :tokens ["item" " " "text"]}])))

  (testing "indented unordered list"
    (is (= (p/parse sut/parser "  - nested item\n")
           [:list {:indent 2
                   :marker [:unordered \-]
                   :tokens ["nested" " " "item"]}])))

  (testing "list with inline markup"
    (is (= (p/parse sut/parser "- *bold* and /italic/ text\n")
           [:list {:indent 0
                   :marker [:unordered \-]
                   :tokens [[:bold "bold"]
                            " "
                            "and"
                            " "
                            [:italic "italic"]
                            " "
                            "text"]}]))))

(deftest ordered-list-test
  (testing "ordered list with dot"
    (is (= (p/parse sut/parser "1. first item\n")
           [:list {:indent 0
                   :marker [:ordered {:marker [\1], :delimiter \.}]
                   :tokens ["first" " " "item"]}])))

  (testing "ordered list with parenthesis"
    (is (= (p/parse sut/parser "1) first item\n")
           [:list {:indent 0
                   :marker [:ordered {:marker [\1], :delimiter \)}]
                   :tokens ["first" " " "item"]}])))

  (testing "multi-digit ordered list"
    (is (= (p/parse sut/parser "42. answer\n")
           [:list {:indent 0
                   :marker [:ordered {:marker [\4 \2], :delimiter \.}]
                   :tokens ["answer"]}])))

  (testing "indented ordered list"
    (is (= (p/parse sut/parser "  1. nested item\n")
           [:list {:indent 2
                   :marker [:ordered {:marker [\1], :delimiter \.}]
                   :tokens ["nested" " " "item"]}]))))

(deftest checkbox-test
  (testing "unchecked checkbox"
    (is (= (p/parse sut/parser "- [ ] todo item\n")
           [:list {:indent 0
                   :marker [:unordered \-]
                   :checkbox \space
                   :tokens ["todo" " " "item"]}])))

  (testing "checked checkbox with X"
    (is (= (p/parse sut/parser "- [X] done item\n")
           [:list {:indent 0
                   :marker [:unordered \-]
                   :checkbox \X
                   :tokens ["done" " " "item"]}])))

  (testing "checked checkbox with lowercase x"
    (is (= (p/parse sut/parser "- [x] done item\n")
           [:list {:indent 0
                   :marker [:unordered \-]
                   :checkbox \x
                   :tokens ["done" " " "item"]}])))

  (testing "partial checkbox"
    (is (= (p/parse sut/parser "- [-] partial item\n")
           [:list {:indent 0
                   :marker [:unordered \-]
                   :checkbox \-
                   :tokens ["partial" " " "item"]}])))

  (testing "checkbox in ordered list"
    (is (= (p/parse sut/parser "1. [ ] todo item\n")
           [:list {:indent 0
                   :marker [:ordered {:marker [\1], :delimiter \.}]
                   :checkbox \space
                   :tokens ["todo" " " "item"]}])))

  (testing "indented checkbox"
    (is (= (p/parse sut/parser "  - [X] nested done\n")
           [:list {:indent 2
                   :marker [:unordered \-]
                   :checkbox \X
                   :tokens ["nested" " " "done"]}]))))

(deftest edge-cases-test
  (testing "list with EOF instead of newline"
    (is (= (p/parse sut/parser "- item")
           [:list {:indent 0
                   :marker [:unordered \-]
                   :tokens ["item"]}])))

  (testing "empty list item"
    (is (= (p/parse sut/parser "- \n")
           [:list {:indent 0
                   :marker [:unordered \-]
                   :tokens nil}])))

  (testing "list with multiple spaces after marker"
    (is (= (p/parse sut/parser "-   item\n")
           [:list {:indent 0
                   :marker [:unordered \-]
                   :tokens ["  " "item"]}]))))
