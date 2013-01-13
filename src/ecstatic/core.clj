(ns ecstatic.core
  (:require [me.raynes.cegdown :as md]
            [me.raynes.laser :as l]
            [fs.core :as fs]
            [slugger.core :as slug])
  (:use ecstatic.io
        [clj-time.core :only (year month)]
        clj-time.format))

(declare copy-resources)

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

(defn render-post [path template]
  (let [t (slurp template)]
    (l/document (l/parse t)
                    (l/or (l/element= :title) (l/element= :h3))
                    (l/content (:title (metadata path)))
                    (l/element= :article)
                    (l/html-content
                     (md/to-html (second (split-file path))
                                 [:fenced-code-blocks])))))

(defn post-url [file]
  (let [metadata (metadata file)
        date (:date metadata)
        parsed-date (parse date)]
    (str "/blog/"
         (year parsed-date) "/"
         (month parsed-date) "/"
         (-> (:title metadata)
             (clojure.string/replace "+" "")
             (slug/->slug)))))

(defn prepare-dirs [dir output]
  (let [output-structure (reduce (fn [dirs file]
                                   (let [slug (post-url file)]
                                     (conj dirs (str output slug))))
                                 #{} (md-files dir))]
    (doall (map fs/mkdirs output-structure))))

(defn write-files [in-dir output]
  (doall (map (fn [file]
                (let [slug (post-url file)]
                  (spit (str output "/" slug "/index.html")
                        (render-post file (str in-dir "/templates/post1.html")))))
              (md-files in-dir))))

(defn process-posts [in-dir output]
  (let [tmp (.getPath (fs/temp-dir "ecst"))]
    (prepare-dirs in-dir tmp)
    (write-files in-dir tmp)
    (copy-resources in-dir tmp)
    (fs/delete-dir output)
    (fs/copy-dir tmp output)
    (fs/delete-dir tmp)))

(defn all-posts [in-dir]
  (map (fn [file] (assoc (metadata file) :file file))  (md-files in-dir)))

(defn tag-buckets [posts]
  (->> (reduce (fn [m post]
                 (concat m  (interleave (:tags post) (repeat (vector post)))))
               [] posts)
       (partition 2)
       (map #(hash-map (first %) (second %)))
       (apply merge-with concat)
       ))

(defn all-tags [posts]
  (->> posts
       (map #(:tags %))
       (apply concat)
       (set)))

(defn copy-resources [in-dir output]
  (fs/copy-dir (str in-dir "/resources") (str output "/resources")))

(defn generate-item [post]
  (let [item (:title post)
        a (l/parse-fragment (str "<a>" item "</a>"))
        link (post-url (:file post))
        html (l/fragment-to-html (l/fragment a
                                             (l/element= :a)
                                             (l/attr :href link)))]
    ;(println html)
    html))

(defn generate-index [in-dir]
  (let [t (slurp (str in-dir "/templates/index.html"))]
    ;; (println t)
    (l/document (l/parse t)
                    (l/element= :section)
                    (fn [node]
                      (reduce (fn [node post]
                                (update-in node [:content]
                                           conj (generate-item post)))
                              node (all-posts in-dir))))))

(defn write-index [in-dir output]
  (spit (str output "/index.html")
        (generate-index in-dir)))

(defn foo
  "I don't do a whole lot."
  [x]
  (println x "Hello, World!"))
