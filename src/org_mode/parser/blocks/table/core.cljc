(ns org-mode.parser.blocks.table.core
  (:require
   [org-mode.parser.blocks.table.tags :as tags]
   [org-mode.parser.inline.core :as inline]
   [org-mode.parser.tokenizer.core :refer [token]]
   [org-mode.parser.utils.core :refer [end]]
   [strojure.parsesso.char :as char]
   [strojure.parsesso.parser :as p]))

(def table-delimiter (char/is "|"))

;; Separator -------------------------------------------------------------------

(def separator-char
  (p/alt (char/is "|")
         (char/is "+")))

(def separtor-cell
  (p/for [content (p/*many-till (char/is "-") (p/look-ahead separator-char))
          _ separator-char]
    (p/result (token tags/column content))))

(def table-separator
  (p/for [_ table-delimiter
          separators (p/*many-till separtor-cell end)]
    (p/result
     (token tags/table-separator separators))))

;; Table Rows ------------------------------------------------------------------

(def table-cell
  (p/for [content (p/*many-till inline/inline (p/look-ahead table-delimiter))
          _ table-delimiter]
    (p/result (token tags/column content))))

(def table-row
  (p/for [_ table-delimiter
          cells (p/*many-till table-cell end)]
    (p/result
     (token tags/table-row cells))))

;; Main ------------------------------------------------------------------------

(def parser
  (p/alt
   (p/maybe table-separator)
   (p/maybe table-row)))
