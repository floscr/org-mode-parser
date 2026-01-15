(ns org-mode.parser.blocks.clock.core
  (:require
   [org-mode.parser.blocks.clock.tags :as tags]
   [org-mode.parser.blocks.keyword-line :as keyword-line]))

(def parser
  (keyword-line/parser "CLOCK" tags/clock))
