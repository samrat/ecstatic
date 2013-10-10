

(defn row [& rest]
  "Creates the div-box for a row in the bootstrap grid layout."
  `[:div {:class "row"} ~@rest])

(defn col [class-def & rest]
  "Creates the div-box for a column in the bootstrap grid layout.
'class-def' is the specification of the column."
  `[:div {:class ~class-def} ~@rest])
