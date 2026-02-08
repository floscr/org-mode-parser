(ns org-mode.actions.heading.properties-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [org-mode.actions.heading.properties :as sut]
   [org-mode.computed.heading.blocks :as c.blocks]
   [org-mode.core :as org]
   [org-mode.parser.inline.core :as p.inline]))

(defn- make-heading
  [title-str & {:keys [body]}]
  {:level 1
   :title (p.inline/parse title-str)
   :body (or body [])
   :trailing-blank? false})

(defn- parsed-heading
  "Parse org text and return the first heading."
  [s]
  (first (:headers (org/parse s))))

(deftest set-property-test
  (testing "creates drawer when none exists"
    (let [h (make-heading "My task")
          result (sut/set-property "ID" "abc" h)
          props (c.blocks/body-properties result)]
      (is (contains? props "ID"))
      ;; Should have drawer-start, property, drawer-end
      (is (some c.blocks/drawer-start-token? (:body result)))
      (is (some c.blocks/drawer-end-token? (:body result)))))

  (testing "adds property to existing drawer"
    (let [h (parsed-heading "* My task\n:PROPERTIES:\n:ID: abc\n:END:")
          result (sut/set-property "EFFORT" "2h" h)
          props (c.blocks/body-properties result)]
      (is (contains? props "ID"))
      (is (contains? props "EFFORT"))))

  (testing "replaces existing property"
    (let [h (parsed-heading "* My task\n:PROPERTIES:\n:ID: abc\n:END:")
          result (sut/set-property "ID" "xyz" h)
          props (c.blocks/body-properties result)]
      (is (contains? props "ID"))
      ;; Only one ID property
      (let [prop-count (count (filter c.blocks/property-token? (:body result)))]
        (is (= 1 prop-count))))))

(deftest remove-property-test
  (testing "removes a property"
    (let [h (parsed-heading "* My task\n:PROPERTIES:\n:ID: abc\n:EFFORT: 2h\n:END:")
          result (sut/remove-property "ID" h)
          props (c.blocks/body-properties result)]
      (is (not (contains? props "ID")))
      (is (contains? props "EFFORT"))))

  (testing "removes drawer when last property removed"
    (let [h (parsed-heading "* My task\n:PROPERTIES:\n:ID: abc\n:END:")
          result (sut/remove-property "ID" h)]
      (is (not (some c.blocks/drawer-start-token? (:body result))))
      (is (not (some c.blocks/drawer-end-token? (:body result))))))

  (testing "no-op when property doesn't exist"
    (let [h (make-heading "My task")]
      (is (= h (sut/remove-property "ID" h))))))

(deftest set-properties-test
  (testing "sets all properties"
    (let [h (make-heading "My task")
          result (sut/set-properties {"ID" "abc" "EFFORT" "2h"} h)
          props (c.blocks/body-properties result)]
      (is (contains? props "ID"))
      (is (contains? props "EFFORT"))))

  (testing "replaces existing drawer"
    (let [h (parsed-heading "* My task\n:PROPERTIES:\n:ID: old\n:END:")
          result (sut/set-properties {"CATEGORY" "work"} h)
          props (c.blocks/body-properties result)]
      (is (not (contains? props "ID")))
      (is (contains? props "CATEGORY"))))

  (testing "empty map removes drawer"
    (let [h (parsed-heading "* My task\n:PROPERTIES:\n:ID: abc\n:END:")
          result (sut/set-properties {} h)]
      (is (not (some c.blocks/drawer-start-token? (:body result)))))))
