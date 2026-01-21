(ns org-mode.chunker.core)

(defn- heading-start?
  "Returns true when the string starting at `idx` is a heading marker: one or
  more `*` characters followed by a space or tab."
  [^String s idx len]
  (when (< idx len)
    (loop [i idx]
      (cond
        (>= i len) false
        (= \* (.charAt s i)) (recur (inc i))
        (and (> i idx)
             (let [ch (.charAt s i)]
               (or (= ch \space) (= ch \tab)))) true
        :else false))))

(defn chunk-headings
  "Chunks headings by scanning for newlines whose following line starts with a
  heading marker. This avoids large-regex overhead on big documents while still
  ensuring only `*` lines followed by a space/tab count as headings. Nil input
  is treated as an empty string.

  Preserves blank lines before headings by including the trailing newline in
  the chunk when a blank line precedes the heading."
  [s]
  (let [^String s (or s "")
        len (.length s)]
    (loop [start 0
           idx 0
           acc []]
      (if (>= idx len)
        (cond
          (< start len) (conj acc (subs s start len))
          (empty? acc) [""]
          :else (conj acc ""))
        (let [ch (.charAt s idx)]
          (if (and (= ch \newline)
                   (heading-start? s (inc idx) len))
            ;; Check if there's a blank line (newline before this one)
            (let [has-blank? (and (> idx 0)
                                  (= (.charAt s (dec idx)) \newline))
                  ;; Include the extra newline in chunk if blank line exists
                  chunk-end (if has-blank? (inc idx) idx)]
              (recur (inc idx)
                     (inc idx)
                     (conj acc (subs s start chunk-end))))
            (recur start (inc idx) acc)))))))
