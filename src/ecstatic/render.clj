(ns ecstatic.render
  "A Dummy namespace to render the tempates in.

The namespace also provides shortcuts for hiccup namespaces"
  (:use [hiccup.core :as h]
        [hiccup.element :as helem]
        [hiccup.form :as hform]
        [hiccup.util :as hutil]
        [ecstatic.code :as code]
        [ecstatic.api]
        [ecstatic.core :only [metadata content]]))
