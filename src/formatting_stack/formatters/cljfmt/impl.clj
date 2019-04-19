(ns formatting-stack.formatters.cljfmt.impl
  (:require
   [cljfmt.core]
   [clojure.java.classpath :as classpath]
   [clojure.java.io :as io]
   [clojure.string :as string]
   [clojure.tools.namespace.file :as file]
   [clojure.tools.namespace.find :as find]
   [clojure.tools.namespace.parse :as parse]
   [formatting-stack.util :refer [dissoc-by rcomp]])
  (:import
   (java.io File)))

(def ^:dynamic *cache* nil)

;; :block is for things like do, with*, ->
;; :inner is for things like def (and for workarounding multi-arity defns with some sort of "body" argument)
;; see https://git.io/fhMtH for examples
(defn to-cljfmt-indent [{cider-indent  :style/indent
                         cljfmt-indent :style.cljfmt/indent
                         cljfmt-type   :style.cljfmt/type}]
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
  (let [name (-> var-ref meta :name)]
    (-> var-ref meta :ns (str "/" name) symbol)))

(defn find-files [dirs platform]
  (->> dirs
       (map io/file)
       (map #(.getCanonicalFile ^File %))
       (filter #(.exists ^File %))
       (mapcat #(find/find-sources-in-dir % platform))
       (map #(.getCanonicalFile ^File %))))

(defn project-namespaces []
  (->> (find-files (classpath/classpath-directories) find/clj)
       (mapcat (fn [file]
                 (let [decl (-> file file/read-file-ns-decl)
                       n (-> decl parse/name-from-ns-decl)
                       deps (-> decl parse/deps-from-ns-decl)]
                   (conj deps n))))
       (distinct)
       (map find-ns)
       (filter identity)))

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

(defn cljfmt-indents-for
  "Returns a value derived from `#'cljfmt.core/default-indents`, with:
  * the indentation specs defined in your project (and its dependencies) via metadata; and
  * the specs explicitly passed as an argument to this defn.

  The ns of `file` is analysed, for resolving `:def`-ed and `:refer`-ed symbols accurately."
  [file third-party-intent-specs]
  (let [macro-mappings (project-macro-mappings)
        ns-mappings (if (some-> file (string/ends-with? ".cljs"))
                      {}
                      (some-> file file/read-file-ns-decl parse/name-from-ns-decl ns-map))
        result (atom cljfmt.core/default-indents)]
    (doseq [[var-ref metadata] macro-mappings]
      (swap! result
             (fn [v]
               (let [indent (to-cljfmt-indent metadata)
                     fqn (fully-qualified-name-of var-ref)]
                 (if-not indent
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
            :when (some (fn [[k v]]
                          (= k fqn))
                        @result)
            :let [indent (get @result fqn)]]
      (swap! result assoc sym indent))
    @result))
