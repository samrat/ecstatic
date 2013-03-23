(ns ecstatic.core
  (:gen-class)
  (:require [me.raynes.cegdown :as md]
            [fs.core :as fs]
            [slugger.core :as slug]
            [clj-rss.core :as rss]
            [watchtower.core :as watcher])
  (:use ecstatic.io
        ecstatic.utils
        hiccup.core
        [clojure.tools.cli :only (cli)]
        [clj-time.core :only (year month)]
        clj-time.format
        clj-time.local
        clj-time.coerce))

(defn metadata [path]
  "Returns map containing page metadata."
  (let [meta (first (split-file path))]
    (reduce (fn [h [_ k v]]
              (let [key (keyword (.toLowerCase k))]
                (if-not (h key)
                  (assoc h key (if (= key :tags)
                                 (->> (clojure.string/split v #",")
                                      (map #(.trim %))
                                      vec)
                                 v))
                  h)))
            {} (re-seq #"([^:#\+]+): (.+)(\n|$)" meta))))

(defn content [path]
  "Return the page content."
  (second (split-file path)))

;; Other metadata
(defn page-url [file]
  "Return relative URL of a post."
  (let [metadata (metadata file)
        slug (or nil (:slug metadata))
        parsed-date (or (or (parse (:date metadata)) nil)
                        (local-now))]
    (if slug slug
        (str "/blog/"
             (year parsed-date) "/"
             (if (= (count (str (month parsed-date))) 2)
               (month parsed-date)
               (str "0" (month parsed-date))) "/"
               (slug/->slug (:title metadata))))))

(defn all-pages [in-dir]
  "List of maps containing post-info."
  (->> (map (fn [file]
              (-> (assoc (metadata file) :file file)
                  (assoc :url (page-url file))
                  (assoc :human-readable-date (unparse
                                               (formatter "dd MMMMM, YYYY")
                                               (parse (:date (metadata file)))))))
            (md-files in-dir))
       (sort-by :date)
       (reverse)))

(defn pager [posts post]
  "Gives the previous and next posts(chronologically)."
  (let [posts (reverse posts)
        i (.indexOf posts post)
        prev (when-not (<= i 0)
               (nth posts (dec i)))
        next (when-not (>= i (dec (count posts)))
               (nth posts (inc i)))]
    [prev next]))

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
       (map :tags)
       (apply concat)
       (set)))

(def ^:dynamic c nil)
(def ^:dynamic m nil)

(defn render-template
  [in-dir template c m]
  (binding [c c
            m m]
    (if (= template "base")
      (html
       (eval (read-template
              (str in-dir "/templates/" template ".clj"))))
      (render-template in-dir
                       "base"
                       (eval (read-template
                              (str in-dir "/templates/" template ".clj")))
                       m))))

(defn render-page [post in-dir & template]
  "Render HTML file from markdown file."
  (let [file (:file post)
        template (or (or (first template) nil) "post")
        [prev next] (pager (all-pages in-dir) post)]
    (render-template in-dir
                     template
                     {:content (md/to-html (content file)
                                           [:fenced-code-blocks])}
                     {:site-name (:site-name (config in-dir))
                      :site-url (:site-url (config in-dir))
                      :title (:title (metadata file))
                      :url   (page-url file)
                      :date (unparse
                             (formatter "dd MMMMM, YYYY")
                             (parse (:date (metadata file))))
                      :prev (or nil prev)
                      :next (or nil next)})))

(defn generate-index [in-dir]
  "Generate content for index.html"
  (render-template in-dir
                   "index"
                   (all-pages (str in-dir "/posts"))
                   {:site-name (:site-name (config in-dir))}))

(defn write-index [in-dir output]
  (spit (str output "/index.html")
        (generate-index in-dir)))

(defn prepare-dirs [in-dir output]
  "Prepare directory structure."
  (let [output-structure (reduce (fn [dirs file]
                                   (let [slug (page-url file)]
                                     (conj dirs (str output slug))))
                                 #{(str output "/feeds")} (md-files in-dir))]
    (doall (map fs/mkdirs output-structure))))

(defn write-pages [in-dir output]
  "Write HTML files to location."
  (do (prepare-dirs in-dir output)
      (doall (map (fn [post]
                    (let [file (:file post)
                          slug (page-url file)
                          metadata (metadata file)]
                      (spit (str output "/" slug "/index.html")
                            (render-page post in-dir (or (:template metadata)
                                                         nil)))))
                  (all-pages in-dir)))))

(defn copy-resources [in-dir output]
  "Copy in-dir/resources containing js,css and images"
  (do (fs/delete-dir (str output "/resources"))
      (fs/copy-dir (str in-dir "/resources") (str output "/resources"))))

;; Feed
(defn generate-feed [posts tag config output]
  "Generate and write RSS feed."
  (->> (apply rss/channel-xml
              (reduce (fn [p post]
                        (let [metadata (metadata post)
                              date (parse (:date metadata))]
                          (conj p {:title (:title metadata)
                                   :link (str (:site-url config) (page-url post))
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

(defn create-site [in-dir output]
  "Read and create posts."
  (do
    (write-index in-dir output)
    (write-pages in-dir output)
    (generate-main-feed in-dir output)
    (doall (map #(generate-tag-feed in-dir output %)
                (all-tags (all-pages in-dir))))
    (copy-resources in-dir output)
    (println "Successfully compiled site.")))

(defn auto-regen [in-dir output]
  (watcher/watcher [in-dir]
           (watcher/rate 1000)
           (watcher/file-filter :ignore-dotfiles)
           (watcher/on-change (create-site in-dir output))))

(defn -main [& args]
  (let [[opts _ banner] (cli args
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