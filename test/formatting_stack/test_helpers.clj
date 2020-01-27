(ns formatting-stack.test-helpers
  (:require
   [nedap.speced.def :as speced])
  (:import
   (java.io File)))

(speced/defn complete-filename [^string? filename]
  (str "file:" (-> filename
                   File.
                   .getAbsolutePath)))
