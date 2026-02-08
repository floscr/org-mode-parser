(ns org-mode.actions.heading.todo
  "TODO keyword actions for headings."
  (:require
   [org-mode.computed.heading.core :as c.heading]))

(defn set-todo
  "Set TODO keyword. Replaces existing keyword or prepends to title.

   `kw` must be a member of the keyword set (defaults to default-todo-keywords)."
  [kw heading & {:keys [todo-keywords]
                 :or {todo-keywords c.heading/default-todo-keywords}}]
  (let [title (:title heading)
        current-todo (c.heading/todo heading :todo-keywords todo-keywords)]
    (cond
      ;; Already has this keyword — no-op
      (= current-todo kw)
      heading

      ;; Has a different keyword — replace first token
      current-todo
      (assoc heading :title (into [kw] (rest title)))

      ;; No keyword — prepend keyword + space
      :else
      (assoc heading :title (into [kw " "] title)))))

(defn remove-todo
  "Remove TODO keyword from title."
  [heading & {:keys [todo-keywords]
              :or {todo-keywords c.heading/default-todo-keywords}}]
  (let [current-todo (c.heading/todo heading :todo-keywords todo-keywords)]
    (if current-todo
      ;; Remove keyword and the following space (if present)
      (let [title (:title heading)
            rest-tokens (rest title)
            ;; Drop leading space token after keyword
            rest-tokens (if (and (seq rest-tokens)
                                 (= " " (first rest-tokens)))
                          (rest rest-tokens)
                          rest-tokens)]
        (assoc heading :title (vec rest-tokens)))
      heading)))

(defn toggle-todo
  "Toggle between TODO and DONE."
  [heading & {:keys [todo-keywords]
              :or {todo-keywords c.heading/default-todo-keywords}}]
  (let [current-todo (c.heading/todo heading :todo-keywords todo-keywords)]
    (cond
      (= current-todo "DONE") (remove-todo heading :todo-keywords todo-keywords)
      (= current-todo "TODO") (set-todo "DONE" heading :todo-keywords todo-keywords)
      :else (set-todo "TODO" heading :todo-keywords todo-keywords))))

(defn cycle-todo
  "Cycle through an ordered keyword list.
   e.g. (cycle-todo heading [\"TODO\" \"DOING\" \"DONE\"])
   cycles: nil → TODO → DOING → DONE → nil → TODO ..."
  [heading keywords]
  (let [kw-set (set keywords)
        current-todo (c.heading/todo heading :todo-keywords kw-set)]
    (if (nil? current-todo)
      ;; No keyword → set to first
      (set-todo (first keywords) heading :todo-keywords kw-set)
      ;; Find current index and advance
      (let [idx (.indexOf ^java.util.List (vec keywords) current-todo)
            next-idx (inc idx)]
        (if (>= next-idx (count keywords))
          ;; Past the end → remove keyword
          (remove-todo heading :todo-keywords kw-set)
          ;; Advance to next keyword
          (set-todo (nth keywords next-idx) heading :todo-keywords kw-set))))))
