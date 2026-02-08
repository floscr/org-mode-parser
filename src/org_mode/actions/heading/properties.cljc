(ns org-mode.actions.heading.properties
  "Property drawer actions for headings."
  (:require
   [org-mode.computed.heading.blocks :as c.blocks]
   [org-mode.parser.blocks.core :as blocks]
   [org-mode.parser.inline.core :as inline]))

(defn- find-properties-drawer
  "Find the indices of :drawer-start \"PROPERTIES\" and :drawer-end \"END\" in body.
   Returns [start-idx end-idx] or nil if no properties drawer."
  [body]
  (let [start-idx (first (keep-indexed
                          (fn [i tok]
                            (when (and (c.blocks/drawer-start-token? tok)
                                       (= "PROPERTIES" (second tok)))
                              i))
                          body))]
    (when start-idx
      ;; Find matching drawer-end after start
      (let [end-idx (first (keep-indexed
                            (fn [i tok]
                              (when (and (> i start-idx)
                                         (c.blocks/drawer-end-token? tok))
                                i))
                            body))]
        (when end-idx
          [start-idx end-idx])))))

(defn- make-property-token
  "Create a property token. Value string is parsed with a leading space for roundtrip."
  [key value]
  [blocks/tag-property {:key key :value (inline/parse (str " " value))}])

(defn- make-properties-drawer
  "Create a full properties drawer with the given property tokens."
  [prop-tokens]
  (vec (concat [[blocks/tag-drawer-start "PROPERTIES"]]
               prop-tokens
               [[blocks/tag-drawer-end "END"]])))

(defn set-property
  "Set a property in the PROPERTIES drawer. Creates the drawer if needed.
   Value is a string."
  [key value heading]
  (let [body (or (:body heading) [])
        drawer (find-properties-drawer body)
        new-prop (make-property-token key value)]
    (if drawer
      (let [[start-idx end-idx] drawer
            ;; Extract tokens between drawer-start and drawer-end
            drawer-contents (subvec body (inc start-idx) end-idx)
            ;; Find existing property with same key
            existing-prop-idx (first (keep-indexed
                                      (fn [i tok]
                                        (when (and (c.blocks/property-token? tok)
                                                   (= key (:key (second tok))))
                                          i))
                                      drawer-contents))
            new-contents (if existing-prop-idx
                           ;; Replace existing property
                           (assoc drawer-contents existing-prop-idx new-prop)
                           ;; Add new property before drawer-end
                           (conj drawer-contents new-prop))
            ;; Reconstruct body
            before (subvec body 0 start-idx)
            after (subvec body (inc end-idx))
            new-drawer (make-properties-drawer new-contents)]
        (assoc heading :body (vec (concat before new-drawer after))))
      ;; No drawer — create one at the start of body
      (let [new-drawer (make-properties-drawer [new-prop])]
        (assoc heading :body (vec (concat new-drawer body)))))))

(defn remove-property
  "Remove a property from the PROPERTIES drawer.
   Removes the entire drawer if it becomes empty."
  [key heading]
  (let [body (or (:body heading) [])
        drawer (find-properties-drawer body)]
    (if drawer
      (let [[start-idx end-idx] drawer
            drawer-contents (subvec body (inc start-idx) end-idx)
            new-contents (vec (remove (fn [tok]
                                        (and (c.blocks/property-token? tok)
                                             (= key (:key (second tok)))))
                                      drawer-contents))]
        (if (empty? new-contents)
          ;; Remove entire drawer
          (let [before (subvec body 0 start-idx)
                after (subvec body (inc end-idx))]
            (assoc heading :body (vec (concat before after))))
          ;; Keep drawer with remaining properties
          (let [before (subvec body 0 start-idx)
                after (subvec body (inc end-idx))
                new-drawer (make-properties-drawer new-contents)]
            (assoc heading :body (vec (concat before new-drawer after))))))
      ;; No drawer — nothing to remove
      heading)))

(defn set-properties
  "Replace all properties with a map of {key value-string}.
   Creates the drawer if needed. Removes drawer if props-map is empty."
  [props-map heading]
  (let [body (or (:body heading) [])
        drawer (find-properties-drawer body)]
    (if (empty? props-map)
      ;; Remove the entire drawer if it exists
      (if drawer
        (let [[start-idx end-idx] drawer
              before (subvec body 0 start-idx)
              after (subvec body (inc end-idx))]
          (assoc heading :body (vec (concat before after))))
        heading)
      ;; Create or replace drawer
      (let [prop-tokens (mapv (fn [[k v]] (make-property-token k v)) props-map)
            new-drawer (make-properties-drawer prop-tokens)]
        (if drawer
          (let [[start-idx end-idx] drawer
                before (subvec body 0 start-idx)
                after (subvec body (inc end-idx))]
            (assoc heading :body (vec (concat before new-drawer after))))
          ;; No existing drawer — insert at start
          (assoc heading :body (vec (concat new-drawer body))))))))
