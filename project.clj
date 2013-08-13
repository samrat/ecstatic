(defproject ecstatic "0.2.4"
  :description "A static site generator"
  :url "http://samrat.me/ecstatic"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [hiccup "1.0.2"]
                 [fs "1.3.3"]
                 [clj-time "0.4.4"]
                 [clj-rss "0.1.2"]
                 [filevents "0.1.0"]
                 [org.clojure/tools.cli "0.2.2"]
                 [ring/ring-core "1.0.3"]
                 [ring/ring-jetty-adapter "1.0.3"]
                 [me.raynes/cegdown "0.1.0"]]
  :main ecstatic.core
  :bin {:name "ecstatic"
        :bin-path "~/bin"})
