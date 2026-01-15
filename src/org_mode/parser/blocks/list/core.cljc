(ns org-mode.parser.blocks.list.core
  (:require
   [org-mode.parser.blocks.list.tags :as tags]
   [org-mode.parser.inline.core :as inline]
   [org-mode.parser.tokenizer.core :refer [token]]
   [strojure.parsesso.char :as char]
   [strojure.parsesso.parser :as p]
   [strojure.parsesso.unicode :as unicode]))

;; Checkbox parser -------------------------------------------------------------

(def checkbox-parser
  (p/for [_ (char/is "[")
          state (p/alt (char/is " ")
                       (char/is "X")
                       (char/is "x")
                       (char/is "-"))
          _ (char/is "]")
          _ (char/is " ")]
    (p/result state)))

;; List marker parsers ---------------------------------------------------------

(def unordered-marker
  (p/for [marker (p/alt (char/is "-")
                        (char/is "+")
                        (char/is "*"))
          _ (char/is " ")]
    (p/result (token :unordered marker))))

(def ordered-marker
  (p/for [marker (p/+many char/letter-or-number?)
          delimiter (p/alt (char/is ".")
                           (char/is ")"))
          _ (char/is " ")]
    (p/result (token :ordered {:marker marker
                               :delimiter delimiter}))))

(def list-marker
  (p/alt unordered-marker ordered-marker))

;; Main list parser ------------------------------------------------------------

(def parser
  (p/for [indent (p/value (p/*many unicode/space?) count)
          marker list-marker
          checkbox (p/option checkbox-parser)
          content inline/parser]
    (p/result
     (let [data (cond-> {:indent indent
                         :marker marker
                         :tokens content}
                  checkbox (assoc :checkbox checkbox))]
       (token tags/list-item data)))))
