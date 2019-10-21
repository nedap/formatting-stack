(ns formatting-stack.formatters.clean-ns
  (:require
   [formatting-stack.formatters.clean-ns.impl :as impl]
   [formatting-stack.formatters.how-to-ns]
   [formatting-stack.protocols.formatter]
   [formatting-stack.protocols.linter]
   [formatting-stack.util :refer [process-in-parallel! try-require]]
   [formatting-stack.util.ns :refer [replace-ns-form!]]
   [medley.core :refer [deep-merge]]
   [nedap.speced.def :as speced]
   [refactor-nrepl.config])
  (:import
   (java.io File)))

(defn make-cleaner [how-to-ns-opts refactor-nrepl-opts namespaces-that-should-never-cleaned libspec-whitelist filename]
  (speced/fn ^{::speced/spec (complement #{"nil"})} [original-ns-form]
    (some-> (impl/clean-ns-form {:how-to-ns-opts                       how-to-ns-opts
                                 :refactor-nrepl-opts                  refactor-nrepl-opts
                                 :filename                             filename
                                 :original-ns-form                     original-ns-form
                                 :namespaces-that-should-never-cleaned namespaces-that-should-never-cleaned
                                 :libspec-whitelist                    libspec-whitelist})
            pr-str)))

(defn clean! [how-to-ns-opts refactor-nrepl-opts namespaces-that-should-never-cleaned libspec-whitelist filename]
  (replace-ns-form! filename
                    (make-cleaner how-to-ns-opts refactor-nrepl-opts namespaces-that-should-never-cleaned libspec-whitelist filename)
                    "Cleaning unused imports:"
                    how-to-ns-opts))

(def default-libspecs
  ["specs" "imports" "exports" "extensions" "side-effects" "init" "initialization" "load" "migration" "migrations"])

(defn make-default-libspec-whitelist [& {:keys [libspecs]
                                         :or   {libspecs default-libspecs}}]
  (letfn [(variations [x]
            #{(str "^" x ".")
              (str "." x ".")
              (str "." x "$")})]
    (->> libspecs
         (mapcat variations)
         (into #{"^cljsjs." "clojure.main"}))))

(def default-libspec-whitelist
  (make-default-libspec-whitelist))

(def default-nrepl-opts
  (-> refactor-nrepl.config/*config*
      (assoc :prefix-rewriting false)))

(def default-namespaces-that-should-never-cleaned
  #{'user 'dev})

(defrecord Formatter [how-to-ns-opts refactor-nrepl-opts namespaces-that-should-never-cleaned libspec-whitelist]
  formatting-stack.protocols.linter/Linter
  (lint! [this files]
    (let [changed-files (atom [])]
      (with-redefs [spit (fn [f & _]
                           (swap! changed-files conj (if (string? f) f (.getPath ^File f))))]
        (with-out-str (formatting-stack.protocols.formatter/format! this files))
        @changed-files)))

  formatting-stack.protocols.formatter/Formatter
  (format! [this files]
    (let [refactor-nrepl-opts (deep-merge refactor-nrepl.config/*config*
                                          (or refactor-nrepl-opts default-nrepl-opts))
          how-to-ns-opts (deep-merge formatting-stack.formatters.how-to-ns/default-how-to-ns-opts
                                     (or how-to-ns-opts {}))
          namespaces-that-should-never-cleaned (or namespaces-that-should-never-cleaned
                                                   default-namespaces-that-should-never-cleaned)
          libspec-whitelist (or libspec-whitelist default-libspec-whitelist)]
      (->> files
           (process-in-parallel! (fn [filename]
                                   (when (and (try-require filename)
                                              (not (impl/has-duplicate-requires? filename)))
                                     (clean! how-to-ns-opts
                                             refactor-nrepl-opts
                                             namespaces-that-should-never-cleaned
                                             libspec-whitelist
                                             filename))))))))
