(ns org-mode.writer.string.blocks.clock.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [org-mode.parser.blocks.clock.tags :as tags]
   [org-mode.writer.string.blocks.clock.core :as w]))

(deftest clock-writer
  (testing "writes clock line"
    (is (= "CLOCK: 10:00-11:00\n"
           (w/token->string [tags/clock ["10:00-11:00"]])))))

