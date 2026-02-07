(ns org-mode.computed.document.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [org-mode.computed.document.core :as sut]
   [org-mode.parser.heading.core :as heading]))

;; Helpers --------------------------------------------------------------------

(defn- parse [s]
  (heading/parse-document s))

;; Tests ----------------------------------------------------------------------

(deftest headings-test
  (testing "returns all headings"
    (let [doc (parse "* A\n** B\n* C")]
      (is (= 3 (count (sut/headings doc))))))

  (testing "with update-heading transform"
    (let [doc (parse "* First\n* Second")
          result (sut/headings doc :update-heading #(assoc % :custom true))]
      (is (every? :custom result)))))

(deftest find-headings-test
  (testing "filters headings by predicate"
    (let [doc (parse "* A\n** B\n* C")]
      (is (= 2 (count (sut/find-headings #(= 1 (:level %)) doc))))
      (is (= 1 (count (sut/find-headings #(= 2 (:level %)) doc)))))))

(deftest headings-at-level-test
  (testing "returns headings at specific level"
    (let [doc (parse "* A\n** B\n*** C\n* D")]
      (is (= 2 (count (sut/headings-at-level 1 doc))))
      (is (= 1 (count (sut/headings-at-level 2 doc))))
      (is (= 1 (count (sut/headings-at-level 3 doc)))))))

(deftest top-level-headings-test
  (testing "returns only level-1 headings"
    (let [doc (parse "* A\n** B\n* C\n** D")]
      (is (= 2 (count (sut/top-level-headings doc)))))))

(deftest multi-document-headings-test
  (testing "collects headings from multiple documents"
    (let [doc1 (parse "* A\n* B")
          doc2 (parse "* C")]
      (is (= 3 (count (sut/multi-document-headings [doc1 doc2]))))))

  (testing "with update-heading receives document context"
    (let [doc1 (parse "* A")
          doc2 (parse "* B")
          result (sut/multi-document-headings
                  [doc1 doc2]
                  :update-heading (fn [doc heading]
                                    (assoc heading :header-count
                                           (count (:headers doc)))))]
      (is (= [1 1] (map :header-count result))))))
