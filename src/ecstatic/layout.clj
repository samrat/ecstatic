(ns ecstatic.layout
  (:use hiccup.core))

(defn base
  [metadata content]
  (html
   [:html
    [:head
     [:meta {:charset "utf-8"}]
     [:title (if (:title metadata)
               (str (:title metadata) " | " (:site-name metadata))
               (:site-name metadata))]
     [:meta {:name "description"
             :content "Personal weblog/site of Samrat Man Singh."}]
     [:meta {:name "author"
             :content "Samrat Man Singh"}]

     [:meta {:name "viewport"
             :content "width=device-width, initial-scale=1, maximum-scale=1"}]

     [:link {:rel "stylesheet", :href "/resources/stylesheets/base.css"}]
     [:link {:rel "stylesheet", :href "/resources/stylesheets/skeleton.css"}]
     [:link {:rel "stylesheet", :href "/resources/stylesheets/layout.css"}]
     [:link {:rel "stylesheet", :href "/resources/stylesheets/font-awesome.min.css"}]
     
     [:link {:rel "shortcut icon", :href "/favicon.ico"}]

     [:script
      {:src "https://ajax.googleapis.com/ajax/libs/jquery/1.8.3/jquery.min.js"
       :type "text/javascript"}]
     [:script
      {:src "/resources/js/jquery.timeago.js", :type "text/javascript"}]
     [:script
      {:src "/resources/js/samrat.js", :type "text/javascript"}]]
    [:body
     [:link
      {:rel "stylesheet",
       :href "/resources/stylesheets/tomorrow-night.css"}]

     [:script {:src "/resources/js/highlight.pack.js"}]
     [:script "hljs.initHighlightingOnLoad();"]
     
     [:header
      [:div {:class "container"}
       [:div {:class "five columns"}
        [:a
         {:href "/"}
         [:h1 {:class "site-title"}
          (:site-name metadata)]]]
       [:div
        {:id "social"
         :class "four columns"}
        [:ul
         [:li [:a {:href "http://twitter.com/samratmansingh"
                   :title "@samratmansingh on Twitter"}
               [:i {:class "icon-twitter"}]]]
         [:li [:a {:href "http://github.com/samrat"
                   :title "samrat on Github"}
               [:i {:class "icon-github"}]]]
         [:li [:a {:href "mailto:samrat@samrat.me"
                   :title "Email me"}
               [:i {:class "icon-envelope"}]]]
         [:li [:a {:href "http://samrat.me/feeds/all.xml"
                   :title "Subscribe to this site"}
               [:i {:class "icon-rss"}]]]]]]]
     content

     [:footer
      [:div {:class "container"}
       [:div
        {:class "five columns"}
        [:p
         [:b "Hi, I'm Samrat."]
         " I'm a 19-year old from Nepal."]
        [:p
         "I like working on some of my "
         [:a {:href "/projects"} "projects"]
         ",\n and I enjoy "
         [:a {:href "/books"} "reading"]
         "."]
        [:p
         "Follow me on "
         [:a {:href "http://twitter.com/samratmansingh"} "Twitter"]
         "\n or subscribe\n to "
         [:a
          {:href "http://samrat.github.com/feeds/all.xml"}
          "this blog"]
         "."]
        [:p
         "To get in touch, "
         [:a {:href "mailto:samrat@samrat.me"} "email me"]
         "."]]
       
       [:div {:class "five columns"}
        [:p "This site is generated using "
         [:a {:href "/ecstatic"} "Ecstatic"]
         " and is hosted on "
         [:a {:href "http://pages.github.com"} "Github Pages."]]]]]
     "<script type='text/javascript'>

  var _gaq = _gaq || [];
  _gaq.push(['_setAccount', 'UA-18986645-3']);
  _gaq.push(['_setDomainName', 'samrat.me']);
  _gaq.push(['_trackPageview']);

  (function() {
    var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
    ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
    var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
  })();

</script>"]]))

(defn site-index
  [metadata post-list]
  [:div {:class "container"}
 [:section {:id "content"}
  [:div {:class "sixteen columns"}
   [:div
    {:class "five columns"}
    [:p
     [:b "Hi, I'm Samrat."]
     " I'm a 19-year old from Nepal."]
    [:p
     "I like working on some of my "
     [:a {:href "/projects"} "projects"]
     ",\n and I enjoy "
     [:a {:href "/books"} "reading"]
     "."]
    [:p
     "Follow me on "
     [:a {:href "http://twitter.com/samratmansingh"} "Twitter"]
     "\n or subscribe\n to "
     [:a
      {:href "http://samrat.github.com/feeds/all.xml"}
      "this blog"]
     "."]
    [:p
     "To get in touch, "
     [:a {:href "mailto:samrat@samrat.me"} "email me"]
     "."]
    [:hr]]
   [:div
    {:class "ten columns archive"}
    [:div {:class "entry-title"} [:h3 "Blog posts"]]
    [:ul (for [post post-list]
           [:li [:h4
                 {:class "article-title"}
                 [:a
                  {:href (:url post)
                   :title (str "Permalink to " (:title post))}
                  (:title post)]]
            [:div {:id "entrymeta"} (:human-readable-date post)]])]]
   [:br {:class "clear"}]]]])

(defn page
  [metadata content]
  (html
   [:div {:class "container"}
    [:div {:class "fourteen columns offset-by-two"
           :style "text-align: justify;"}
     [:section {:id "content"}
      [:h3 {:class "entry-title"}
       [:a {:href (:url metadata)
            :title (:title metadata)}
        (:title metadata)]]
      [:br]
      [:div {:class "entry-content"}
       (:content content)]
      [:hr]]]]))

(defn post
  [metadata content]
  (html
   [:div {:class "container"}
    [:div {:class "fourteen columns offset-by-two"
           :style "text-align: justify;"}
     [:section {:id "content"}
      [:h3 {:class "entry-title"}
       [:a {:href (:url metadata)
            :title (:title metadata)}
        (:title metadata)]]
      (when (:prev metadata)
        [:p {:id "bigleft"}
         [:a {:href (:url (:prev metadata))
              :title (:title (:prev metadata))}
          "«"]])
      (when (:next metadata)
        [:p {:id "bigright"}
         [:a {:href (:url (:next metadata))
              :title (:title (:next metadata))}
          "»"]])
      [:div {:id "entrymeta"}
       "Posted "
       [:span {:class "timeago"
               :title (:date metadata)}]]
      [:br]
      [:div {:class "entry-content"}
       (:content content)
       [:hr]
       [:div {:id "footer-message"}
        "If you liked this post, you should "
        [:a {:href "http://twitter.com/samratmansingh"} "follow @samratmansingh"]
        " or "
        [:a {:href "http://samrat.me/feeds/all.xml"}
         "subscribe to this blog."]
        [:br]
        [:div {:id "tweet"}
         [:span {:style "font-size: 14px;"} "Share: "]
         [:a {:href (str "https://twitter.com/home?status="
                         (:title metadata) " " (:site-url metadata) (:url metadata))
              :target "_blank"}
          [:i {:class "icon-twitter-sign"} "&nbsp;"]]
         [:a {:href (str "http://www.facebook.com/sharer/sharer.php?u="
                         (:site-url metadata) (:url metadata))
              :target "_blank"}
          [:i {:class "icon-facebook-sign"} "&nbsp;"]]
         [:a {:href (str "https://plus.google.com/share?url="
                         (:site-url metadata) (:url metadata))
              :target "_blank"}
          [:i {:class "icon-google-plus-sign"} "&nbsp;"]]
         [:br]
         [:a {:href (format "https://flattr.com/submit/auto?user_id=samrat&url=%s&title=%s&language=en_US"
                            (str (:site-url metadata) (:url metadata))
                            (:title metadata))}
          [:img {:src "https://api.flattr.com/button/flattr-badge-large.png"
                 :alt "Flattr this post"
                 :style "border:none;background:transparent;"}]]]]]
      [:hr]
      [:div {:style "text-align:center;"}
       [:a {:href "#comments"
            :id "showcomments"
            :onclick "document.getElementById(\"comments\").classList.toggle(\"showme\");document.getElementById(\"disqus_script\").src='http://samratmansingh.disqus.com/embed.js'"}
        "Show comments"]]
      [:div {:id "comments"}
       [:div {:id "disqus_thread"}]
       [:script {:id "disqus_script"
                 :type "text/javascript"}]]]]]))