(ns formatting-stack.formatters.clean-ns
  (:require
   [clojure.string :as str]
   [com.gfredericks.how-to-ns :as how-to-ns]
   [formatting-stack.formatters.clean-ns.impl :as impl]
   [formatting-stack.formatters.how-to-ns]
   [formatting-stack.protocols.formatter]
   [medley.core :refer [deep-merge]]
   [refactor-nrepl.config]))

(defn clean! [how-to-ns-opts refactor-nrepl-opts filename]
  (let [buffer (slurp filename)
        original-ns-form (how-to-ns/slurp-ns-from-string buffer)]
    (when-let [clean-ns-form (impl/clean-ns-form {:how-to-ns-opts how-to-ns-opts
                                                  :refactor-nrepl-opts refactor-nrepl-opts,
                                                  :filename filename
                                                  :original-ns-form (read-string original-ns-form)})]
      (when-not (= original-ns-form clean-ns-form)
        (println "Cleaning unused imports:" filename)
        (->> original-ns-form
             count
             (subs buffer)
             (str clean-ns-form)
             (spit filename))))))

(def default-nrepl-opts
  (-> refactor-nrepl.config/*config*
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
