(ns ecstatic.io
  (:require [clojure.java.io :as io]
            [clojure.pprint :as pp])
  (:import [java.io PushbackReader]))

(defn config [in-dir]
  "Read config file"
  (binding [*read-eval* false]
    (with-open [r (io/reader (str in-dir "/config.clj"))]
      (read (PushbackReader. r)))))

(defn read-template [path]
  (read-string (slurp path)))

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
     (first (filter #(re-find (re-pattern name) (.getPath %))
                    (snippet-files in-dir)))))

(defn file-type [file]
  "A dispatch function for filetypes."
  (cond (markdown-file? file) :markdown
        (clojure-file? file) :clojure))

(defn code-files [in-dir]
  "Return a sequence of clojure files that represent the custom code in 'code/"
  (regex-file-seq #".*\.clj" (io/file in-dir "code")))

(defn create-directory-scaffold [base-dir]
  "Create the scaffold for a new website project"
  (prinln "Creating directory scaffold.")
  (doall (for [dir ["pages"
                    "posts"
                    "resources"
                    "templates"
                    "snippets"
                    "code"]]
           (io/make-parents (io/file base-dir "src" dir "dummy"))))
  (io/make-parents (io/file base-dir "site" "dummy")))
