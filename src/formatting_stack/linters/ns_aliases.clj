(ns formatting-stack.linters.ns-aliases
  "Observes these guidelines: https://stuartsierra.com/2015/05/10/clojure-namespace-aliases"
  (:require
   [clojure.java.io :as io]
   [clojure.string :as string]
   [clojure.tools.namespace.parse :as parse]
   [clojure.tools.reader.reader-types :refer [indexing-push-back-reader]]
   [formatting-stack.protocols.linter :as linter]
   [formatting-stack.util :refer [ensure-coll process-in-parallel!]]
   [nedap.utils.modular.api :refer [implement]])
  (:import
   (java.io PushbackReader)))

(defn clause= [a b]
  (->> [a b]
       (map (fn [s]
              (-> s
                  (string/replace "-clj" "")
                  (string/replace "clj-" "")
                  (string/replace "-cljs" "")
                  (string/replace "cljs-" "")
                  (string/replace "-clojure" "")
                  (string/replace "clojure-" ""))))
       (apply =)))

(defn name-and-alias [[ns-name :as require-clause]]
  [ns-name
   (->> require-clause
        (reduce (fn [{:keys [found-as? alias] :as result} member]
                  (cond
                    alias          result
                    found-as?      {:found-as? true, :alias member}
                    (= :as member) {:found-as? true, :alias nil}))
                {:found-as? false
                 :alias     nil})
        :alias)])

(defn derived? [alias _from ns-name]
  {:pre [(#{:from} _from)]}
  (let [[alias-fragments ns-fragments] (->> [alias ns-name]
                                            (map (fn [x]
                                                   (-> x
                                                       name
                                                       (string/split #"\.")
                                                       (reverse)))))
        ns-fragments-without-core (->> ns-fragments (remove #{"core" "alpha" "api" "kws"}))]
    (->> [ns-fragments ns-fragments-without-core]
         (distinct)
         (some #(->> (map clause= alias-fragments %)
                     (every? true?)))
         (boolean))))

(def default-acceptable-aliases-whitelist
  '{d        [datomic.api datomic.client.api]
    impl     [::anything]
    log      [::anything]
    s        [clojure.spec.alpha cljs.spec.alpha]
    spec     [clojure.spec.alpha cljs.spec.alpha]
    speced   [nedap.speced.def]
    str      [clojure.string]
    sut      [::anything]
    sut.impl [::anything]
    t        [clojure.test cljs.test]})

(defn acceptable-require-clause? [whitelist require-clause]
  (if-not (vector? require-clause)
    true
    (let [[ns-name alias] (name-and-alias require-clause)
          whitelisted-namespaces (get whitelist alias)]
      (if-not alias
        true
        (-> (or (some #{::anything ns-name} whitelisted-namespaces)
                (derived? alias :from ns-name))
            (boolean))))))

(defn read-ns-decl
  "Reads file with line/column metadata"
  [filename]
  (with-open [rdr (-> (io/reader filename) PushbackReader. indexing-push-back-reader)]
    (parse/read-ns-decl rdr)))

(defn lint! [{:keys [acceptable-aliases-whitelist]} filenames]
  (->> filenames
       (process-in-parallel! (fn [filename]
                               (->> filename
                                    read-ns-decl
                                    formatting-stack.util/require-from-ns-decl
                                    (rest)
                                    (remove (partial acceptable-require-clause?
                                                     acceptable-aliases-whitelist))
                                    (map (fn [bad-alias]
                                           {:filename            filename
                                            :line                (:line (meta bad-alias))
                                            :column              (:column (meta bad-alias))
                                            :level               :warning
                                            :warning-details-url "https://stuartsierra.com/2015/05/10/clojure-namespace-aliases"
                                            :msg                 (str bad-alias " is not a derived alias")
                                            :source              :formatting-stack/ns-aliases})))))
       (mapcat ensure-coll)))

(defn new [{:keys [acceptable-aliases-whitelist]
            :or   {acceptable-aliases-whitelist default-acceptable-aliases-whitelist}}]
  (implement {:acceptable-aliases-whitelist acceptable-aliases-whitelist}
    linter/--lint! lint!))
