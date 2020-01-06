(ns formatting-stack.formatters.cljfmt.impl
  (:require
   [cljfmt.core]
   [clojure.spec.alpha :as spec]
   [clojure.string :as string]
   [clojure.tools.namespace.file :as file]
   [clojure.tools.namespace.parse :as parse]
   [formatting-stack.indent-specs]
   [formatting-stack.project-parsing :refer [project-namespaces]]
   [formatting-stack.util :refer [dissoc-by rcomp require-lock]]
   [nedap.speced.def :as speced]))

(def ^:dynamic *cache* nil)

(defn safe-ns-map
  "Works around:

  * .clj files not required yet (bad Reloaded integration);
  * .clj files not meant to be required (test files, scripts, etc); and
  * .cljs files (cannot be required from JVM clojure)."
  [namespace]
  (try
    (locking require-lock
      (require namespace))
    (ns-map namespace)
    (catch Exception _
      {})))

(spec/def ::indent-key #{:style/indent :style.cljfmt/indent :style.cljfmt/type})

(spec/def ::indent-spec (spec/map-of ::indent-key any?))

(spec/def ::indent-mapping (spec/map-of symbol? ::indent-spec))

;; :block is for things like do, with*, ->
;; :inner is for things like def (and for workarounding multi-arity defns with some sort of "body" argument)
;; see https://git.io/fhMtH for examples
(speced/defn to-cljfmt-indent [{cider-indent  :style/indent
                                cljfmt-indent :style.cljfmt/indent
                                cljfmt-type   :style.cljfmt/type
                                :as           ^::speced/nilable ^::indent-spec _}]
  (or cljfmt-indent
      (and (number? cider-indent) [[(or cljfmt-type :block)
                                    cider-indent]])
      (and (#{:defn} cider-indent) [[:inner 0]])
      nil))

(defn cljfmt-third-party-indent-specs [specs]
  (->> specs
       (map (fn [[k v]]
              [k (to-cljfmt-indent v)]))
       (remove (rcomp second nil?))
       (into {})))

(defn fully-qualified-name-of [var-ref]
  (let [name (some-> var-ref meta :name)]
    (some-> var-ref meta :ns (str "/" name) symbol)))

(defn namespace-macros [ns]
  (some->> ns ns-publics vals (filter (fn [var-ref]
                                        (or (#{:macro :arglists} (meta var-ref))
                                            (fn? @var-ref))))))

(defn project-macro-mappings []
  (or (some-> *cache* deref ::project-macro-mappings)
      (let [v (->> (project-namespaces)
                   (map namespace-macros)
                   (filter seq)
                   (flatten)
                   (map #(vector % (-> % meta (select-keys [:style/indent :style.cljfmt/indent :style.cljfmt/type]))))
                   (filter (rcomp second seq)))]
        (some-> *cache* (swap! assoc ::project-macro-mappings v))
        v)))

(speced/defn cljfmt-indents-for
  "Returns a value derived from `#'cljfmt.core/default-indents`, with:
  * the indentation specs defined in your project (and its dependencies) via metadata; and
  * the specs explicitly passed as an argument to this defn.

  The ns of `file` is analysed, for resolving `:def`-ed and `:refer`-ed symbols accurately."
  [file, ^::indent-mapping third-party-intent-specs]
  (let [macro-mappings (project-macro-mappings)
        ns-mappings (if (some-> file (string/ends-with? ".cljs"))
                      {}
                      (some-> file file/read-file-ns-decl parse/name-from-ns-decl safe-ns-map))
        result (atom cljfmt.core/default-indents)]
    (doseq [[var-ref metadata] macro-mappings
            :when (and var-ref metadata)]
      (swap! result
             (fn [v]
               (let [indent (to-cljfmt-indent metadata)
                     fqn (fully-qualified-name-of var-ref)]
                 (if-not (and indent fqn)
                   v
                   (assoc v fqn indent))))))
    (swap! result
           #(merge % (cljfmt-third-party-indent-specs third-party-intent-specs)))
    ;; brings https://github.com/weavejester/cljfmt/pull/163/files:
    (swap! result
           #(-> %
                (dissoc-by (fn [x] ;; regexes can't be compared, hence this contraption
                             (not= (pr-str x)
                                   (pr-str #"^def"))))
                (assoc #"^def(?!ault)(?!late)(?!er)" [[:inner 0]])))
    ;; :refer awareness:
    (doseq [[sym var-ref] ns-mappings
            :when (var? var-ref)
            :let [fqn (fully-qualified-name-of var-ref)]
            :when (some (fn [[k _]]
                          (= k fqn))
                        @result)
            :let [indent (get @result fqn)]]
      (swap! result assoc sym indent))
    (doseq [:when file
            :let [deps (-> file file/read-file-ns-decl parse/deps-from-ns-decl)]
            [triggering-ns indents] formatting-stack.indent-specs/magic-symbol-mappings
            :when (-> deps set (contains? triggering-ns))
            [sym raw-indent] indents
            :let [indent (to-cljfmt-indent raw-indent)]]
      (swap! result assoc sym indent))
    @result))
