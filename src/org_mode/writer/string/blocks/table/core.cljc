(ns org-mode.writer.string.blocks.table.core
  (:require
   [clojure.string :as str]
   [org-mode.writer.string.inline.core :as inline]))

(defn- write-table-separator [[_ columns]]
  (let [cols (map (fn [[_ dashes]] (apply str dashes)) columns)
        middle (str/join "+" cols)]
    (str "|" middle "|\n")))

(defn- write-table-row [[_ columns]]
  (let [cols (map (fn [[_ content]] (inline/tokens->string content)) columns)
        middle (str/join "|" cols)]
    (str "|" middle "|\n")))

(defn token->string [[tag :as tok]]
  (case tag
    :table-separator (write-table-separator tok)
    :table-row (write-table-row tok)
    (str tok)))

