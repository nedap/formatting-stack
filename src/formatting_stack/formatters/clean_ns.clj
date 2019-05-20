(ns formatting-stack.formatters.clean-ns
  (:require
   [com.gfredericks.how-to-ns :as how-to-ns]
   [formatting-stack.formatters.clean-ns.impl :as impl]
   [formatting-stack.formatters.how-to-ns]
   [formatting-stack.protocols.formatter]
   [formatting-stack.util :refer [process-in-parallel! try-require without-aliases]]
   [medley.core :refer [deep-merge]]
   [refactor-nrepl.config]))

(defn clean! [how-to-ns-opts refactor-nrepl-opts namespaces-that-should-never-cleaned libspec-whitelist filename]
  (let [buffer (slurp filename)
        original-ns-form (how-to-ns/slurp-ns-from-string buffer)]
    (when-let [clean-ns-form (without-aliases
                               (impl/clean-ns-form {:how-to-ns-opts                       how-to-ns-opts
                                                    :refactor-nrepl-opts                  refactor-nrepl-opts
                                                    :filename                             filename
                                                    :original-ns-form                     (read-string original-ns-form)
                                                    :namespaces-that-should-never-cleaned namespaces-that-should-never-cleaned
                                                    :libspec-whitelist                    libspec-whitelist}))]
      (when-not (= original-ns-form clean-ns-form)
        (println "Cleaning unused imports:" filename)
        (->> original-ns-form
             count
             (subs buffer)
             (str clean-ns-form)
             (spit filename))))))

(def default-libspecs
  ["specs" "imports" "exports" "extensions" "side-effects" "init" "initialization" "load" "migration" "migrations"])

(defn make-default-libspec-whitelist [& {:keys [libspecs]
                                         :or   {libspecs default-libspecs}}]
  (letfn [(variations [x]
            #{(str "^" x ".")
              (str "." x ".")
              (str "." x "$")})]
    (->> libspecs
         (mapcat variations)
         (into #{"^cljsjs." "clojure.main"}))))

(def default-libspec-whitelist
  (make-default-libspec-whitelist))

(def default-nrepl-opts
  (-> refactor-nrepl.config/*config*
      (assoc :prefix-rewriting false)))

(def default-namespaces-that-should-never-cleaned
  #{'user 'dev})

(defrecord Formatter [how-to-ns-opts refactor-nrepl-opts namespaces-that-should-never-cleaned libspec-whitelist]
  formatting-stack.protocols.formatter/Formatter
  (format! [this files]
    (let [refactor-nrepl-opts (deep-merge refactor-nrepl.config/*config*
                                          (or refactor-nrepl-opts default-nrepl-opts))
          how-to-ns-opts (deep-merge formatting-stack.formatters.how-to-ns/default-how-to-ns-opts
                                     (or how-to-ns-opts {}))
          namespaces-that-should-never-cleaned (or namespaces-that-should-never-cleaned
                                                   default-namespaces-that-should-never-cleaned)
          libspec-whitelist (or libspec-whitelist default-libspec-whitelist)]
      (->> files
           (process-in-parallel! (fn [filename]
                                   (when (and (try-require filename)
                                              (not (impl/has-duplicate-requires? filename)))
                                     (clean! how-to-ns-opts
                                             refactor-nrepl-opts
                                             namespaces-that-should-never-cleaned
                                             libspec-whitelist
                                             filename))))))))
