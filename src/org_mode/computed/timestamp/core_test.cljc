(ns org-mode.computed.timestamp.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [org-mode.computed.timestamp.core :as sut]))

(deftest parse-timestamp-str-test
  (testing "basic date"
    (is (= {:year 2024 :month 2 :day 29
            :day-name nil :hour nil :minute nil
            :end-hour nil :end-minute nil
            :repeater nil :warning nil}
           (sut/parse-timestamp-str "2024-02-29"))))

  (testing "date with day name"
    (is (= {:year 2024 :month 2 :day 29
            :day-name "Thu" :hour nil :minute nil
            :end-hour nil :end-minute nil
            :repeater nil :warning nil}
           (sut/parse-timestamp-str "2024-02-29 Thu"))))

  (testing "date with time"
    (is (= {:year 2024 :month 2 :day 29
            :day-name "Thu" :hour 10 :minute 30
            :end-hour nil :end-minute nil
            :repeater nil :warning nil}
           (sut/parse-timestamp-str "2024-02-29 Thu 10:30"))))

  (testing "date with time range"
    (is (= {:year 2024 :month 2 :day 29
            :day-name "Thu" :hour 10 :minute 30
            :end-hour 11 :end-minute 0
            :repeater nil :warning nil}
           (sut/parse-timestamp-str "2024-02-29 Thu 10:30-11:00"))))

  (testing "date with repeater"
    (is (= {:year 2024 :month 2 :day 29
            :day-name "Thu" :hour nil :minute nil
            :end-hour nil :end-minute nil
            :repeater "+1w" :warning nil}
           (sut/parse-timestamp-str "2024-02-29 Thu +1w"))))

  (testing "date with warning"
    (is (= {:year 2024 :month 2 :day 29
            :day-name "Thu" :hour nil :minute nil
            :end-hour nil :end-minute nil
            :repeater nil :warning "-3d"}
           (sut/parse-timestamp-str "2024-02-29 Thu -3d"))))

  (testing "nil and empty"
    (is (nil? (sut/parse-timestamp-str nil)))
    (is (nil? (sut/parse-timestamp-str "")))))

(deftest timestamp-compare-test
  (testing "equal timestamps"
    (is (= 0 (sut/timestamp-compare
              (sut/parse-timestamp-str "2024-02-29")
              (sut/parse-timestamp-str "2024-02-29")))))

  (testing "earlier year"
    (is (= -1 (sut/timestamp-compare
               (sut/parse-timestamp-str "2023-02-29")
               (sut/parse-timestamp-str "2024-02-29")))))

  (testing "later month"
    (is (= 1 (sut/timestamp-compare
              (sut/parse-timestamp-str "2024-03-01")
              (sut/parse-timestamp-str "2024-02-28")))))

  (testing "time comparison"
    (is (= -1 (sut/timestamp-compare
               (sut/parse-timestamp-str "2024-02-29 Thu 09:00")
               (sut/parse-timestamp-str "2024-02-29 Thu 10:30"))))))

(deftest parse-timestamp-token-test
  (testing "active timestamp token"
    (is (= {:type :active
            :from {:year 2024 :month 2 :day 29
                   :day-name "Thu" :hour nil :minute nil
                   :end-hour nil :end-minute nil
                   :repeater nil :warning nil}
            :to nil}
           (sut/parse-timestamp-token [:timestamp "2024-02-29 Thu"]))))

  (testing "inactive timestamp token"
    (is (= {:type :inactive
            :from {:year 2024 :month 2 :day 29
                   :day-name "Thu" :hour nil :minute nil
                   :end-hour nil :end-minute nil
                   :repeater nil :warning nil}
            :to nil}
           (sut/parse-timestamp-token [:inactive-timestamp "2024-02-29 Thu"]))))

  (testing "timestamp range token"
    (let [result (sut/parse-timestamp-token
                  [:timestamp-range {:start "2024-02-29 Thu"
                                     :end "2024-03-01 Fri"}])]
      (is (= :active (:type result)))
      (is (= 2024 (get-in result [:from :year])))
      (is (= 3 (get-in result [:to :month])))))

  (testing "with custom parse-date"
    (let [result (sut/parse-timestamp-token
                  [:timestamp "2024-02-29 Thu"]
                  :parse-date (fn [{:keys [year month day]}]
                                [year month day]))]
      (is (= {:type :active :from [2024 2 29] :to nil}
             result))))

  (testing "non-timestamp token returns nil"
    (is (nil? (sut/parse-timestamp-token [:bold "text"])))))

(deftest timestamp->date-test
  (testing "converts with custom function"
    (let [ts (sut/parse-timestamp-str "2024-02-29 Thu")
          result (sut/timestamp->date ts (fn [{:keys [year month day]}]
                                           {:date [year month day]}))]
      (is (= {:date [2024 2 29]} result))))

  (testing "nil timestamp"
    (is (nil? (sut/timestamp->date nil identity)))))
