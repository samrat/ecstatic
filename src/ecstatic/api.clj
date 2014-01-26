(ns ecstatic.api
  (:use [ecstatic.core :only [*content* *metadata*]]
        [hiccup.core :only [html]])
  (:require [ecstatic.core :as core]
            [me.raynes.cegdown :as md]))

(def page-url core/page-url)
(def markdown md/to-html)

(def all-posts core/all-posts)

(def all-pages core/all-pages)

(def tag-buckets core/tag-buckets)

(defn snippet [name]
  (core/snippet name))

(defn with-base-template [template-name hiccup-content]
  "Wrap the hiccup markup 'hiccup-content' in the template with the
name 'template-name'."
  (let [content (html hiccup-content)]
    (core/render-template-partially template-name
                                    content
                                    *metadata*)))
