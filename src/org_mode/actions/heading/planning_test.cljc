(ns org-mode.actions.heading.planning-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [org-mode.actions.heading.planning :as sut]
   [org-mode.computed.heading.blocks :as c.blocks]
   [org-mode.core :as org]
   [org-mode.parser.blocks.core :as blocks]
   [org-mode.parser.inline.core :as p.inline]))

(defn- make-heading
  [title-str & {:keys [body]}]
  {:level 1
   :title (p.inline/parse title-str)
   :body (or body [])
   :trailing-blank? false})

(defn- parsed-heading
  "Parse org text and return the first heading."
  [s]
  (first (:headers (org/parse s))))

(deftest set-scheduled-test
  (testing "adds SCHEDULED to empty body"
    (let [h (make-heading "My task")
          result (sut/set-scheduled "<2024-02-29 Thu>" h)]
      (is (some c.blocks/scheduled-token? (:body result)))))

  (testing "replaces existing SCHEDULED"
    (let [h (parsed-heading "* My task\nSCHEDULED: <2024-01-01 Mon>")
          result (sut/set-scheduled "<2024-02-29 Thu>" h)
          scheduled-tokens (filter c.blocks/scheduled-token? (:body result))]
      (is (= 1 (count scheduled-tokens))))))

(deftest remove-scheduled-test
  (testing "removes SCHEDULED"
    (let [h (parsed-heading "* My task\nSCHEDULED: <2024-01-01 Mon>")
          result (sut/remove-scheduled h)]
      (is (not (some c.blocks/scheduled-token? (:body result))))))

  (testing "no-op when no SCHEDULED"
    (let [h (make-heading "My task")]
      (is (= h (sut/remove-scheduled h))))))

(deftest set-deadline-test
  (testing "adds DEADLINE to empty body"
    (let [h (make-heading "My task")
          result (sut/set-deadline "<2024-03-15 Fri>" h)]
      (is (some c.blocks/deadline-token? (:body result)))))

  (testing "replaces existing DEADLINE"
    (let [h (parsed-heading "* My task\nDEADLINE: <2024-01-01 Mon>")
          result (sut/set-deadline "<2024-03-15 Fri>" h)
          deadline-tokens (filter c.blocks/deadline-token? (:body result))]
      (is (= 1 (count deadline-tokens))))))

(deftest remove-deadline-test
  (testing "removes DEADLINE"
    (let [h (parsed-heading "* My task\nDEADLINE: <2024-01-01 Mon>")
          result (sut/remove-deadline h)]
      (is (not (some c.blocks/deadline-token? (:body result)))))))

(deftest set-closed-test
  (testing "adds CLOSED to empty body"
    (let [h (make-heading "My task")
          result (sut/set-closed "[2024-02-29 Thu]" h)]
      (is (some c.blocks/closed-token? (:body result))))))

(deftest remove-closed-test
  (testing "removes CLOSED"
    (let [h (parsed-heading "* My task\nCLOSED: [2024-01-01 Mon]")
          result (sut/remove-closed h)]
      (is (not (some c.blocks/closed-token? (:body result)))))))

(deftest planning-after-properties-test
  (testing "planning is inserted after properties drawer"
    (let [h (parsed-heading "* My task\n:PROPERTIES:\n:ID: abc\n:END:")
          result (sut/set-scheduled "<2024-02-29 Thu>" h)
          body (:body result)
          ;; Find the drawer-end index and scheduled index
          end-idx (first (keep-indexed (fn [i t] (when (c.blocks/drawer-end-token? t) i)) body))
          sched-idx (first (keep-indexed (fn [i t] (when (c.blocks/scheduled-token? t) i)) body))]
      (is (some? end-idx))
      (is (some? sched-idx))
      (is (> sched-idx end-idx)))))
