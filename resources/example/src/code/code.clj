(defn year [date]
  (clj-time.core/year
   (clj-time.coerce/from-string date)))
