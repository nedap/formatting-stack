(ns formatting-stack.test-helpers
  (:require
   [clojure.java.io :as io]
   [clojure.java.shell :refer [sh with-sh-dir]]
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

;; https://gist.github.com/edw/5128978#gistcomment-2956766
(defn recursive-delete
  [& fs]
  (when-let [f (first fs)]
    (if-let [cs (seq (.listFiles (io/file f)))]
      (recur (concat cs fs))
      (do (io/delete-file f :silently)
          (recur (rest fs))))))

(def git-integration-dir
  "./git-integration-testing")

(defn with-git-repo
  "Clones current repo into #'git-integration-dir for use in testing.
  Folder is removed afterwards."
  [t]
  (with-sh-dir git-integration-dir
    (try
      (recursive-delete git-integration-dir)
      (.mkdirs (io/as-file git-integration-dir))
      (sh "git" "clone" (System/getProperty "user.dir") ".")
      (t)
      (finally
        (recursive-delete git-integration-dir)))))
