(ns org-mode.computed.document.core
  "Document-level computed properties.

   Operates on parsed documents of the shape:
     {:body <block-tokens> :headers [{:level :title :body :trailing-blank?} ...]}")

;; Accessors ------------------------------------------------------------------

(defn headings
  "Return all headings from a parsed document.

   Options:
     :update-heading – optional (fn [heading] heading) transform applied to each heading"
  [document & {:keys [update-heading]}]
  (let [headers (:headers document)]
    (if update-heading
      (mapv update-heading headers)
      headers)))

(defn find-headings
  "Return headings matching `pred` from a parsed document."
  [pred document]
  (filterv pred (:headers document)))

(defn headings-at-level
  "Return headings at a specific level."
  [level document]
  (find-headings #(= (:level %) level) document))

(defn top-level-headings
  "Return all level-1 headings."
  [document]
  (headings-at-level 1 document))

(defn multi-document-headings
  "Collect headings from multiple parsed documents.

   Options:
     :update-heading – optional (fn [document heading] heading) transform.
                       Receives both the source document and the heading."
  [documents & {:keys [update-heading]}]
  (mapcat
   (fn [doc]
     (let [updater (when update-heading
                     (fn [heading]
                       (update-heading doc heading)))]
       (headings doc :update-heading updater)))
   documents))
