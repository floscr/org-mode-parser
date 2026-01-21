(ns org-mode.parser.blocks.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [org-mode.parser.blocks.core :as blocks]
   [org-mode.parser.blocks.tags :refer [newlines text]]
   [org-mode.parser.inline.tags :refer [bold]]))

(deftest newline-test
  (testing "Newline parser within content"
    ;; Pure newline input is treated as empty
    ;; Newlines are tracked between content
    (is (= (blocks/parse "text\n\nmore")
           [[text ["text"]]
            [newlines 1]
            [text ["more"]]]))))

(deftest paragraph-test
  (testing "Simple paragraph parser"
    (is (= (blocks/parse "Hello\n\n*world*")
           [[text ["Hello"]]
            [newlines 1]
            [text [[bold "world"]]]]))))

(deftest src-block-test
  (testing "Basic src block with language"
    (is (= (blocks/parse "#+begin_SRC clojure\n(defn foo [])\n#+END_SRC\n")
           [[:block {:type "src"
                     :begin "#+BEGIN_SRC"
                     :args [" " "clojure"]
                     :content "(defn foo [])"
                     :end "#+END_SRC"}]])))

  (testing "Src block without language"
    (is (= (blocks/parse "#+BEGIN_SRC\nsome code\n#+END_SRC\n")
           [[:block {:type "src"
                     :begin "#+BEGIN_SRC"
                     :args nil
                     :content "some code"
                     :end "#+END_SRC"}]])))

  (testing "Src block with multiple lines"
    (is (= (blocks/parse "#+BEGIN_SRC javascript\nfunction hello() {\n  console.log('world');\n}\n#+END_SRC\n")
           [[:block {:type "src"
                     :begin "#+BEGIN_SRC"
                     :args [" " "javascript"]
                     :content "function hello() {\n  console.log('world');\n}"
                     :end "#+END_SRC"}]])))

  (testing "Empty src block"
    (is (= (blocks/parse "#+BEGIN_SRC python\n\n#+END_SRC\n")
           [[:block {:type "src"
                     :begin "#+BEGIN_SRC"
                     :args [" " "python"]
                     :content ""
                     :end "#+END_SRC"}]])))

  (testing "Src block with language and parameters"
    (is (= (blocks/parse "#+BEGIN_SRC clojure :results output\n(println \"test\")\n#+END_SRC\n")
           [[:block {:type "src"
                     :begin "#+BEGIN_SRC"
                     :args [" " "clojure" " " ":results" " " "output"]
                     :content "(println \"test\")"
                     :end "#+END_SRC"}]]))))

