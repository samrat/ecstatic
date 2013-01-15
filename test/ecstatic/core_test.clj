(ns ecstatic.core-test
  (:use clojure.test
        ecstatic.core
        ecstatic.io
        ecstatic.dummy-fs)
  (:require [fs.core :as fs]))

(defn dummy-fs-fixture [f]
  (create-dummy-fs)
  (create-site "test-site" "html")
  (f)
  (fs/delete-dir "test-site")
  (fs/delete-dir "html"))

(use-fixtures :once dummy-fs-fixture)

(deftest test-markdown
  (let [path  "test-site/posts/dummy_md.markdown"
        [metadata content] [(metadata path) (content path)]]
    (testing "Testing post tags"
      (is (= ["test", "dummy"] (:tags metadata))))))
