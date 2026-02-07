(ns org-mode.computed.timestamp.core
  "Parsing and comparison of org-mode timestamp strings.

   Timestamps are stored as raw strings by the parser (e.g. \"2024-02-29 Thu 10:30\").
   This module provides:
   - `parse-timestamp-str`  – parse the string into a data map
   - `timestamp-compare`    – compare two parsed timestamp maps
   - `timestamp->date`      – convert a parsed map using a custom date constructor

   The `parse-date` option lets consumers plug in their own date library,
   for example tick:

       (require '[tick.core :as t])
       (timestamp->date ts (fn [{:keys [year month day]}]
                             (t/new-date year month day)))"
  (:require
   [clojure.string :as str]))

;; Parsing --------------------------------------------------------------------

(defn- parse-int [^String s]
  #?(:clj  (Long/parseLong s)
     :cljs (js/parseInt s 10)))

(defn parse-timestamp-str
  "Parse an org timestamp content string into a map.

   Input examples (the text *inside* angle/square brackets):
     \"2024-02-29 Thu\"
     \"2024-02-29 Thu 10:30\"
     \"2024-02-29 Thu 10:30-11:00\"
     \"2024-02-29 Thu +1w\"
     \"2024-02-29 Thu -3d .+2w\"

   Returns a map:
     {:year     2024
      :month    2
      :day      29
      :day-name \"Thu\"   ; or nil
      :hour     10       ; or nil
      :minute   30       ; or nil
      :end-hour   nil    ; for time ranges like 10:30-11:00
      :end-minute nil
      :repeater  \"+1w\"  ; or nil
      :warning   \"-3d\"  ; or nil}"
  [^String s]
  (when (and s (pos? (count s)))
    (let [parts (str/split (str/trim s) #"\s+")
          ;; First part is always the date YYYY-MM-DD
          date-str (first parts)
          date-parts (str/split date-str #"-")
          year (parse-int (nth date-parts 0))
          month (parse-int (nth date-parts 1))
          day (parse-int (nth date-parts 2))
          rest-parts (rest parts)
          ;; Second part may be day name (3 letter like Mon, Tue, etc.)
          [day-name rest-parts]
          (if (and (seq rest-parts)
                   (re-matches #"[A-Za-z]{2,3}" (first rest-parts)))
            [(first rest-parts) (rest rest-parts)]
            [nil rest-parts])
          ;; Next part may be time HH:MM or time range HH:MM-HH:MM
          [hour minute end-hour end-minute rest-parts]
          (if (and (seq rest-parts)
                   (re-matches #"\d{1,2}:\d{2}(?:-\d{1,2}:\d{2})?" (first rest-parts)))
            (let [time-str (first rest-parts)
                  dash-idx (.indexOf time-str "-" (long 1))]
              (if (and (pos? dash-idx)
                       ;; Make sure it's a time range, not negative
                       (re-matches #"\d{1,2}:\d{2}-\d{1,2}:\d{2}" time-str))
                (let [start-time (subs time-str 0 dash-idx)
                      end-time (subs time-str (inc dash-idx))
                      [sh sm] (str/split start-time #":")
                      [eh em] (str/split end-time #":")]
                  [(parse-int sh) (parse-int sm)
                   (parse-int eh) (parse-int em)
                   (rest rest-parts)])
                (let [[h m] (str/split time-str #":")]
                  [(parse-int h) (parse-int m) nil nil (rest rest-parts)])))
            [nil nil nil nil rest-parts])
          ;; Remaining parts are repeater/warning markers
          repeater (first (filter #(re-matches #"\+\+?\d+[hdwmy]" %) rest-parts))
          ;; Also match .+ repeater
          repeater (or repeater
                       (first (filter #(re-matches #"\.\+\d+[hdwmy]" %) rest-parts)))
          warning (first (filter #(re-matches #"--?\d+[hdwmy]" %) rest-parts))]
      {:year year
       :month month
       :day day
       :day-name day-name
       :hour hour
       :minute minute
       :end-hour end-hour
       :end-minute end-minute
       :repeater repeater
       :warning warning})))

;; Comparison -----------------------------------------------------------------

(defn timestamp-compare
  "Compare two parsed timestamp maps (as returned by `parse-timestamp-str`).
   Returns -1, 0, or 1."
  [a b]
  (let [fields [[:year] [:month] [:day] [:hour] [:minute]]]
    (reduce
     (fn [_ field]
       (let [va (or (get a (first field)) 0)
             vb (or (get b (first field)) 0)]
         (cond
           (< va vb) (reduced -1)
           (> va vb) (reduced 1)
           :else 0)))
     0 fields)))

;; Date conversion ------------------------------------------------------------

(defn timestamp->date
  "Convert a parsed timestamp map to a date using `parse-date-fn`.

   `parse-date-fn` receives a map with at least {:year :month :day}
   and optionally {:hour :minute}.

   Example with tick:
     (timestamp->date ts (fn [{:keys [year month day]}]
                           (t/new-date year month day)))"
  [parsed-ts parse-date-fn]
  (when parsed-ts
    (parse-date-fn parsed-ts)))

;; Token helpers --------------------------------------------------------------

(defn parse-timestamp-token
  "Parse a timestamp inline token into a map with :type and :from/:to dates.

   `token` is a vector like [:timestamp \"2024-02-29 Thu\"] or
   [:timestamp-range {:start \"...\" :end \"...\"}].

   Options:
     :parse-date  – function to convert parsed timestamp map to a date value.
                    If not provided, returns the raw parsed maps.

   Returns:
     {:type  :active | :inactive
      :from  <parsed-or-date>
      :to    <parsed-or-date> | nil}"
  [token & {:keys [parse-date]}]
  (let [[tag content] token
        convert (or parse-date identity)
        active? (case tag
                  (:timestamp :timestamp-range) true
                  (:inactive-timestamp :inactive-timestamp-range) false
                  nil)]
    (when (some? active?)
      (case tag
        (:timestamp :inactive-timestamp)
        {:type (if active? :active :inactive)
         :from (some-> content parse-timestamp-str convert)
         :to nil}

        (:timestamp-range :inactive-timestamp-range)
        {:type (if active? :active :inactive)
         :from (some-> (:start content) parse-timestamp-str convert)
         :to (some-> (:end content) parse-timestamp-str convert)}))))
