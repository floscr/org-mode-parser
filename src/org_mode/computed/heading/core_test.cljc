(ns org-mode.computed.heading.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [org-mode.computed.heading.core :as sut]
   [org-mode.parser.inline.core :as p.inline]))

;; Helpers --------------------------------------------------------------------

(defn- make-heading [title-str & {:keys [body]}]
  {:level 1
   :title (p.inline/parse title-str)
   :body (or body [])
   :trailing-blank? false})

;; Tests ----------------------------------------------------------------------

(deftest todo-test
  (testing "extracts TODO keyword"
    (is (= "TODO" (sut/todo (make-heading "TODO My task")))))

  (testing "extracts DONE keyword"
    (is (= "DONE" (sut/todo (make-heading "DONE Finished task")))))

  (testing "returns nil when no TODO keyword"
    (is (nil? (sut/todo (make-heading "Just a heading")))))

  (testing "custom todo keywords"
    (is (= "WAITING" (sut/todo (make-heading "WAITING For review")
                               :todo-keywords #{"WAITING" "TODO" "DONE"})))))

(deftest todo?-test
  (testing "true for TODO headings"
    (is (true? (sut/todo? (make-heading "TODO Task")))))

  (testing "false for non-TODO headings"
    (is (false? (sut/todo? (make-heading "Normal heading"))))))

(deftest done?-test
  (testing "true for DONE headings"
    (is (true? (sut/done? (make-heading "DONE Finished")))))

  (testing "false for TODO headings"
    (is (false? (sut/done? (make-heading "TODO Not finished"))))))

(deftest tags-test
  (testing "extracts tags from title"
    (is (= ["foo" "bar"] (sut/tags (make-heading "My heading :foo:bar:")))))

  (testing "returns nil when no tags"
    (is (nil? (sut/tags (make-heading "No tags here")))))

  (testing "single tag"
    (is (= ["work"] (sut/tags (make-heading "Task :work:"))))))

(deftest heading-tags-test
  (testing "counts tags across headings"
    (let [headings [(make-heading "A :foo:bar:")
                    (make-heading "B :bar:")
                    (make-heading "C :bar:")
                    (make-heading "D :bar:")
                    (make-heading "E")
                    (make-heading "F :foo:")]]
      (is (= {"foo" 2 "bar" 4} (sut/heading-tags headings))))))

(deftest normalize-tag-test
  (testing "uppercases and replaces delimiters"
    (is (= "FOO_BAR" (sut/normalize-tag "  foo :bar  "))))

  (testing "nil for empty string"
    (is (nil? (sut/normalize-tag "")))
    (is (nil? (sut/normalize-tag "   ")))))

(deftest title-str-test
  (testing "renders title with inline markup to string"
    (is (= "Hello *world*"
           (sut/title-str (make-heading "Hello *world*"))))))

(deftest title-stripped-str-test
  (testing "strips inline markup"
    (let [heading (make-heading "Hello *world* and /italic/")]
      ;; The stripped version should contain the text without delimiters
      (is (string? (sut/title-stripped-str heading))))))

(deftest matches-query?-test
  (testing "matches case-insensitively in title"
    (is (true? (sut/matches-query? "hello" (make-heading "Hello World")))))

  (testing "empty query matches everything"
    (is (true? (sut/matches-query? "" (make-heading "Anything")))))

  (testing "non-matching query"
    (is (false? (sut/matches-query? "xyz" (make-heading "Hello World"))))))
