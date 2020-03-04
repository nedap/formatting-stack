(ns formatting-stack.reporters.impl
  (:require
   [clojure.string :as string]
   [nedap.speced.def :as speced]))

(speced/defn normalize-filenames
  "Removes the CWD from the filenames, for more concise output, and also for ensuring correct grouping."
  [{:keys [^string? filename] :as report}]
  (assoc report :filename (-> filename
                              (string/replace (re-pattern (str "^" (System/getProperty "user.dir")))
                                              "")
                              (string/replace #"^/" ""))))
