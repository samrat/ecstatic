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
                  ;; parse post tags
                  (if (= key :tags)
                    (assoc h key (->> (clojure.string/split v #",")
                                      (map #(.trim %))
                                      vec))
                    (assoc h key v))
                  h)))
            {} (re-seq #"([^:#\+]+): (.+)(\n|$)" meta))))

(defn file->html [path template]
  (let [t (slurp template)]
    (laser/document (laser/parse t)
                    (laser/or (laser/element= :title) (laser/element= :h1))
                    (laser/content (:title (metadata path)))
                    (laser/id= "content")
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
    (doall (map fs/mkdirs output-structure))))

(defn write-files [in-dir output]
  (doall (map (fn [file]
                (let [slug (slugify file)]
                  (spit (str output "/" slug "/index.html")
                        (file->html file (str in-dir "/templates/post1.html")))))
              (md-files in-dir))))

(defn process-dir [in-dir output]
  (let [tmp (.getPath (fs/temp-dir "ecst"))]
    (prepare-dirs in-dir tmp)
    (write-files in-dir tmp)
    (copy-resources in-dir tmp)
    (clean-dir output)
    (fs/copy-dir tmp output)
    (fs/delete-dir tmp)))

(defn all-posts [in-dir]
  (map (fn [file] (assoc (metadata file) :file file))  (md-files in-dir)))

(defn all-tags [posts]
  (reduce conj #{} (map )))

(defn posts-in-tag [posts]
  (reduce (fn [p post]
            (println p)
            (let [tags (:tags post)]
              (for [tag tags]
                (assoc-in p [0 tag] post))))
          [] posts))

(defn copy-resources [in-dir output]
  (fs/copy-dir (str in-dir "/resources") (str output "/resources")))

(defn generate-index [template in-dir]
  (let [t (slurp template)]
    (laser/document (laser/parse t)
                    (laser/id= "content")
                    )))

(defn foo
  "I don't do a whole lot."
  [x]
  (println x "Hello, World!"))
