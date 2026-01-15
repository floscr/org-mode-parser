(ns org-mode.parser.blocks.greater.core
  (:require
   [clojure.string :as str]
   [org-mode.parser.inline.core :as inline]
   [org-mode.parser.tokenizer.core :refer [token]]
   [org-mode.parser.utils.core :refer [end]]
   [strojure.parsesso.char :as char]
   [strojure.parsesso.parser :as p]
   [strojure.parsesso.unicode :as unicode]))

(defn end-delimiter [block-type]
  (p/word (str "#+END_" (str/upper-case block-type)) :ic))

(def raw-content-blocks
  "Blocks that should preserve raw content without parsing inline elements"
  #{"verse" "comment" "example" "export" "src"})

(def parser
  "Generic parser for all greater blocks: #+BEGIN_<type> ... #+END_<type>"
  (p/for [begin-prefix (p/word "#+BEGIN_" :ic)
          block-type (p/+many unicode/letter-or-digit?)
          args inline/raw-parser
          content (p/*many-till p/any-token
                                (p/maybe (p/after char/newline
                                                  (p/look-ahead (end-delimiter (char/str* block-type))))))
          end-prefix (p/word "#+END_" :ic)
          end-type (p/+many unicode/letter-or-digit?)
          _ end]
    (let [block-type-str (str/lower-case (char/str* block-type))
          content-str (char/str* content)
          parse-content? (not (contains? raw-content-blocks block-type-str))
          parsed-content (when (and parse-content? (seq content-str))
                           (p/parse inline/parser content-str))]
      (p/result
       (token :block
              {:type block-type-str
               :begin (str begin-prefix (char/str* block-type))
               :end (str end-prefix (char/str* end-type))
               :args args
               :content (if parse-content? parsed-content content-str)})))))
