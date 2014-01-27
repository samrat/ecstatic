(ns ecstatic.core
  (:gen-class)
  (:use [filevents.core]
        [hiccup.core]
        [hiccup.page :only [html5]]
        [clj-time.core :only [year month day]]
        [clj-time.format :only [parse
                                unparse
                                formatter
                                formatters]]
        [clj-time.local :only [local-now]]
        [clj-time.coerce :only [to-date]]
        [ecstatic.io])
  (:require [me.raynes.cegdown :as md]
            [fs.core :as fs]
            [clj-rss.core :as rss]
            [taoensso.timbre :as timbre :refer (error info)]
            [clojure.string :refer [split]]))

(timbre/set-config! [:prefix-fn] (fn [log]
                                   (str (timbre/color-str
                                         :red
                                         (name (:level log))))))
(def in-dir (atom nil))
(def ^:dynamic *in-dir* nil)
(def cache (atom {}))

(defn file-metadata
  "Returns map containing page metadata."
  [path]
  (let [meta (first (split-file path))]
    (reduce (fn [h [_ k v]]
              (let [key (keyword (.toLowerCase k))]
                (if-not (h key)
                  (assoc h key (condp = key
                                 :tags (->> (clojure.string/split v #",")
                                            (map #(.trim %))
                                            vec)
                                 :date (unparse (formatters :date-time-no-ms)
                                                (parse v))
                                 v))
                  h)))
            {} (re-seq #"([^:#\+]+): (.+)(\n|$)" meta))))

(defn file-content
  "Return the page content."
  [path]
  (second (split-file path)))

;; Other metadata
(defn page-url
  "Return relative URL of a post from its file name."
  [file]
  (let [file-name (fs/name file)
        type (fs/name (fs/parent file))]
    (if (= type "posts") ; blogpost or page.
      (let [date (take 2 (clojure.string/split file-name #"-"))
            file-name (drop 3 (clojure.string/split file-name #"-"))]
        (str "/blog/"
             (clojure.string/join "/" date) "/"
             (clojure.string/join "-" file-name)))
      (str "/" file-name))))

(defn all-pages
  "List of maps containing post-info."
  []
  (map (fn [file]
         (-> (assoc (file-metadata file) :file file)
             ;;(assoc :content (file-content file))
             (assoc :url (page-url file))))
       (page-files @in-dir)))

(defn all-posts
  "List of maps containing post-info."
  []
  (->> (map (fn [file]
              (-> (assoc (file-metadata file) :file file)
                  (assoc :content (file-content file))
                  (assoc :url (page-url file))
                  (assoc :human-readable-date (unparse
                                               (formatter "dd MMMMM, YYYY")
                                               (parse (:date (file-metadata file)))))))
            (post-files @in-dir))
       (sort-by :date)
       (reverse)))

(defn pager
  "Gives the previous and next posts in chronological order."
  [posts post]
  (let [posts (reverse posts)
        i (.indexOf posts post)
        prev (when-not (<= i 0)
               (nth posts (dec i)))
        next (when-not (>= i (dec (count posts)))
               (nth posts (inc i)))]
    [prev next]))

(defn tag-buckets
  "Categorizes posts under tags. Returns {tag1 [post1 post2], tag2
   [post1]}"
  []
  (->> (all-posts)
       (reduce (fn [m post]
                 (concat m (interleave (:tags post)
                                       (repeat (vector post)))))
               [])
       (partition 2)
       (map #(hash-map (first %) (second %)))
       (apply merge-with concat)))

(defn all-tags
  "Return list of all tags."
  [posts]
  (->> posts
       (map :tags)
       (apply concat)
       (set)))

(defn render-hiccup [hiccup-data]
  (binding [*ns* (the-ns 'ecstatic.render)
            *in-dir* @in-dir]
    (html (eval hiccup-data))))

(defmulti split-and-to-html (fn [file] (file-type file)))

(defmethod split-and-to-html :markdown [file]
  (md/to-html (file-content file) [:fenced-code-blocks]))

(defmethod split-and-to-html :clojure [file]
  (render-hiccup (read-string (file-content file))))

(defn snippet [name]
  "Expects the name of a snippet and returns the corresponding html."
  (let [file (snippet-files @in-dir name)]
    (cond
     (markdown-file? file) (md/to-html (slurp file))
     (clojure-file? file) (render-hiccup (read-template (.getPath file)))))) ; TODO refactor call

(def ^:dynamic *content* nil)
(def ^:dynamic *metadata* nil)

(defn render-template-content
  "Render the hiccup template 'template-content' without wrapping it with the doctype."
  [template-content cont meta]
  (binding [*content* cont
            *metadata*  meta]
    (render-hiccup template-content)))

(defn render-template-partially
  "Render the template with the name 'template-name' without wrapping it with
the doctype."
  [template-name cont meta]
  (let [template (read-template (str @in-dir "/templates/" template-name ".clj"))]
    (render-template-content template cont meta)))

(defn render-template
  "Fully render the template 'template' to a html5 page."
  [template-name cont meta]
  (html5 (render-template-partially template-name cont meta)))

(defn render-page
  "Render HTML file from markdown file."
  [post & template]
  (let [file (:file post)
        template (or (first template) "post")
        [prev next] (pager (all-posts) post)]
    (render-template template
                     {:content (split-and-to-html file)}
                     {:site-name (:site-name (config @in-dir))
                      :site-url (:site-url (config @in-dir))
                      :title (:title (file-metadata file))
                      :url   (page-url file)
                      :date (:date (file-metadata file))
                      :human-readable-date (unparse
                             (formatter "dd MMMMM, YYYY")
                             (parse (:date (file-metadata file))))
                      :prev (or prev nil)
                      :next (or next nil)})))

(defn generate-index
  "Generate content for index.html"
  []
  (println "Generating index...")
  (render-template "index"
                   (all-posts)
                   {:site-name (:site-name (config @in-dir))}))

(defn write-index [output]
  (spit (str output "/index.html")
        (generate-index)))

(defn prepare-dirs
  "Prepare directory structure."
  [output]
  (println "Preparing directory structure...")
  (let [output-structure (reduce (fn [dirs file]
                                   (let [slug (page-url file)]
                                     (conj dirs (str output slug))))
                                 #{(str output "/feeds")}
                                 (all-page-and-post-files @in-dir))]
    (doall (map fs/mkdirs output-structure))))

(defn write-single-article
  [article output]
  (let [file (:file article)
        slug (page-url file)
        metadata (file-metadata file)]
      (spit (str output "/" slug "/index.html")
            (render-page article (or (:template metadata)
                                     nil)))))

(defn write-pages
  "Write HTML files to location. Avoids regenerating files by checking
  the last modified timestamp.

  Always regenerates .clj files and files specified in :always-update
  in config."
  [output]
  (println "Finding updated posts and pages...")
  
  (doseq [article (concat (all-posts)
                          (all-pages))]
    (let [path (.getPath (:file article))
          last-modified (.lastModified (:file article))
          tags (:tags (file-metadata (:file article)))
          relative-path (second (split path (re-pattern @in-dir)))]
      (when (or (= (fs/extension (:file article)) ".clj")
                (< (get @cache path) last-modified)
                (some (set (map (partial str @in-dir)
                                (:always-update (config @in-dir))))
                      [path]))
        (println "\tUpdated" relative-path)
        (write-single-article article output)
        (swap! cache assoc path last-modified)

        ;; Update last-modified timestamp for each tag in post
        (doseq [tag (conj tags "all")]
          (swap! cache assoc tag (System/currentTimeMillis))))))
  (spit (str @in-dir "/site.cache") @cache))

(defn copy-resources
  "Copy in-dir/resources containing js,css and images"
  [output]
  (println "Copying resources...")
  (do (fs/delete-dir (str output "/resources"))
      (fs/copy-dir (str @in-dir "/resources") (str output "/resources"))))

;; Feed
(defn generate-feed
  "Generate and write RSS feed."
  [posts tag config output]
  (let [xml-path (str output "/feeds/" tag ".xml")
        xml-modified (.lastModified (clojure.java.io/file xml-path))
        tag-modified (get @cache tag)]
    (when (and tag-modified
               (< xml-modified tag-modified))
      (println "Updated feed:" (str tag ".xml"))
      (->> (apply rss/channel-xml
                  (reduce (fn [p post]
                            (let [metadata (file-metadata post)
                                  date (parse (:date metadata))]
                              (conj p {:title (:title metadata)
                                       :link (str (:site-url config)
                                                  (page-url post)
                                                  ;; for feed analytics
                                                  "?utm_source=feed")
                                       :pubDate (to-date date)
                                       :author (:site-author config)
                                       :description (split-and-to-html post)})))
                          [{:title (:site-name config)
                            :link (:site-url config)
                            :description (:site-description config)
                            :lastBuildDate (to-date (local-now))}]
                          posts))
           (spit xml-path)))))
  (println "\tUpdated feed:" (str tag ".xml"))

(defn generate-main-feed [output]
  (println "Generating main feed...")
  (generate-feed (map :file (all-posts))
                 "all"
                 (config @in-dir)
                 output))

(defn generate-tag-feed [output tag tag-buckets]
  (generate-feed (doall (map :file (get tag-buckets tag)))
                 tag
                 (config @in-dir)
                 output))

(defn load-custom-code
  "Load the custom code that can be placed in 'code/'."
  []
  (println "Loading custom code...")
  (doall
   (map (fn [file]
          (binding [*ns* (the-ns 'ecstatic.code)
                    *in-dir* @in-dir]
            (try (load-file (.getPath file))
                 (catch Exception e
                   (do (error "Could not load"
                              (fs/base-name file)
                              ":" (timbre/color-str :red e))
                       (System/exit 0))))))
        (code-files @in-dir))))

(defn create-site
  "Read and create posts."
  [in-dir output]
  (do (reset! ecstatic.core/in-dir in-dir)
      (load-custom-code)
      (prepare-dirs output)
      (reset! cache
              (try (read-string (slurp (str in-dir "/site.cache")))
                   (catch Exception _ nil)))
      (write-pages output)
      
      (generate-main-feed output)
      
      (let [tag-buckets (tag-buckets)]
        (println "Finding updated tag feeds...")
        (doseq [tag (keys tag-buckets)]
          (generate-tag-feed output tag tag-buckets)))
      
      (copy-resources output)
      (write-index output)
      (println "Successfully compiled site.")))

(defn auto-regen [in-dir output]
  (create-site in-dir output)
  (watch (fn [_ file]
           (when (#{".md" ".markdown" ".css" ".clj"} (fs/extension file))
             (do (println)
                 (println "Regenerating site...")
                 (future (create-site in-dir output)))))
         in-dir))
