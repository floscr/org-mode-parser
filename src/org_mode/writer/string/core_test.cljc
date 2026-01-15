(ns org-mode.writer.string.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [org-mode.parser.blocks.core :as p]
   [org-mode.parser.blocks.tags :as blk-tags]
   [org-mode.writer.string.core :as w]))

(deftest roundtrip-basic-document
  (testing "roundtrip for keywords, blank line, plain text line"
    (let [doc "#+TITLE: Hello\n\nA plain line with *bold* and /italic/\n"
          parsed (p/parse doc)
          rendered (w/blocks->string parsed)]
      (is (= doc rendered)))))

(deftest roundtrip-comprehensive-document
  (testing "roundtrip for lists, table, planning, drawer, comments, block"
    (let [doc (str
               "# A comment line\n"
               "#+AUTHOR: Jane Doe\n"
               "SCHEDULED: <2024-01-01 Mon>\n"
               ":PROPERTIES:\n"
               ":FOO: bar\n"
               ":END:\n"
               "\n"
               "- [X] item one\n"
               "1. second item\n"
               "\n"
               "| a | b |\n"
               "|---+---|\n"
               "| c | d |\n"
               "\n"
               "#+BEGIN_SRC clj\n(+ 1 2)\n#+END_SRC\n")
          parsed (p/parse doc)
          rendered (w/blocks->string parsed)]
      (is (= doc rendered)))))

(deftest text-block-accepts-vector-and-list
  (testing "text tokens render regardless of sequential type"
    (doseq [[label content] [[:vector ["Hello"]]
                             [:list (list "Hello")]]]
      (is (= "Hello\n"
             (w/blocks->string [[blk-tags/text content]]))
          (str "content type " label)))))
