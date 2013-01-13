(ns ecstatic.io
  (:require [clojure.java.io :as io])
  (:import [java.io PushbackReader]))

(defn config [in-dir]
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
