(ns org-mode.parser.inline.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [org-mode.parser.inline.core :as inline]
   [org-mode.parser.inline.tags :as tags]
   [org-mode.parser.inline.tags :refer [bold code footnote-def footnote-ref
                                        inactive-timestamp italic link
                                        link-with-title macro raw-link
                                        strike-through target timestamp
                                        underline verbatim]]
   [org-mode.writer.string.inline.core :as w.inline]))

(deftest single-inline-test
  (testing "Single inline parsers"
    (is (= (inline/parse "*bold*") [[bold "bold"]]))
    (is (= (inline/parse "/italic/") [[italic "italic"]]))
    (is (= (inline/parse "_underline_") [[underline "underline"]]))
    (is (= (inline/parse "=verbatim=") [[verbatim "verbatim"]]))
    (is (= (inline/parse "~code~") [[code "code"]]))
    (is (= (inline/parse "+strike-through+") [[strike-through "strike-through"]]))))

(deftest multiple-inline-test
  (testing "Multiple inline parsers"
    (is (= (inline/parse "hello *world*") ["hello" " " [bold "world"]]))
    (is (= (inline/parse "*hello* /world/") [[bold "hello"] " " [italic "world"]]))
    (is (= (inline/parse "*hello* /world/ _underline_") [[bold "hello"] " " [italic "world"] " " [underline "underline"]]))))

(deftest timestamp-test
  (testing "Timestamp parser"
    (is (= (inline/parse "<2024-05-22 Wed>")
           [[timestamp "2024-05-22 Wed"]]))
    (is (= (inline/parse "A timestamp <2024-05-22 Wed>")
           ["A" " " "timestamp" " " [timestamp "2024-05-22 Wed"]]))))

(deftest inactive-timestamp-test
  (testing "Timestamp parser"
    (is (= (inline/parse "[2024-05-22 Wed]")
           [[inactive-timestamp "2024-05-22 Wed"]]))
    (is (= (inline/parse "A timestamp [2024-05-22 Wed]")
           ["A" " " "timestamp" " " [inactive-timestamp "2024-05-22 Wed"]]))))

(deftest link-test
  (testing "Link parser"
    (is (= (inline/parse "[[https://example.com]]")
           [[link "https://example.com"]]))
    (is (= (inline/parse "[[https://example.com][An example]]")
           [[link-with-title {:link "https://example.com" :title "An example"}]]))
    (is (= (inline/parse "A link to [[https://example.com][An example]]")
           ["A" " " "link" " " "to" " " [link-with-title {:link "https://example.com" :title "An example"}]]))))

(deftest target-test
  (testing "Target parser"
    (is (= (inline/parse "<<my-target>>")
           [[target "my-target"]]))
    (is (= (inline/parse "A target <<my-target>>")
           ["A" " " "target" " " [target "my-target"]]))))

(deftest footnote-test
  (testing "Footnote parser"
    (is (= (inline/parse "[fn:1]")
           [[footnote-ref "1"]]))
    (is (= (inline/parse "[fn:1:a footnote definition]")
           [[footnote-def {:name "1" :definition "a footnote definition"}]]))
    (is (= (inline/parse "A footnote [fn:1]")
           ["A" " " "footnote" " " [footnote-ref "1"]]))))

(deftest footnote-word-boundary-test
  (testing "Footnote requires word boundary"
    ;; Footnotes directly attached to words should NOT be parsed as footnote-ref
    ;; (they get collected as word tokens instead)
    (is (= (inline/parse "footnote[fn:1].")
           ["footnote" "[fn:1]."]))
    ;; But with space before, they should be parsed as footnote-ref
    (is (= (inline/parse "footnote [fn:1].")
           ["footnote" " " [footnote-ref "1"] "."]))))

(deftest macro-test
  (testing "Macro parser"
    (is (= (inline/parse "{{{macro_name}}}")
           [[macro {:name "macro_name"}]]))
    (is (= (inline/parse "{{{macro_name(arg1, arg2)}}}")
           [[macro {:name "macro_name" :args ["arg1" "arg2"]}]]))
    (is (= (inline/parse "{{{date}}}")
           [[macro {:name "date"}]]))
    (is (= (inline/parse "A macro {{{title}}} here")
           ["A" " " "macro" " " [macro {:name "title"}] " " "here"]))))

(deftest stats-cookie-test
  (testing "Statistics cookie parser"
    (is (= (inline/parse "[/]")
           [[tags/stats-range-cookie {:from nil :to nil}]]))
    (is (= (inline/parse "[%]")
           [[tags/stats-percent-cookie nil]]))
    (is (= (inline/parse "[2/5]")
           [[tags/stats-range-cookie {:from 2 :to 5}]]))
    (is (= (inline/parse "[40%]")
           [[tags/stats-percent-cookie 40]]))))

(deftest edge-cases-test
  (testing "Newline handling"
    ;; Newlines are preserved as tokens
    (is (= (inline/parse "* Some text\n") ["*" " " "Some" " " "text" "\n"])))
  (testing "Disregard non closing"
    (is (= (inline/parse "+ Some text here") ["+" " " "Some" " " "text" " " "here"])))
  (testing "No empty styled spans"
    ;; Empty delimiters are not parsed as styled text
    (is (= (inline/parse "**") ["*" "*"]))
    (is (= (inline/parse "* *") ["*" " " "*"])))
  (testing "Multiple delimiters"
    ;; Extra delimiters outside the styled span remain as separate tokens
    (is (= (inline/parse "***Multiple Delimiters***") ["*" "*" [bold "Multiple Delimiters"] "*" "*"])))
  (testing "Non greedy delimiters"
    ;; Trailing delimiter after styled span is a separate token
    (is (= (inline/parse "*Non Greedy* Rest*") [[bold "Non Greedy"] " " "Rest" "*"])))
  (testing "Dont parse nested delimiters"
    (is (= (inline/parse "/=No Nested/=") [[italic "=No Nested"] "="]))
    (is (= (inline/parse "=~No Nested~=") [[verbatim "~No Nested~"]]))))

(deftest raw-link-test
  (testing "https URL"
    (is (= (inline/parse "https://example.com")
           [[raw-link "https://example.com"]])))

  (testing "http URL"
    (is (= (inline/parse "http://example.com")
           [[raw-link "http://example.com"]])))

  (testing "URL with path"
    (is (= (inline/parse "https://example.com/foo/bar?q=1#anchor")
           [[raw-link "https://example.com/foo/bar?q=1#anchor"]])))

  (testing "URL in text"
    (is (= (inline/parse "Visit https://example.com for info")
           ["Visit" " " [raw-link "https://example.com"] " " "for" " " "info"])))

  (testing "URL stops at whitespace"
    (is (= (inline/parse "https://a.com https://b.com")
           [[raw-link "https://a.com"] " " [raw-link "https://b.com"]])))

  (testing "URL stops at angle brackets"
    (is (= (inline/parse "https://a.com<2024-01-01>")
           [[raw-link "https://a.com"] [:timestamp "2024-01-01"]])))

  (testing "non-http word starting with h is not a link"
    (is (= (inline/parse "hello") ["hello"])))

  (testing "roundtrip preserves raw URL"
    (let [input "See https://example.com/path for details"
          tokens (inline/parse input)]
      (is (= input (w.inline/tokens->string tokens))))))
