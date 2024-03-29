(ns formatting-stack.processors.test-runner
  "A test runner meant to be integrated with VCSs. JVM-only, and only `clojure.test` is targeted.

  This test runner gathers Clojure ns's out of filenames, derives _even more_ testing ns's out of them
  (via naming variations, project-wide `:require` analysis, and metadata analysis),
  and invokes `#'clojure.test/run-tests` out of that result."
  (:require
   [clojure.test]
   [formatting-stack.processors.test-runner.impl :refer [ns->sym testable-namespaces]]
   [formatting-stack.protocols.processor :as processor]
   [formatting-stack.strategies :as strategies :refer [git-completely-staged git-diff-against-default-branch git-not-completely-staged]]
   [nedap.utils.modular.api :refer [implement]]))

;; Not provided into any default stack, as it would be overly assuming about users' practices
(defn process! [_ filenames]
  (assert clojure.test/*load-tests*)
  (when-let [test-namespaces (->> filenames
                                  (testable-namespaces)
                                  (map ns->sym)
                                  (seq))]
    (apply clojure.test/run-tests test-namespaces))
  nil)

(defn test!
  "Convenience function provided in case it is desired to leverage this ns's functionality,
  without adding its component into your 'stack'.

  It gathers files from:
    * the `git diff` between the current Git branch and the `:target-branch` argument; plus
    * any files returned by `git status`.

  Out of those files, namespaces are derived (1:N, using smart heuristics),
  and those namespaces are run via `#'clojure.test/run-tests`."
  [& {:keys [target-branch]
      :or   {target-branch (strategies/default-branch-name)}}]
  (let [filenames (->> (git-diff-against-default-branch :target-branch target-branch)
                       (concat (git-completely-staged :files []))
                       (concat (git-not-completely-staged :files []))
                       (distinct))]
    (process! {} filenames)))

(defn new []
  (implement {:id ::id}
    processor/--process! process!))
