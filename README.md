# org-mode-parser

A Clojure parser for [Org-mode](https://orgmode.org/) documents.

## Installation

Add to your `deps.edn`:

```clojure
{:deps {org-mode-parser {:git/url "https://github.com/floscr/org-mode-parser" :sha "main"}}}
```

## Usage

```clojure
(require '[org-mode.core :as org])

;; Parse an org document
(org/parse "* Heading\nSome text\n** Subheading")
;; => {:body [], :headers [{:level 1, :title [...], :body [...]}]}

;; For large files on use parallel parsing
(org/parse-parallel (slurp "large-file.org"))
```

### Writing back to string

```clojure
(require '[org-mode.writer.string.core :as writer])

(writer/document->string (org/parse "* Hello\nWorld"))
;; => "* Hello\nWorld\n"
```

### Hiccup output

```clojure
(require '[org-mode.writer.hiccup.core :as hiccup])

(hiccup/document->hiccup (org/parse "* Hello\n*bold* text"))
;; => [:div [:section [:h1 "Hello"] [:p [:strong "bold"] " text"]]]
```

### Actions (mutating parsed documents)

```clojure
(require '[org-mode.actions.heading.todo :as todo])
(require '[org-mode.actions.heading.tags :as tags])
(require '[org-mode.actions.heading.planning :as plan])
(require '[org-mode.actions.heading.properties :as props])
(require '[org-mode.actions.heading.core :as h])
(require '[org-mode.actions.document.core :as doc])

(def d (org/parse "* TODO My Task :work:\nSCHEDULED: <2024-02-29 Thu>\n:PROPERTIES:\n:ID: abc\n:END:\nSome content"))
(def heading (first (:headers d)))

;; TODO actions
(-> heading (todo/toggle-todo))             ;; TODO -> DONE
(-> heading (todo/set-todo "DOING" :todo-keywords #{"TODO" "DOING" "DONE"}))
(-> heading (todo/cycle-todo ["TODO" "DOING" "DONE"]))

;; Tag actions
(-> heading (tags/add-tag "urgent"))         ;; :work:urgent:
(-> heading (tags/remove-tag "work"))        ;; removes :work:
(-> heading (tags/toggle-tag "flagged"))     ;; add/remove

;; Planning actions
(-> heading (plan/set-deadline "<2024-03-15 Fri>"))
(-> heading (plan/remove-scheduled))

;; Property actions
(-> heading (props/set-property "EFFORT" "2h"))
(-> heading (props/remove-property "ID"))

;; Basic heading actions
(-> heading (h/set-level 2))

;; Document-level actions
(-> d (doc/update-heading-at 0 todo/toggle-todo))
(-> d (doc/update-headings #(tags/add-tag "reviewed" %)))
(-> d (doc/remove-heading-at 0))
```

## Supported elements

**Blocks:** headings, paragraphs, lists, tables, code blocks, drawers, comments, clocks, planning lines, horizontal rules

**Inline:** bold, italic, underline, code, verbatim, strike-through, links, timestamps, footnotes, macros, targets
