---
title: Archives
template: page
---
[:ul (for [year (group-by #(c/year (:date %)) (all-posts))]
       [:div [:h4 (key year)]
        (for [post (val year)]
          [:span
           [:li
            [:span
             [:a {:href (:url post)
                  :title (str "Permalink to " (:title post))}
              (:title post)] [:br]
             [:span " " (:human-readable-date post)]]]])])] 
