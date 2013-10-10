[:html {:lang "en"}
 [:head
  [:meta {:charset "utf-8"}]
  [:meta {:name "viewport"
          :content "width=device-width, initial-scale=1.0"}]
  [:title "Ecstatic"]
  (hpage/include-css "/resources/css/bootstrap.css")]
 [:body
  [:div {:class "navbar navbar-static-top"}
   [:div {:class "container"}
    [:a {:class "navbar-brand" :href "/index.html"} "Ecstatic"]
    (helem/unordered-list {:class "nav navbar-nav"}
                          (do (println (all-pages))
                              (for [page (all-pages)]
                                (helem/link-to (:url page) (:title page)))))]]
  [:div {:class "container"} *content*]]]
