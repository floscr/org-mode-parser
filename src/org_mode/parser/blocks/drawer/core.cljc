(ns org-mode.parser.blocks.drawer.core
  (:require
   [clojure.string :as str]
   [org-mode.parser.blocks.drawer.tags :as tags]
   [org-mode.parser.inline.core :as inline]
   [org-mode.parser.tokenizer.core :refer [token]]
   [org-mode.parser.utils.core :refer [between end]]
   [strojure.parsesso.char :as char]
   [strojure.parsesso.parser :as p]))

(def property
  ":KEY: value
  Without a value this is a `drawer`"
  (p/for [property-key (between ":")
          value inline/parser]
    (p/result
     (token tags/property {:key property-key
                           :value value}))))

(def drawer
  ":KEY: `[:drawer-start key]`
  :END: `[:drawer-end key]`"
  (p/for [drawer-name (between ":")
          _ end]
    (p/result
     (let [s (char/str* drawer-name)]
       (if (= (str/lower-case s) "end")
         (token tags/drawer-end s)
         (token tags/drawer-start s))))))

(def parser
  (p/alt
   (p/maybe drawer)
   (p/maybe property)))
