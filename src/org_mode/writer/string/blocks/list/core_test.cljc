(ns org-mode.writer.string.blocks.list.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [org-mode.parser.blocks.list.tags :as tags]
   [org-mode.writer.string.blocks.list.core :as w]))

(deftest list-writer
  (testing "writes unordered list item"
    (is (= "- item\n"
           (w/token->string [tags/list-item {:indent 0 :marker [:unordered \-] :tokens ["item"]}])))))

