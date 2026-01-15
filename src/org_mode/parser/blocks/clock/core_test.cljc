(ns org-mode.parser.blocks.clock.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [org-mode.parser.blocks.clock.core :as clock]
   [strojure.parsesso.parser :as p]))

(deftest clock-line-test
  (testing "parses clock entry with timestamp range"
    (is (= (p/parse clock/parser "CLOCK: [2024-01-01 Mon 09:00]--[2024-01-01 Mon 17:00]")
           [:clock [[:inactive-timestamp-range {:start "2024-01-01 Mon 09:00" :end "2024-01-01 Mon 17:00"}]]])))

  (testing "parses clock entry with lowercase"
    (is (= (p/parse clock/parser "clock: [2024-01-01 Mon 09:00]--[2024-01-01 Mon 17:00]")
           [:clock [[:inactive-timestamp-range {:start "2024-01-01 Mon 09:00" :end "2024-01-01 Mon 17:00"}]]])))

  (testing "parses clock entry with mixed case"
    (is (= (p/parse clock/parser "ClOcK: [2024-01-01 Mon 09:00]--[2024-01-01 Mon 17:00]")
           [:clock [[:inactive-timestamp-range {:start "2024-01-01 Mon 09:00" :end "2024-01-01 Mon 17:00"}]]])))

  (testing "parses clock entry with extra whitespace after colon"
    (is (= (p/parse clock/parser "CLOCK:   [2024-01-01 Mon 09:00]--[2024-01-01 Mon 17:00]")
           [:clock ["  " [:inactive-timestamp-range {:start "2024-01-01 Mon 09:00" :end "2024-01-01 Mon 17:00"}]]])))

  (testing "parses clock entry with time only"
    (is (= (p/parse clock/parser "CLOCK: [09:00]--[17:00]")
           [:clock [[:inactive-timestamp-range {:start "09:00" :end "17:00"}]]])))

  (testing "parses clock entry with single timestamp"
    (is (= (p/parse clock/parser "CLOCK: [2024-01-01 Mon 09:00]")
           [:clock [[:inactive-timestamp "2024-01-01 Mon 09:00"]]])))

  (testing "parses clock entry with duration"
    (is (= (p/parse clock/parser "CLOCK: [2024-01-01 Mon 09:00]--[2024-01-01 Mon 17:00] =>  8:00")
           [:clock [[:inactive-timestamp-range {:start "2024-01-01 Mon 09:00" :end "2024-01-01 Mon 17:00"}]
                    " " "=>" "  " "8:00"]]))))
