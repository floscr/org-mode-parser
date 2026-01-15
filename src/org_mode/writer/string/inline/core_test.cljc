(ns org-mode.writer.string.inline.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [org-mode.parser.inline.core :as p-inline]
   [org-mode.writer.string.inline.core :as w-inline]))

(deftest roundtrip-inline-basic
  (testing "basic styles and links"
    (let [s "*bold* and /italic/ with [[https://x][y]]"
          tokens (p-inline/parse s)]
      (is (= s (w-inline/tokens->string tokens))))))

(deftest timestamps-roundtrip
  (testing "timestamp range"
    (let [s "<2024-01-01 Mon>--<2024-01-02 Tue>"
          tokens (p-inline/parse s)]
      (is (= s (w-inline/tokens->string tokens))))))

