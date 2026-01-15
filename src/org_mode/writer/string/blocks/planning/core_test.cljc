(ns org-mode.writer.string.blocks.planning.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [org-mode.parser.blocks.planning.tags :as tags]
   [org-mode.writer.string.blocks.planning.core :as w]))

(deftest planning-writer
  (testing "writes scheduled line"
    (is (= "SCHEDULED: <2024-01-01 Mon>\n"
           (w/token->string [tags/scheduled [[:timestamp "2024-01-01 Mon"]]])))))

