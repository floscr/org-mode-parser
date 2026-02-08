(ns org-mode.actions.heading.todo-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [org-mode.actions.heading.todo :as sut]
   [org-mode.computed.heading.core :as c.heading]
   [org-mode.parser.inline.core :as p.inline]))

(defn- make-heading [title-str]
  {:level 1
   :title (p.inline/parse title-str)
   :body []
   :trailing-blank? false})

(deftest set-todo-test
  (testing "sets TODO on heading without keyword"
    (let [h (make-heading "My task")
          result (sut/set-todo "TODO" h)]
      (is (= "TODO" (c.heading/todo result)))))

  (testing "replaces existing keyword"
    (let [h (make-heading "TODO My task")
          result (sut/set-todo "DONE" h)]
      (is (= "DONE" (c.heading/todo result)))))

  (testing "no-op when keyword already matches"
    (let [h (make-heading "TODO My task")
          result (sut/set-todo "TODO" h)]
      (is (= (:title h) (:title result)))))

  (testing "works with custom keywords"
    (let [h (make-heading "My task")
          result (sut/set-todo "WAITING" h :todo-keywords #{"WAITING" "DONE"})]
      (is (= "WAITING" (c.heading/todo result :todo-keywords #{"WAITING" "DONE"}))))))

(deftest remove-todo-test
  (testing "removes TODO keyword"
    (let [h (make-heading "TODO My task")
          result (sut/remove-todo h)]
      (is (nil? (c.heading/todo result)))
      (is (= "My" (first (:title result))))))

  (testing "no-op when no keyword"
    (let [h (make-heading "My task")]
      (is (= (:title h) (:title (sut/remove-todo h)))))))

(deftest toggle-todo-test
  (testing "nil -> TODO"
    (let [h (make-heading "My task")
          result (sut/toggle-todo h)]
      (is (= "TODO" (c.heading/todo result)))))

  (testing "TODO -> DONE"
    (let [h (make-heading "TODO My task")
          result (sut/toggle-todo h)]
      (is (= "DONE" (c.heading/todo result)))))

  (testing "DONE -> nil"
    (let [h (make-heading "DONE My task")
          result (sut/toggle-todo h)]
      (is (nil? (c.heading/todo result))))))

(deftest cycle-todo-test
  (let [keywords ["TODO" "DOING" "DONE"]]
    (testing "nil -> first keyword"
      (let [h (make-heading "My task")
            result (sut/cycle-todo h keywords)]
        (is (= "TODO" (c.heading/todo result :todo-keywords (set keywords))))))

    (testing "TODO -> DOING"
      (let [h (make-heading "TODO My task")
            result (sut/cycle-todo h keywords)]
        (is (= "DOING" (c.heading/todo result :todo-keywords (set keywords))))))

    (testing "DOING -> DONE"
      (let [h (make-heading "DOING My task")
            result (sut/cycle-todo h keywords)]
        (is (= "DONE" (c.heading/todo result :todo-keywords (set keywords))))))

    (testing "DONE -> nil"
      (let [h (make-heading "DONE My task")
            result (sut/cycle-todo h keywords)]
        (is (nil? (c.heading/todo result :todo-keywords (set keywords))))))))
