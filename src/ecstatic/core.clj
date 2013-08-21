(ns ecstatic.core
  (:gen-class)
  (:require [me.raynes.cegdown :as md]
            [fs.core :as fs]
            [clj-rss.core :as rss])
  (:use filevents.core
        [ecstatic io utils]
        [hiccup core page]
        [clojure.tools.cli :only (cli)]
        [clj-time format local coerce]
        [clj-time.core :only (year month day)]))

(defn metadata
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

(defn content
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
  (->> (map (fn [file]
              (-> (assoc (metadata file) :file file)
                  (assoc :url (page-url file))
                  (assoc :human-readable-date (unparse
                                               (formatter "dd MMMMM, YYYY")
                                               (parse (:date (metadata file)))))))
            (md-files in-dir))
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

(def tag-buckets
  "Categorizes posts under tags. Returns {tag1 [post1 post2], tag2
   [post1]}"
  (memoize
   (fn [posts]
     (->> (reduce (fn [m post]
                    (concat m (interleave (:tags post)
                                          (repeat (vector post)))))
                  [] posts)
          (partition 2)
          (map #(hash-map (first %) (second %)))
          (apply merge-with concat)))))

(defn all-tags
  "Return list of all tags."
  [posts]
  (->> posts
       (map :tags)
       (apply concat)
       (set)))

(def ^:dynamic cont nil)
(def ^:dynamic met nil)

(defn render-template
  [in-dir template page-content page-metadata]
  (let [base (read-template (str in-dir "/templates/base.clj"))
        template (read-template (str in-dir "/templates/" template ".clj"))
        base-content (binding [*ns* (the-ns 'ecstatic.core)
                               cont page-content
                               met  page-metadata]
                       (html (eval template)))]
    (binding [*ns* (the-ns 'ecstatic.core)
              cont base-content
              met  page-metadata]
      (html5 (eval base)))))

(defn related-posts
  "Returns n posts related to `post`."
  [in-dir post n]
  (->> (:tags post)
       (map #(get (tag-buckets (all-pages in-dir)) %))
       (apply concat)
       (remove #{post})
       (frequencies)
       (sort-by second)
       (take n)
       (map key)))

(defn render-page
  "Render HTML file from markdown file."
  [post in-dir & template]
  (let [file (:file post)
        template (or (or (first template) nil)
                     "post")
        [prev next] (pager (all-pages in-dir) post)]
    (println (related-posts in-dir post 1))
    (render-template in-dir
                     template
                     {:content (md/to-html (content file)
                                           [:fenced-code-blocks])}
                     {:site-name (:site-name (config in-dir))
                      :site-url (:site-url (config in-dir))
                      :title (:title (metadata file))
                      :url   (page-url file)
                      :date (:date (metadata file))
                      :human-readable-date (unparse
                             (formatter "dd MMMMM, YYYY")
                             (parse (:date (metadata file))))
                      :prev (or nil prev)
                      :next (or nil next)
                      :related-posts (related-posts in-dir post 3)})))

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
                                 #{(str output "/feeds")} (md-files in-dir))]
    (doall (map fs/mkdirs output-structure))))

(defn write-pages
  "Write HTML files to location."
  [in-dir output]
  (println "Writing posts and pages...")
  (do (prepare-dirs in-dir output)
      (doall (map (fn [post]
                    (let [file (:file post)
                          slug (page-url file)
                          metadata (metadata file)]
                      (spit (str output "/" slug "/index.html")
                            (render-page post in-dir (or (:template metadata)
                                                         nil)))))
                  (all-pages in-dir)))))

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
                        (let [metadata (metadata post)
                              date (parse (:date metadata))]
                          (conj p {:title (:title metadata)
                                   :link (str (:site-url config)
                                              (page-url post)
                                              ;; for feed analytics
                                              "?utm_source=feed")
                                   :pubDate (to-date date)
                                   :author (:site-author config)
                                   :description
                                   (md/to-html (content post)
                                               [:fenced-code-blocks])})))
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

(defn create-site
  "Read and create posts."
  [in-dir output]
  (do
    (write-index in-dir output)
    (write-pages in-dir output)
    (generate-main-feed in-dir output)
    (doall (map #(generate-tag-feed in-dir output %)
                (all-tags (all-pages in-dir))))
    (copy-resources in-dir output)
    (println "Successfully compiled site.")))

(defn auto-regen [in-dir output]
  (create-site in-dir output)
  (watch (fn [_ file]
           (println)
           (println "Regenerating site...")
           (create-site in-dir output))
         in-dir))

(defn -main [& args]
  (let [[opts args banner] (cli args
                              ["-h" "--help" "Print this help text and exit"]
                              ["-s" "--src" "Source for site."]
                              ["-o" "--output" "Output for site." :default "./_site"]
                              ["-p" "--preview" "Run jetty server on http://localhost:8080"]
                              ["-w" "--watch" "Auto site generation."])
        {:keys [help preview src output watch]} opts]
    (when help
      (println banner)
      (System/exit 0))
    (cond src (create-site src output)
          preview (serve preview)
          watch (auto-regen watch output))))