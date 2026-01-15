(ns org-mode.parser.blocks.planning.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [org-mode.parser.blocks.planning.core :as planning]
   [strojure.parsesso.parser :as p]))

(deftest scheduled-test
  (testing "parses SCHEDULED with active timestamp"
    (is (= (p/parse planning/parser "SCHEDULED: <2024-01-01 Mon>")
           [:scheduled [[:timestamp "2024-01-01 Mon"]]])))

  (testing "parses scheduled with lowercase"
    (is (= (p/parse planning/parser "scheduled: <2024-01-01 Mon>")
           [:scheduled [[:timestamp "2024-01-01 Mon"]]])))

  (testing "parses scheduled with extra whitespace"
    (is (= (p/parse planning/parser "SCHEDULED:   <2024-01-01 Mon>")
           [:scheduled ["  " [:timestamp "2024-01-01 Mon"]]])))

  (testing "parses scheduled with time range"
    (is (= (p/parse planning/parser "SCHEDULED: <2024-01-01 Mon 09:00-17:00>")
           [:scheduled [[:timestamp "2024-01-01 Mon 09:00-17:00"]]])))

  (testing "parses scheduled with repeater"
    (is (= (p/parse planning/parser "SCHEDULED: <2024-01-01 Mon +1w>")
           [:scheduled [[:timestamp "2024-01-01 Mon +1w"]]]))))

(deftest deadline-test
  (testing "parses DEADLINE with active timestamp"
    (is (= (p/parse planning/parser "DEADLINE: <2024-01-01 Mon>")
           [:deadline [[:timestamp "2024-01-01 Mon"]]])))

  (testing "parses deadline with lowercase"
    (is (= (p/parse planning/parser "deadline: <2024-01-15 Mon>")
           [:deadline [[:timestamp "2024-01-15 Mon"]]]))))

(deftest closed-test
  (testing "parses CLOSED with inactive timestamp"
    (is (= (p/parse planning/parser "CLOSED: [2024-01-01 Mon 09:00]")
           [:closed [[:inactive-timestamp "2024-01-01 Mon 09:00"]]])))

  (testing "parses closed with lowercase"
    (is (= (p/parse planning/parser "closed: [2024-01-01 Mon]")
           [:closed [[:inactive-timestamp "2024-01-01 Mon"]]]))))

(deftest mixed-content-test
  (testing "parses scheduled with text before timestamp"
    (is (= (p/parse planning/parser "SCHEDULED: some text <2024-01-01 Mon>")
           [:scheduled ["some" " " "text" " " [:timestamp "2024-01-01 Mon"]]])))

  (testing "parses scheduled with text after timestamp"
    (is (= (p/parse planning/parser "SCHEDULED: <2024-01-01 Mon> and more")
           [:scheduled [[:timestamp "2024-01-01 Mon"] " " "and" " " "more"]]))))
