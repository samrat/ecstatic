(ns ecstatic.main
  "This namespace is necessary to avoid cyclic dependencies.

The core module relies on the api, code and render namespaces being
correctly initialized. This ensures that those namespaces are loaded."
  (:gen-class)
  (:require [ecstatic.core :refer :all]
            [ecstatic.render]
            [ecstatic.api]
            [ecstatic.code]
            [clojure.tools.cli :refer [cli]]
            [ecstatic.utils :refer :all]))

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
