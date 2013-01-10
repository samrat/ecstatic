(ns ecstatic.core
  (:require [clojure.java.io :as io]
            [me.raynes.cegdown :as md]
            [me.raynes.laser :as laser]
            [fs.core :as fs])
  (:use clj-time.format
        [clj-time.core :only (year month)]))

(defn regex-file-seq
  "Lazily filter a directory based on regex."
  [regex dir]
  (filter #(re-find regex (.getPath %)) (file-seq (io/file dir))))

(defn md-files [dir]
  "Return list of markdown files from dir"
  (regex-file-seq #".*\.(md|markdown)" dir))

(defn split-file [path]
  (let [content (slurp path)
        idx (.indexOf content "---" 4)]
    [(subs content 4 idx) (subs content (+ idx 4))]))

(defn metadata [path]
  (let [meta (first (split-file path))]
    (reduce (fn [h [_ k v]]
              (let [key (keyword (.toLowerCase k))]
                (if (not (h key))
                  (assoc h key v)
                  h)))
            {} (re-seq #"([^:#\+]+): (.+)(\n|$)" meta))))

(defn file->html [path template]
  (let [t (slurp template)]
    (laser/document (laser/parse t)
                    (laser/element= :title)
                    (laser/content (:title (metadata path)))
                    (laser/class= "content")
                    (laser/html-content
                     (md/to-html (second (split-file path)) [:fenced-code-blocks])))))

(defn html-filename [path]
  (str (first (clojure.string/split path #"\.")) ".html"))

(defn slugify [date htmlfile]
  (let [parsed-date (parse date)]
    ;parsed-date
    (str "blog/" (year parsed-date) "/"  (month parsed-date) "/" htmlfile)
    ))

(defn clean-dir [dir]
  (fs/delete-dir dir))

(defn prepare-dirs [dir output]
  (let [output-structure (reduce (fn [dirs file]
                                   (let [date (parse (:date (metadata file)))
                                         slug (str (year date) "/" (month date))]
                                     (conj dirs (str output "/blog/" slug))))
                                 #{} (md-files dir))]
    (println output-structure)
    (map fs/mkdirs output-structure)))

(defn process-dir [dir output]
  (do (clean-dir output)
      (prepare-dirs dir output)
      (map (fn [file]
             (spit (str output "/" (slugify (:date (metadata file))
                                            (html-filename (.getName file))))
                   (file->html file "./mdrand/post.html")))
           (md-files dir))))

(defn foo
  "I don't do a whole lot."
  [x]
  (println x "Hello, World!"))
