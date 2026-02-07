# Agents

## Overview

A Clojure/ClojureScript org-mode parser with bidirectional conversion (parse and write). Zero external dependencies for parsing. Supports sequential and parallel (JVM) parsing, string roundtrip, and hiccup/HTML output.

## Tech Stack

- **Language:** Clojure (`.cljc` cross-platform files)
- **Build:** Babashka (`bb`) for tasks, Clojure CLI (`deps.edn`) for deps
- **Test:** `clojure.test` with eftest runner
- **Java:** 21+
- **No external parsing dependencies** — hand-written character-by-character parser

## Project Layout

```
src/org_mode/
  core.cljc                    # Public API: parse, parse-parallel
  chunker/core.cljc            # Split document into heading chunks
  parser/
    heading/core.cljc          # Document structure & heading extraction
    blocks/core.cljc           # Block-level elements (line-by-line)
    blocks/*/tags.cljc         # Tag definitions per block type
    inline/core.cljc           # Inline markup (char-by-char)
    inline/tags.cljc           # Inline tag definitions
    inline/delimiters.cljc     # Delimiter constants
  writer/
    engine.cljc                # Generic rendering engine
    string/core.cljc           # Org-mode string output (roundtrip)
    string/inline/core.cljc    # Inline -> string
    string/blocks/*/core.cljc  # Block writers per type
    hiccup/core.cljc           # Hiccup/HTML output
    hiccup/inline/core.cljc    # Inline -> hiccup
    hiccup/blocks/core.cljc    # Block -> hiccup
  computed/
    timestamp/core.cljc        # Timestamp parsing, comparison, pluggable date conversion
    heading/core.cljc          # Heading accessors: TODO, tags, search, text conversion
    heading/blocks.cljc        # Block extraction: properties, planning/schedule, source blocks
    heading/links.cljc         # Link extraction from headings
    document/core.cljc         # Document-level utilities (heading iteration, filtering)
```

Tests are colocated as `*_test.cljc` alongside source files.

## Architecture

### Three-stage pipeline

1. **Chunker** — fast line scan splitting the document at heading boundaries (`*` at line start). Preserves blank lines for roundtrip fidelity.
2. **Parser** — two levels:
   - **Heading parser** extracts level, title (as inline tokens), and body per chunk
   - **Block parser** processes body lines into tagged tokens (keywords, drawers, lists, tables, code blocks, etc.)
   - **Inline parser** processes text into markup tokens (bold, links, raw HTTP links, timestamps, etc.)
3. **Writer** — converts the AST back to output via a pluggable rendering engine:
   - **String writer** for lossless org-mode roundtrip
   - **Hiccup writer** for HTML generation

### Token format

All parsed elements are `[tag data]` vectors:

```clojure
;; Block tokens
[:keyword {:key "TITLE" :value "My Doc"}]
[:list {:indent 0 :bullet "-" :checkbox nil :body [...]}]

;; Inline tokens (mixed strings and tagged vectors)
["hello " [:bold "world"] " text"]
[:raw-link "https://example.com"]          ;; Bare http(s) URLs
```

### Document structure

```clojure
{:body [...]                    ;; Content before first heading
 :body-trailing-blank? true     ;; Blank line tracking for roundtrip
 :headers [{:level 1
            :title [...]        ;; Inline tokens
            :body [...]         ;; Block tokens
            :trailing-blank? false}]}
```

## Public API

```clojure
;; Parse
(require '[org-mode.core :as org])
(org/parse s)                          ;; Sequential
(org/parse-parallel s)                 ;; Parallel (JVM only)

;; String roundtrip
(require '[org-mode.writer.string.core :as w])
(w/document->string doc)
(w/blocks->string tokens)
(w/inline->string tokens)

;; Hiccup / HTML
(require '[org-mode.writer.hiccup.core :as h])
(h/document->hiccup doc)
(h/document->html doc)
(h/blocks->hiccup tokens)
(h/inline->hiccup tokens)

;; Computed — derived properties from parsed AST
(require '[org-mode.computed.heading.core :as ch])
(ch/todo heading)                          ;; => "TODO" or nil
(ch/done? heading)                         ;; => true/false
(ch/tags heading)                          ;; => ["tag1" "tag2"] or nil
(ch/title-str heading)                     ;; => "My heading"
(ch/matches-query? "search" heading)       ;; => true/false
(ch/heading-tags headings)                 ;; => {"tag" count}

(require '[org-mode.computed.heading.blocks :as cb])
(cb/body-properties heading)               ;; => {"ID" <tokens>, ...}
(cb/scheduled heading)                     ;; => {:type :active :from {...} :to nil}
(cb/deadline heading)                      ;; => same shape
(cb/collect-source-blocks heading)         ;; => [{:type "src" :content "..."}]
;; Pluggable date parsing (e.g. tick):
(cb/scheduled heading :parse-date (fn [{:keys [year month day]}]
                                    (t/new-date year month day)))

(require '[org-mode.computed.heading.links :as cl])
(cl/heading-links heading)                 ;; => [[:link "url"] ...]
(cl/link-href link-token)                  ;; => "url"

(require '[org-mode.computed.timestamp.core :as ts])
(ts/parse-timestamp-str "2024-02-29 Thu")  ;; => {:year 2024 :month 2 :day 29 ...}
(ts/timestamp-compare ts-a ts-b)           ;; => -1, 0, or 1

(require '[org-mode.computed.document.core :as cd])
(cd/headings doc)                          ;; => all headers
(cd/top-level-headings doc)                ;; => level-1 headers only
(cd/find-headings pred doc)                ;; => filtered headers
```

## Commands

```sh
bb test          # Run all tests
bb watch:test    # Watch mode
bb fmt           # Format code with cljfmt
```

## Conventions

- **Tag-based tokens:** all elements are `[keyword data]` vectors for uniform dispatch
- **Colocated tests:** `_test.cljc` files sit next to source in the same directory
- **Roundtrip preservation:** parser tracks blank lines and whitespace so `parse -> write` is lossless
- **Modular element types:** each org element (list, table, drawer, etc.) has its own `tags.cljc` (parser) and `core.cljc` (writer)
- **Pluggable writers:** the rendering engine accepts custom handler maps to override any element's output
- **No external deps:** parsing uses only Clojure stdlib; hiccup writer uses `hiccup2.core`
