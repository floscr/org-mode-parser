(ns org-mode.core
  (:require
   [org-mode.parser.heading.core :as heading]))

(defn parse
  "Parses a full Org document sequentially into {:body ... :headers [...]}"
  [s]
  (heading/parse-document s))

(defn parse-parallel
  "Parses a document but, on the JVM, processes heading chunks in parallel for
  better throughput on large files. Falls back to the sequential parser when
  running on ClojureScript."
  [s]
  (heading/parse-document s {:parallel? true}))
