(ns formatting-stack.formatters.clean-ns.impl
  (:require
   [clojure.tools.namespace.parse :as parse]
   [clojure.tools.reader :as tools.reader]
   [clojure.tools.reader.reader-types :refer [push-back-reader]]
   [clojure.walk :as walk]
   [com.gfredericks.how-to-ns :as how-to-ns]
   [formatting-stack.util :refer [rcomp]]
   [refactor-nrepl.ns.clean-ns :refer [clean-ns]]))

(defn ns-form-of [filename]
  (-> filename slurp push-back-reader parse/read-ns-decl))

(defn used-namespace-names
  "NOTE: this returns the set of namespace _names_ that are used, not the set of namespaces that are used.

  e.g. a namespace which is exclusively used through `:refer` has a 'unused namespace name',
  but it is not unused (because it is referred).

  Use with caution accordingly, and not as a exclusive source of truth."
  [filename]
  (let [buffer (slurp filename)
        ns-obj (-> filename ns-form-of parse/name-from-ns-decl the-ns)
        _ (assert ns-obj)
        [ns-form & contents] (binding [tools.reader/*alias-map* (ns-aliases ns-obj)]
                               (tools.reader/read-string {} (str "[ " buffer " ]")))
        _ (assert (and (list? ns-form)
                       (= 'ns (first ns-form)))
                  (str "Filename " filename ": expected the first form to be of `(ns ...)` type."))
        requires (-> ns-form parse/deps-from-ns-decl set)
        result (atom #{})
        aliases-keys (-> ns-obj ns-aliases keys set)
        expand-ident (fn [ident]
                       (when-let [n (some-> ident namespace symbol)]
                         (cond (requires n)
                               n

                               (aliases-keys n)
                               (-> ns-obj ns-aliases (get n) str symbol))))]
    (walk/postwalk (fn traverse [x]
                     (some->> x meta (walk/postwalk traverse))
                     (when-let [n (and (ident? x) (expand-ident x))]
                       (when (requires n)
                         (swap! result conj n)))
                     x)
                   contents)
    @result))

(defn parse-require-form [ns-form]
  (let [require-form (atom nil)]
    (walk/postwalk (fn [x]
                     (when (and (sequential? x)
                                (ident? (first x))
                                (= "require" (name (first x))))
                       (reset! require-form x))
                     x)
                   ns-form)
    @require-form))

(defn ensure-vector [x]
  (if (sequential? x)
    x
    [x]))

(defn any-leaf?
  "Does any leaf in `form` (to be traversed with clojure.walk) satisfy `pred`?"
  [pred form]
  (let [result (atom false)]
    (walk/postwalk (fn [x]
                     (when (pred x)
                       (reset! result true))
                     x)
                   form)
    @result))

(defn ensure-whitelist [ns-form whitelist original-ns-form]
  (let [require-form (parse-require-form ns-form)
        original-form (parse-require-form original-ns-form)
        ensured-form (->> whitelist
                          (remove (->> require-form
                                       rest
                                       (map (rcomp ensure-vector first))
                                       set))
                          (map (fn [x]
                                 (->> original-form
                                      rest
                                      (map ensure-vector)
                                      (filter (fn [[lib-name]]
                                                (= lib-name x)))
                                      first)))
                          (concat require-form))
        first-attempt (walk/postwalk-replace {require-form ensured-form} ns-form)]
    (if (any-leaf? #{:require} first-attempt) ;; refactor-nrepl may have stripped the `:require` altogether
      first-attempt
      (let [[ns-keyword ns-name & tail] first-attempt]
        `(~ns-keyword ~ns-name (:require ~@ensured-form) ~@tail)))))

(defn clean-ns-form [how-to-ns-opts filename original-ns-form]
  (when-let [c (clean-ns {:path filename})]
    (let [whitelist (used-namespace-names filename)]
      (-> c
          (ensure-whitelist whitelist original-ns-form)
          (pr-str)
          (how-to-ns/format-ns-str how-to-ns-opts)))))
