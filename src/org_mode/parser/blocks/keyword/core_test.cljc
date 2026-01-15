(ns org-mode.parser.blocks.keyword.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [org-mode.parser.blocks.keyword.core :as keyword]
   [org-mode.parser.blocks.keyword.tags :as tags]
   [strojure.parsesso.parser :as p]))

(deftest keyword-line-test
  (testing "parses simple keyword"
    (is (= (p/parse keyword/parser "#+TITLE: My Document")
           [:keyword {:key "TITLE" :value "My Document"}])))

  (testing "parses keyword with uppercase"
    (is (= (p/parse keyword/parser "#+AUTHOR: John Doe")
           [:keyword {:key "AUTHOR" :value "John Doe"}])))

  (testing "parses keyword with lowercase"
    (is (= (p/parse keyword/parser "#+date: 2024-01-01")
           [:keyword {:key "DATE" :value "2024-01-01"}])))

  (testing "parses keyword with mixed case"
    (is (= (p/parse keyword/parser "#+OpTiOnS: toc:nil")
           [:keyword {:key "OPTIONS" :value "toc:nil"}])))

  (testing "parses keyword with hyphen"
    (is (= (p/parse keyword/parser "#+STARTUP-FOLDED: show-all")
           [:keyword {:key "STARTUP-FOLDED" :value "show-all"}])))

  (testing "parses keyword with underscore"
    (is (= (p/parse keyword/parser "#+CUSTOM_ID: my-section")
           [:keyword {:key "CUSTOM_ID" :value "my-section"}])))

  (testing "parses keyword with empty value"
    (is (= (p/parse keyword/parser "#+TITLE:")
           [:keyword {:key "TITLE" :value ""}])))

  (testing "parses keyword with whitespace before value"
    (is (= (p/parse keyword/parser "#+TITLE:    My Title")
           [:keyword {:key "TITLE" :value "My Title"}])))

  (testing "parses keyword with longer value"
    (is (= (p/parse keyword/parser "#+DESCRIPTION: This is a longer description")
           [:keyword {:key "DESCRIPTION" :value "This is a longer description"}]))))
