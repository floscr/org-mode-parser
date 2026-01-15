(ns org-mode.parser.blocks.greater.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [org-mode.parser.blocks.greater.core :as greater]
   [strojure.parsesso.parser :as p]))

(deftest center-block-test
  (testing "Basic center block"
    (is (= (p/parse greater/parser "#+BEGIN_CENTER
This is centered
#+END_CENTER
")
           [:block {:type "center"
                    :begin "#+BEGIN_CENTER"
                    :end "#+END_CENTER"
                    :args nil
                    :content '("This" " " "is" " " "centered")}])))

  (testing "Center block with inline formatting"
    (is (= (p/parse greater/parser "#+BEGIN_CENTER
*bold* and /italic/
#+END_CENTER
")
           [:block {:type "center"
                    :begin "#+BEGIN_CENTER"
                    :end "#+END_CENTER"
                    :args nil
                    :content '([:bold "bold"] " " "and" " " [:italic "italic"])}])))

  (testing "Empty center block"
    (is (= (p/parse greater/parser "#+BEGIN_CENTER

#+END_CENTER
")
           [:block {:type "center"
                    :begin "#+BEGIN_CENTER"
                    :end "#+END_CENTER"
                    :args nil
                    :content nil}]))))

(deftest quote-block-test
  (testing "Basic quote block"
    (is (= (p/parse greater/parser "#+BEGIN_QUOTE
A famous quote here
#+END_QUOTE
")
           [:block {:type "quote"
                    :begin "#+BEGIN_QUOTE"
                    :end "#+END_QUOTE"
                    :args nil
                    :content '("A" " " "famous" " " "quote" " " "here")}])))

  (testing "Quote block with single line"
    (is (= (p/parse greater/parser "#+BEGIN_QUOTE
To be or not to be
#+END_QUOTE
")
           [:block {:type "quote"
                    :begin "#+BEGIN_QUOTE"
                    :end "#+END_QUOTE"
                    :args nil
                    :content '("To" " " "be" " " "or" " " "not" " " "to" " " "be")}]))))

(deftest verse-block-test
  (testing "Basic verse block preserves raw content"
    (is (= (p/parse greater/parser "#+BEGIN_VERSE
Roses are red
Violets are blue
#+END_VERSE
")
           [:block {:type "verse"
                    :begin "#+BEGIN_VERSE"
                    :end "#+END_VERSE"
                    :args nil
                    :content "Roses are red\nViolets are blue"}])))

  (testing "Verse block preserves indentation"
    (is (= (p/parse greater/parser "#+BEGIN_VERSE
  Indented line
    More indented
#+END_VERSE
")
           [:block {:type "verse"
                    :begin "#+BEGIN_VERSE"
                    :end "#+END_VERSE"
                    :args nil
                    :content "  Indented line\n    More indented"}]))))

(deftest comment-block-test
  (testing "Basic comment block"
    (is (= (p/parse greater/parser "#+BEGIN_COMMENT
This won't be exported
#+END_COMMENT
")
           [:block {:type "comment"
                    :begin "#+BEGIN_COMMENT"
                    :end "#+END_COMMENT"
                    :args nil
                    :content "This won't be exported"}])))

  (testing "Comment block doesn't parse inline markup"
    (is (= (p/parse greater/parser "#+BEGIN_COMMENT
*not bold* and /not italic/
* not a heading
#+END_COMMENT
")
           [:block {:type "comment"
                    :begin "#+BEGIN_COMMENT"
                    :end "#+END_COMMENT"
                    :args nil
                    :content "*not bold* and /not italic/\n* not a heading"}]))))

(deftest example-block-test
  (testing "Basic example block"
    (is (= (p/parse greater/parser "#+BEGIN_EXAMPLE
Some example text
#+END_EXAMPLE
")
           [:block {:type "example"
                    :begin "#+BEGIN_EXAMPLE"
                    :end "#+END_EXAMPLE"
                    :args nil
                    :content "Some example text"}])))

  (testing "Example block with code-like content"
    (is (= (p/parse greater/parser "#+BEGIN_EXAMPLE
(defn foo []
  :bar)
#+END_EXAMPLE
")
           [:block {:type "example"
                    :begin "#+BEGIN_EXAMPLE"
                    :end "#+END_EXAMPLE"
                    :args nil
                    :content "(defn foo []\n  :bar)"}]))))

(deftest export-block-test
  (testing "HTML export block"
    (is (= (p/parse greater/parser "#+BEGIN_EXPORT html
<div class=\"special\">
  HTML content
</div>
#+END_EXPORT
")
           [:block {:type "export"
                    :begin "#+BEGIN_EXPORT"
                    :end "#+END_EXPORT"
                    :args '(" " "html")
                    :content "<div class=\"special\">\n  HTML content\n</div>"}])))

  (testing "LaTeX export block"
    (is (= (p/parse greater/parser "#+BEGIN_EXPORT latex
\\begin{equation}
  E = mc^2
\\end{equation}
#+END_EXPORT
")
           [:block {:type "export"
                    :begin "#+BEGIN_EXPORT"
                    :end "#+END_EXPORT"
                    :args '(" " "latex")
                    :content "\\begin{equation}\n  E = mc^2\n\\end{equation}"}]))))

(deftest src-block-test
  (testing "Basic src block with language"
    (is (= (p/parse greater/parser "#+BEGIN_SRC clojure
(defn foo [])
#+END_SRC
")
           [:block {:type "src"
                    :begin "#+BEGIN_SRC"
                    :end "#+END_SRC"
                    :args '(" " "clojure")
                    :content "(defn foo [])"}])))

  (testing "Src block without language"
    (is (= (p/parse greater/parser "#+BEGIN_SRC
some code
#+END_SRC
")
           [:block {:type "src"
                    :begin "#+BEGIN_SRC"
                    :end "#+END_SRC"
                    :args nil
                    :content "some code"}])))

  (testing "Src block with multiple lines"
    (is (= (p/parse greater/parser "#+BEGIN_SRC javascript
function hello() {
  console.log('world');
}
#+END_SRC
")
           [:block {:type "src"
                    :begin "#+BEGIN_SRC"
                    :end "#+END_SRC"
                    :args '(" " "javascript")
                    :content "function hello() {\n  console.log('world');\n}"}])))

  (testing "Src block with language and parameters"
    (is (= (p/parse greater/parser "#+BEGIN_SRC clojure :results output
(println \"test\")
#+END_SRC
")
           [:block {:type "src"
                    :begin "#+BEGIN_SRC"
                    :end "#+END_SRC"
                    :args '(" " "clojure" " " ":results" " " "output")
                    :content "(println \"test\")"}]))))

(deftest special-block-test
  (testing "Custom named block"
    (is (= (p/parse greater/parser "#+BEGIN_note
This is a note
#+END_note
")
           [:block {:type "note"
                    :begin "#+BEGIN_note"
                    :end "#+END_note"
                    :args nil
                    :content '("This" " " "is" " " "a" " " "note")}])))

  (testing "Custom block with arguments"
    (is (= (p/parse greater/parser "#+BEGIN_warning :level high
Danger ahead!
#+END_warning
")
           [:block {:type "warning"
                    :begin "#+BEGIN_warning"
                    :end "#+END_warning"
                    :args '(" " ":level" " " "high")
                    :content '("Danger" " " "ahead!")}])))

  (testing "Custom block with inline formatting"
    (is (= (p/parse greater/parser "#+BEGIN_SIDEBAR
*Important* side content
#+END_SIDEBAR
")
           [:block {:type "sidebar"
                    :begin "#+BEGIN_SIDEBAR"
                    :end "#+END_SIDEBAR"
                    :args nil
                    :content '([:bold "Important"] " " "side" " " "content")}]))))

(deftest case-insensitive-test
  (testing "Block names are case insensitive"
    (is (some? (p/parse greater/parser "#+begin_center
text
#+end_center
")))
    (is (some? (p/parse greater/parser "#+Begin_Quote
text
#+End_Quote
")))
    (is (some? (p/parse greater/parser "#+BEGIN_VERSE
text
#+END_VERSE
")))))
