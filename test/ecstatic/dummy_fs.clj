(ns ecstatic.dummy-fs
  (:require [fs.core :as fs]))

(defn make-base-template
  []
  (spit "test-site/src/templates/base.clj"
        "[:html [:head [:title (:sitename met)]]
                [:body cont]]"))

(defn make-post-template
  []
  (spit "test-site/src/templates/post.clj"
        "[:div [:a {:href (:url met)} (:title met)]
            (:content cont)]"))

(defn make-page-template
  []
  (spit "test-site/src/templates/page.clj"
        "[:div [:a {:href (:url met)} (:title met)]
            (:content cont)]"))

(defn make-index-template
  []
  (spit "test-site/src/templates/index.clj"
        "[:div
          [:ul (for [post cont]
             [:li [:a {:href (:url post)} (:title post)]])]]"))

(defn make-posts
  []
  (map spit ["test-site/src/posts/2011-01-01-dummy-post-one.md"
             "test-site/src/posts/2013-04-05-dummy-post-two.markdown"]
       ["---\ntitle: Dummy One\ndate: 2011-01-01\ntags: dummy, foo\n---\n\nThis is a sentence. [This](http://google.com) is a link."
        
        "---\ntitle: Dummy Post two\ntags: foo, bar\ndate: 2013-04-05T10:11\n---\n\nDummy 2... This is a sentence. [This](http://google.com) is a link."

        ]))

(defn make-page
  []
  (spit "test-site/src/pages/books.md"
        "---\ntitle: Books\ntemplate: page\n---\n\n* Cryptonomicon\n*Reamde"))

(defn make-config
  []
  (spit "test-site/src/config.clj"
        "{:site-name \"Foobar\"
          :site-url  \"http://example.com\"
          :site-description \"Example site\"
          :site-author \"Mr. Foo Bar\"}
         "))

(defn create-dummy-fs
  []
  (do (doall (map #(fs/mkdirs %) ["test-site/src"
                                  "test-site/_site"
                                  "test-site/src/templates"
                                  "test-site/src/posts"
                                  "test-site/src/pages"]))
      (make-config)
      (doall (make-posts))
      (make-page)
      (make-base-template)
      (make-post-template)
      (make-page-template)
      (make-index-template)))