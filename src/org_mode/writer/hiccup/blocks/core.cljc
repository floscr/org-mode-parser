(ns org-mode.writer.hiccup.blocks.core
  (:require
   [clojure.string :as str]
   [org-mode.parser.blocks.clock.tags :as clock-tags]
   [org-mode.parser.blocks.comment.tags :as comment-tags]
   [org-mode.parser.blocks.drawer.tags :as drawer-tags]
   [org-mode.parser.blocks.greater.tags :as greater-tags]
   [org-mode.parser.blocks.horizontal-rule.tags :as hr-tags]
   [org-mode.parser.blocks.keyword.tags :as keyword-tags]
   [org-mode.parser.blocks.list.tags :as list-tags]
   [org-mode.parser.blocks.planning.tags :as planning-tags]
   [org-mode.parser.blocks.table.tags :as table-tags]
   [org-mode.parser.blocks.tags :as blk-tags]
   [org-mode.writer.engine :as engine]
   [org-mode.writer.hiccup.accumulator :as acc]
   [org-mode.writer.hiccup.inline.core :as inline]))

(defn- maybe-attrs [tag attrs children]
  (let [base (if (seq attrs) [tag attrs] [tag])]
    (if (seq children)
      (into base children)
      base)))

(defn- paragraph [{:keys [render-inline]} line]
  (let [children (render-inline line)]
    (when (seq children)
      (into [:p] children))))

(defn- text-node [ctx [_ tokens]]
  (paragraph ctx tokens))

(defn- newline-node [[_ count]]
  (let [n (max 1 (or count 1))]
    (repeat n [:br])))

(defn- keyword-node [[_ {:keys [key value]}]]
  [:meta {:data-type "keyword" :data-key key :data-value value}])

(defn- planning-node [type {:keys [render-inline]} [_ tokens]]
  (maybe-attrs :div {:data-type "planning" :data-planning-type type} (render-inline tokens)))

(defn- clock-node [{:keys [render-inline]} [_ tokens]]
  (maybe-attrs :div {:data-type "clock" :data-label "CLOCK"} (render-inline tokens)))

(defn- comment-node [[_ text]]
  [:!-- text])

(defn- hr-node [_]
  [:hr])

(defn- drawer-node [state _ [_ name]]
  [:div {:data-type "drawer" :data-state state :data-name name}])

(defn- property-node [{:keys [render-inline]} [_ {:keys [key value]}]]
  (maybe-attrs :div {:data-type "property" :data-key key} (render-inline value)))

(defn- column->td [{:keys [render-inline]} [_ content]]
  (into [:td] (render-inline content)))

