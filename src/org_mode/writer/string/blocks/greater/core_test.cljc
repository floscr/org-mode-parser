(ns org-mode.writer.string.blocks.greater.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [org-mode.writer.string.blocks.greater.core :as w]))

(deftest greater-writer
  (testing "writes src block"
    (is (= "#+BEGIN_SRC elisp\n(message \"hi\")\n#+END_SRC\n"
           (w/token->string [:block {:type "src" :begin "#+BEGIN_SRC" :end "#+END_SRC" :args [" elisp"] :content "(message \"hi\")"}])))))
