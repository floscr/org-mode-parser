(ns org-mode.computed.heading.blocks-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [org-mode.computed.heading.blocks :as sut]
   [org-mode.parser.blocks.core :as blocks]
   [org-mode.parser.heading.core :as heading]))

;; Helpers --------------------------------------------------------------------

(defn- parse-heading
  "Parse an org heading string and return the first header."
  [s]
  (first (:headers (heading/parse-document s))))

;; Token predicate tests ------------------------------------------------------

(deftest token-predicates-test
  (testing "property-token?"
    (is (true? (sut/property-token? [:property {:key "ID" :value ["abc"]}])))
    (is (false? (sut/property-token? [:text ["hello"]]))))

  (testing "newline-token?"
    (is (true? (sut/newline-token? [:newlines 1])))
    (is (false? (sut/newline-token? [:text ["x"]]))))

  (testing "scheduled-token?"
    (is (true? (sut/scheduled-token? [:scheduled [[:timestamp "2024-01-01"]]])))
    (is (false? (sut/scheduled-token? [:deadline [[:timestamp "2024-01-01"]]]))))

  (testing "deadline-token?"
    (is (true? (sut/deadline-token? [:deadline [[:timestamp "2024-01-01"]]])))
    (is (false? (sut/deadline-token? [:scheduled [[:timestamp "2024-01-01"]]]))))

  (testing "planning-token?"
    (is (true? (sut/planning-token? [:scheduled [[:timestamp "2024-01-01"]]])))
    (is (true? (sut/planning-token? [:deadline [[:timestamp "2024-01-01"]]])))
    (is (true? (sut/planning-token? [:closed [[:timestamp "2024-01-01"]]])))
    (is (false? (sut/planning-token? [:text ["x"]]))))

  (testing "source-block-token?"
    (is (true? (sut/source-block-token?
                [:block {:type "src" :begin "#+BEGIN_SRC" :end "#+END_SRC"
                         :args nil :content "(+ 1 2)"}])))
    (is (false? (sut/source-block-token?
                 [:block {:type "example" :begin "#+BEGIN_EXAMPLE"
                          :end "#+END_EXAMPLE" :args nil :content "text"}])))))

;; Properties tests -----------------------------------------------------------

(deftest body-properties-test
  (testing "extracts properties from parsed heading"
    (let [heading (parse-heading "* My heading\n:PROPERTIES:\n:ID: abc-123\n:CATEGORY: work\n:END:\nBody text")
          props (sut/body-properties heading)]
      ;; Property values are inline token vectors preserving original spacing
      (is (= [" " "abc-123"] (get props "ID")))
      (is (= [" " "work"] (get props "CATEGORY")))))

  (testing "empty when no properties"
    (let [heading (parse-heading "* Bare heading\nJust text")]
      (is (empty? (sut/body-properties heading))))))

(deftest split-properties-body-test
  (testing "splits properties from body"
    (let [heading (parse-heading "* Heading\n:PROPERTIES:\n:ID: abc\n:END:\nBody text")
          [props rest-body] (sut/split-properties-body (:body heading))]
      (is (pos? (count props)))
      (is (pos? (count rest-body))))))

;; Planning tests -------------------------------------------------------------

(deftest collect-planning-test
  (testing "extracts scheduled timestamp"
    (let [heading (parse-heading "* Task\nSCHEDULED: <2024-02-29 Thu>")
          planning (sut/collect-planning heading)]
      (is (some? (:scheduled planning)))
      (is (= :active (get-in planning [:scheduled :date :parsed :type])))
      (is (= 2024 (get-in planning [:scheduled :date :parsed :from :year])))
      (is (= 2 (get-in planning [:scheduled :date :parsed :from :month])))
      (is (= 29 (get-in planning [:scheduled :date :parsed :from :day])))))

  (testing "extracts deadline timestamp"
    (let [heading (parse-heading "* Task\nDEADLINE: <2024-03-15 Fri>")
          planning (sut/collect-planning heading)]
      (is (some? (:deadline planning)))
      (is (= 2024 (get-in planning [:deadline :date :parsed :from :year])))
      (is (= 3 (get-in planning [:deadline :date :parsed :from :month])))))

  (testing "extracts both scheduled and deadline"
    (let [heading (parse-heading "* Task\nSCHEDULED: <2024-02-29 Thu>\nDEADLINE: <2024-03-15 Fri>")
          planning (sut/collect-planning heading)]
      (is (some? (:scheduled planning)))
      (is (some? (:deadline planning)))))

  (testing "with custom parse-date"
    (let [heading (parse-heading "* Task\nSCHEDULED: <2024-02-29 Thu>")
          planning (sut/collect-planning heading
                                        :parse-date (fn [{:keys [year month day]}]
                                                      [year month day]))]
      (is (= [2024 2 29] (get-in planning [:scheduled :date :parsed :from]))))))

(deftest scheduled-test
  (testing "returns scheduled info"
    (let [heading (parse-heading "* Task\nSCHEDULED: <2024-02-29 Thu>")]
      (is (= :active (:type (sut/scheduled heading))))
      (is (= 2024 (get-in (sut/scheduled heading) [:from :year])))))

  (testing "returns nil when no scheduled"
    (let [heading (parse-heading "* Task\nJust body")]
      (is (nil? (sut/scheduled heading))))))

(deftest deadline-test
  (testing "returns deadline info"
    (let [heading (parse-heading "* Task\nDEADLINE: <2024-03-15 Fri>")]
      (is (= :active (:type (sut/deadline heading))))
      (is (= 3 (get-in (sut/deadline heading) [:from :month])))))

  (testing "returns nil when no deadline"
    (let [heading (parse-heading "* Task\nJust body")]
      (is (nil? (sut/deadline heading))))))

;; Source block tests ----------------------------------------------------------

(deftest collect-source-blocks-test
  (testing "extracts source blocks"
    (let [heading (parse-heading "* Code heading\n#+BEGIN_SRC clj\n(+ 1 2)\n#+END_SRC")
          blocks (sut/collect-source-blocks heading)]
      (is (= 1 (count blocks)))
      (is (= "src" (:type (first blocks))))
      (is (= "(+ 1 2)" (:content (first blocks))))))

  (testing "empty when no source blocks"
    (let [heading (parse-heading "* Plain heading\nJust text")]
      (is (empty? (sut/collect-source-blocks heading))))))
