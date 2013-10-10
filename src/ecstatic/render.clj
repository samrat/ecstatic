(ns ecstatic.render
  (:require [hiccup.core :refer [html]]
            [hiccup.page :refer [html5]]
            [ecstatic.io :refer :all]
            [ecstatic.core :refer [in]]))

(def ^:dynamic content nil)
(def ^:dynamic metadata nil)

(def all-posts (partial ecstatic.core/all-pages (str @in "/posts")))

(defn render-template
  [in-dir template cont meta]
  (let [base (read-template (str in-dir "/templates/base.clj"))
        template (read-template (str in-dir "/templates/" template ".clj"))
        base-content (binding [*ns* (the-ns 'ecstatic.render)
                               content cont
                               metadata meta]
                       (html (eval template)))]
    (binding [*ns* (the-ns 'ecstatic.render)
              content base-content
              metadata meta]
      (html5 (eval base)))))