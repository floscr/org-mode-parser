(ns org-mode.writer.string.blocks.drawer.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [org-mode.parser.blocks.drawer.tags :as tags]
   [org-mode.writer.string.blocks.drawer.core :as w]))

(deftest drawer-writer
  (testing "writes drawer start/end and property"
    (is (= ":PROPERTIES:\n"
           (w/token->string [tags/drawer-start "PROPERTIES"])))
    (is (= ":END:\n"
           (w/token->string [tags/drawer-end "END"])))
    (is (= ":FOO: bar\n"
           (w/token->string [tags/property {:key "FOO" :value [" " "bar"]}])))))
