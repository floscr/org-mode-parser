(ns org-mode.writer.hiccup.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [org-mode.parser.blocks.tags :as blk-tags]
   [org-mode.writer.hiccup.core :as sut]))

(deftest document->hiccup-renders-body-and-headings
  (testing "document rendering stitches together body blocks and headings"
    (let [doc {:body [[blk-tags/text ["Intro" " line"]]]
               :headers [{:level 1
                          :title ["Heading"]
                          :body [[blk-tags/text ["Nested"]]]}]}]
      (is (= [:article
              [:p "Intro line"]
              [:section {:data-level 1}
               [:h1 "Heading"]
               [:p "Nested"]]]
             (sut/document->hiccup doc))))))

(deftest heading-levels-are-capped
  (testing "headings beyond h6 are capped"
    (let [doc {:body []
               :headers [{:level 7
                          :title ["Deep"]
                          :body []}]}]
      (is (= [:article
              [:section {:data-level 7}
               [:h6 "Deep"]]]
             (sut/document->hiccup doc))))))
