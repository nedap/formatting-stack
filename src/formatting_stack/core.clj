(ns formatting-stack.core
  (:require
   [clojure.main]
   [clojure.pprint :as pprint]
   [formatting-stack.background]
   [formatting-stack.config :as config]
   [formatting-stack.defaults :refer [default-formatters default-linters default-processors default-strategies]]
   [formatting-stack.hooks :refer [run-hook]]
   [formatting-stack.indent-specs :refer [default-third-party-indent-specs]]
   [formatting-stack.plugin :as plugin]
   [formatting-stack.protocols.formatter :as protocols.formatter]
   [formatting-stack.protocols.linter :as protocols.linter]
   [formatting-stack.protocols.processor :as protocols.processor]
   [formatting-stack.protocols.reporter :refer [report]]
   [formatting-stack.report :as report]
   [formatting-stack.reporters.impl :refer [normalize-filenames]]
   [formatting-stack.reporters.pretty-printer :as reporters.pretty-printer]
   [formatting-stack.util :refer [resolve-sym with-serialized-output]]))

(defn files-from-strategies [strategies]
  (->> strategies
       (reduce (fn [files strategy]
                 (strategy :files files))
               [])
       distinct))

(defn process! [method members category-strategies default-strategies]
  ;; `memoize` rationale: results are cached not for performance,
  ;; but for avoiding the scenario where one `member` alters the git status,
  ;; so the subsequent `member`s' strategies won't perceive the same set of files than the first one.
  ;; e.g. cljfmt may operate upon `strategies/git-completely-staged`, formatting some files accordingly.
  ;; Then `how-to-ns`, which follows the same strategy, would perceive a dirty git status.
  ;; Accordingly it would do nothing, which is undesirable.
  (let [files (memoize (fn [strategies]
                         (files-from-strategies strategies)))]
    (with-serialized-output
      (->> members
           (mapcat (fn [member]
                     (let [{specific-strategies :strategies} member
                           strategies (or specific-strategies category-strategies default-strategies)]
                       (try
                         (->> strategies files (method member))
                         (catch Exception e
                           [{:exception e
                             :source    :formatting-stack/process!
                             :msg       (str "Exception during " member)
                             :level     :exception}])
                         (catch AssertionError e
                           [{:exception e
                             :source    :formatting-stack/process!
                             :msg       (str "Exception during " member)
                             :level     :exception}])))))
           (doall)))))

(defn format! [& {:keys [strategies
                         third-party-indent-specs
                         formatters
                         linters
                         processors
                         reporter
                         in-background?]}]
  ;; the following `or` clauses ensure that Components don't pass nil values
  (let [strategies               (or strategies default-strategies)
        third-party-indent-specs (or third-party-indent-specs default-third-party-indent-specs)
        formatters               (or formatters (default-formatters third-party-indent-specs))
        linters                  (or linters default-linters)
        processors               (or processors (default-processors third-party-indent-specs))
        reporter                 (or reporter (reporters.pretty-printer/new {}))
        in-background?           (if (some? in-background?)
                                   in-background?
                                   true)
        {formatters-strategies :formatters
         linters-strategies    :linters
         processors-strategies :processors} strategies
        impl (bound-fn [] ;; important that it's a bound-fn (for an undetermined reason)
               (->> [(process! protocols.formatter/format!  formatters  formatters-strategies strategies)
                     (process! protocols.linter/lint!       linters     linters-strategies    strategies)
                     (process! protocols.processor/process! processors  processors-strategies strategies)]
                    (apply concat)
                    (mapv normalize-filenames)
                    (report reporter)))]
    (if in-background?
      (do
        (reset! formatting-stack.background/workload impl)
        'Running...)
      (do
        (impl)
        'Done))))

(defn run [{:keys [config arguments options]}]
  (let [{:formatting-stack/keys [plugins reporters]
         :as config} (-> config
                         (config/apply-cli-opts options)
                         (config/apply-cli-args arguments)
                         (config/update-files)
                         (config/resolve-plugins))]
    (binding [report/*reporters* (mapv resolve-sym reporters)]
      (let [config (->> config
                        (run-hook #'plugin/config merge plugins)
                        (run-hook #'plugin/post-config merge plugins))]
        (binding [report/*reporters* (mapv resolve-sym (:formatting-stack/reporters config))]
          (cond
            (contains? options :print-config)
            (do
              (binding [*print-namespace-maps* false]
                (pprint/pprint config))
              0)

            (not (seq (:formatting-stack/files config)))
            (do
              (println "No files passed!")
              -1)

            :else
            (let [config (->> config
                              (run-hook #'plugin/pre-process merge plugins)
                              (run-hook #'plugin/process #(merge-with into %1 %2) plugins)
                              (run-hook #'plugin/post-process merge plugins))]
              (report/report {:type    :summary
                              :config  config})
              config)))))))
