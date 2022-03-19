(ns formatting-stack.linters.ns-aliases.impl
  (:require
   [clojure.set :as set]
   [clojure.spec.alpha :as spec]
   [formatting-stack.strategies :as strategies]
   [nedap.speced.def :as speced])
  (:import
   (java.io File)))

(spec/def ::alias (spec/or :simple-alias symbol?
                           :directive keyword?))

(spec/def ::aliases (spec/coll-of ::alias :kind vector?))

(spec/def ::project-aliases (spec/map-of symbol? ::aliases))

(speced/defn ^::project-aliases merge-aliases [^::project-aliases m1, ^::project-aliases m2]
  (merge-with (fn [x y]
                (vec (into #{} cat [x y])))
              m1
              m2))

(def namespace-aliases-for*
  (when (strategies/refactor-nrepl-3-4-1-available?)
    @(requiring-resolve 'refactor-nrepl.ns.libspecs/namespace-aliases-for)))

(defn namespace-aliases-for [files]
  (when namespace-aliases-for*
    (let [{:keys [clj cljs]} (namespace-aliases-for* files true)]
      (merge-aliases clj cljs))))

;; NOTE: this isn't necessarily a "strategy" (which would reside in the `strategies` ns),
;; since it's composed of other strategy calls.
;; This is more of a handy helper at the moment.
(defn stable-files
  "Files that already existed as of the default branch,
  and that haven't been touched in the current branch"
  []
  (let [with (set (strategies/all-files :files []))
        without (into #{} cat [(strategies/git-diff-against-default-branch :files [])
                               (strategies/git-completely-staged :files [])
                               (strategies/git-not-completely-staged :files [])])
        corpus (set/difference with without)]
    (->> corpus
         (mapv (speced/fn [^String s]
                 (File. s))))))

(def project-aliases
  (memoize
   (fn [{_cache-key :cache-key}] ;; there's a cache key for correct memoization

     ;; note that memoizing results is correct -
     ;; results don't have to be recomputed as the git status changes:
     ;; touching more files doesn't alter the fact that these aliases already were existing.
     (namespace-aliases-for (stable-files)))))
