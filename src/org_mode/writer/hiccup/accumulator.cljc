(ns org-mode.writer.hiccup.accumulator
  (:require
   [org-mode.writer.hiccup.text :as text]))

(def default-conjoin-tags
  "Tags that should be merged when they appear consecutively."
  #{:p :table :ul :ol})

(defn- trailing-whitespace? [s]
  (and (string? s)
       (boolean (re-find #"\s$" s))))

(defn- leading-whitespace? [s]
  (and (string? s)
       (boolean (re-find #"^\s" s))))

(defn- node-body [node]
  (let [body (rest node)
        attrs (when (map? (first body)) (first body))
        children (if attrs (rest body) body)]
    {:attrs attrs
     :children children}))

(defn default-conjoin?
  "Return true when two hiccup nodes should be merged into a single node.
  By default, nodes are conjoined when they are vectors with the same tag
  and that tag is included in `default-conjoin-tags`."
  [prev-node next-node]
  (let [tag (when (and (vector? prev-node)
                       (vector? next-node)
                       (= (first prev-node) (first next-node)))
              (first prev-node))
        prev-attrs (when tag (:attrs (node-body prev-node)))
        next-attrs (when tag (:attrs (node-body next-node)))]
    (and (contains? default-conjoin-tags tag)
         (or (and (nil? prev-attrs) (nil? next-attrs))
             (= prev-attrs next-attrs)))))

(defn- merge-nodes [prev-node next-node]
  (let [{prev-attrs :attrs prev-children :children} (node-body prev-node)
        {next-attrs :attrs next-children :children} (node-body next-node)
        merged-attrs (merge prev-attrs next-attrs)
        tag (first prev-node)
        insert-gap? (and (= :p tag)
                         (seq prev-children)
                         (seq next-children)
                         (not (trailing-whitespace? (last prev-children)))
                         (not (leading-whitespace? (first next-children))))
        merged-children (text/merge-strings (concat prev-children
                                                    (when insert-gap? [" "])
                                                    next-children))
        base (if (seq merged-attrs)
               [(first prev-node) merged-attrs]
               [(first prev-node)])]
    (into base merged-children)))

(defn accumulate
  "Accumulate hiccup nodes, merging adjacent nodes that satisfy `conjoin?`.
  Accepts either a single node or a collection of nodes.
  Returns the updated accumulator vector."
  ([acc node]
   (accumulate acc node default-conjoin?))
  ([acc node conjoin?]
   (let [nodes (cond
                 (nil? node) []
                 (and (sequential? node) (not (vector? node))) node
                 :else [node])]
     (reduce (fn [acc' node']
               (let [prev (peek acc')]
                 (if (and prev (conjoin? prev node'))
                   (conj (pop acc') (merge-nodes prev node'))
                   (conj acc' node'))))
             (vec acc)
             nodes))))
