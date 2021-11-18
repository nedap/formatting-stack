(ns formatting-stack.formatters.clean-ns
  (:require
   [formatting-stack.formatters.clean-ns.impl :as impl]
   [formatting-stack.formatters.how-to-ns]
   [formatting-stack.protocols.formatter :as formatter]
   [formatting-stack.protocols.linter :as linter]
   [formatting-stack.util :refer [ensure-sequential process-in-parallel! try-require unlimited-pr-str]]
   [formatting-stack.util.diff :as diff :refer [diff->line-numbers]]
   [formatting-stack.util.ns :as util.ns :refer [write-ns-replacement!]]
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
            unlimited-pr-str)))

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
        (assoc :prefix-rewriting false
               :print-right-margin nil
               :print-miser-width nil))))

(def default-namespaces-that-should-never-cleaned
  #{'user 'dev})

(defn replaceable-ns-form
  [{:keys [how-to-ns-opts refactor-nrepl-opts namespaces-that-should-never-cleaned libspec-whitelist]} filename]
  (when (and (try-require filename)
             (not (impl/has-duplicate-requires? filename)))
    (util.ns/replaceable-ns-form
     filename
     (make-cleaner how-to-ns-opts refactor-nrepl-opts namespaces-that-should-never-cleaned libspec-whitelist filename)
     how-to-ns-opts)))

(defn format!
  [this files]
  (->> files
       (process-in-parallel! (fn [filename]
                               (when-let [ns-replacement (replaceable-ns-form this filename)]
                                 (println "Cleaning unused imports:" filename)
                                 (write-ns-replacement! filename ns-replacement)))))
  nil)

(defn lint! [this files]
  (->> files
       (process-in-parallel! (fn [filename]
                               (when-let [{:keys [final-ns-form-str
                                                  original-ns-form-str]} (replaceable-ns-form this filename)]
                                 (let [diff (diff/unified-diff filename original-ns-form-str final-ns-form-str)]
                                   (->> (diff->line-numbers diff)
                                        (mapv (fn [{:keys [start]}]
                                                {:filename filename
                                                 :diff diff
                                                 :level :warning
                                                 :column 0
                                                 :line start
                                                 :msg "ns can be cleaned"
                                                 :source :formatting-stack/clean-ns})))))))
       (filter some?)
       (mapcat ensure-sequential)))

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
    formatter/--format! format!
    linter/--lint!      lint!))
