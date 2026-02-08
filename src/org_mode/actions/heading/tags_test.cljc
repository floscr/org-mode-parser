(ns org-mode.actions.heading.tags-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [org-mode.actions.heading.tags :as sut]
   [org-mode.computed.heading.core :as c.heading]
   [org-mode.parser.inline.core :as p.inline]))

(defn- make-heading [title-str]
  {:level 1
   :title (p.inline/parse title-str)
   :body []
   :trailing-blank? false})

(deftest set-tags-test
  (testing "sets tags on heading without tags"
    (let [h (make-heading "My task")
          result (sut/set-tags ["work" "urgent"] h)]
      (is (= ["work" "urgent"] (c.heading/tags result)))))

  (testing "replaces existing tags"
    (let [h (make-heading "My task :old:")
          result (sut/set-tags ["new"] h)]
      (is (= ["new"] (c.heading/tags result)))))

  (testing "empty tags removes all"
    (let [h (make-heading "My task :work:")
          result (sut/set-tags [] h)]
      (is (nil? (c.heading/tags result))))))

(deftest add-tag-test
  (testing "adds tag to heading without tags"
    (let [h (make-heading "My task")
          result (sut/add-tag "work" h)]
      (is (= ["work"] (c.heading/tags result)))))

  (testing "adds tag to heading with existing tags"
    (let [h (make-heading "My task :work:")
          result (sut/add-tag "urgent" h)]
      (is (= ["work" "urgent"] (c.heading/tags result)))))

  (testing "no-op if tag already exists"
    (let [h (make-heading "My task :work:")
          result (sut/add-tag "work" h)]
      (is (= ["work"] (c.heading/tags result))))))

(deftest remove-tag-test
  (testing "removes a tag"
    (let [h (make-heading "My task :work:urgent:")
          result (sut/remove-tag "work" h)]
      (is (= ["urgent"] (c.heading/tags result)))))

  (testing "removes last tag removes all tags"
    (let [h (make-heading "My task :work:")
          result (sut/remove-tag "work" h)]
      (is (nil? (c.heading/tags result))))))

(deftest toggle-tag-test
  (testing "adds tag when absent"
    (let [h (make-heading "My task")
          result (sut/toggle-tag "work" h)]
      (is (= ["work"] (c.heading/tags result)))))

  (testing "removes tag when present"
    (let [h (make-heading "My task :work:")
          result (sut/toggle-tag "work" h)]
      (is (nil? (c.heading/tags result))))))

(deftest remove-all-tags-test
  (testing "removes all tags"
    (let [h (make-heading "My task :work:urgent:")
          result (sut/remove-all-tags h)]
      (is (nil? (c.heading/tags result))))))
