(ns org-mode.writer.engine)

(defn- normalize-content [content]
  (cond
    (nil? content) []
    (sequential? content) content
    :else [content]))

(defn inline-renderer
  "Builds a function that renders inline token sequences.

  Options:
  - :handlers map of tag -> handler fn (render-children fn, token) => rendered value
  - :default-handler fallback handler when tag not found
  - :scalar-handler handles non-vector tokens (defaults to identity)
  - :combine function that aggregates a sequence of rendered children. Defaults to vec."
  [{:keys [handlers default-handler scalar-handler combine]
    :or {handlers {}
         combine vec}}]
  (let [scalar-handler (or scalar-handler identity)
        default-handler (or default-handler (fn [_ token] (scalar-handler token)))]
    (letfn [(render-token [token]
              (cond
                (nil? token) nil
                (vector? token)
                (let [[tag] token
                      handler (get handlers tag default-handler)]
                  (handler render-content token))
                :else (scalar-handler token)))
            (render-content [content]
              (->> (normalize-content content)
                   (map render-token)
                   (remove nil?)
                   combine))]
      render-content)))

(defn default-line?
  "Heuristic to detect plain inline lines emitted by the parser."
  [token]
  (and (sequential? token)
       (not (vector? token))
       (let [first-token (first token)]
         (not (keyword? first-token)))))

(defn render-blocks
  "Generic block renderer.

  Options:
  - :tokens sequence of parser outputs
  - :inline-renderer function produced by inline-renderer
  - :handlers map of block tag -> handler ((ctx token) => rendered block)
  - :default-handler fallback handler when no tag match
  - :line-handler function handling plain inline lines (ctx, line) => rendered block
  - :line? predicate to identify inline lines (defaults to default-line?)
  - :step reducing function to accumulate results (defaults to conj)
  - :init initial accumulator (defaults to [])
  - :finalize fn applied to reduce result (defaults to identity)"
  [{:keys [tokens inline-renderer handlers default-handler line-handler line?
           step init]
    :or {handlers {}
         step conj
         init []}}]
  (let [line? (or line? default-line?)
        default-handler (or default-handler (fn [_ token] token))
        ctx {:render-inline inline-renderer}]
    (reduce (fn [acc token]
              (let [node (cond
                           (line? token) (line-handler ctx token)
                           (vector? token)
                           (let [[tag] token
                                 handler (get handlers tag default-handler)]
                             (handler ctx token))
                           :else token)]
                (if (nil? node)
                  acc
                  (step acc node))))
            init
            (or tokens []))))
