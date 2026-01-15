(ns org-mode.parser.blocks.comment.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [org-mode.parser.blocks.comment.core :as comment]
   [strojure.parsesso.parser :as p])
  (:import
   [java.lang Exception]))

(deftest comment-line-test
  (testing "parses simple comment"
    (is (= (p/parse comment/parser "# This is a comment")
           [:comment ["This" " " "is" " " "a" " " "comment"]])))

  (testing "parses empty comment (just # and space)"
    (is (= (p/parse comment/parser "# ")
           [:comment nil])))

  (testing "does not parse keyword lines (#+)"
    (is (thrown? Exception (p/parse comment/parser "#+TITLE: Not a comment"))))

  (testing "does not parse keyword-like lines"
    (is (thrown? Exception (p/parse comment/parser "#+BEGIN_SRC"))))

  (testing "does not parse # without space"
    (is (thrown? Exception (p/parse comment/parser "#No space")))))
