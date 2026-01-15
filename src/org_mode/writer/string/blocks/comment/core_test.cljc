(ns org-mode.writer.string.blocks.comment.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [org-mode.parser.blocks.comment.tags :as tags]
   [org-mode.writer.string.blocks.comment.core :as w]))

(deftest comment-writer
  (testing "writes comment line"
    (is (= "# hello world\n"
           (w/token->string [tags/comment-line ["hello" " " "world"]])))))

