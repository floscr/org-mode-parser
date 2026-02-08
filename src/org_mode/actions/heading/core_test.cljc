(ns org-mode.actions.heading.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [org-mode.actions.heading.core :as sut]
   [org-mode.parser.inline.core :as p.inline]))

(defn- make-heading [title-str & {:keys [body level]}]
  {:level (or level 1)
   :title (p.inline/parse title-str)
   :body (or body [])
   :trailing-blank? false})

(deftest set-level-test
  (testing "sets heading level"
    (let [h (make-heading "Title")]
      (is (= 2 (:level (sut/set-level 2 h))))
      (is (= 3 (:level (sut/set-level 3 h)))))))

(deftest set-title-test
  (testing "sets title tokens"
    (let [h (make-heading "Old title")
          new-tokens (p.inline/parse "New title")]
      (is (= new-tokens (:title (sut/set-title new-tokens h)))))))

(deftest set-body-test
  (testing "sets body tokens"
    (let [h (make-heading "Title")
          new-body [[:text ["Some" " " "content"]]]]
      (is (= new-body (:body (sut/set-body new-body h)))))))
