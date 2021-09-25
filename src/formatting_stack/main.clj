(ns formatting-stack.main
  (:require
   [clojure.tools.cli :as tools.cli]
   [formatting-stack.config :as config]
   [formatting-stack.hooks :refer [run-hook]]
   [formatting-stack.plugin :as plugin]
   [formatting-stack.core :as formatting-stack]
   [formatting-stack.util :refer [accumulate resolve-keyword resolve-sym]]))

(def ^:private cli-options
  [["-c" "--config-file FILE" "Config file to read."
    :default "formatting-stack.edn"]
   ["-p" "--profile PROFILE" "Configuration profile."
    :default :default
    :parse-fn keyword]
   [nil "--reporter SYMBOL" "Overwrite test reporter, can be specified multiple times"
    :id          :reporters
    :assoc-fn    accumulate
    :parse-fn    symbol
    :validate-fn resolve-sym]
   [nil "--plugin SYMBOL" "Add plugin, can be specified multiple times"
    :id          :plugins
    :assoc-fn    accumulate
    :parse-fn    symbol
    :validate-fn resolve-keyword]
   [nil "--dry-run" "Run formatters in dry-run mode, defaults to false"
    :id :dry-run?]
   ["-h" "--help" "Display this help message."]
   [nil "--print-config" "Display the fully merged and normalized config, then exit."]])

(defn- -main* [& args]
  (let [{{:keys [config-file profile plugins]} :options} (tools.cli/parse-opts args cli-options)
        {:formatting-stack/keys [plugins] :as config} (-> (config/read-config config-file {:profile profile})
                                                          (update :formatting-stack/plugins into (when (seq plugins)
                                                                                                   plugins))
                                                          (config/resolve-plugins))

        option-spec                                (run-hook #'plugin/cli-options into plugins cli-options)
        {:keys [errors options arguments summary]} (tools.cli/parse-opts args option-spec)]

    (cond
      (seq errors)
      (do
        (run! println errors)
        (println summary)
        -1)

      (contains? options :help)
      (do (println summary) 0)

      :else
      (formatting-stack/run
       {:errors       errors
        :arguments    arguments
        :options      options
        :summary      summary
        :config       config}))))

(defn -main [& args]
  (System/exit (apply -main* args)))
