(ns org-mode.writer.string.blocks.keyword.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [org-mode.parser.blocks.keyword.tags :as tags]
   [org-mode.writer.string.blocks.keyword.core :as w]))

(deftest keyword-writer
  (testing "writes keyword line"
    (is (= "#+TITLE: Hello\n"
           (w/token->string [tags/keyword-line {:key "TITLE" :value "Hello"}])))))

