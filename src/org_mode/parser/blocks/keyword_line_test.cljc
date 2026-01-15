(ns org-mode.parser.blocks.keyword-line-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [org-mode.parser.blocks.keyword-line :as keyword-line]
   [strojure.parsesso.parser :as p]))

(deftest keyword-line-test
  (testing "parses keyword with single word"
    (is (= (p/parse (keyword-line/parser "TEST" :test) "TEST: hello")
           [:test ["hello"]])))

  (testing "parses keyword with multiple words"
    (is (= (p/parse (keyword-line/parser "TEST" :test) "TEST: hello world")
           [:test ["hello" " " "world"]])))

  (testing "parses keyword case-insensitively"
    (is (= (p/parse (keyword-line/parser "TEST" :test) "test: hello")
           [:test ["hello"]]))
    (is (= (p/parse (keyword-line/parser "TEST" :test) "TeSt: hello")
           [:test ["hello"]])))

  (testing "parses keyword with timestamp"
    (is (= (p/parse (keyword-line/parser "SCHEDULED" :scheduled) "SCHEDULED: <2024-01-01 Mon>")
           [:scheduled [[:timestamp "2024-01-01 Mon"]]])))

  (testing "parses keyword with extra whitespace"
    (is (= (p/parse (keyword-line/parser "TEST" :test) "TEST:   hello")
           [:test ["  " "hello"]])))

  (testing "parses keyword with bold text"
    (is (= (p/parse (keyword-line/parser "TEST" :test) "TEST: *bold* text")
           [:test [[:bold "bold"] " " "text"]])))

  (testing "parses keyword with mixed content"
    (is (= (p/parse (keyword-line/parser "TEST" :test) "TEST: some <2024-01-01> text")
           [:test ["some" " " [:timestamp "2024-01-01"] " " "text"]]))))
