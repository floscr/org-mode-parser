(ns org-mode.writer.string.inline.core
  (:require
   [clojure.string :as str]
   [org-mode.parser.inline.delimiters :as d]
   [org-mode.parser.inline.tags :as t]
   [org-mode.writer.engine :as engine]))

(defn- surround [delim s]
  (str delim s delim))

(defn- write-link [[_ url]]
  (str d/link-start url d/link-end))

(defn- write-link-with-title [[_ {:keys [link title]}]]
  (str d/link-start link d/link-separator title d/link-end))

(defn- write-macro [[_ {:keys [name args]}]]
  (str d/macro-start
       name
       (when (seq args)
         (str d/macro-args-start (str/join ", " args) d/macro-args-end))
       d/macro-end))

(defn- write-stats-range [[_ {:keys [from to]}]]
  (str d/stats-cookie-start
       (when (some? from) from)
       "/"
       (when (some? to) to)
       d/stats-cookie-end))

(defn- write-stats-percent [[_ n]]
  (str d/stats-cookie-start (when (some? n) n) "%" d/stats-cookie-end))

(def default-inline-writers
  {t/bold (fn [render [_ content]] (surround d/bold (render content)))
   t/italic (fn [render [_ content]] (surround d/italic (render content)))
   t/underline (fn [render [_ content]] (surround d/underline (render content)))
   t/verbatim (fn [render [_ content]] (surround d/verbatim (render content)))
   t/code (fn [render [_ content]] (surround d/code (render content)))
   t/strike-through (fn [render [_ content]] (surround d/strike-through (render content)))

   t/timestamp (fn [_ [_ content]] (str d/timestamp-start content d/timestamp-end))
   t/inactive-timestamp (fn [_ [_ content]] (str d/inactive-timestamp-start content d/inactive-timestamp-end))
   t/timestamp-range (fn [_ [_ {:keys [start end]}]]
                       (str d/timestamp-start start d/timestamp-end
                            "--"
                            d/timestamp-start end d/timestamp-end))
   t/inactive-timestamp-range (fn [_ [_ {:keys [start end]}]]
                                (str d/inactive-timestamp-start start d/inactive-timestamp-end
                                     "--"
                                     d/inactive-timestamp-start end d/inactive-timestamp-end))

   t/link (fn [_ tok] (write-link tok))
   t/link-with-title (fn [_ tok] (write-link-with-title tok))
   t/raw-link (fn [_ [_ url]] url)
   t/target (fn [_ [_ content]] (str d/target-start content d/target-end))
   t/footnote-ref (fn [_ [_ content]] (str d/footnote-start content d/footnote-end))
   t/footnote-def (fn [_ [_ {:keys [name definition]}]]
                    (str d/footnote-start name d/footnote-separator definition d/footnote-end))
   t/macro (fn [_ tok] (write-macro tok))
   t/stats-range-cookie (fn [_ tok] (write-stats-range tok))
   t/stats-percent-cookie (fn [_ tok] (write-stats-percent tok))})

(defn- fallback-inline-handler [_ token]
  (str token))

(defn renderer
  ([] (renderer {}))
  ([{:keys [writers]}]
   (let [handlers (merge default-inline-writers writers)]
     (engine/inline-renderer {:handlers handlers
                              :default-handler fallback-inline-handler
                              :scalar-handler identity
                              :combine (fn [segments] (apply str segments))}))))

(defn tokens->string
  "Render a sequence of inline tokens (strings and token vectors) back to text."
  ([tokens] (tokens->string tokens {}))
  ([tokens opts]
   ((renderer opts) tokens)))