(deftest keyword-test
  (testing "Single keyword line"
    (is (= (blocks/parse "#+TITLE: My Document")
           [[:keyword {:key "TITLE" :value "My Document"}]])))

  (testing "Multiple keyword lines"
    (is (= (blocks/parse "#+TITLE: My Document\n#+AUTHOR: John Doe\n#+DATE: 2024-01-01\n")
           [[:keyword {:key "TITLE" :value "My Document"}]
            [:keyword {:key "AUTHOR" :value "John Doe"}]
            [:keyword {:key "DATE" :value "2024-01-01"}]])))

  (testing "Keywords with different cases"
    (is (= (blocks/parse "#+title: lowercase\n#+AUTHOR: UPPERCASE\n#+OpTiOnS: MiXeD\n")
           [[:keyword {:key "TITLE" :value "lowercase"}]
            [:keyword {:key "AUTHOR" :value "UPPERCASE"}]
            [:keyword {:key "OPTIONS" :value "MiXeD"}]])))

  (testing "Keywords with underscores and hyphens"
    (is (= (blocks/parse "#+CUSTOM_ID: my-section\n#+STARTUP-FOLDED: show-all\n")
           [[:keyword {:key "CUSTOM_ID" :value "my-section"}]
            [:keyword {:key "STARTUP-FOLDED" :value "show-all"}]])))

  (testing "Keyword with empty value"
    (is (= (blocks/parse "#+TITLE:")
           [[:keyword {:key "TITLE" :value ""}]])))

  (testing "Keywords mixed with content"
    (is (= (blocks/parse "#+TITLE: Document\n\nSome text here\n\n#+AUTHOR: John\n")
           [[:keyword {:key "TITLE" :value "Document"}]
            [newlines 1]
            [text ["Some" " " "text" " " "here"]]
            [newlines 1]
            [:keyword {:key "AUTHOR" :value "John"}]])))

  (testing "Keywords before src block"
    (is (= (blocks/parse "#+TITLE: Code Example\n#+BEGIN_SRC clojure\n(+ 1 2)\n#+END_SRC\n")
           [[:keyword {:key "TITLE" :value "Code Example"}]
            [:block {:type "src"
                     :begin "#+BEGIN_SRC"
                     :args [" " "clojure"]
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
    (is (= (blocks/parse "Before text\n\n-----\n\nAfter text")
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

(deftest table-test
  (testing "Simple table row"
    ;; Column content is parsed as inline tokens
    (is (= (blocks/parse "| cell1 | cell2 |\n")
           [[:table-row [[:column [" " "cell1" " "]] [:column [" " "cell2" " "]]]]])))

  (testing "Table separator"
    (is (= (blocks/parse "|---+---|\n")
           [[:table-separator [[:column [\- \- \-]] [:column [\- \- \-]]]]]))))

(deftest list-test
  (testing "Unordered list with dash"
    ;; Parser always includes :checkbox key (nil when not present)
    (is (= (blocks/parse "- item text\n")
           [[:list {:indent 0 :marker [:unordered \-] :checkbox nil :tokens ["item" " " "text"]}]])))

  (testing "Unordered list with checkbox"
    (is (= (blocks/parse "- [ ] todo item\n")
           [[:list {:indent 0 :marker [:unordered \-] :checkbox \space :tokens ["todo" " " "item"]}]])))

  (testing "Ordered list"
    ;; Marker digits are in a list (sequence) format
    (is (= (first (blocks/parse "1. first item\n"))
           [:list {:indent 0 :marker [:ordered {:marker '(\1) :delimiter \.}] :checkbox nil :tokens ["first" " " "item"]}]))))

(deftest planning-test
  (testing "Scheduled planning line"
    (is (= (blocks/parse "SCHEDULED: <2024-01-01 Mon>\n")
           [[:scheduled [[:timestamp "2024-01-01 Mon"]]]])))

  (testing "Deadline planning line"
    (is (= (blocks/parse "DEADLINE: <2024-01-01 Mon>\n")
           [[:deadline [[:timestamp "2024-01-01 Mon"]]]]))))

(deftest drawer-test
  (testing "Property drawer"
    ;; Property values are parsed as inline tokens (preserving leading space)
    (is (= (blocks/parse ":PROPERTIES:\n:ID: abc123\n:END:\n")
           [[:drawer-start "PROPERTIES"]
            [:property {:key "ID" :value [" " "abc123"]}]
            [:drawer-end "END"]]))))

(deftest clock-test
  (testing "Clock line with single timestamp"
    (is (= (blocks/parse "CLOCK: [2024-01-01 Mon 09:00]\n")
           [[:clock [[:inactive-timestamp "2024-01-01 Mon 09:00"]]]])))

  (testing "Clock line with timestamp range"
    (let [result (first (blocks/parse "CLOCK: [2024-01-01 Mon 09:00]--[2024-01-01 Mon 17:00]\n"))]
      (is (= :clock (first result)))
      (is (= :inactive-timestamp-range (first (first (second result)))))))

  (testing "Clock line case insensitive - lowercase"
    (let [result (first (blocks/parse "clock: [2024-01-01 Mon 09:00]\n"))]
      (is (= :clock (first result)))))

  (testing "Clock line case insensitive - mixed case"
    (let [result (first (blocks/parse "ClOcK: [2024-01-01 Mon 09:00]\n"))]
      (is (= :clock (first result)))))

  (testing "Clock line with duration"
    (let [result (first (blocks/parse "CLOCK: [2024-01-01 Mon 09:00]--[2024-01-01 Mon 17:00] =>  8:00\n"))]
      (is (= :clock (first result)))))

  (testing "Clock line with extra whitespace after colon"
    (let [result (first (blocks/parse "CLOCK:   [2024-01-01 Mon 09:00]\n"))]
      (is (= :clock (first result)))))

  (testing "Clock line with time only"
    (let [result (first (blocks/parse "CLOCK: [09:00]--[17:00]\n"))]
      (is (= :clock (first result))))))

;; Greater block tests (from deleted greater/core_test.cljc)

(deftest quote-block-test
  (testing "Basic quote block"
    (let [result (first (blocks/parse "#+BEGIN_QUOTE\nA famous quote here\n#+END_QUOTE\n"))]
      (is (= :block (first result)))
      (is (= "quote" (:type (second result))))
      (is (= "#+BEGIN_QUOTE" (:begin (second result))))
      (is (= "#+END_QUOTE" (:end (second result)))))))

(deftest verse-block-test
  (testing "Verse block preserves raw content"
    (let [result (first (blocks/parse "#+BEGIN_VERSE\nRoses are red\nViolets are blue\n#+END_VERSE\n"))]
      (is (= :block (first result)))
      (is (= "verse" (:type (second result))))
      (is (= "Roses are red\nViolets are blue" (:content (second result)))))))

(deftest example-block-test
  (testing "Example block preserves content"
    (let [result (first (blocks/parse "#+BEGIN_EXAMPLE\nSome example text\n#+END_EXAMPLE\n"))]
      (is (= :block (first result)))
      (is (= "example" (:type (second result))))
      (is (= "Some example text" (:content (second result)))))))

(deftest export-block-test
  (testing "HTML export block"
    (let [result (first (blocks/parse "#+BEGIN_EXPORT html\n<div>content</div>\n#+END_EXPORT\n"))]
      (is (= :block (first result)))
      (is (= "export" (:type (second result))))
      (is (= "<div>content</div>" (:content (second result)))))))

(deftest comment-block-test
  (testing "Comment block preserves raw content"
    (let [result (first (blocks/parse "#+BEGIN_COMMENT\nThis won't be exported\n#+END_COMMENT\n"))]
      (is (= :block (first result)))
      (is (= "comment" (:type (second result))))
      (is (= "This won't be exported" (:content (second result)))))))

(deftest center-block-test
  (testing "Center block with content"
    (let [result (first (blocks/parse "#+BEGIN_CENTER\nCentered text\n#+END_CENTER\n"))]
      (is (= :block (first result)))
      (is (= "center" (:type (second result)))))))

(deftest custom-block-test
  (testing "Custom named block"
    (let [result (first (blocks/parse "#+BEGIN_note\nThis is a note\n#+END_note\n"))]
      (is (= :block (first result)))
      (is (= "note" (:type (second result))))))

  (testing "Custom block with arguments"
    (let [result (first (blocks/parse "#+BEGIN_warning :level high\nDanger!\n#+END_warning\n"))]
      (is (= :block (first result)))
      (is (= "warning" (:type (second result))))))

  (testing "Custom block with inline formatting"
    (let [result (first (blocks/parse "#+BEGIN_SIDEBAR\n*Important* content\n#+END_SIDEBAR\n"))]
      (is (= :block (first result)))
      (is (= "sidebar" (:type (second result)))))))

(deftest block-case-insensitive-test
  (testing "Block names are case insensitive - lowercase"
    (let [result (first (blocks/parse "#+begin_center\ntext\n#+end_center\n"))]
      (is (= :block (first result)))
      (is (= "center" (:type (second result))))))

  (testing "Block names are case insensitive - mixed case"
    (let [result (first (blocks/parse "#+Begin_Quote\ntext\n#+End_Quote\n"))]
      (is (= :block (first result)))
      (is (= "quote" (:type (second result)))))))

;; Additional comment tests (from deleted comment/core_test.cljc)

(deftest comment-line-variations-test
  (testing "Comment with empty content"
    (is (= (blocks/parse "# \n")
           [[:comment []]])))

  (testing "Comment is not confused with keyword"
    ;; #+TITLE is a keyword, not a comment
    (is (= :keyword (first (first (blocks/parse "#+TITLE: test"))))))

  (testing "Hash without space is not a comment"
    ;; #NoSpace should be treated as text, not a comment
    (let [result (first (blocks/parse "#NoSpace\n"))]
      (is (not= :comment (first result)))))

  (testing "Comment not confused with BEGIN_SRC"
    ;; #+BEGIN_SRC is a block start, not a comment
    (let [result (first (blocks/parse "#+BEGIN_SRC\ncode\n#+END_SRC\n"))]
      (is (= :block (first result))))))

;; Additional list tests (from deleted list/core_test.cljc)

(deftest list-variations-test
  (testing "Unordered list with plus"
    (let [result (first (blocks/parse "+ item\n"))]
      (is (= :list (first result)))
      (is (= [:unordered \+] (:marker (second result))))))

  (testing "Unordered list with asterisk"
    (let [result (first (blocks/parse "* item text\n"))]
      (is (= :list (first result)))
      (is (= [:unordered \*] (:marker (second result))))))

  (testing "Checked checkbox with X"
    (let [result (first (blocks/parse "- [X] done\n"))]
      (is (= \X (:checkbox (second result))))))

  (testing "Checked checkbox with lowercase x"
    (let [result (first (blocks/parse "- [x] done\n"))]
      (is (= \x (:checkbox (second result))))))

  (testing "Partial checkbox with dash"
    (let [result (first (blocks/parse "- [-] partial\n"))]
      (is (= \- (:checkbox (second result))))))

  (testing "Indented list item"
    (let [result (first (blocks/parse "  - nested\n"))]
      (is (= 2 (:indent (second result))))))

  (testing "Ordered list with parenthesis"
    (let [result (first (blocks/parse "1) item\n"))]
      (is (= :ordered (first (:marker (second result)))))
      (is (= \) (:delimiter (second (:marker (second result))))))))

  (testing "Multi-digit ordered list"
    (let [result (first (blocks/parse "42. answer\n"))]
      (is (= :list (first result)))
      (is (= :ordered (first (:marker (second result)))))))

  (testing "Indented ordered list"
    (let [result (first (blocks/parse "  1. nested\n"))]
      (is (= 2 (:indent (second result))))))

  (testing "Checkbox in ordered list"
    (let [result (first (blocks/parse "1. [ ] todo\n"))]
      (is (= \space (:checkbox (second result))))))

  (testing "Indented checkbox"
    (let [result (first (blocks/parse "  - [X] nested done\n"))]
      (is (= 2 (:indent (second result))))
      (is (= \X (:checkbox (second result))))))

  (testing "List with inline markup"
    (let [result (first (blocks/parse "- *bold* text\n"))]
      (is (= :list (first result)))))

  (testing "Empty list item"
    (let [result (first (blocks/parse "- \n"))]
      (is (= :list (first result)))))

  (testing "List with multiple spaces after marker"
    (let [result (first (blocks/parse "-   item\n"))]
      (is (= :list (first result))))))

;; Additional table tests (from deleted table/core_test.cljc)

(deftest table-variations-test
  (testing "Table row with inline formatting"
    (let [result (first (blocks/parse "| *bold* | /italic/ |\n"))]
      (is (= :table-row (first result)))))

  (testing "Table with multiple columns"
    (let [result (first (blocks/parse "| a | b | c | d |\n"))]
      (is (= 4 (count (second result))))))

  (testing "Full table separator"
    (let [result (first (blocks/parse "|-------+-------|\n"))]
      (is (= :table-separator (first result)))))

  (testing "Table row with empty cells"
    (let [result (first (blocks/parse "|  | |  |\n"))]
      (is (= :table-row (first result)))
      (is (= 3 (count (second result))))))

  (testing "Table row with single cell"
    (let [result (first (blocks/parse "| single |\n"))]
      (is (= :table-row (first result)))
      (is (= 1 (count (second result))))))

  (testing "Simple separator line"
    (let [result (first (blocks/parse "|---+---|\n"))]
      (is (= :table-separator (first result))))))

;; Additional planning tests (from deleted planning/core_test.cljc)

(deftest planning-variations-test
  (testing "CLOSED planning line"
    (is (= :closed (first (first (blocks/parse "CLOSED: [2024-01-01 Mon]\n"))))))

  (testing "Planning with time"
    (let [result (first (blocks/parse "SCHEDULED: <2024-01-01 Mon 09:00>\n"))]
      (is (= :scheduled (first result)))))

  (testing "Planning case insensitive - lowercase scheduled"
    (let [result (first (blocks/parse "scheduled: <2024-01-01 Mon>\n"))]
      (is (= :scheduled (first result)))))

  (testing "Planning case insensitive - lowercase deadline"
    (let [result (first (blocks/parse "deadline: <2024-01-15 Mon>\n"))]
      (is (= :deadline (first result)))))

  (testing "Planning case insensitive - lowercase closed"
    (let [result (first (blocks/parse "closed: [2024-01-01 Mon]\n"))]
      (is (= :closed (first result)))))

  (testing "Scheduled with repeater"
    (let [result (first (blocks/parse "SCHEDULED: <2024-01-01 Mon +1w>\n"))]
      (is (= :scheduled (first result)))))

  (testing "Scheduled with time range"
    (let [result (first (blocks/parse "SCHEDULED: <2024-01-01 Mon 09:00-17:00>\n"))]
      (is (= :scheduled (first result))))))

;; Additional keyword tests (from deleted keyword/core_test.cljc and keyword_line_test.cljc)

(deftest keyword-variations-test
  (testing "Keyword with whitespace before value"
    (is (= (blocks/parse "#+TITLE:    My Title")
           [[:keyword {:key "TITLE" :value "My Title"}]])))

  (testing "Keyword with longer value"
    (is (= (blocks/parse "#+DESCRIPTION: This is a longer description")
           [[:keyword {:key "DESCRIPTION" :value "This is a longer description"}]])))

  (testing "Keyword with numbers in key"
    (is (= (blocks/parse "#+ATTR_HTML: :width 100")
           [[:keyword {:key "ATTR_HTML" :value ":width 100"}]])))

  (testing "Planning keyword with timestamp"
    (let [result (first (blocks/parse "SCHEDULED: <2024-01-01 Mon>\n"))]
      (is (= :scheduled (first result)))))

  (testing "Planning keyword with extra whitespace"
    (let [result (first (blocks/parse "SCHEDULED:   <2024-01-01 Mon>\n"))]
      (is (= :scheduled (first result)))))

  (testing "Planning keyword with bold text"
    (let [result (first (blocks/parse "SCHEDULED: *today* <2024-01-01 Mon>\n"))]
      (is (= :scheduled (first result)))))

  (testing "Planning keyword with mixed content"
    (let [result (first (blocks/parse "SCHEDULED: some <2024-01-01> text\n"))]
      (is (= :scheduled (first result)))))

  (testing "Planning keyword with multiple words"
    (let [result (first (blocks/parse "SCHEDULED: hello world\n"))]
      (is (= :scheduled (first result)))))

  (testing "Planning keyword mixed case - ScHeDuLeD"
    (let [result (first (blocks/parse "ScHeDuLeD: <2024-01-01 Mon>\n"))]
      (is (= :scheduled (first result))))))

;; Additional horizontal rule tests (from deleted horizontal_rule/core_test.cljc)

(deftest horizontal-rule-variations-test
  (testing "Minimum dashes (5)"
    (is (= [:horizontal-rule 5] (first (blocks/parse "-----")))))

  (testing "Many dashes (10)"
    (is (= [:horizontal-rule 10] (first (blocks/parse "----------")))))

  (testing "Many dashes (20)"
    (is (= [:horizontal-rule 20] (first (blocks/parse "--------------------")))))

  (testing "Four dashes is not a horizontal rule"
    ;; Should be treated as text, not horizontal rule
    (let [result (first (blocks/parse "----\n"))]
      (is (not= :horizontal-rule (first result)))))

  (testing "Three dashes is not a horizontal rule"
    (let [result (first (blocks/parse "---\n"))]
      (is (not= :horizontal-rule (first result))))))
