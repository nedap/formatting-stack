(ns formatting-stack.reporters.impl
  (:require
   [clojure.string :as string]
   [medley.core :refer [assoc-some]]
   [nedap.speced.def :as speced]))

(speced/defn normalize-filenames
  "Removes the CWD from the filenames, for more concise output, and also for ensuring correct grouping."
  [{:keys [^::speced/nilable ^string? filename] :as report}]
  (assoc-some report :filename (some-> filename
                                       (string/replace (re-pattern (str "^" (System/getProperty "user.dir")))
                                                       "")
                                       (string/replace #"^/" ""))))
