(ns formatting-stack.formatters.clean-ns
  (:require
   [clojure.string :as str]
   [com.gfredericks.how-to-ns :as how-to-ns]
   [formatting-stack.formatters.how-to-ns]
   [formatting-stack.protocols.formatter]
   [medley.core :refer [deep-merge]]
   [refactor-nrepl.config]
   [refactor-nrepl.ns.clean-ns :refer [clean-ns]]))

(defn clean! [how-to-ns-opts refactor-nrepl-opts filename]
  (binding [refactor-nrepl.config/*config* refactor-nrepl-opts]
    (let [buffer (slurp filename)
          original-ns-form (how-to-ns/slurp-ns-from-string buffer)]
      (when-let [clean-ns-form (some-> (clean-ns {:path filename})
                                       (pr-str)
                                       (how-to-ns/format-ns-str how-to-ns-opts))]
        (println "Cleaning unused imports:" filename)
        (->> original-ns-form
             count
             (subs buffer)
             (str clean-ns-form)
             (spit filename))))))

(def default-nrepl-opts
  (-> refactor-nrepl.config/*config*
      (update :libspec-whitelist conj ".*\\.protocols\\..*" ".*\\.extensions\\..*" ".*\\.imports\\..*")
      (assoc :prefix-rewriting false)))

(defrecord Formatter [how-to-ns-opts refactor-nrepl-opts]
  formatting-stack.protocols.formatter/Formatter
  (format! [this files]
    (let [files (remove #(str/ends-with? % ".edn") files)
          refactor-nrepl-opts (deep-merge refactor-nrepl.config/*config*
                                          (or refactor-nrepl-opts default-nrepl-opts))
          how-to-ns-opts (deep-merge formatting-stack.formatters.how-to-ns/default-how-to-ns-opts
                                     (or how-to-ns-opts {}))]
      (mapv (partial clean! how-to-ns-opts refactor-nrepl-opts)
            files))))
