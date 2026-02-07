(ns org-mode.computed.heading.links-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [org-mode.computed.heading.links :as sut]
   [org-mode.parser.heading.core :as heading]))

;; Helpers --------------------------------------------------------------------

(defn- parse-heading
  "Parse an org heading string and return the first header."
  [s]
  (first (:headers (heading/parse-document s))))

;; Tests ----------------------------------------------------------------------

(deftest link-token?-test
  (testing "plain link"
    (is (true? (sut/link-token? [:link "https://example.com"]))))

  (testing "link with title"
    (is (true? (sut/link-token? [:link-with-title {:link "url" :title "title"}]))))

  (testing "raw link"
    (is (true? (sut/link-token? [:raw-link "https://example.com"]))))

  (testing "non-link"
    (is (false? (sut/link-token? [:bold "text"])))
    (is (false? (sut/link-token? "plain string")))))

(deftest link-href-test
  (testing "plain link href"
    (is (= "https://example.com"
           (sut/link-href [:link "https://example.com"]))))

  (testing "titled link href"
    (is (= "https://example.com"
           (sut/link-href [:link-with-title {:link "https://example.com"
                                             :title "Example"}]))))

  (testing "raw link href"
    (is (= "https://example.com"
           (sut/link-href [:raw-link "https://example.com"])))))

(deftest link-title-test
  (testing "plain link title falls back to href"
    (is (= "https://example.com"
           (sut/link-title [:link "https://example.com"]))))

  (testing "titled link returns title"
    (is (= "Example"
           (sut/link-title [:link-with-title {:link "https://example.com"
                                              :title "Example"}]))))

  (testing "raw link title falls back to URL"
    (is (= "https://example.com"
           (sut/link-title [:raw-link "https://example.com"])))))

(deftest title-links-test
  (testing "extracts links from title"
    (let [heading (parse-heading "* Foo [[https://abc.xyz]] bar [[link][title]]")]
      (is (= 2 (count (sut/title-links heading))))
      (is (= "https://abc.xyz" (sut/link-href (first (sut/title-links heading)))))
      (is (= "link" (sut/link-href (second (sut/title-links heading)))))))

  (testing "no links in title"
    (let [heading (parse-heading "* Plain heading")]
      (is (empty? (sut/title-links heading))))))

(deftest body-links-test
  (testing "extracts links from body text"
    (let [heading (parse-heading "* Heading\n[[https://abc.xyz]] other [[link][title]]")]
      (is (= 2 (count (sut/body-links heading))))
      (is (= "https://abc.xyz" (sut/link-href (first (sut/body-links heading)))))))

  (testing "extracts links from list items"
    (let [heading (parse-heading "* Heading\n- Item with [[https://example.com]]")]
      (is (= 1 (count (sut/body-links heading))))))

  (testing "no links in body"
    (let [heading (parse-heading "* Heading\nPlain text only")]
      (is (empty? (sut/body-links heading))))))

(deftest heading-links-test
  (testing "combines title and body links"
    (let [heading (parse-heading "* Title [[https://a.com]]\n[[https://b.com]] text")]
      (is (= 2 (count (sut/heading-links heading))))
      (is (= "https://a.com" (sut/link-href (first (sut/heading-links heading)))))
      (is (= "https://b.com" (sut/link-href (second (sut/heading-links heading)))))))

  (testing "combines bracket and raw links"
    (let [heading (parse-heading "* See [[link][docs]] and https://raw.com\nBody https://body.com")]
      (is (= 3 (count (sut/heading-links heading))))
      (is (= "link" (sut/link-href (first (sut/heading-links heading)))))
      (is (= "https://raw.com" (sut/link-href (second (sut/heading-links heading)))))
      (is (= "https://body.com" (sut/link-href (nth (sut/heading-links heading) 2)))))))

(deftest raw-link-extraction-test
  (testing "raw links in title"
    (let [heading (parse-heading "* Visit https://example.com today")]
      (is (= 1 (count (sut/title-links heading))))
      (is (= "https://example.com" (sut/link-href (first (sut/title-links heading)))))))

  (testing "raw links in body"
    (let [heading (parse-heading "* Heading\nSee https://example.com for details")]
      (is (= 1 (count (sut/body-links heading))))
      (is (= "https://example.com" (sut/link-href (first (sut/body-links heading)))))))

  (testing "raw links in list items"
    (let [heading (parse-heading "* Heading\n- Check https://example.com")]
      (is (= 1 (count (sut/body-links heading))))
      (is (= "https://example.com" (sut/link-href (first (sut/body-links heading))))))))
