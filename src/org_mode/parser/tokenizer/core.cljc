(ns org-mode.parser.tokenizer.core
  (:require
   [org-mode.parser.utils.core :refer [between]]
   [strojure.parsesso.parser :as p]))

(defn token
  ([tag]
   [tag])
  ([tag content]
   [tag content]))

(defn token-between [delimiter tag]
  (-> (between delimiter)
      (p/value #(token tag %))))
