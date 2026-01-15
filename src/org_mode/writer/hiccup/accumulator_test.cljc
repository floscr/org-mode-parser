(ns org-mode.writer.hiccup.accumulator-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [org-mode.writer.hiccup.accumulator :as sut]))

(deftest accumulate-merges-like-tags
  (testing "table rows are merged under a single table node"
    (let [nodes [[:table [:tr [:td "a"]]]
                 [:table [:tr [:td "b"]]]]]
      (is (= [[:table [:tr [:td "a"]] [:tr [:td "b"]]]]
             (reduce #(sut/accumulate %1 %2 sut/default-conjoin?) [] nodes))))))

(deftest accumulate-keeps-different-attrs-separate
  (testing "ul nodes with different marker metadata are not merged"
    (let [nodes [[:ul {:data-marker "-"} [:li "one"]]
                 [:ul {:data-marker "*"} [:li "two"]]]]
      (is (= nodes
             (reduce #(sut/accumulate %1 %2 sut/default-conjoin?) [] nodes))))))
