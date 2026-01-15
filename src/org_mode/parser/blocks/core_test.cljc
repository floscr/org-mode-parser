(ns org-mode.parser.blocks.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [org-mode.parser.blocks.core :as blocks]
   [org-mode.parser.blocks.tags :refer [newlines text]]
   [org-mode.parser.inline.tags :refer [bold]]))

(deftest newline-test
  (testing "Newline parser"
    (is (= (blocks/parse "\n") [[newlines 1]]))
    (is (= (blocks/parse "\n\n") [[newlines 2]]))))

(deftest paragraph-test
  (testing "Simple paragraph parser"
    (is (= (blocks/parse "Hello

*world*")
           [[text ["Hello"]]
            [newlines 1]
            [text [[bold "world"]]]]))))

(deftest src-block-test
  (testing "Basic src block with language"
    (is (= (blocks/parse "#+begin_SRC clojure
(defn foo [])
#+END_SRC
")
           [[:block {:type "src"
                     :begin "#+BEGIN_SRC"
                     :args '(" " "clojure")
                     :content "(defn foo [])"
                     :end "#+END_SRC"}]])))

  (testing "Src block without language"
    (is (= (blocks/parse "#+BEGIN_SRC
some code
#+END_SRC
")
           [[:block {:type "src"
                     :begin "#+BEGIN_SRC"
                     :args nil
                     :content "some code"
                     :end "#+END_SRC"}]])))

  (testing "Src block with multiple lines"
    (is (= (blocks/parse "#+BEGIN_SRC javascript
function hello() {
  console.log('world');
}
#+END_SRC
")
           [[:block {:type "src"
                     :begin "#+BEGIN_SRC"
                     :args '(" " "javascript")
                     :content "function hello() {\n  console.log('world');\n}"
                     :end "#+END_SRC"}]])))

  (testing "Empty src block"
    (is (= (blocks/parse "#+BEGIN_SRC python

#+END_SRC
")
           [[:block {:type "src"
                     :begin "#+BEGIN_SRC"
                     :args '(" " "python")
                     :content ""
                     :end "#+END_SRC"}]])))

  (testing "Src block with language and parameters"
    (is (= (blocks/parse "#+BEGIN_SRC clojure :results output
(println \"test\")
#+END_SRC
")
           [[:block {:type "src"
                     :begin "#+BEGIN_SRC"
                     :args '(" " "clojure" " " ":results" " " "output")
                     :content "(println \"test\")"
                     :end "#+END_SRC"}]]))))

(deftest keyword-test
  (testing "Single keyword line"
    (is (= (blocks/parse "#+TITLE: My Document")
           [[:keyword {:key "TITLE" :value "My Document"}]])))

  (testing "Multiple keyword lines"
    (is (= (blocks/parse "#+TITLE: My Document
#+AUTHOR: John Doe
#+DATE: 2024-01-01
")
           [[:keyword {:key "TITLE" :value "My Document"}]
            [:keyword {:key "AUTHOR" :value "John Doe"}]
            [:keyword {:key "DATE" :value "2024-01-01"}]])))

  (testing "Keywords with different cases"
    (is (= (blocks/parse "#+title: lowercase
#+AUTHOR: UPPERCASE
#+OpTiOnS: MiXeD
")
           [[:keyword {:key "TITLE" :value "lowercase"}]
            [:keyword {:key "AUTHOR" :value "UPPERCASE"}]
            [:keyword {:key "OPTIONS" :value "MiXeD"}]])))

  (testing "Keywords with underscores and hyphens"
    (is (= (blocks/parse "#+CUSTOM_ID: my-section
#+STARTUP-FOLDED: show-all
")
           [[:keyword {:key "CUSTOM_ID" :value "my-section"}]
            [:keyword {:key "STARTUP-FOLDED" :value "show-all"}]])))

  (testing "Keyword with empty value"
    (is (= (blocks/parse "#+TITLE:")
           [[:keyword {:key "TITLE" :value ""}]])))

  (testing "Keywords mixed with content"
    (is (= (blocks/parse "#+TITLE: Document

Some text here

#+AUTHOR: John
")
           [[:keyword {:key "TITLE" :value "Document"}]
            [newlines 1]
            [text ["Some" " " "text" " " "here"]]
            [newlines 1]
            [:keyword {:key "AUTHOR" :value "John"}]])))

  (testing "Keywords before src block"
    (is (= (blocks/parse "#+TITLE: Code Example
#+BEGIN_SRC clojure
(+ 1 2)
#+END_SRC
")
           [[:keyword {:key "TITLE" :value "Code Example"}]
            [:block {:type "src"
                     :begin "#+BEGIN_SRC"
                     :args '(" " "clojure")
                     :content "(+ 1 2)"
                     :end "#+END_SRC"}]]))))

(deftest comment-test
  (testing "Simple comment line"
    (is (= (blocks/parse "# This is a comment")
           [[:comment ["This" " " "is" " " "a" " " "comment"]]]))))

(deftest horizontal-rule-test
  (testing "Simple horizontal rule"
    (is (= (blocks/parse "-----")
           [[:horizontal-rule 5]])))

  (testing "Horizontal rule between content"
    (is (= (blocks/parse "Before text

-----

After text")
           [[text ["Before" " " "text"]]
            [newlines 1]
            [:horizontal-rule 5]
            [newlines 1]
            [text ["After" " " "text"]]])))

  (testing "Multiple horizontal rules"
    (is (= (blocks/parse "-----\n\n----------")
           [[:horizontal-rule 5]
            [newlines 1]
            [:horizontal-rule 10]]))))
