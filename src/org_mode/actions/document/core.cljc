(ns org-mode.actions.document.core
  "Document-level actions for modifying parsed documents.")

(defn update-heading-at
  "Update heading at index with function f."
  [idx f doc]
  (update-in doc [:headers idx] f))

(defn update-headings
  "Update all headings with function f."
  [f doc]
  (update doc :headers (fn [headers] (mapv f headers))))

(defn update-body
  "Update document body with function f."
  [f doc]
  (update doc :body f))

(defn remove-heading-at
  "Remove heading at index."
  [idx doc]
  (update doc :headers (fn [headers]
                         (vec (concat (subvec headers 0 idx)
                                      (subvec headers (inc idx)))))))
