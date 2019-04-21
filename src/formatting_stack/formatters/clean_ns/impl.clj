(ns formatting-stack.formatters.clean-ns.impl
  (:require
   [clojure.tools.namespace.file :as file]
   [clojure.tools.namespace.parse :as parse]
   [clojure.tools.reader :as tools.reader]
   [clojure.tools.reader.reader-types :refer [push-back-reader]]
   [clojure.walk :as walk]
   [com.gfredericks.how-to-ns :as how-to-ns]
   [formatting-stack.util]
   [formatting-stack.util :refer [rcomp]]
   [refactor-nrepl.config]
   [refactor-nrepl.ns.clean-ns :refer [clean-ns]]))

(defn ns-form-of [filename]
  (try
    (-> filename slurp push-back-reader parse/read-ns-decl)
    (catch Exception e
      (if (-> e ex-data :type #{:reader-exception})
        nil
        (throw e)))))

(defn used-namespace-names
  "NOTE: this returns the set of namespace _names_ that are used, not the set of namespaces that are used.

  e.g. a namespace which is exclusively used through `:refer` has a 'unused namespace name',
  but it is not unused (because it is referred).

  Use with caution accordingly, and not as a exclusive source of truth.

  `namespaces-that-should-never-cleaned` refers to the namespaces that are requiring libs - not the required libs themselves."
  [filename namespaces-that-should-never-cleaned]
  {:pre [(string? filename)
         (set? namespaces-that-should-never-cleaned)]}
  (let [buffer (slurp filename)
        ns-obj (-> filename ns-form-of parse/name-from-ns-decl the-ns)
        _ (assert ns-obj)
        [ns-form & contents] (binding [tools.reader/*alias-map* (ns-aliases ns-obj)]
                               (tools.reader/read-string {} (str "[ " buffer " ]")))
        _ (assert (and (list? ns-form)
                       (= 'ns (first ns-form)))
                  (str "Filename " filename ": expected the first form to be of `(ns ...)` type."))
        ns-name (-> ns-form parse/name-from-ns-decl)
        requires (-> ns-form parse/deps-from-ns-decl set)
        result (atom #{})
        aliases-keys (-> ns-obj ns-aliases keys set)
        expand-ident (fn [ident]
                       (when-let [n (some-> ident namespace symbol)]
                         (cond
                           (requires n)
                           n

                           (aliases-keys n)
                           (-> ns-obj ns-aliases (get n) str symbol))))]
    (if (namespaces-that-should-never-cleaned ns-name)
      (reset! result #{'.*})
      (walk/postwalk (fn traverse [x]
                       (some->> x meta (walk/postwalk traverse))
                       (when-let [n (and (ident? x) (expand-ident x))]
                         (when (requires n)
                           (swap! result conj n)))
                       x)
                     contents))
    @result))

(defn clean-ns-form [{:keys [how-to-ns-opts
                             refactor-nrepl-opts
                             filename
                             original-ns-form
                             namespaces-that-should-never-cleaned
                             libspec-whitelist]}]
  {:pre [how-to-ns-opts
         refactor-nrepl-opts
         filename
         original-ns-form
         namespaces-that-should-never-cleaned
         libspec-whitelist]}
  (let [whitelist (into libspec-whitelist (map str) (used-namespace-names filename namespaces-that-should-never-cleaned))]
    (binding [refactor-nrepl.config/*config* (-> refactor-nrepl-opts
                                                 (update :libspec-whitelist into whitelist))]
      (when-let [c (clean-ns {:path filename})]
        (when-not (= c original-ns-form)
          (let [v (-> c
                      (pr-str)
                      (how-to-ns/format-ns-str how-to-ns-opts))]
            (when-not (= v (how-to-ns/format-ns-str (str original-ns-form) how-to-ns-opts))
              v)))))))

(defonce require-lock (Object.))

(defn try-require [filename]
  (try
    (when-let [namespace (some-> filename file/read-file-ns-decl parse/name-from-ns-decl)]
      (locking require-lock
        (require namespace)))
    true
    (catch Exception _
      false)))

(defn has-duplicate-requires? [filename]
  (->> filename
       ns-form-of
       formatting-stack.util/require-from-ns-decl
       rest
       (map (fn [x]
              (if (coll? x)
                x
                [x])))
       (group-by first)
       (vals)
       (some (rcomp count (complement #{1})))))
