(ns ecstatic.core
  (:require [clojure.java.io :as io]
            [me.raynes.cegdown :as md]
            [me.raynes.laser :as laser]
            [fs.core :as fs])
  (:use [clj-time.core :only (year month)]
        clj-time.format))

(defn regex-file-seq
  "Lazily filter a directory based on regex."
  [regex in-dir]
  (filter #(re-find regex (.getPath %)) (file-seq (io/file in-dir))))

(defn md-files [in-dir]
  "Return a seq of markdown files from in-dir"
  (regex-file-seq #".*\.(md|markdown)" in-dir))

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
                    (laser/or (laser/element= :title) (laser/element= :h1))
                    (laser/content (:title (metadata path)))
                    (laser/class= "content")
                    (laser/html-content
                     (md/to-html (second (split-file path)) [:fenced-code-blocks])))))

(defn slugify [file]
  (let [metadata (metadata file)
        date (:date metadata)
        parsed-date (parse date)]
    (str "/blog/"
         (year parsed-date) "/"
         (month parsed-date) "/"
         (-> (:title metadata)
             .toLowerCase
             space->dash))))

(defn clean-dir [dir]
  (fs/delete-dir dir))

(defn space->dash [s]
  (clojure.string/replace s " " "-"))

(defn prepare-dirs [dir output]
  (let [output-structure (reduce (fn [dirs file]
                                   (let [slug (slugify file)]
                                     (conj dirs (str output slug))))
                                 #{} (md-files dir))]
    ;;(println output-structure)
    (map fs/mkdirs output-structure)))

(defn process-dir [in-dir output]
  (do
      (prepare-dirs in-dir output)
      (map (fn [file]
             (let [slug (slugify file)]
               (spit (str output "/" slug "/index.html")
                     (file->html file (str in-dir "/templates/post.html")))))
           (md-files in-dir))))

(defn all-posts [in-dir]
  (map (fn [file] (assoc (metadata file) :file file))  (md-files in-dir)))

(defn generate-index [template in-dir]
  )

(defn foo
  "I don't do a whole lot."
  [x]
  (println x "Hello, World!"))
