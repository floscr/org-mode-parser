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

## Supported elements

**Blocks:** headings, paragraphs, lists, tables, code blocks, drawers, comments, clocks, planning lines, horizontal rules

**Inline:** bold, italic, underline, code, verbatim, strike-through, links, timestamps, footnotes, macros, targets
