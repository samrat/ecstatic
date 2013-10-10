(ns ecstatic.api
  (:require [ecstatic.core :refer [in]]))

(def all-posts (partial ecstatic.core/all-pages (str @in "/posts")))
