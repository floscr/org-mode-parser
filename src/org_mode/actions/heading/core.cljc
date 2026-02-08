(ns org-mode.actions.heading.core
  "Basic heading actions: set level, title, and body.")

(defn set-level
  "Set heading level."
  [level heading]
  (assoc heading :level level))

(defn set-title
  "Set title (raw inline tokens)."
  [title-tokens heading]
  (assoc heading :title title-tokens))

(defn set-body
  "Set body (raw block tokens)."
  [body-tokens heading]
  (assoc heading :body body-tokens))
