(with-base-template "base"
  (let [col-spec "col-12 col-lg-6"]
    [:div
     (code/row
      (code/col "col-12"
                [:p "Ecstatic is a static website and blog generator that allows you
to write your site and blog using either hiccup-markup or markdown as syntax"]
                [:p "The features are:"]))
     (code/row
      (code/col col-spec
                [:h3 (helem/link-to (page-url "snippets.clj") "Snippets")]
                (snippet "snippet-intro"))
      (code/col col-spec
                [:h3 (helem/link-to (page-url "custom-code.clj") "Custom-Code")]
                "Write helper function to clean up your markup."))
     (code/row
      (code/col col-spec
                [:h3 (helem/link-to (page-url "rss.clj") "Rss-Feed")]
                "Something about rss") ;TODO
      (code/col col-spec
                [:h3 (helem/link-to (page-url "templates.clj") "Templates")]
                "Something about templates"))])) ; TODO
