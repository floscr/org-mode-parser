(ns org-mode.writer.hiccup.inline.core
  (:require
   [org-mode.parser.inline.tags :as tags]
   [org-mode.writer.engine :as engine]
   [org-mode.writer.hiccup.text :as text]))

(defn- element
  ([tag children] (element tag nil children))
  ([tag attrs children]
   (if (seq attrs)
     (into [tag attrs] children)
     (into [tag] children))))

(defn- fallback-inline-handler [render [tag content]]
  (let [children (render content)
        org-tag (keyword "org" (name tag))]
    (if (seq children)
      (element org-tag children)
      [org-tag content])))

(def default-inline-writers
  "Map of inline token tags to hiccup writer fns. Each writer receives two
  args: `render`, a fn that converts nested inline content to hiccup, and the
  original token vector."
  {tags/bold (fn [render [_ content]] (element :strong (render content)))
   tags/italic (fn [render [_ content]] (element :em (render content)))
   tags/underline (fn [render [_ content]] (element :u (render content)))
   tags/verbatim (fn [render [_ content]] (element :code {:data-inline "verbatim"} (render content)))
   tags/code (fn [render [_ content]] (element :code (render content)))
   tags/strike-through (fn [render [_ content]] (element :del (render content)))
   tags/link (fn [_ [_ url]] [:a {:href url} url])
   tags/link-with-title (fn [_ [_ {:keys [link title]}]] [:a {:href link} title])
   tags/raw-link (fn [_ [_ url]] [:a {:href url} url])
   tags/macro (fn [_ [_ {:keys [name args]}]]
                [:org/macro {:name name
                             :args args}])
   tags/target (fn [_ [_ target]] [:a {:name target} target])
   tags/footnote-ref (fn [_ [_ name]]
                       [:sup [:a {:href (str "#fn-" name)
                                  :data-ref name} name]])
   tags/footnote-def (fn [_ [_ {:keys [name definition]}]]
                       [:div {:id (str "fn-" name)
                              :data-role "footnote"} definition])
   tags/stats-range-cookie (fn [_ [_ {:keys [from to]}]]
                             [:span {:data-org/stats "range"} (str (or from 0) "/" (or to 0))])
   tags/stats-percent-cookie (fn [_ [_ n]]
                               [:span {:data-org/stats "percent"} (str (or n 0) "%")])
   tags/timestamp (fn [_ [_ ts]] [:time {:data-active true} ts])
   tags/inactive-timestamp (fn [_ [_ ts]] [:time {:data-active false} ts])
   tags/timestamp-range (fn [_ [_ {:keys [start end]}]]
                          [:time {:data-active true :data-range true} (str start "--" end)])
   tags/inactive-timestamp-range (fn [_ [_ {:keys [start end]}]]
                                   [:time {:data-active false :data-range true} (str start "--" end)])})

(defn renderer
  ([]
   (renderer {}))
  ([{:keys [writers]}]
   (let [handlers (merge default-inline-writers writers)]
     (engine/inline-renderer {:handlers handlers
                              :default-handler fallback-inline-handler
                              :scalar-handler identity
                              :combine text/merge-strings}))))

(defn tokens->hiccup
  "Render a sequence of inline tokens to hiccup nodes. Optional opts map
  accepts :writers for overriding `default-inline-writers`."
  ([tokens]
   (tokens->hiccup tokens {}))
  ([tokens opts]
   ((renderer opts) tokens)))
