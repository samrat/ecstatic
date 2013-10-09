(ns ecstatic.render
  (:require [hiccup.core :refer [html]]
            [hiccup.page :refer [html5]]
            [ecstatic.io :refer :all]))

(def ^:dynamic content nil)
(def ^:dynamic metadata nil)
(def ^:dynamic in nil)
(def all-pages #'ecstatic.core/all-pages)

(defn render-template
  [in-dir template cont meta]
  (let [base (read-template (str in-dir "/templates/base.clj"))
        template (read-template (str in-dir "/templates/" template ".clj"))
        base-content (binding [*ns* (the-ns 'ecstatic.render)
                               content cont
                               metadata meta
                               in in-dir]
                       (html (eval template)))]
    (binding [*ns* (the-ns 'ecstatic.render)
              content base-content
              metadata meta
              in in-dir]
      (html5 (eval base)))))