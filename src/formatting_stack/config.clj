(ns formatting-stack.config
  (:require
   [aero.core :as aero]
   [clojure.java.io :as io]
   [clojure.set :refer [rename-keys]]
   [clojure.spec.alpha :as spec]
   [formatting-stack.util :refer [resolve-keyword]]
   [medley.core :refer [assoc-some]]
   [nedap.speced.def :as speced]
   [nedap.utils.spec.predicates :as predicates])
  (:import
   (java.io File)))

(spec/def :formatting-stack/files (spec/coll-of ::predicates/present-string))
(spec/def :formatting-stack/plugins (spec/coll-of qualified-keyword?))
(spec/def :formatting-stack/reporters (spec/coll-of qualified-symbol?))
(spec/def :formatting-stack/cli-opts (spec/map-of keyword? some?))
(spec/def :formatting-stack/cli-args (spec/coll-of string?))
(spec/def :formatting-stack/dry-run? boolean?)

(spec/def ::config ;; maybe rename to `context`
  (spec/keys :req [:formatting-stack/files
                   :formatting-stack/plugins]
             :opt [:formatting-stack/reporters
                   :formatting-stack/cli-opts
                   :formatting-stack/cli-args
                   :formatting-stack/dry-run?]))

(defn normalize [config]
  (rename-keys config {:dry-run?  :formatting-stack/dry-run?
                       :reporters :formatting-stack/reporters
                       :files     :formatting-stack/files
                       :plugins   :formatting-stack/plugins}))

(defn update-files [{:formatting-stack/keys [cli-args files]
                     :as config}]
  (assoc config :formatting-stack/files (->> (or (seq cli-args) files)
                                             (mapcat (comp file-seq io/file))
                                             (filter #(.isFile ^File %))
                                             (map #(.getCanonicalPath ^File %)))))

(speced/defn ^::config apply-cli-opts [config options]
  (-> config
      (assoc :formatting-stack/dry-run? (get options :dry-run?
                                             (get config :formatting-stack/dry-run?
                                                  false)))
      (assoc :formatting-stack/reporters (get options :reporters
                                              (get config :formatting-stack/reporters
                                                   '[formatting-stack.v2.reporter.pretty-summary/report
                                                     formatting-stack.v2.reporter.progress/report])))
      (assoc :formatting-stack/cli-opts options)))

(speced/defn ^::config apply-cli-args [config arguments]
  (assoc-some config
              :formatting-stack/cli-args (seq arguments)))

(speced/defn ^::config resolve-plugins [{:formatting-stack/keys [plugins] :as ^::config config}]
  (run! resolve-keyword plugins)
  config)

(speced/defn ^::config read-config [filename {:keys [profile] :as opts}]
  (-> (io/file filename)
      (aero/read-config opts)
      (normalize)))
