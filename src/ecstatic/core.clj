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
            [taoensso.timbre :as timbre
             :refer (error)]))

(timbre/set-config! [:prefix-fn] (fn [log]
                                   (str (timbre/color-str
                                         :red
                                         (name (:level log))))))

(def ^:dynamic *in-dir* nil)

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
  [in-dir]
  (map (fn [file]
         (-> (assoc (file-metadata file) :file file)
             ;;(assoc :content (file-content file))
             (assoc :url (page-url file))))
       (page-files in-dir)))

(defn all-posts
  "List of maps containing post-info."
  [in-dir]
  (->> (map (fn [file]
              (-> (assoc (file-metadata file) :file file)
                  (assoc :content (file-content file))
                  (assoc :url (page-url file))
                  (assoc :human-readable-date (unparse
                                               (formatter "dd MMMMM, YYYY")
                                               (parse (:date (file-metadata file)))))))
            (post-files in-dir))
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
  [in-dir]
  (->> (all-posts in-dir)
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

(defn render-hiccup [in-dir hiccup-data]
  (binding [*ns* (the-ns 'ecstatic.render)
            *in-dir* in-dir]
    (html (eval hiccup-data))))

(defmulti split-and-to-html (fn [in-dir file] (file-type file)))

(defmethod split-and-to-html :markdown [in-dir file]
  (md/to-html (file-content file) [:fenced-code-blocks]))

(defmethod split-and-to-html :clojure [in-dir file]
  (render-hiccup in-dir (read-string (file-content file))))

(defn snippet [in-dir name]
  "Expects the name of a snippet and returns the corresponding html."
  (let [file (snippet-files in-dir name)]
    (cond
     (markdown-file? file) (md/to-html (slurp file))
     (clojure-file? file) (render-hiccup in-dir (read-template (.getPath file)))))) ; TODO refactor call

(def ^:dynamic *content* nil)
(def ^:dynamic *metadata* nil)

(defn render-template-content
  "Render the hiccup template 'template-content' without wrapping it with the doctype."
  [in-dir template-content cont meta]
  (binding [*content* cont
            *metadata*  meta]
    (render-hiccup in-dir template-content)))

(defn render-template-partially
  "Render the template with the name 'template-name' without wrapping it with
the doctype."
  [in-dir template-name cont meta]
  (let [template (read-template (str in-dir "/templates/" template-name ".clj"))]
    (render-template-content in-dir template cont meta)))

(defn render-template
  "Fully render the template 'template' to a html5 page."
  [in-dir template-name cont meta]
  (html5 (render-template-partially in-dir template-name cont meta)))

(defn render-page
  "Render HTML file from markdown file."
  [post in-dir & template]
  (let [file (:file post)
        template (or (or (first template) nil)
                     "post")
        [prev next] (pager (all-pages in-dir) post)]
    (render-template in-dir
                     template
                     {:content (split-and-to-html in-dir file)}
                     {:site-name (:site-name (config in-dir))
                      :site-url (:site-url (config in-dir))
                      :title (:title (file-metadata file))
                      :url   (page-url file)
                      :date (:date (file-metadata file))
                      :human-readable-date (unparse
                             (formatter "dd MMMMM, YYYY")
                             (parse (:date (file-metadata file))))
                      :prev (or nil prev)
                      :next (or nil next)

(defn generate-index
  "Generate content for index.html"
  [in-dir]
  (println "Generating index...")
  (render-template in-dir
                   "index"
                   (all-pages (str in-dir "/posts"))
                   {:site-name (:site-name (config in-dir))}))

(defn write-index [in-dir output]
  (spit (str output "/index.html")
        (generate-index in-dir)))

(defn prepare-dirs
  "Prepare directory structure."
  [in-dir output]
  (println "Preparing directory structure...")
  (let [output-structure (reduce (fn [dirs file]
                                   (let [slug (page-url file)]
                                     (conj dirs (str output slug))))
                                 #{(str output "/feeds")} (page-files in-dir))]
    (doall (map fs/mkdirs output-structure))))

(defn write-pages
  "Write HTML files to location."
  [in-dir output]
  (println "Writing posts and pages...")
  (doall (pmap (fn [post]
                (let [file (:file post)
                      slug (page-url file)
                      metadata (file-metadata file)]
                  (spit (str output "/" slug "/index.html")
                        (render-page post in-dir (or (:template metadata)
                                                     nil)))))
              (all-pages in-dir))))

(defn copy-resources
  "Copy in-dir/resources containing js,css and images"
  [in-dir output]
  (println "Copying resources...")
  (do (fs/delete-dir (str output "/resources"))
      (fs/copy-dir (str in-dir "/resources") (str output "/resources"))))

;; Feed
(defn generate-feed
  "Generate and write RSS feed."
  [posts tag config output]
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
                                   :description
                                   (split-and-to-html post)})))
                      [{:title (:site-name config)
                        :link (:site-url config)
                        :description (:site-description config)
                        :lastBuildDate (to-date (local-now))}]
                      posts))
       (spit (str output "/feeds/" tag ".xml"))))

(defn generate-main-feed [in-dir output]
  (println "Generating main feed...")
  (generate-feed (map :file (all-pages (str in-dir "/posts")))
                 "all"
                 (config in-dir)
                 output))

(defn generate-tag-feed [in-dir output tag]
  (generate-feed (map :file (get (tag-buckets (all-pages
                                               (str in-dir "/posts")))
                                 tag))
                 tag
                 (config in-dir)
                 output))

(defn load-custom-code
  "Load the custom code that can be placed in 'code/'."
  [in-dir]
  (println "Loading custom code...")
  (doall
   (map (fn [file]
          (binding [*ns* (the-ns 'ecstatic.code)
                    *in-dir* in-dir]
            (try (load-file (.getPath file))
                 (catch Exception e
                   (do (error "Could not load"
                              (fs/base-name file)
                              ":" (timbre/color-str :red e))
                       (System/exit 0))))))
        (code-files in-dir))))

(defn create-site
  "Read and create posts."
  [in-dir output]
  (do (load-custom-code in-dir)
      (prepare-dirs in-dir output)
      (write-index in-dir output)
      (write-pages in-dir output)
      (generate-main-feed in-dir output)
      (pmap #(generate-tag-feed in-dir output %) (all-tags (all-pages in-dir)))
      (copy-resources in-dir output)
      (println "Successfully compiled site.")))

(defn auto-regen [in-dir output]
  (create-site in-dir output)
  (watch (fn [_ file]
           (when (#{".md" ".markdown" ".css" ".clj"} (fs/extension file))
             (do (println)
                 (println "Regenerating site...")
                 (future (create-site in-dir output)))))
         in-dir))
