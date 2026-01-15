(ns org-mode.parser.inline.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [org-mode.parser.inline.core :as inline]
   [org-mode.parser.inline.tags :as tags]
   [org-mode.parser.inline.tags :refer [bold code footnote-def footnote-ref
                                        inactive-timestamp italic link
                                        link-with-title macro strike-through
                                        target timestamp underline verbatim]]))

(deftest single-inline-test
  (testing "Single inline parsers"
    (is (= (inline/parse "*bold*") [[bold "bold"]]))
    (is (= (inline/parse "/italic/") [[italic "italic"]]))
    (is (= (inline/parse "_underline_") [[underline "underline"]]))
    (is (= (inline/parse "=verbatim=") [[verbatim "verbatim"]]))
    (is (= (inline/parse "~code~") [[code "code"]]))
    (is (= (inline/parse "+strike-through+") [[strike-through "strike-through"]]))))

(deftest multiple-inline-test
  (testing "Multiple inline parsers"
    (is (= (inline/parse "hello *world*") ["hello" " " [bold "world"]]))
    (is (= (inline/parse "*hello* /world/") [[bold "hello"] " " [italic "world"]]))
    (is (= (inline/parse "*hello* /world/ _underline_") [[bold "hello"] " " [italic "world"] " " [underline "underline"]]))))

(deftest timestamp-test
  (testing "Timestamp parser"
    (is (= (inline/parse "<2024-05-22 Wed>")
           [[timestamp "2024-05-22 Wed"]]))
    (is (= (inline/parse "A timestamp <2024-05-22 Wed>")
           ["A" " " "timestamp" " " [timestamp "2024-05-22 Wed"]]))))

(deftest inactive-timestamp-test
  (testing "Timestamp parser"
    (is (= (inline/parse "[2024-05-22 Wed]")
           [[inactive-timestamp "2024-05-22 Wed"]]))
    (is (= (inline/parse "A timestamp [2024-05-22 Wed]")
           ["A" " " "timestamp" " " [inactive-timestamp "2024-05-22 Wed"]]))))

(deftest link-test
  (testing "Link parser"
    (is (= (inline/parse "[[https://example.com]]")
           [[link "https://example.com"]]))
    (is (= (inline/parse "[[https://example.com][An example]]")
           [[link-with-title {:link "https://example.com" :title "An example"}]]))
    (is (= (inline/parse "A link to [[https://example.com][An example]]")
           ["A" " " "link" " " "to" " " [link-with-title {:link "https://example.com" :title "An example"}]]))))

(deftest target-test
  (testing "Target parser"
    (is (= (inline/parse "<<my-target>>")
           [[target "my-target"]]))
    (is (= (inline/parse "A target <<my-target>>")
           ["A" " " "target" " " [target "my-target"]]))))

(deftest footnote-test
  (testing "Footnote parser"
    (is (= (inline/parse "[fn:1]")
           [[footnote-ref "1"]]))
    (is (= (inline/parse "[fn:1:a footnote definition]")
           [[footnote-def {:name "1" :definition "a footnote definition"}]]))
    (is (= (inline/parse "A footnote [fn:1]")
           ["A" " " "footnote" " " [footnote-ref "1"]]))))

(deftest macro-test
  (testing "Macro parser"
    (is (= (inline/parse "{{{macro_name}}}")
           [[macro {:name "macro_name"}]]))
    (is (= (inline/parse "{{{macro_name(arg1, arg2)}}}")
           [[macro {:name "macro_name" :args ["arg1" "arg2"]}]]))
    (is (= (inline/parse "{{{date}}}")
           [[macro {:name "date"}]]))
    (is (= (inline/parse "A macro {{{title}}} here")
           ["A" " " "macro" " " [macro {:name "title"}] " " "here"]))))

(deftest stats-cookie-test
  (testing "Statistics cookie parser"
    (is (= (inline/parse "[/]")
           [[tags/stats-range-cookie {:from nil :to nil}]]))
    (is (= (inline/parse "[%]")
           [[tags/stats-percent-cookie nil]]))
    (is (= (inline/parse "[2/5]")
           [[tags/stats-range-cookie {:from 2 :to 5}]]))
    (is (= (inline/parse "[40%]")
           [[tags/stats-percent-cookie 40]]))))

(deftest edge-cases-test
  (testing "Dont parse beyond newline"
    (is (= (inline/parse "* Some text
") ["*" " " "Some" " " "text"])))
  (testing "Disregard non closing"
    (is (= (inline/parse "+ Some text here") ["+" " " "Some" " " "text" " " "here"])))
  (testing "No empty"
    (is (= (inline/parse "**") ["**"]))
    (is (= (inline/parse "* *") ["*" " " "*"])))
  (testing "Double delimiter bug"
    (is (= (inline/parse "***Multiple Delimiters***") [[bold "Multiple Delimiters"]])))
  (testing "Non greedy delimiters"
    (is (= (inline/parse "*Non Greedy* Rest*") [[bold "Non Greedy"] " " "Rest*"])))
  (testing "Dont parse nested delimiters"
    (is (= (inline/parse "/=No Nested/=") [[italic "=No Nested"] "="]))
    (is (= (inline/parse "=~No Nested~=") [[verbatim "~No Nested~"]]))))
