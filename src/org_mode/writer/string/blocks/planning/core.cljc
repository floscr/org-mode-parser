(ns org-mode.writer.string.blocks.planning.core
  (:require
   [org-mode.parser.blocks.planning.tags :as tags]
   [org-mode.writer.string.inline.core :as inline]))

(defn- label [tag]
  (cond
    (= tag tags/scheduled) "SCHEDULED"
    (= tag tags/deadline) "DEADLINE"
    (= tag tags/closed) "CLOSED"))

(defn token->string [[tag tokens]]
  (str (label tag) ": " (inline/tokens->string tokens) "\n"))
