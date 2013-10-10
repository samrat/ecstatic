(ns ecstatic.api
  (:use [ecstatic.core :only [*content* *metadata*]])
  (:require [ecstatic.core :as core]))

(def page-url core/page-url)

(defn all-posts []
  (filter #(re-find #"[/\\]posts[/\\]" (-> % :file .getPath))
          (core/all-pages core/*in-dir*)))

(defn all-pages []
  (filter #(re-find #"[/\\]pages[/\\]" (-> % :file .getPath))
          (core/all-pages core/*in-dir*)))

(defn snippet [name]
  (core/snippet core/*in-dir* name))
