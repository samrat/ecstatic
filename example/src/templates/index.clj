(with-base-template "base"
  [:ul (for [post (take 5 (all-posts))]
            [:li
             [:span {:class "info"} " " (:human-readable-date post) " "]
             [:a {:href (:url post)
                  :title (:title post)}
              (:title post)]])])
