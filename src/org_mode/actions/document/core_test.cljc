(ns org-mode.actions.document.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [org-mode.actions.document.core :as sut]
   [org-mode.actions.heading.todo :as todo]
   [org-mode.computed.heading.core :as c.heading]
   [org-mode.core :as org]))

(deftest update-heading-at-test
  (testing "updates heading at index"
    (let [doc (org/parse "* TODO First\n* Second\n* Third")
          result (sut/update-heading-at 0 #(todo/toggle-todo %) doc)]
      (is (= "DONE" (c.heading/todo (nth (:headers result) 0))))
      (is (nil? (c.heading/todo (nth (:headers result) 1)))))))

(deftest update-headings-test
  (testing "updates all headings"
    (let [doc (org/parse "* First\n* Second\n* Third")
          result (sut/update-headings #(todo/set-todo "TODO" %) doc)]
      (is (every? #(= "TODO" (c.heading/todo %)) (:headers result))))))

(deftest update-body-test
  (testing "updates document body"
    (let [doc (org/parse "Some preamble\n* Heading")
          result (sut/update-body (fn [_] [[:text ["New body"]]]) doc)]
      (is (= [[:text ["New body"]]] (:body result))))))

(deftest remove-heading-at-test
  (testing "removes heading at index"
    (let [doc (org/parse "* First\n* Second\n* Third")
          result (sut/remove-heading-at 1 doc)]
      (is (= 2 (count (:headers result))))
      (is (= "First" (first (:title (first (:headers result)))))))))
