(ns org-mode.parser.blocks.planning.core
  (:require
   [org-mode.parser.blocks.keyword-line :as keyword-line]
   [org-mode.parser.blocks.planning.tags :as tags]
   [strojure.parsesso.parser :as p]))

(def scheduled (keyword-line/parser "SCHEDULED" tags/scheduled))
(def deadline (keyword-line/parser "DEADLINE" tags/deadline))
(def closed (keyword-line/parser "CLOSED" tags/closed))

(def parser
  (p/alt
   (p/maybe scheduled)
   (p/maybe deadline)
   (p/maybe closed)))
