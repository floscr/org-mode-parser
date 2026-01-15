(ns org-mode.parser.utils.core
  (:require
   [strojure.parsesso.char :as char]
   [strojure.parsesso.parser :as p]))

(def end (p/alt char/newline p/eof))

(def between-end-parser
  (char/is-not \newline))

(defn between
  ([delimiter]
   (between delimiter delimiter))
  ([start end]
   (p/for [_ (p/+many (p/word start))
           s (p/*many-till between-end-parser (p/maybe
                                               (p/+many (p/word end))))]
     (if (or (empty? s)
             (= '(\space) s))
       (p/fail "Needs to be at least 1 character")
       (p/result (char/str* s))))))

(comment
  (p/parse (between "[[" "]]") "[[some link][right]]")
  (p/parse (between "*") "***so many***")
  (p/parse (between "**") "**lazy tagged result ****")
  (p/parse (between "'") "'1'")

  (p/parse (between "+") "+
+")
  (p/parse (between "**") "*****")
  (p/parse (between "**") "****")
  (p/parse (between "**") "**fails *")
  nil)
