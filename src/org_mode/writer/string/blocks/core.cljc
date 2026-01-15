(ns org-mode.writer.string.blocks.core
  (:require
   [org-mode.parser.blocks.clock.tags :as clock-tags]
   [org-mode.parser.blocks.comment.tags :as comment-tags]
   [org-mode.parser.blocks.drawer.tags :as drawer-tags]
   [org-mode.parser.blocks.greater.tags :as greater-tags]
   [org-mode.parser.blocks.horizontal-rule.tags :as hr-tags]
   [org-mode.parser.blocks.keyword.tags :as kw-tags]
   [org-mode.parser.blocks.list.tags :as list-tags]
   [org-mode.parser.blocks.planning.tags :as planning-tags]
   [org-mode.parser.blocks.table.tags :as table-tags]
   [org-mode.parser.blocks.tags :as blk-tags]
   [org-mode.writer.string.blocks.clock.core :as clock]
   [org-mode.writer.string.blocks.comment.core :as comment]
   [org-mode.writer.string.blocks.drawer.core :as drawer]
   [org-mode.writer.string.blocks.greater.core :as greater]
   [org-mode.writer.string.blocks.horizontal-rule.core :as hr]
   [org-mode.writer.string.blocks.keyword.core :as kw]
   [org-mode.writer.string.blocks.list.core :as l]
   [org-mode.writer.string.blocks.planning.core :as planning]
   [org-mode.writer.string.blocks.table.core :as table]
   [org-mode.writer.engine :as engine]
   [org-mode.writer.string.inline.core :as inline]))

(defn- newline-string [[_ count]]
  (apply str (repeat count "\n")))

(defn- text-block->string [{:keys [render-inline]} [_ tokens]]
  (str (render-inline tokens) "\n"))

(def default-block-writers
  {blk-tags/newlines (fn [_ tok] (newline-string tok))
   blk-tags/text (fn [ctx tok] (text-block->string ctx tok))
   kw-tags/keyword-line (fn [_ tok] (kw/token->string tok))
   clock-tags/clock (fn [_ tok] (clock/token->string tok))

   planning-tags/scheduled (fn [_ tok] (planning/token->string tok))
   planning-tags/deadline (fn [_ tok] (planning/token->string tok))
   planning-tags/closed (fn [_ tok] (planning/token->string tok))

   comment-tags/comment-line (fn [_ tok] (comment/token->string tok))
   hr-tags/horizontal-rule (fn [_ tok] (hr/token->string tok))

   drawer-tags/drawer-start (fn [_ tok] (drawer/token->string tok))
   drawer-tags/drawer-end (fn [_ tok] (drawer/token->string tok))
   drawer-tags/property (fn [_ tok] (drawer/token->string tok))

   table-tags/table-row (fn [_ tok] (table/token->string tok))
   table-tags/table-separator (fn [_ tok] (table/token->string tok))

   list-tags/list-item (fn [_ tok] (l/token->string tok))

   greater-tags/block (fn [_ tok] (greater/token->string tok))})

(defn token->string [tok]
  (let [handler (get default-block-writers (first tok) (fn [_ t] (str t)))]
    (handler {:render-inline (inline/renderer)} tok)))

(defn- line->string [{:keys [render-inline]} line]
  (str (render-inline line) "\n"))

(defn tokens->string
  "Render parser block tokens back to org text."
  ([tokens] (tokens->string tokens {}))
  ([tokens _]
   (engine/render-blocks {:tokens tokens
                          :inline-renderer (inline/renderer)
                          :handlers default-block-writers
                          :default-handler (fn [_ tok] (str tok))
                          :line-handler line->string
                          :init ""
                          :step (fn [acc node] (str acc node))})))
