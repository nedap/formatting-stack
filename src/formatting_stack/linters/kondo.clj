(ns formatting-stack.linters.kondo
  (:require
   [clj-kondo.core :as kondo]
   [clojure.set :as set]
   [clojure.string :as string]
   [formatting-stack.kondo-classpath-cache]
   [formatting-stack.plugin :as plugin]
   [formatting-stack.util :refer [with-spinner silence]]
   [medley.core :refer [deep-merge]]
   [nedap.utils.modular.api :refer [implement]]))

(def default-config
  (let [off {:level :off}]
    {:cache    true
     :parallel true
     :linters  {:cond-else            off  ;; undesired
                :missing-docstring    off  ;; undesired
                :unused-binding       off  ;; undesired
                :private-call         off  ;; undesired
                :unresolved-symbol    off  ;; can give false positives
                :unused-symbol        off  ;; can give false positives
                :unused-private-var   off  ;; can give false positives
                :unresolved-var       off  ;; already offered by clj
                :consistent-alias     off  ;; already offered by how-to-ns
                :duplicate-require    off  ;; already offered by clean-ns
                :unused-import        off  ;; already offered by clean-ns
                :unused-namespace     off  ;; already offered by clean-ns
                :unused-referred-var  off  ;; already offered by clean-ns
                :unresolved-namespace off} ;; already offered by clean-ns

     :lint-as  '{nedap.speced.def/def-with-doc clojure.core/defonce
                 nedap.speced.def/defn         clojure.core/defn
                 nedap.speced.def/defprotocol  clojure.core/defprotocol
                 nedap.speced.def/doc          clojure.repl/doc
                 nedap.speced.def/fn           clojure.core/fn
                 nedap.speced.def/let          clojure.core/let
                 nedap.speced.def/letfn        clojure.core/letfn
                 nedap.utils.reverse/r->       clojure.core/->
                 nedap.utils.reverse/rcomp     clojure.core/comp
                 nedap.utils.reverse/rcond->   clojure.core/cond->}}))

(defmethod plugin/config :formatting-stack.linters/kondo [_ config]
  {::config              (get config ::config @default-config)
   ::classpath-analysis? (get-in config [:formatting-stack/cli-opts :classpath-analysis]
                                 (get config ::classpath-analysis?
                                      false))})

(defmethod plugin/cli-options :formatting-stack.linters/kondo [_]
  [[nil "--[no-]classpath-analysis" "Turns on/off classpath analysis for clj-kondo, defaults to false"]])

(defmethod plugin/process :formatting-stack.linters/kondo [_ {:formatting-stack/keys [files]
                                                              ::keys [config]}]
  {:formatting-stack/results
   (->> (kondo/run! {:lint   files
                     :config config})
        (:findings)
        (map (fn [{source-type :type :as m}]
               (-> (set/rename-keys m {:row     :line
                                       :message :msg
                                       :col     :column})
                   (assoc :source (keyword "kondo" (name source-type)))))))})

(defmethod plugin/pre-process :formatting-stack.linters/kondo [_ {::keys [classpath-analysis? config]}]
  (when classpath-analysis?
    (with-spinner "Populating clj-kondo classpath cache"
      (let [files (-> (System/getProperty "java.class.path")
                      (string/split #"\:"))]
        (silence ;; prevents 'was already linted, skipping' warnings
          (kondo/run! (assoc config
                             :dependencies true
                             :lint files)))))
    (println)))
