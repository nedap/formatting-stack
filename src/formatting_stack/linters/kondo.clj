(ns formatting-stack.linters.kondo
  (:require
   [clj-kondo.core :as kondo]
   [clojure.set :as set]
   [formatting-stack.kondo-classpath-cache]
   [formatting-stack.protocols.linter :as protocols.linter]
   [medley.core :refer [deep-merge]]
   [nedap.utils.modular.api :refer [implement]]))

(def off {:level :off})

(def default-options
  {:cache     true
   :cache-dir formatting-stack.kondo-classpath-cache/cache-dir
   :linters   {:cond-else            off ;; undesired
               :missing-docstring    off ;; undesired
               :unused-binding       off ;; undesired
               :unresolved-symbol    off ;; can give false positives
               :unused-symbol        off ;; can give false positives
               :unused-private-var   off ;; can give false positives
               :consistent-alias     off ;; already offered by how-to-ns
               :duplicate-require    off ;; already offered by clean-ns
               :unused-import        off ;; already offered by clean-ns
               :unused-namespace     off ;; already offered by clean-ns
               :unused-referred-var  off ;; already offered by clean-ns
               :unresolved-namespace off ;; already offered by clean-ns
               }
   :lint-as   '{nedap.speced.def/def-with-doc clojure.core/defonce
                nedap.speced.def/defn         clojure.core/defn
                nedap.speced.def/defprotocol  clojure.core/defprotocol
                nedap.speced.def/doc          clojure.repl/doc
                nedap.speced.def/fn           clojure.core/fn
                nedap.speced.def/let          clojure.core/let
                nedap.speced.def/letfn        clojure.core/letfn
                nedap.utils.reverse/r->       clojure.core/->
                nedap.utils.reverse/rcomp     clojure.core/comp
                nedap.utils.reverse/rcond->   clojure.core/cond->}
   :output    {:exclude-files ["test-resources/*"
                               "test/unit/formatting_stack/formatters/cljfmt/impl/sample_data.clj"]}})

(def clj-options
  ;; .clj files are also linted by Eastwood, so we disable duplicate linters:
  {:linters {:misplaced-docstring off
             :invalid-arity       off
             :deprecated-var      off
             :inline-def          off
             :redefined-var       off}})

(defn lint! [{:keys [kondo-clj-options kondo-cljs-options]}
             filenames]

  @formatting-stack.kondo-classpath-cache/classpath-cache

  (let [kondo-clj-options (or kondo-clj-options {})
        kondo-cljs-options (or kondo-cljs-options {})
        {cljs-files true
         clj-files  false} (->> filenames
                                (group-by (fn [f]
                                            (-> (re-find #"\.cljs$" f)
                                                boolean))))]
    (->> [(kondo/run! {:lint   clj-files
                       :config (deep-merge default-options clj-options kondo-clj-options)
                       ;; :lang is unset here, so as not to particularly prefer .clj over .cljc
                       })
          (kondo/run! {:lint   cljs-files
                       :config (deep-merge default-options kondo-cljs-options)
                       :lang   :cljs})]
         (mapcat :findings)
         (map (fn [{source-type :type :as m}]
                (-> (set/rename-keys m {:row     :line
                                        :message :msg
                                        :col     :column})
                    (assoc :source (keyword "kondo" (name source-type)))))))))

(defn new [{:keys [kondo-clj-options
                   kondo-cljs-options]}]
  (implement {:kondo-clj-options  kondo-clj-options
              :kondo-cljs-options kondo-cljs-options}
    protocols.linter/--lint! lint!))
