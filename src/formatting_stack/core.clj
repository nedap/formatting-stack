(ns formatting-stack.core
  (:require
   [clojure.main]
   [formatting-stack.background]
   [formatting-stack.impl.overrides :refer [apply-overrides]]
   [formatting-stack.indent-specs :refer [default-third-party-indent-specs]]
   [formatting-stack.protocols.formatter :as protocols.formatter]
   [formatting-stack.protocols.linter :as protocols.linter]
   [formatting-stack.protocols.processor :as protocols.processor]
   [formatting-stack.protocols.reporter :refer [report]]
   [formatting-stack.reporters.impl :refer [normalize-filenames]]
   [formatting-stack.reporters.pretty-printer :as reporters.pretty-printer]
   [formatting-stack.util :refer [with-serialized-output]]
   [nedap.speced.def :as speced]))

(defn files-from-strategies [strategies]
  (->> strategies
       (reduce (fn [files strategy]
                 (strategy :files files))
               [])
       distinct))

(speced/defn process! [method members ^vector? strategies]
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
                           strategies (into strategies specific-strategies)]
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

(speced/defn format! [& {:keys                                             [^vector? strategies
                                                                            ^::protocols.formatter/formatter-factory formatters
                                                                            ^::protocols.linter/linter-factory linters
                                                                            ^::protocols.processor/processor-factory processors
                                                                            third-party-indent-specs
                                                                            reporter
                                                                            in-background?]
                         {formatter-overrides                 :formatters
                          linter-overrides                    :linters
                          processor-overrides                 :processors
                          {formatters-strategies :formatters
                           linters-strategies    :linters
                           processors-strategies :processors} :strategies} :overrides}]
  ;; the following `or` clauses ensure that Components don't pass nil values
  (let [third-party-indent-specs            (or third-party-indent-specs default-third-party-indent-specs)
        reporter                            (or reporter (reporters.pretty-printer/new {}))
        formatter-overrides                 (or formatter-overrides {})
        linter-overrides                    (or linter-overrides {})
        processor-overrides                 (or processor-overrides {})
        in-background?                      (if (some? in-background?)
                                              in-background?
                                              true)

        formatters-strategies (apply-overrides strategies formatters-strategies)
        linters-strategies    (apply-overrides strategies linters-strategies)
        processors-strategies (apply-overrides strategies processors-strategies)

        formatters                          (-> formatters
                                                (protocols.formatter/formatters formatters-strategies third-party-indent-specs)
                                                (apply-overrides formatter-overrides))
        linters                             (-> linters
                                                (protocols.linter/linters linters-strategies)
                                                (apply-overrides linter-overrides))
        processors                          (-> processors
                                                (protocols.processor/processors processors-strategies third-party-indent-specs)
                                                (apply-overrides processor-overrides))

        impl (bound-fn [] ;; important that it's a bound-fn (for an undetermined reason)
               (->> [(process! protocols.formatter/format!  formatters  formatters-strategies)
                     (process! protocols.linter/lint!       linters     linters-strategies)
                     (process! protocols.processor/process! processors  processors-strategies)]
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
