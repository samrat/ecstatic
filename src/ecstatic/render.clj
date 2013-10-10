(ns ecstatic.render
  "A Dummy namespace to render the tempates in.

The namespace also provides shortcuts for hiccup namespaces"
  (:require [hiccup.core :as h]
            [hiccup.element :as helem]
            [hiccup.form :as hform]
            [hiccup.util :as hutil]
            [hiccup.page :as hpage]
            [ecstatic.code :as code]
            [ecstatic.code :as c])
  (:use [ecstatic.api]
        [ecstatic.core :only  [*metadata* *content*]]))
