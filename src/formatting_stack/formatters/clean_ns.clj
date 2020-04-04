(ns formatting-stack.formatters.clean-ns
  (:require
   [formatting-stack.formatters.clean-ns.impl :as impl]
   [formatting-stack.formatters.how-to-ns]
   [formatting-stack.protocols.formatter :as formatter]
   [formatting-stack.util :refer [process-in-parallel! try-require]]
   [formatting-stack.util.ns :refer [replaceable-ns-form replace-ns-form!]]
   [medley.core :refer [deep-merge]]
   [nedap.speced.def :as speced]
   [nedap.utils.modular.api :refer [implement]]))

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
  (println "Cleaning unused imports:" filename)
  (replace-ns-form! filename
                    (make-cleaner how-to-ns-opts refactor-nrepl-opts namespaces-that-should-never-cleaned libspec-whitelist filename)
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

(def default-nrepl-config-opts
  (delay

    (require ;; lazy-loading in order to support unconfigured consumers
     '[refactor-nrepl.config])

    (-> 'refactor-nrepl.config/*config* resolve deref)))

(def default-nrepl-opts
  (delay
    (-> @default-nrepl-config-opts
        (assoc :prefix-rewriting false))))

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
                                         filename)))))
  nil)


(defn lint! [{:keys [how-to-ns-opts refactor-nrepl-opts namespaces-that-should-never-cleaned libspec-whitelist]} files]
  (->> files
       (process-in-parallel! (fn [filename]
                               (when-let [{:keys [final-ns-form-str
                                                  original-ns-form-str]}
                                          (replaceable-ns-form filename
                                                               (make-cleaner how-to-ns-opts refactor-nrepl-opts namespaces-that-should-never-cleaned libspec-whitelist filename)
                                                               how-to-ns-opts)]
                                 {:filename filename
                                  :diff     (#'cljfmt.diff/unified-diff filename original-ns-form-str final-ns-form-str)
                                  :msg      "ns can be cleaned"
                                  :column   0 ;; FIXME extract from diff
                                  :line     0
                                  :level    :warning
                                  :source   :formatting-stack/clean-ns})))))

(defn new [{:keys [refactor-nrepl-opts libspec-whitelist how-to-ns-opts namespaces-that-should-never-cleaned]
            :or   {namespaces-that-should-never-cleaned default-namespaces-that-should-never-cleaned
                   libspec-whitelist                    default-libspec-whitelist
                   refactor-nrepl-opts                  @default-nrepl-opts
                   how-to-ns-opts                       {}}}]
  (implement {:id ::id
              :refactor-nrepl-opts (deep-merge @default-nrepl-config-opts refactor-nrepl-opts)
              :how-to-ns-opts      (deep-merge formatting-stack.formatters.how-to-ns/default-how-to-ns-opts how-to-ns-opts)
              :libspec-whitelist   libspec-whitelist
              :namespaces-that-should-never-cleaned namespaces-that-should-never-cleaned}
    formatter/--format! format!))
