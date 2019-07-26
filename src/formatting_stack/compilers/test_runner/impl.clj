(ns formatting-stack.compilers.test-runner.impl
  (:require
   [clojure.spec.alpha :as spec]
   [clojure.string :as string]
   [formatting-stack.project-parsing :refer [project-namespaces]]
   [formatting-stack.strategies.impl :refer [filename->ns]]
   [nedap.utils.collections.eager :refer [partitioning-pmap]]
   [nedap.speced.def :as speced])
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

(speced/defn insert-at [^int? i, ^string? s, ^vector? v]
  (reduce into [(subvec v 0 i)
                [s]
                (subvec v i)]))

(speced/defn permutations [^Namespace n, categorizations]
  (let [s (str n)
        ns-fragments (string/split s #"\.")
        index-count (-> ns-fragments count inc)
        indices (->> (range) (take index-count))]
    (->> (for [i indices]
           (->> categorizations
                (map (fn [c]
                       (->> ns-fragments
                            (insert-at i c)
                            (string/join "."))))))
         (apply concat))))

(speced/defn ^::namespaces possible-derived-testing-namespaces [^Namespace n]
  (let [s (str n)
        categorizations #{"test" "unit" "integration" "acceptance" "functional"}
        derivations [(str s "-test")
                     (str s "-spec")
                     (add-t n)]]
    (->> derivations
         (into (permutations n categorizations))
         (map symbol)
         (keep find-ns))))

(speced/defn ^::namespaces sut-consumers
  "Returns the set of namespaces which require `n` under the `sut` alias (or similar)."
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