(defn- row-node [ctx columns & [attrs]]
  (let [cells (map #(column->td ctx %) columns)]
    (into (if attrs [:tr attrs] [:tr]) cells)))

(defn- table-row-node [ctx [_ columns]]
  [:table (row-node ctx columns)])

(defn- table-separator-node [_ [_ columns]]
  (let [cells (map (fn [[_ dashes]]
                     [:td {:data-role "separator"} (apply str dashes)])
                   columns)]
    [:table (into [:tr {:data-role "separator"}] cells)]))

(defn- checkbox-char [c]
  (when c (str (char c))))

(defn- marker-details [[marker-type payload]]
  (case marker-type
    :unordered {:tag :ul
                :attrs {:data-marker (str payload)}}
    :ordered {:tag :ol
              :attrs {:data-marker "ordered"
                      :data-delimiter (str (:delimiter payload))}}
    {:tag :ul
     :attrs {:data-marker (str payload)}}))

(defn- list-node [{:keys [render-inline]} [_ {:keys [indent marker checkbox tokens]}]]
  (let [{:keys [tag attrs]} (marker-details marker)
        li-attrs (cond-> {}
                   (pos? indent) (assoc :data-indent indent)
                   checkbox (assoc :data-checkbox (checkbox-char checkbox)))
        li (into (if (seq li-attrs) [:li li-attrs] [:li])
                 (render-inline tokens))]
    (into (if (seq attrs) [tag attrs] [tag])
          [li])))

(defn- greater-content [{:keys [render-inline]} content]
  (if (string? content)
    [content]
    (render-inline content)))

(defn- greater-node [ctx [_ {:keys [type args content]}]]
  (let [args-str (->> args (apply str) str/trim)
        attrs (cond-> {:data-type type}
                (seq args-str) (assoc :data-args args-str))]
    (case type
      "quote" (maybe-attrs :blockquote attrs (greater-content ctx content))
      "verse" (maybe-attrs :pre (assoc attrs :data-render "verse") [content])
      "example" (maybe-attrs :pre (assoc attrs :data-render "example") [content])
      "src" (let [lang (first (str/split args-str #"\s+" 2))
                  code-attrs (cond-> {:data-type type}
                               lang (assoc :data-language lang))
                  pre-attrs (assoc attrs :data-render "src")]
              (maybe-attrs :pre pre-attrs
                           [(into [:code code-attrs] (greater-content ctx content))]))
      (maybe-attrs :org/block attrs (greater-content ctx content)))))

(def default-block-writers
  "Map from block tags to render fns. Each fn receives a ctx map {:render-inline ...}
  plus the raw token."
  {blk-tags/newlines (fn [_ tok] (newline-node tok))
   blk-tags/text (fn [ctx tok] (text-node ctx tok))
   keyword-tags/keyword-line (fn [_ tok] (keyword-node tok))
   clock-tags/clock clock-node
   planning-tags/scheduled (partial planning-node :scheduled)
   planning-tags/deadline (partial planning-node :deadline)
   planning-tags/closed (partial planning-node :closed)
   comment-tags/comment-line (fn [_ tok] (comment-node tok))
   hr-tags/horizontal-rule (fn [_ tok] (hr-node tok))
   drawer-tags/drawer-start (partial drawer-node :start)
   drawer-tags/drawer-end (partial drawer-node :end)
   drawer-tags/property property-node
   table-tags/table-row table-row-node
   table-tags/table-separator table-separator-node
   list-tags/list-item list-node
   greater-tags/block greater-node})

(defn- fallback-node [[tag :as token]]
  [:div {:data-type "unknown-block" :data-tag tag :data-token token}])

(defn- node-attrs [node]
  (let [body (rest node)]
    (when (map? (first body))
      (first body))))

(defn- drawer-node-attrs [node]
  (when (and (vector? node)
             (= :div (first node)))
    (let [attrs (node-attrs node)]
      (when (= "drawer" (:data-type attrs))
        attrs))))

(defn- add-node [{:keys [drawer-stack] :as state} node conjoin?]
  (if (seq drawer-stack)
    (let [idx (dec (count drawer-stack))]
      (update-in state [:drawer-stack idx :content]
                 #(acc/accumulate % node conjoin?)))
    (update state :nodes #(acc/accumulate % node conjoin?))))

(defn- open-drawer [state attrs]
  (update state :drawer-stack conj {:name (:data-name attrs)
                                    :content []}))

(defn- drawer->details [{:keys [name content]}]
  (let [summary [:summary (or name "")]]
    (into [:details {:data-type "drawer" :data-name name}]
          (cons summary content))))

(defn- close-drawer [{:keys [drawer-stack] :as state} conjoin?]
  (if-let [drawer (peek drawer-stack)]
    (let [state' (update state :drawer-stack pop)]
      (add-node state' (drawer->details drawer) conjoin?))
    state))

(defn- drawer-step [state node conjoin?]
  (if-let [attrs (drawer-node-attrs node)]
    (case (:data-state attrs)
      :start (open-drawer state attrs)
      :end (if (seq (:drawer-stack state))
             (close-drawer state conjoin?)
             (add-node state node conjoin?))
      (add-node state node conjoin?))
    (add-node state node conjoin?)))

(defn- finalize-drawers [state conjoin?]
  (if (seq (:drawer-stack state))
    (recur (close-drawer state conjoin?) conjoin?)
    state))

(defn tokens->hiccup
  "Render parser block tokens to hiccup. Accepts opts:
   :block-writers - map overriding `default-block-writers`
   :inline-writers - map forwarded to inline writer
   :conjoin? - predicate used when accumulating nodes."
  ([tokens] (tokens->hiccup tokens {}))
  ([tokens {:keys [block-writers inline-writers conjoin?]}]
   (let [render-inline (inline/renderer (cond-> {}
                                          inline-writers (assoc :writers inline-writers)))
         writers (merge default-block-writers block-writers)
         conjoin? (or conjoin? acc/default-conjoin?)]
     (let [state (engine/render-blocks {:tokens tokens
                                        :inline-renderer render-inline
                                        :handlers writers
                                        :default-handler (fn [_ tok] (fallback-node tok))
                                        :line-handler paragraph
                                        :step (fn [state node]
                                                (drawer-step state node conjoin?))
                                        :init {:nodes [] :drawer-stack []}})
           {:keys [nodes]} (finalize-drawers state conjoin?)]
       nodes))))
