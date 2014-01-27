(with-base-template "base"
  [:div
   [:h1 (:title *metadata*)]
   [:span {:class "info"}
    (str "Posted on " (:human-readable-date *metadata*))]
   (:content *content*)])
