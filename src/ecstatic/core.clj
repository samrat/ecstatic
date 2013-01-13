(ns ecstatic.core
  (:require [me.raynes.cegdown :as md]
            [me.raynes.laser :as l]
            [fs.core :as fs]
            [slugger.core :as slug]
            [clj-rss.core :as rss])
  (:use ecstatic.io
        [clj-time.core :only (year month)]
        clj-time.format
        clj-time.coerce)
  (:import java.util.Date))

(declare copy-resources
         post-url
         write-index)

(defn metadata [path]
  "Returns map containing post metadata."
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

(defn content [path]
  "Return the post content."
  (second (split-file path)))

(defn render-post [path in-dir]
  "Render HTML file from Markdown file"
  (let [t (slurp (str in-dir "/templates/post.html"))
        a (l/parse-fragment "<a></a>")]
    (l/document
     (l/parse t)
     (l/class= "site-title")
     (l/content (.toUpperCase (:site-name (config in-dir))))
     (l/element= :title)
     (l/content (:title (metadata path)))
     (l/class= "entry-title")
     (l/html-content (l/fragment
                      a
                      (l/element= :a)
                      (l/attr :href (post-url path))
                      (l/element= :a)
                      (l/content (:title (metadata path)))))
     (l/element= :article)
     (l/html-content
      (md/to-html (content path)
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
  "Prepare directory structure."
  (let [output-structure (reduce (fn [dirs file]
                                   (let [slug (post-url file)]
                                     (conj dirs (str output slug))))
                                 #{(str output "/feeds")} (md-files dir))]
    (doall (map fs/mkdirs output-structure))))

(defn write-files [in-dir output]
  "Write HTML files to location."
  (do (prepare-dirs in-dir output)
      (doall (map (fn [file]
                    (let [slug (post-url file)]
                      (spit (str output "/" slug "/index.html")
                            (render-post file in-dir))))
                  (md-files in-dir)))))

(defn process-posts [in-dir output]
  "Read and create posts."
  (let [tmp (.getPath (fs/temp-dir "ecst"))]
    (write-files in-dir tmp)
    (write-index in-dir tmp)
    (generate-main-feed in-dir tmp)
    (doall (map #(generate-tag-feed in-dir tmp %)
                (all-tags (all-posts in-dir))))
    (copy-resources in-dir tmp)
    (fs/delete-dir output)
    (fs/copy-dir tmp output)
    (fs/delete-dir tmp)))

(defn all-posts [in-dir]
  "List of maps containing post-info."
  (map (fn [file] (assoc (metadata file) :file file))  (md-files in-dir)))

(defn tag-buckets [posts]
  "Categorizes posts under tags.
   Returns {tag1 [post1 post2], tag2 [post1]}"
  (->> (reduce (fn [m post]
                 (concat m  (interleave (:tags post) (repeat (vector post)))))
               [] posts)
       (partition 2)
       (map #(hash-map (first %) (second %)))
       (apply merge-with concat)))

(defn all-tags [posts]
  "Return list of all tags."
  (->> posts
       (map #(:tags %))
       (apply concat)
       (set)))

(defn copy-resources [in-dir output]
  "Copy in-dir/resources(which should contain images,stylesheets and js files."
  (fs/copy-dir (str in-dir "/resources") (str output "/resources")))

(defn generate-item [post]
  (let [item (:title post)
        a (l/parse-fragment (str "<h4 class=\"article-title\"><a>" item "</a></h4>"))
        link (post-url (:file post))
        html (l/fragment a
                         (l/element= :a)
                         (l/attr :href link))]
    html))

(defn generate-index [in-dir]
  (let [t (slurp (str in-dir "/templates/index.html"))]
    (l/document (l/parse t)
                (l/class= "site-title")
                (l/content (.toUpperCase (:site-name (config in-dir))))
                (l/element= :section)
                (fn [node]
                  (reduce (fn [node post]
                            (update-in node [:content]
                                       concat (generate-item post)))
                          node (->> (all-posts in-dir)
                                    (sort-by :date)
                                    (reverse)))))))

(defn write-index [in-dir output]
  (spit (str output "/index.html")
        (generate-index in-dir)))

(defn generate-feed [posts tag config output]
  "Generate and write RSS feed."
  (->> (apply rss/channel-xml
              (reduce (fn [p post]
                        (let [metadata (metadata post)
                              date (parse (:date metadata))]
                          (conj p {:title (:title metadata)
                                   :pubDate (to-date date)
                                   :author (:site-author config)
                                   :description
                                   (md/to-html (content post)
                                               [:fenced-code-blocks])})))
                      [{:title (:site-name config)
                        :link (:site-url config)
                        :description (:site-description config)
                        :lastBuildDate (new Date)}]
                      posts))
       (spit (str output "/feeds/" tag ".xml"))))

(defn generate-main-feed [in-dir output]
  (generate-feed (map #(:file %) (all-posts in-dir))
                 "all"
                 (config in-dir)
                 output))

(defn generate-tag-feed [in-dir output tag]
  (generate-feed (map #(:file %) (get (tag-buckets (all-posts in-dir)) tag))
                 tag
                 (config in-dir)
                 output))

(defn foo
  "I don't do a whole lot."
  [x]
  (println x "Hello, World!"))
