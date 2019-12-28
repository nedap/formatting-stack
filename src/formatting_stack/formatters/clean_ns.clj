(ns formatting-stack.formatters.clean-ns
  (:require
   [formatting-stack.formatters.clean-ns.impl :as impl]
   [formatting-stack.formatters.how-to-ns]
   [formatting-stack.protocols.formatter :as formatter]
   [formatting-stack.util :refer [process-in-parallel! try-require]]
   [formatting-stack.util.ns :refer [replace-ns-form!]]
   [medley.core :refer [deep-merge]]
   [nedap.utils.modular.api :refer [implement]]
   [nedap.speced.def :as speced]
   [refactor-nrepl.config]))

(defn make-cleaner [how-to-ns-opts refactor-nrepl-opts namespaces-that-should-never-cleaned libspec-whitelist filename]
  (speced/fn ^{::speced/spec (complement #{"nil"})} [original-ns-form]
    (some-> (impl/clean-ns-form {:how-to-ns-opts                       how-to-ns-opts
                                 :refactor-nrepl-opts                  refactor-nrepl-opts
                                 :filename                             filename
                                 :original-ns-form                     original-ns-form
                                 :namespaces-that-should-never-cleaned namespaces-that-should-never-cleaned
                                 :libspec-whitelist                    libspec-whitelist})
            pr-str)))

(defn clean! [how-to-ns-opts refactor-nrepl-opts namespaces-that-should-never-cleaned libspec-whitelist filename]
  (replace-ns-form! filename
                    (make-cleaner how-to-ns-opts refactor-nrepl-opts namespaces-that-should-never-cleaned libspec-whitelist filename)
                    "Cleaning unused imports:"
                    how-to-ns-opts))

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

(defn format!
  [{:keys [how-to-ns-opts refactor-nrepl-opts namespaces-that-should-never-cleaned libspec-whitelist]}
   files]
  (->> files
       (process-in-parallel! (fn [filename]
                               (when (and (try-require filename)
                                          (not (impl/has-duplicate-requires? filename)))
                                 (clean! how-to-ns-opts
                                         refactor-nrepl-opts
                                         namespaces-that-should-never-cleaned
                                         libspec-whitelist
                                         filename))))))

(defn new [{:keys [refactor-nrepl-opts libspec-whitelist how-to-ns-opts namespaces-that-should-never-cleaned]
            :or {namespaces-that-should-never-cleaned default-namespaces-that-should-never-cleaned
                 libspec-whitelist                    default-libspec-whitelist
                 refactor-nrepl-opts                  default-nrepl-opts
                 how-to-ns-opts                       {}}}]
  (implement {:refactor-nrepl-opts (deep-merge refactor-nrepl.config/*config* refactor-nrepl-opts)
              :how-to-ns-opts      (deep-merge formatting-stack.formatters.how-to-ns/default-how-to-ns-opts how-to-ns-opts)
              :libspec-whitelist   libspec-whitelist
              :namespaces-that-should-never-cleaned namespaces-that-should-never-cleaned}
    formatter/--format! format!))
