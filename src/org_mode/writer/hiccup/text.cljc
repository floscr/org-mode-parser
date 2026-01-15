(ns org-mode.writer.hiccup.text)

(defn merge-strings
  "Collapse consecutive string nodes into a single string while preserving the
  relative order of non-string nodes."
  [nodes]
  (reduce (fn [acc node]
            (cond
              (nil? node)
              acc

              (and (string? node) (string? (peek acc)))
              (conj (pop acc) (str (peek acc) node))

              :else
              (conj acc node)))
          []
          nodes))
