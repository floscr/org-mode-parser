(ns org-mode.chunker.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [org-mode.chunker.core :as chunker]))

(deftest chunk-headings-splits-headlines
  (testing "splits document text on real headline boundaries, preserving blank lines"
    (let [doc (str
               "Prelude line\n"
               "\n"
               "* Heading One\n"
               "Body under heading.\n"
               "\n"
               "** Child\n"
               "Child body line\n")
          chunks (chunker/chunk-headings doc)]
      ;; Blank lines before headings are preserved in the preceding chunk
      (is (= ["Prelude line\n\n"
              "* Heading One\nBody under heading.\n\n"
              "** Child\nChild body line\n"]
             chunks)))))

(deftest chunk-headings-ignores-inline-stars
  (testing "does not split on inline or list stars without whitespace"
    (let [doc (str
               "Paragraph line\n"
               "*not a heading*\n"
               "Another line\n")
          chunks (chunker/chunk-headings doc)]
      (is (= [doc] chunks)))))

(deftest chunk-headings-ignores-star-only-lines
  (testing "does not split when a line is just stars without trailing space"
    (let [doc (str
               "foo\n"
               "*****\n"
               "\n"
               "bar\n")
          chunks (chunker/chunk-headings doc)]
      (is (= [doc] chunks)))))
