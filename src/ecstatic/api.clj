(ns ecstatic.api
  (:use [ecstatic.core :only [*content* *metadata*]])
  (:require [ecstatic.core :as core]))

(def page-url core/page-url)

(defn all-posts []
  (core/all-pages (str core/*in-dir* "/posts")))

(defn snippet [name]
  (core/snippet core/*in-dir* name))
