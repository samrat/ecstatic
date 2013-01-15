(ns ecstatic.utils
  (:use ring.adapter.jetty
        ring.middleware.file
        ring.util.response))

(defn serve-static-wrapper [output]
  (defn serve-static [req] 
    (let [mime-types {".clj" "text/plain"
                      ".mp4" "video/mp4"
                      ".ogv" "video/ogg"}]
      (if-let [f (file-response (:uri req) {:root output})] 
        (if-let [mimetype (mime-types (re-find #"\..+$" (:uri req)))] 
          (merge f {:headers {"Content-Type" mimetype}}) 
          f)))))

(defn serve [output]
  (do (future (run-jetty (serve-static-wrapper output) {:port 8080}))))