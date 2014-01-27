(with-base-template "base"
  [:div
   [:h1 (:title *metadata*)]
   (:content *content*)])
