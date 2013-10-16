(ns ecstatic.io
  (:require [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [taoensso.timbre :as timbre
             :refer (error)])
  (:import [java.io PushbackReader]))

(defn config [in-dir]
  "Read config file"
  (binding [*read-eval* false]
    (try (with-open [r (io/reader (str in-dir "/config.clj"))]
           (read (PushbackReader. r)))
         (catch java.io.FileNotFoundException _
           (do (error "No config.clj file at" in-dir)
               (System/exit 0))))))

(defn read-template [path]
  (try (read-string (slurp path))
       (catch java.io.FileNotFoundException _
         (do (error "No template file at" (timbre/color-str :red path))
             (System/exit 0)))))

(defn all-page-and-post-files [in-dir]
  "Get all files in the page and post directories"
  (concat (file-seq (io/file in-dir "pages"))
          (file-seq (io/file in-dir "posts"))))

(defn- regex-file-seq
  "Lazily filter a directory based on regex."
  [regex in-dir]
  (filter #(re-find regex (.getPath %)) (file-seq (io/file in-dir))))

(defn file-ending-predicate [& endings]
  (let [regex (re-pattern (pp/cl-format nil ".*\\.(~{~a~^|~})" endings))]
    (fn [file]
      (re-find regex (.getPath file)))))

(def markdown-file? (file-ending-predicate "md" "markdown"))

(def clojure-file? (file-ending-predicate "clj"))

(defn md-files [in-dir]
  "Return a seq of markdown files from in-dir"
  (filter markdown-file? (all-page-and-post-files in-dir)))

(defn hiccup-files [in-dir]
  (filter clojure-file? (all-page-and-post-files in-dir)))

(defn page-files [in-dir]
  (concat (md-files in-dir) (hiccup-files in-dir)))

(defn split-file [path]
  "Return [metadata content] from a markdown file."
  (let [content (slurp path)
        idx (.indexOf content "---" 4)]
    [(subs content 4 idx) (subs content (+ idx 4))]))

(defn snippet-files
  ([in-dir]
     (file-seq (io/file in-dir "snippets")))
  ([in-dir name]
     "Get the snippet file with the name 'name'"
     (or (first (filter #(re-find (re-pattern name) (.getPath %))
                        (snippet-files in-dir)))
         (do (error "No snippet with name" (timbre/color-str :red name))
             (System/exit 0)))))

(defn file-type [file]
  "A dispatch function for filetypes."
  (cond (markdown-file? file) :markdown
        (clojure-file? file) :clojure))

(defn code-files [in-dir]
  "Return a sequence of clojure files that represent the custom code in 'code/"
  (regex-file-seq #".*\.clj" (io/file in-dir "code")))

(defn create-directory-scaffold [base-dir]
  "Create the scaffold for a new website project under 'base-dir'."
  (println "Creating directory scaffold.")
  (doseq [dir ["pages"
               "posts"
               "resources"
               "templates"
               "snippets"
               "code"]]
    (io/make-parents (io/file base-dir "src" dir "dummy")))
  (io/make-parents (io/file base-dir "site" "dummy"))
  (doseq [path [["templates" "base.clj"]
                ["templates" "index.clj"]
                ["templates" "page.clj"]
                ["templates" "post.clj"]]]
    (spit (apply io/file base-dir "src" path) ""))
  
  (let [scaffold-config {:site-name "FIXME: My Ecstatic Site"
                         :site-url "http://FIXME.com"
                         :site-description "FIXME: Enter a site description"
                         :site-author "FIXME: Enter author's name"}]
    (spit (io/file base-dir "src" "config.clj")
          (with-out-str (clojure.pprint/pprint scaffold-config))))
  
  (spit (io/file base-dir ".gitignore") "site/*\nsrc/target/*")
  nil)
