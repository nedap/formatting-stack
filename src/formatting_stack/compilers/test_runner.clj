(ns formatting-stack.compilers.test-runner
  "A test runner meant to be integrated with VCSs. JVM-only, and only `clojure.test` is targeted.

  This test runner gathers Clojure ns's out of filenames, derives _even more_ testing ns's out of them
  (via naming variations, project-wide `:require` analysis, and metadata analysis),
  and invokes `#'clojure.test/run-tests` out of that result."
  (:require
   [clojure.spec.alpha :as spec]
   [clojure.string :as string]
   [clojure.test]
   [formatting-stack.project-parsing :refer [project-namespaces]]
   [formatting-stack.protocols.compiler]
   [formatting-stack.strategies :refer [git-completely-staged git-diff-against-default-branch git-not-completely-staged]]
   [formatting-stack.strategies.impl :refer [filename->ns]]
   [nedap.utils.collections.eager :refer [partitioning-pmap]]
   [nedap.utils.speced :as speced])
  (:import
   (clojure.lang Namespace)))

(spec/def ::namespaces (spec/coll-of (partial instance? Namespace)))

(speced/defn testable-namespace? [^Namespace n]
  (->> n
       ns-publics
       vals
       (some (fn [var-ref]
               (-> var-ref meta :test ifn?)))
       boolean))

(speced/defn ^string? add-t
  "Some projects have the naming convention of prefixing the last segment with `t-`
   to denote a testing namespace."
  [^Namespace n]
  (let [s (str n)
        segments (-> n str (string/split #"\."))
        first-segments (butlast segments)
        last-segment (->> segments last (str "t-"))]
    (->> [last-segment]
         (concat first-segments)
         (string/join "."))))

(speced/defn ^::namespaces possible-derived-testing-namespaces [^Namespace n]
  (let [s (str n)
        derivations [(str s "-test")
                     (str s "-spec")

                     (str "test." s)
                     (str "unit." s)
                     (str "integration." s)
                     (str "acceptance." s)
                     (str "functional." s)

                     (str s ".test")
                     (str s ".unit")
                     (str s ".integration")
                     (str s ".acceptance")
                     (str s ".functional")

                     (add-t n)]]
    (->> derivations
         (map symbol)
         (keep find-ns))))

(speced/defn ^::namespaces sut-consumers
  "Returns the set namespaces which require `n` under the `sut` alias (or similar)."
  [^::namespaces corpus, ^Namespace n]
  (->> corpus
       (partitioning-pmap (speced/fn [^Namespace project-namespace]
                            (when (->> project-namespace
                                       ns-aliases
                                       (filter (speced/fn [[^symbol? k, ^Namespace v]]
                                                 (and (#{'sut 'subject 'system-under-test} k)
                                                      (= n v))))
                                       (seq))
                              project-namespace)))
       (keep identity)
       (vec)))

(speced/defn ^::namespaces testable-namespaces [filenames]
  (->> filenames
       (keep filename->ns)
       (keep (speced/fn [^Namespace n]
               (let [derived-from-name (->> n
                                            (possible-derived-testing-namespaces)
                                            (filterv testable-namespace?))
                     consumers (-> (project-namespaces) (sut-consumers n))]
                 (cond-> []
                   (testable-namespace? n) (conj n)
                   (seq consumers)         (into consumers)
                   (seq derived-from-name) (into derived-from-name)))))
       (apply concat)
       (distinct)))

(speced/defn ns->sym [^Namespace n]
  (-> n str symbol))

(ns-unmap *ns* 'Compiler)

;; Not provided into any default stack, as it would be overly assuming about users' practices
(defrecord Compiler []
  formatting-stack.protocols.compiler/Compiler
  (compile! [_ filenames]
    (when-let [test-namespaces (->> filenames
                                    (testable-namespaces)
                                    (map ns->sym)
                                    (seq))]
      (apply clojure.test/run-tests test-namespaces))))

(defn test!
  "Convenience function provided in case it is desired to leverage this ns's functionality,
  without adding its `#'Compiler` into your 'stack'.

  It gathers files from:
    * the `git diff` between the current Git branch and the `:target-branch` argument; plus
    * any files returned by `git status`.

  Out of those files, namespaces are derived (1:N, using smart heuristics),
  and those namespaces are run via `#'clojure.test/run-tests`."
  [& {:keys [target-branch]
      :or   {target-branch "master"}}]
  (let [filenames (->> (git-diff-against-default-branch :target-branch target-branch)
                       (concat (git-completely-staged))
                       (concat (git-not-completely-staged))
                       (distinct))]
    (-> (Compiler.)
        (formatting-stack.protocols.compiler/compile! filenames))))
