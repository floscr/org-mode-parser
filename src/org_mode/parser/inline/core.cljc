(ns org-mode.parser.inline.core
  (:require
   [cuerdas.core :as str]
   [org-mode.parser.inline.delimiters :as delimiters]
   [org-mode.parser.inline.tags :as tags]
   [org-mode.parser.tokenizer.core :refer [token token-between]]
   [org-mode.parser.utils.core :refer [between end]]
   [strojure.parsesso.char :as char]
   [strojure.parsesso.parser :as p]
   [strojure.parsesso.unicode :as unicode]))

;; Style Parsers ---------------------------------------------------------------

(def bold
  (token-between delimiters/bold tags/bold))

(def italic
  (token-between delimiters/italic tags/italic))

(def underline
  (token-between delimiters/underline tags/underline))

(def verbatim
  (token-between delimiters/verbatim tags/verbatim))

(def code
  (token-between delimiters/code tags/code))

(def strike-through
  (token-between delimiters/strike-through tags/strike-through))

;; String Parsers --------------------------------------------------------------

(def space
  (-> (p/+many unicode/space?)
      (p/value char/str*)))

(def word
  (-> (p/*many-till p/any-token (p/look-ahead
                                 (p/alt
                                  unicode/space?
                                  char/newline
                                  (p/eof))))
      (p/value char/str*)))

;; Special Parsers -------------------------------------------------------------

(def link
  (-> (between delimiters/link-start delimiters/link-end)
      (p/value
       (fn [v]
         (let [[link title] (str/split (char/str* v) delimiters/link-separator 2)]
           (if title
             (token tags/link-with-title {:link link :title title})
             (token tags/link link)))))))

(def timestamp
  (-> (between delimiters/timestamp-start delimiters/timestamp-end)
      (p/value #(token tags/timestamp (char/str* %)))))

(def inactive-timestamp
  (-> (between delimiters/inactive-timestamp-start delimiters/inactive-timestamp-end)
      (p/value #(token tags/inactive-timestamp (char/str* %)))))

(def timestamp-range
  "Parse a timestamp range: <timestamp>--<timestamp>"
  (p/for [start timestamp
          _ (p/word "--")
          end timestamp]
    (p/result (token tags/timestamp-range {:start (second start) :end (second end)}))))

(def inactive-timestamp-range
  "Parse an inactive timestamp range: [timestamp]--[timestamp]"
  (p/for [start inactive-timestamp
          _ (p/word "--")
          end inactive-timestamp]
    (p/result (token tags/inactive-timestamp-range {:start (second start) :end (second end)}))))

(def footnote
  (-> (between delimiters/footnote-start delimiters/footnote-end)
      (p/value
       (fn [v]
         (let [[name definition] (str/split (char/str* v) delimiters/footnote-separator 2)]
           (if definition
             (token tags/footnote-def {:name name :definition definition})
             (token tags/footnote-ref name)))))))

(def target
  (-> (between delimiters/target-start delimiters/target-end)
      (p/value #(token tags/target (char/str* %)))))

;; TODO Use?
(def macro
  (-> (between delimiters/macro-start delimiters/macro-end)
      (p/value
       (fn [v]
         (let [content (char/str* v)
               paren-pos (str/index-of content delimiters/macro-args-start)]
           (if paren-pos
             (let [name (str/slice content 0 paren-pos)
                   args-str (str/slice content (inc paren-pos) (str/last-index-of content delimiters/macro-args-end))
                   args (when (not (str/empty? args-str))
                          (map str/trim (str/split args-str #",")))]
               (token tags/macro {:name name :args (vec args)}))
             (token tags/macro {:name content})))))))

(def stats-cookie
  (p/for [_ (char/is delimiters/stats-cookie-start)
          from (p/*many-till char/number? (p/look-ahead (p/alt (char/is "/") (char/is "%"))))
          del (p/alt (char/is "/") (char/is "%"))
          percent? (p/result (= (str del) "%"))
          to (if percent?
               (char/is delimiters/stats-cookie-end)
               (p/*many-till char/number? (char/is delimiters/stats-cookie-end)))]
    (p/result
     (if percent?
       (token tags/stats-percent-cookie (parse-long (char/str* from)))
       (token tags/stats-range-cookie {:from (parse-long (char/str* from)) :to (parse-long (char/str* to))})))))

;; Main ------------------------------------------------------------------------

(def raw-inline
  (p/alt
   (p/maybe space)
   word))

(def raw-parser
  (p/*many-till raw-inline end))

(def inline
  (p/alt
   (p/maybe link)
   (p/maybe target)
   (p/maybe footnote)
   (p/maybe stats-cookie)
   (p/maybe timestamp-range)
   (p/maybe inactive-timestamp-range)
   (p/maybe timestamp)
   (p/maybe inactive-timestamp)
   (p/maybe macro)
   (p/maybe bold)
   (p/maybe italic)
   (p/maybe underline)
   (p/maybe verbatim)
   (p/maybe code)
   (p/maybe strike-through)
   (p/maybe space)
   word))

(def parser
  (p/*many-till inline end))

(defn parse [s]
  (p/parse parser s))

(comment
  (parse "[foo]")
  (parse "[12/12]")
  nil)
