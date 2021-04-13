(ns formatting-stack.reporters.impl
  (:require
   [clojure.spec.alpha :as spec]
   [clojure.string :as string]
   [formatting-stack.protocols.spec :as protocols.spec]
   [medley.core :refer [assoc-some]]
   [nedap.speced.def :as speced]))

(speced/def-with-doc ::filename
  "Filename which can be nil when an exception occurred"
  (spec/nilable ::protocols.spec/filename))

(speced/defn normalize-filenames
  "Removes the CWD from the filenames, for more concise output, and also for ensuring correct grouping."
  [{:keys [^::filename filename] :as report}]
  (assoc-some report :filename (some-> filename
                                       (string/replace (re-pattern (str "^" (System/getProperty "user.dir")))
                                                       "")
                                       (string/replace #"^/" ""))))

(speced/defn truncate-line-wise [^string? s, length]
  (if (= s "\n")
    s
    (->> (string/split s #"\n")
         (map (fn [s]
                (let [suffix "…"
                      string-length (count s)
                      suffix-length (count suffix)]
                  (if (<= string-length length)
                    s
                    (str (subs s
                               0
                               (- length suffix-length))
                         suffix)))))
         (string/join "\n"))))
