(ns formatting-stack.test-helpers
  (:require
   [formatting-stack.util.diff :as util.diff]
   [nedap.speced.def :as speced])
  (:import
   (java.io File)))

(speced/defn filename-as-resource [^string? filename]
  (str "file:" (-> filename
                   File.
                   .getAbsolutePath)))

(defn with-mocked-diff-path
  "Fixture to stub the absolute path in #'util.diff/unified-diff"
  [t]
  (with-redefs [util.diff/to-absolute-path (fn [filename]
                                             (str "/mocked/absolute/path/" filename))]
    (t)))
