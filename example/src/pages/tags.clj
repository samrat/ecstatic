---
title: Tags
template: page
---
[:ul (for [tag (sort (tag-buckets))]
       [:div {:id (key tag)}
        [:li [:a {:href (str "#" (key tag))}
              (key tag)]
         [:ul (for [post (val tag)]
                [:li
                 [:span
                  {:class "article-title"}
                  [:a {:href (:url post)
                       :title (str "Permalink to " (:title post))}
                   (:title post)]
                  [:span {:id "entrymeta"} " " (:human-readable-date post)]]])]]])]
