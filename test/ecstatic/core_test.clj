(ns ecstatic.core-test
  (:use clojure.test
        ecstatic.core
        ecstatic.io
        ecstatic.dummy-fs)
  (:require [fs.core :as fs]))

(defn dummy-fs-fixture [f]
  (create-dummy-fs)
  (create-site "test-site/src" "test-site/_site")
  (f)
  (fs/delete-dir "test-site"))

(use-fixtures :once dummy-fs-fixture)

(deftest test-markdown
  (let [path  "test-site/src/posts/2011-01-01-dummy-post-one.md"
        [metadata content] [(metadata path) (content path)]]
    (testing "Testing post tags"
      (is (= ["dummy", "foo"] (:tags metadata))))
    (testing "Testing post title"
      (is (= "Dummy One" (:title metadata))))
    (testing "Testing post content"
      (is (= "This is a sentence."
             (re-find #"This is a sentence\." content))))
    (testing "Post date"
      (is (= "2011-01-01")))))

(deftest test-io
  (is (fs/exists? "test-site/_site/index.html"))
  (is (fs/exists?
       "test-site/_site/blog/2011/01/dummy-post-one/index.html"))
  (is (fs/exists? "test-site/_site/books/index.html")))

#_(deftest test-rss
  (let [feed-dir "test-site/_site/feeds/"]
    (is (fs/exists? (str feed-dir "all.xml")))
    (is (fs/exists? (str feed-dir "foo.xml")))
    (testing "all.xml"
      (is (= "<title>Dummy One</title>"
             (re-find #"<title>Dummy One</title>"
                      (slurp (str feed-dir "all.xml"))))))

    (testing "foo.xml"
      (is (= "<title> Dummy Post two </title>"
             (re-find #"<title> Dummy Post two </title>"
                      (slurp (str feed-dir "foo.xml"))))))))

(deftest test-processed-site
  (let [out (slurp
             "test-site/_site/blog/2011/01/dummy-post-one/index.html")]
    (is (= "This is a sentence."
           (re-find #"This is a sentence\." out)))))