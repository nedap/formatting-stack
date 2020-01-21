(ns formatting-stack.core
  (:require
   [clojure.main]
   [formatting-stack.background]
   [formatting-stack.defaults :refer [default-processors default-formatters default-linters default-strategies]]
   [formatting-stack.indent-specs :refer [default-third-party-indent-specs]]
   [formatting-stack.protocols.formatter :as protocols.formatter]
   [formatting-stack.protocols.linter :as protocols.linter]
   [formatting-stack.protocols.processor :as protocols.processor]
   [formatting-stack.protocols.reporter :refer [report]]
   [formatting-stack.util :refer [with-serialized-output]]
   [nedap.utils.modular.api :refer [implement]]))

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
                        (println "Encountered an exception, which will be printed in the next line."
                                 "formatting-stack execution has *not* been aborted.")
                        (-> e .printStackTrace)
                        nil)
                      (catch AssertionError e
                        (println "Encountered an exception, which will be printed in the next line."
                                 "formatting-stack execution has *not* been aborted.")
                        (-> e .printStackTrace)
                        nil)))))))))

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
        in-background?           (if (some? in-background?)
                                   in-background?
                                   true)
        {formatters-strategies :formatters
         linters-strategies    :linters
         processors-strategies :processors} strategies
        impl (bound-fn [] ;; important that it's a bound-fn (for an undetermined reason)
               (process! protocols.formatter/format! formatters formatters-strategies strategies)
               (->> (process! protocols.linter/lint! linters    linters-strategies    strategies)
                    (report reporter))
               (process! protocols.processor/process! processors  processors-strategies  strategies))]
    (if in-background?
      (do
        (reset! formatting-stack.background/workload impl)
        'Running...)
      (do
        (impl)
        'Done))))
