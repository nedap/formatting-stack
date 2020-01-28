(ns formatting-stack.test-helpers
  (:require
   [nedap.speced.def :as speced])
  (:import
   (java.io File)))

(speced/defn filename-as-resource [^string? filename]
  (str "file:" (-> filename
                   File.
                   .getAbsolutePath)))
