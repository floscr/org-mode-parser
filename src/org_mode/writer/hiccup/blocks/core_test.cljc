(ns org-mode.writer.hiccup.blocks.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [org-mode.parser.blocks.drawer.tags :as drawer-tags]
   [org-mode.parser.blocks.table.tags :as table-tags]
   [org-mode.parser.blocks.tags :as blk-tags]
   [org-mode.parser.inline.tags :as inline-tags]
   [org-mode.writer.hiccup.blocks.core :as sut]))

(defn- column [& content]
  [:column content])

(defn- row [& columns]
  [table-tags/table-row columns])

(deftest merges-consecutive-paragraph-lines
  (testing "plain lines collapse into a single paragraph until a blank line"
    (let [tokens [[blk-tags/text ["First" " line"]]
                  [blk-tags/text ["and" " second"]]
                  [blk-tags/newlines 1]
                  [blk-tags/text ["Third"]]]]
      (is (= [[:p "First line and second"]
              [:br]
              [:p "Third"]]
             (sut/tokens->hiccup tokens))))))

(deftest table-rows-accumulate
  (testing "multiple table rows end up under a single table node"
    (let [tokens [(row (column " a "))
                  (row (column " b "))]]
      (is (= [[:table [:tr [:td " a "]] [:tr [:td " b "]]]]
             (sut/tokens->hiccup tokens))))))

(deftest inline-writer-overrides-flow-into-blocks
  (testing "inline overrides are honoured in block rendering"
    (let [tokens [[blk-tags/text ["Hello" " " [inline-tags/bold "world"]]]]
          overrides {inline-tags/bold (fn [render [_ content]]
                                        (into [:b] (render content)))}]
      (is (= [[:p "Hello " [:b "world"]]]
             (sut/tokens->hiccup tokens {:inline-writers overrides}))))))

(deftest block-writer-overrides-apply
  (testing "block writer map can be overridden per tag"
    (let [token (row (column "custom"))
          custom-writers {table-tags/table-row (fn [_ tok] [:custom tok])}]
      (is (= [[:custom token]]
             (sut/tokens->hiccup [token] {:block-writers custom-writers}))))))

(deftest drawers-render-as-details
  (testing "drawer start/end tokens accumulate into a details element"
    (let [tokens [[drawer-tags/drawer-start "PROPERTIES"]
                  [drawer-tags/property {:key "FOO" :value [" bar"]}]
                  [drawer-tags/drawer-end "END"]]]
      (is (= [[:details {:data-type "drawer" :data-name "PROPERTIES"}
               [:summary "PROPERTIES"]
               [:div {:data-type "property" :data-key "FOO"} " bar"]]]
             (sut/tokens->hiccup tokens))))))
