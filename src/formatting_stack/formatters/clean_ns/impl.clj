(ns formatting-stack.formatters.clean-ns.impl
  (:require
   [clojure.tools.namespace.parse :as parse]
   [clojure.tools.reader :as tools.reader]
   [clojure.tools.reader.reader-types :refer [indexing-push-back-reader push-back-reader]]
   [clojure.walk :as walk]
   [formatting-stack.util :refer [ensure-coll rcomp]]
   [nedap.speced.def :as speced])
  (:import
   (clojure.lang Namespace)))

(speced/defn safely-read-ns-contents [^string? buffer, ^Namespace ns-obj]
  (binding [tools.reader/*alias-map* (ns-aliases ns-obj)]
    (tools.reader/read-string {:read-cond :allow
                               :features  #{:clj}}
                              (str "[ " buffer " ]"))))

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
        ns-obj (-> filename formatting-stack.util/read-ns-decl parse/name-from-ns-decl the-ns)
        _ (assert ns-obj)
        [ns-form & contents] (safely-read-ns-contents buffer ns-obj)
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

  (require ;; lazy-loading in order to support unconfigured consumers
   '[refactor-nrepl.config]
   '[refactor-nrepl.ns.clean-ns])

  (let [whitelist (into libspec-whitelist (map str) (used-namespace-names filename namespaces-that-should-never-cleaned))
        config-var (resolve 'refactor-nrepl.config/*config*)
        clean-ns (resolve 'refactor-nrepl.ns.clean-ns/clean-ns)]
    (with-bindings* {config-var (-> refactor-nrepl-opts
                                    (update :libspec-whitelist into whitelist))}
      (fn []
        (clean-ns {:path filename})))))

(defn has-duplicate-requires? [filename]
  (->> filename
       formatting-stack.util/read-ns-decl
       formatting-stack.util/require-from-ns-decl
       rest
       (map ensure-coll)
       (group-by first)
       (vals)
       (some (rcomp count (complement #{1})))))
