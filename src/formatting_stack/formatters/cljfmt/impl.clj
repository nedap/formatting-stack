(ns formatting-stack.formatters.cljfmt.impl
  (:require
   [cljfmt.core]
   [clojure.java.classpath :as classpath]
   [clojure.java.io :as io]
   [clojure.tools.namespace.file :as file]
   [clojure.tools.namespace.find :as find]
   [clojure.tools.namespace.parse :as parse]
   [formatting-stack.util :refer [dissoc-by rcomp]])
  (:import
   (java.io File)))

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

(defn setup-cljfmt-indents!
  "Updates `#'cljfmt.core/default-indents` with:
  * the indentation specs defined in your project (and its dependencies) via metadata
  * the specs explicitly passed as an argument to this defn."
  [third-party-intent-specs]
  (letfn [(find-files [dirs platform]
            (->> dirs
                 (map io/file)
                 (map #(.getCanonicalFile ^File %))
                 (filter #(.exists ^File %))
                 (mapcat #(find/find-sources-in-dir % platform))
                 (map #(.getCanonicalFile ^File %))))
          (project-namespaces []
            (->> (find-files (classpath/classpath-directories) find/clj)
                 (mapcat (rcomp file/read-file-ns-decl parse/deps-from-ns-decl))
                 (distinct)
                 (map find-ns)
                 (filter identity)))
          (namespace-macros [ns]
            (->> ns ns-publics vals (filter (fn [var-ref]
                                              (or (#{:macro :arglists} (meta var-ref))
                                                  (fn? @var-ref))))))]
    (let [mappings (->> (project-namespaces)
                        (map namespace-macros)
                        (filter seq)
                        (flatten)
                        (map #(vector % (-> % meta (select-keys [:style/indent :style.cljfmt/indent :style.cljfmt/type]))))
                        (filter (rcomp second seq)))]
      (doseq [[var-ref metadata] mappings]
        (alter-var-root #'cljfmt.core/default-indents
                        (fn [v]
                          (let [indent (to-cljfmt-indent metadata)
                                name (-> var-ref meta :name)
                                fqn (-> var-ref meta :ns (str "/" name) symbol)]
                            (if-not indent
                              v
                              (assoc v fqn indent))))))
      (alter-var-root #'cljfmt.core/default-indents
                      #(merge % (cljfmt-third-party-indent-specs third-party-intent-specs)))
      ;; brings https://github.com/weavejester/cljfmt/pull/163/files:
      (alter-var-root #'cljfmt.core/default-indents
                      #(-> %
                           (dissoc-by (fn [x] ;; regexes can't be compared, hence this contraption
                                        (not= (pr-str x)
                                              (pr-str #"^def"))))
                           (assoc #"^def(?!ault)(?!late)(?!er)" [[:inner 0]]))))))
