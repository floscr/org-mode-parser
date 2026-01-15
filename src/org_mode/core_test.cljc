(ns org-mode.core-test
  (:require
   #?(:clj [clojure.java.io :as io])
   [clojure.test :refer [deftest is testing]]
   [org-mode.core :as core]
   [org-mode.writer.hiccup.core :as hiccup]
   [org-mode.writer.string.core :as writer]
   [babashka.fs :as fs]))

#?(:clj
   (defonce doc (slurp (io/resource "org_mode/example_docs/roundtrip_example.org"))))

(deftest roundtrip-example-document-file
  (testing "roundtrip for example document read from resources"
    (let [parsed (core/parse doc)
          rendered (writer/document->string parsed)]
      (is (= doc rendered)))))

#?(:clj
   (deftest parallel-parser-matches-sequential
     (testing "parallel parse yields the same structure as sequential parse"
       (is (= (core/parse doc)
              (core/parse-parallel doc))))))

(deftest hiccup-example-document-file
  (testing "generate hiccup from example document"
    (let [parsed (core/parse doc)
          hiccup-result (hiccup/document->hiccup parsed)]
      (is (= hiccup-result
             [:article
              [:meta {:data-type "keyword" :data-key "TITLE", :data-value "Example Roundtrip"}]
              [:meta {:data-type "keyword" :data-key "AUTHOR", :data-value "Org Bot"}]
              [:meta {:data-type "keyword" :data-key "DATE", :data-value "<2024-02-29 Thu>"}]
              [:br]
              [:!--
               ["A" " " "comment" " " "line" " " "that" " " "should" " " "roundtrip" " " "unchanged."]]
              [:p
               "Intro paragraph with "
               [:strong "bold"]
               ", "
               [:em "italic"]
               ", "
               [:code {:data-inline "verbatim"} "code"]
               ", and a "
               [:a {:href "link"} "titled link"]
               ". There is also an inactive timestamp "
               [:time {:data-active false} "2024-02-29 Thu"]
               " and a footnote[fn:1]."]
              [:br]
              [:details {:data-type "drawer" :data-name "PROPERTIES"}
               [:summary "PROPERTIES"]
               [:div {:data-type "property" :data-key "CATEGORY"} " Examples"]]
              [:section
               {:data-level 1}
               [:h1 "Heading One"]
               [:p "Body under heading one."]
               [:ul
                {:data-marker "-"}
                [:li {:data-checkbox " "} "Checkbox item"]
                [:li {:data-checkbox "X"} "Completed checkpoint"]
                [:li "Plain bullet"]]
               [:br]
               [:div
                {:data-type "planning" :data-planning-type :scheduled}
                [:time {:data-active true} "2024-02-29 Thu"]]
               [:details {:data-type "drawer" :data-name "LOGBOOK"}
                [:summary "LOGBOOK"]
                [:ul
                 {:data-marker "-"}
                 [:li
                  "State \"DONE\"       from \"TODO\"       "
                  [:time {:data-active false} "2024-01-01 Mon"]]]]
               [:br]
               [:ol
                {:data-marker "ordered", :data-delimiter "."}
                [:li "Ordered item one"]
                [:li "Ordered item two"]]]
              [:section
               {:data-level 2}
               [:h2 "Subheading A"]
               [:p
                "Child paragraph line referencing "
                [:a {:name "target"} "target"]
                "a target. There is also a stats cookie "
                [:span #:data-org{:stats "range"} "2/5"]
                " and percent cookie "
                [:span #:data-org{:stats "percent"} "33%"]
                "."]
               [:br]
               [:table
                [:tr [:td " Col A "] [:td " Col B "]]
                [:tr
                 {:data-role "separator"}
                 [:td {:data-role "separator"} "-------"]
                 [:td {:data-role "separator"} "-------"]]
                [:tr [:td "  foo  "] [:td "  bar  "]]
                [:tr [:td "  baz  "] [:td "  qux  "]]]
               [:br]
               [:pre
                {:data-type "src", :data-args "clj", :data-render "src"}
                [:code {:data-type "src", :data-language "clj"} "(+ 1 2)"]]]
              [:section
               {:data-level 1}
               [:h1 "TODO Heading Two"]
               [:p "Final section content with a horizontal rule below."]
               [:hr]
               [:p "Planning:"]
               [:div
                {:data-type "planning" :data-planning-type :deadline}
                [:time {:data-active true} "2024-03-15 Fri"]]
               [:br]
               [:p
                "Footnotes: "
                [:sup [:a {:href "#fn-1", :data-ref "1"} "1"]]
                " Footnote definition that should survive roundtrip."]]]))))

  (comment
    (do
      (require '[babashka.process :as sh])
      (let [content (-> (core/parse doc)
                        (hiccup/document->html)
                        (str))
              file (str (fs/create-temp-file {:suffix ".html"}))]
          (spit file content)
          (sh/shell "firefox" file)))))
