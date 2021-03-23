(ns formatting-stack.test-helpers
  (:require
   [formatting-stack.util.diff :as util.diff]
   [nedap.speced.def :as speced])
  (:import
   (java.io File)))

(speced/defn filename-as-resource [^string? filename]
  (str "file:" (-> filename
                   File.
                   .getCanonicalPath)))

(defn with-mocked-diff-path
  "Fixture to stub the absolute path in #'util.diff/unified-diff"
  [t]
  (binding [util.diff/*to-absolute-path-fn* (fn [filename]
                                              (str "/mocked/absolute/path/" filename))]
    (t)))
