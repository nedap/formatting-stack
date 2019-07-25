(ns formatting-stack.linters.ns-aliases
  "Observes these guidelines: https://stuartsierra.com/2015/05/10/clojure-namespace-aliases"
  (:require
   [clojure.string :as string]
   [clojure.tools.namespace.file :as file]
   [formatting-stack.protocols.linter]
   [formatting-stack.util :refer [process-in-parallel!]]))

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
  '{d        [datomic.api]
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

(defrecord Linter [acceptable-aliases-whitelist]
  formatting-stack.protocols.linter/Linter
  (lint! [this filenames]
    (let [acceptable-aliases-whitelist (or acceptable-aliases-whitelist
                                           default-acceptable-aliases-whitelist)]
      (->> filenames
           (process-in-parallel! (fn [filename]
                                   (let [bad-require-clauses (->> filename
                                                                  file/read-file-ns-decl
                                                                  formatting-stack.util/require-from-ns-decl
                                                                  (rest)
                                                                  (remove (partial acceptable-require-clause?
                                                                                   acceptable-aliases-whitelist)))]
                                     (when (seq bad-require-clauses)
                                       (let [formatted-bad-requires (->> bad-require-clauses
                                                                         (map (fn [x]
                                                                                (str "    " x)))
                                                                         (string/join "\n"))]
                                         (-> (str "Warning for "
                                                  filename
                                                  ": the following :require aliases are not derived from their refered namespace:"
                                                  "\n"
                                                  formatted-bad-requires
                                                  ". See https://stuartsierra.com/2015/05/10/clojure-namespace-aliases\n")
                                             (println)))))))))))
