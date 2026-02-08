(ns org-mode.actions.heading.planning
  "Planning (SCHEDULED/DEADLINE/CLOSED) actions for headings."
  (:require
   [org-mode.computed.heading.blocks :as c.blocks]
   [org-mode.parser.blocks.core :as blocks]
   [org-mode.parser.inline.core :as inline]))

(defn- planning-insertion-index
  "Find the index where planning tokens should be inserted in the body.
   Planning goes at the start, but after any properties drawer."
  [body]
  (loop [i 0]
    (if (>= i (count body))
      i
      (let [token (nth body i)]
        (cond
          ;; Skip past properties drawer
          (c.blocks/drawer-start-token? token) (recur (inc i))
          (c.blocks/property-token? token) (recur (inc i))
          (c.blocks/drawer-end-token? token) (recur (inc i))
          ;; Skip existing planning tokens
          (c.blocks/planning-token? token) (recur (inc i))
          :else i)))))

(defn- set-planning
  "Generic set for a planning type. `tag` is :scheduled, :deadline, or :closed.
   `pred` is the token predicate (e.g. scheduled-token?)."
  [tag pred timestamp-str heading]
  (let [body (or (:body heading) [])
        inline-tokens (inline/parse timestamp-str)
        new-token [tag inline-tokens]
        ;; Check if there's already a token of this type
        existing-idx (first (keep-indexed (fn [i tok] (when (pred tok) i)) body))]
    (if existing-idx
      ;; Replace existing
      (assoc heading :body (assoc body existing-idx new-token))
      ;; Insert at correct position
      (let [idx (planning-insertion-index body)]
        (assoc heading :body (vec (concat (subvec body 0 idx)
                                          [new-token]
                                          (subvec body idx))))))))

(defn- remove-planning
  "Generic remove for a planning type."
  [pred heading]
  (let [body (or (:body heading) [])]
    (assoc heading :body (vec (remove pred body)))))

(defn set-scheduled
  "Set/replace SCHEDULED line."
  [timestamp-str heading]
  (set-planning blocks/tag-scheduled c.blocks/scheduled-token? timestamp-str heading))

(defn remove-scheduled
  "Remove SCHEDULED line."
  [heading]
  (remove-planning c.blocks/scheduled-token? heading))

(defn set-deadline
  "Set/replace DEADLINE line."
  [timestamp-str heading]
  (set-planning blocks/tag-deadline c.blocks/deadline-token? timestamp-str heading))

(defn remove-deadline
  "Remove DEADLINE line."
  [heading]
  (remove-planning c.blocks/deadline-token? heading))

(defn set-closed
  "Set/replace CLOSED line."
  [timestamp-str heading]
  (set-planning blocks/tag-closed c.blocks/closed-token? timestamp-str heading))

(defn remove-closed
  "Remove CLOSED line."
  [heading]
  (remove-planning c.blocks/closed-token? heading))
