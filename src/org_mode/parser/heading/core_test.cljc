(ns org-mode.parser.heading.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [org-mode.parser.heading.core :as heading]
   [org-mode.writer.string.core :as writer]))

(deftest parse-document-produces-flat-headers
  (testing "documents with nested levels still return a flat header vector"
    (let [doc (str
               "#+TITLE: Demo\n"
               "\n"
               "Prelude paragraph.\n"
               "\n"
               "* Heading One\n"
               "Body under heading one.\n"
               "\n"
               "** Subheading Two\n"
               "Child content line.\n")
          {:keys [body headers]} (heading/parse-document doc)]
      (is (= "#+TITLE: Demo\n\nPrelude paragraph.\n"
             (writer/blocks->string body)))
      (is (= [1 2] (map :level headers)))
      (is (= ["Heading One" "Subheading Two"]
             (map (comp writer/inline->string :title) headers)))
      (is (= "Body under heading one.\n"
             (writer/blocks->string (:body (first headers)))))
      (is (= "Child content line.\n"
             (writer/blocks->string (:body (second headers)))))
      (is (= 2 (count headers))))))

(deftest parse-document-leading-heading
  (testing "documents that start with a heading have an empty body"
    (let [doc (str
               "* Top Heading\n"
               "Top content line.\n"
               "\n"
               "* Next Heading\n"
               "- [X] done\n")
          {:keys [body headers]} (heading/parse-document doc)]
      (is (= "" (writer/blocks->string body)))
      (is (= 2 (count headers)))
      (is (= [1 1] (map :level headers)))
      (is (= ["Top Heading" "Next Heading"]
             (map (comp writer/inline->string :title) headers)))
      (is (= "Top content line.\n"
             (writer/blocks->string (:body (first headers)))))
      (is (= "- [X] done\n"
             (writer/blocks->string (:body (second headers))))))))

(deftest parse-document-bare-stars-in-body
  (testing "lines of only stars without following whitespace stay in the body"
    (let [{:keys [body headers]} (heading/parse-document "***")]
      (is (= "***\n"
             (writer/blocks->string body)))
      (is (empty? headers)))))

#?(:clj
   (deftest parse-document-parallel-matches-serial
     (testing "parallel variant produces identical structure to sequential parse"
       (let [doc (str
                  "#+TITLE: Demo\n\n"
                  "* First\nBody\n\n"
                  "* Second\nMore\n")
             sequential (heading/parse-document doc)
             parallel (heading/parse-document doc {:parallel? true})]
         (is (= sequential parallel))))))
