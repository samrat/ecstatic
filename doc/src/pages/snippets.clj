---
Title: Snippets
template: page
---

(let [col-spec "col-12 col-lg-6"]
  [:div
   (code/row
    (code/col "col-12"
              [:h1 "Snippets"]
              (snippet "snippet-intro")
              [:p "This feature is useful espacially if you need to keep
            data in different places on your site. Lets say adress
            data:"]))
   (code/row
    (code/col col-spec
              [:div {:class "well"} (snippet "address")])
    (code/col col-spec
              [:div {:class "well"} (snippet "address")]))
   (code/row
    (code/col "col-12"
              [:p "By keeping data like this in one spot, you avoid
              problems with inconsisten information on your page."]
              [:p "You can write your snippets either using the hiccup
              format or using markdown, dependent on what fits you the
              best."]
              [:p "See the code for this page for an example of how to use snippets."]))])
