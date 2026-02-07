(ns org-mode.computed.heading.blocks
  "Block-level computed properties for headings.

   Extract properties, planning (scheduled/deadline/closed), and source blocks
   from the heading body."
  (:require
   [org-mode.computed.timestamp.core :as ts]
   [org-mode.parser.blocks.core :as blocks]
   [org-mode.parser.inline.tags :as inline-tags]))

;; Token predicates -----------------------------------------------------------

(defn property-token?
  "Returns true if the block token is a :property."
  [token]
  (= (first token) blocks/tag-property))

(defn newline-token?
  "Returns true if the block token is :newlines."
  [token]
  (= (first token) blocks/tag-newlines))

(defn text-token?
  "Returns true if the block token is :text."
  [token]
  (= (first token) blocks/tag-text))

(defn scheduled-token?
  "Returns true if the block token is :scheduled."
  [token]
  (= (first token) blocks/tag-scheduled))

(defn deadline-token?
  "Returns true if the block token is :deadline."
  [token]
  (= (first token) blocks/tag-deadline))

(defn closed-token?
  "Returns true if the block token is :closed."
  [token]
  (= (first token) blocks/tag-closed))

(defn planning-token?
  "Returns true if the block token is a planning keyword (:scheduled, :deadline, :closed)."
  [token]
  (or (scheduled-token? token)
      (deadline-token? token)
      (closed-token? token)))

(defn drawer-start-token?
  "Returns true if the block token is :drawer-start."
  [token]
  (= (first token) blocks/tag-drawer-start))

(defn drawer-end-token?
  "Returns true if the block token is :drawer-end."
  [token]
  (= (first token) blocks/tag-drawer-end))

(defn source-block-token?
  "Returns true if the block token is a source block."
  [token]
  (and (= (first token) blocks/tag-block)
       (= (:type (second token)) "src")))

;; Properties -----------------------------------------------------------------

(defn- begin-properties-token?
  "Token that can appear at the beginning of a body before content
   (properties, newlines, or drawer boundaries)."
  [token]
  (or (property-token? token)
      (newline-token? token)
      (drawer-start-token? token)
      (drawer-end-token? token)))

(defn split-properties-body
  "Split body tokens into [properties-section rest-section].
   The properties section includes properties, newlines, and drawer boundaries
   at the beginning of the body."
  [body]
  (when (seq body)
    (split-with begin-properties-token? body)))

(defn body-properties
  "Extract properties from the heading body as a map of {key value-tokens}.

   Properties appear as [:property {:key \"KEY\" :value <inline-tokens>}] tokens."
  [heading]
  (reduce
   (fn [acc token]
     (if (property-token? token)
       (let [{:keys [key value]} (second token)]
         (assoc acc key value))
       acc))
   {} (:body heading)))

;; Planning / Schedule --------------------------------------------------------

(defn- find-timestamp-in-inline
  "Find the first timestamp token in inline content.
   Returns the token or nil."
  [inline-tokens]
  (first
   (filter (fn [tok]
             (and (vector? tok)
                  (#{inline-tags/timestamp
                     inline-tags/inactive-timestamp
                     inline-tags/timestamp-range
                     inline-tags/inactive-timestamp-range} (first tok))))
           inline-tokens)))

(defn- extract-planning-date
  "Extract date info from a planning token's inline content.
   Returns {:timestamp-token <token> :parsed <parsed-ts>} or nil."
  [inline-tokens & {:keys [parse-date]}]
  (when-let [ts-token (find-timestamp-in-inline inline-tokens)]
    (let [parsed (ts/parse-timestamp-token ts-token :parse-date parse-date)]
      {:timestamp-token ts-token
       :parsed parsed})))

(defn collect-planning
  "Extract scheduled, deadline, and closed info from a heading's body.

   Options:
     :parse-date  – custom date constructor (see timestamp module)

   Returns a map (may have :scheduled, :deadline, :closed keys), each with:
     {:date          <parsed-timestamp-info>
      :inline-tokens <raw inline tokens from the planning line>}"
  [heading & {:keys [parse-date]}]
  (reduce
   (fn [acc token]
     (let [tag (first token)
           inline-tokens (second token)]
       (cond
         (scheduled-token? token)
         (assoc acc :scheduled {:date (extract-planning-date inline-tokens :parse-date parse-date)
                                :inline-tokens inline-tokens})

         (deadline-token? token)
         (assoc acc :deadline {:date (extract-planning-date inline-tokens :parse-date parse-date)
                               :inline-tokens inline-tokens})

         (closed-token? token)
         (assoc acc :closed {:date (extract-planning-date inline-tokens :parse-date parse-date)
                             :inline-tokens inline-tokens})

         :else acc)))
   {} (:body heading)))

(defn scheduled
  "Extract the scheduled date from a heading. Returns the parsed timestamp
   info map or nil.

   Options:
     :parse-date  – custom date constructor"
  [heading & {:keys [parse-date]}]
  (some-> (collect-planning heading :parse-date parse-date)
          :scheduled :date :parsed))

(defn deadline
  "Extract the deadline date from a heading. Returns the parsed timestamp
   info map or nil.

   Options:
     :parse-date  – custom date constructor"
  [heading & {:keys [parse-date]}]
  (some-> (collect-planning heading :parse-date parse-date)
          :deadline :date :parsed))

;; Source blocks ---------------------------------------------------------------

(defn collect-source-blocks
  "Collect all source blocks from a heading's body.

   Returns a vector of maps:
     {:type    \"src\"
      :args    <parsed-args>
      :content <string>}"
  [heading]
  (into []
        (comp
         (filter source-block-token?)
         (map second)
         (map (fn [{:keys [type args content]}]
                {:type type
                 :args args
                 :content content})))
        (:body heading)))
