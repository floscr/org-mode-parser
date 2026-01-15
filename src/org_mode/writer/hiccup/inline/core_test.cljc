(ns org-mode.writer.hiccup.inline.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [org-mode.parser.inline.tags :as tags]
   [org-mode.writer.hiccup.inline.core :as sut]))

(deftest tokens->hiccup-basic
  (testing "converts inline tokens to hiccup"
    (is (= ["Hello " [:strong "world"]]
           (sut/tokens->hiccup ["Hello" " " [tags/bold "world"]])))))

(deftest inline-writer-overrides
  (testing "allows overriding specific inline writers"
    (let [custom {tags/timestamp (fn [_ [_ value]] [:span {:class "ts"} value])}]
      (is (= [[:span {:class "ts"} "2024-01-01"]]
             (sut/tokens->hiccup [[tags/timestamp "2024-01-01"]] {:writers custom}))))))
