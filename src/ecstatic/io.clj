(ns ecstatic.io
  (:require [clojure.java.io :as io]
            [taoensso.timbre :as timbre :refer (error)]
            [fs.core :refer [extension]]
            [clojure.pprint :as pp])
  (:import [java.io PushbackReader]))

(defn config [in-dir]
  "Read config file"
  (binding [*read-eval* false]
    (try (with-open [r (io/reader (str in-dir "/config.clj"))]
           (read (PushbackReader. r)))
         (catch java.io.FileNotFoundException _
           (do (error "No config.clj file at" in-dir)
               (System/exit 0))))))

(defn read-template [path]
  (try (read-string (slurp path))
       (catch java.io.FileNotFoundException _
         (do (error "No template file at" (timbre/color-str :red path))
             (System/exit 0)))))

(letfn [(file-ending-with? [& endings]
          (fn [file]
            ((set endings) (extension file))))]
  (def markdown-file? (file-ending-with? ".md" ".markdown"))
  (def clojure-file? (file-ending-with? ".clj")))

(defn post-files [in-dir]
  "Get all files in the posts directories"
  (filter #(or (markdown-file? %)
               (clojure-file? %))
          (file-seq (io/file in-dir "posts"))))

(defn page-files [in-dir]
  "Get all files in the pages directories"
  (filter #(or (markdown-file? %)
               (clojure-file? %))
          (file-seq (io/file in-dir "pages"))))

(defn all-page-and-post-files [in-dir]
  "Get all files in the page and post directories"
  (apply concat ((juxt page-files post-files) in-dir)))

(defn split-file [path]
  "Return [metadata content] from a markdown file."
  (let [content (slurp path)
        idx (.indexOf content "---" 4)]
    [(subs content 4 idx) (subs content (+ idx 4))]))

(defn snippet-files
  ([in-dir]
     (file-seq (io/file in-dir "snippets")))
  ([in-dir name]
     "Get the snippet file with the name 'name'"
     (or (first (filter #(re-find (re-pattern name) (.getPath %))
                        (snippet-files in-dir)))
         (do (error "No snippet with name" (timbre/color-str :red name))
             (System/exit 0)))))

(defn file-type [file]
  "A dispatch function for filetypes."
  (cond (markdown-file? file) :markdown
        (clojure-file? file) :clojure))

(defn code-files [in-dir]
  "Return a sequence of clojure files that represent the custom code in 'code/"
  (filter clojure-file? (file-seq (io/file in-dir "code"))))

(def example-site-files ["/src/resources/stylesheets/default.css"
                         "/src/snippets/ga.clj"
                         "/src/posts/2014-01-27-lorem-markdownum.md"
                         "/src/posts/2014-01-01-foo-bar.md"
                         
                         "/src/templates/index.clj"
                         "/src/templates/post.clj"
                         "/src/templates/base.clj"
                         "/src/templates/page.clj"
                         
                         "/src/code/code.clj"
                         "/src/config.clj"
                         
                         "/src/pages/tags.clj"
                         "/src/pages/archives.clj"])

(defn create-directory-scaffold [site-name]
  "Create the scaffold for a new website project under 'base-dir'."
  (println "Creating directory scaffold.")
  (doseq [f example-site-files]
    (io/make-parents (str site-name f))
    (spit (str site-name f) (slurp (io/resource (str "example" f))))))
