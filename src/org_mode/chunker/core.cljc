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
  is treated as an empty string."
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
            (recur (inc idx)
                   (inc idx)
                   (conj acc (subs s start idx)))
            (recur start (inc idx) acc)))))))
