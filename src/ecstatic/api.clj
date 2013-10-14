(ns ecstatic.api
  (:use [ecstatic.core :only [*content* *metadata*]]
        [hiccup.core :only [html]])
  (:require [ecstatic.core :as core]
            [me.raynes.cegdown :as md]))

(def page-url core/page-url)
(def markdown md/to-html)

(defn all-posts []
  (filter #(re-find #"[/\\]posts[/\\]" (-> % :file .getPath))
          (core/all-pages core/*in-dir*)))

(defn all-pages []
  (filter #(re-find #"[/\\]pages[/\\]" (-> % :file .getPath))
          (core/all-pages core/*in-dir*)))

(defn snippet [name]
  (core/snippet core/*in-dir* name))

(defn with-base-template [template-name hiccup-content]
  "Wrap the hiccup markup 'hiccup-content' in the template with the
name 'template-name'."
  (let [content (html hiccup-content)]
    (core/render-template-partially core/*in-dir*
                                    template-name
                                    content
                                    *metadata*)))
