(ns ecstatic.io
  (:require [clojure.java.io :as io])
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

(defn markdown-file? [file]
  (re-find #".*\.(md|markdown)" (.getPath file)))

(defn clojure-file? [file]
  (re-find #".*\.clj" (.getPath file)))

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

;; TODO: refactor with higher order function!

(defn file-type [file]
  "A dispatch function for filetypes."
  (cond (markdown-file? file) :markdown
        (clojure-file? file) :clojure))