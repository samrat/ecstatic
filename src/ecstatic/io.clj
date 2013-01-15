(ns ecstatic.io
  (:require [clojure.java.io :as io])
  (:import [java.io PushbackReader]))

(defn config [in-dir]
  "Read config file"
  (binding [*read-eval* false]
    (with-open [r (io/reader (str in-dir "/config.clj"))]
      (read (PushbackReader. r)))))

(defn- regex-file-seq
  "Lazily filter a directory based on regex."
  [regex in-dir]
  (filter #(re-find regex (.getPath %)) (file-seq (io/file in-dir))))

(defn md-files [in-dir]
  "Return a seq of markdown files from in-dir"
  (regex-file-seq #".*\.(md|markdown)" in-dir))

(defn md-posts [in-dir]
  (md-files (str in-dir "/posts")))

(defn split-file [path]
  "Return [metadata content] from a markdown file."
  (let [content (slurp path)
        idx (.indexOf content "---" 4)]
    [(subs content 4 idx) (subs content (+ idx 4))]))

