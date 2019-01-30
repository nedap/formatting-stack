(ns formatting-stack.core
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [formatting-stack.impl :as impl]
   [formatting-stack.indent-specs :refer [default-third-party-indent-specs]]
   [formatting-stack.strategies :as strategies]))

(def default-how-to-ns-opts {:require-docstring?      false
                             :sort-clauses?           true
                             ;; should be false, but https://git.io/fhMLm can break code:
                             :allow-refer-all?        true
                             :allow-extra-clauses?    false
                             :align-clauses?          false
                             :import-square-brackets? false})

(defn format! [& {:keys [strategies
                         how-to-ns-opts
                         cljfmt-opts
                         third-party-indent-specs]}]
  (require 'cljfmt.main)
  (require 'com.gfredericks.how-to-ns.main)
  ;; the following `or` clauses ensure that Components don't pass nil values
  (let [strategies (or strategies [strategies/git-completely-staged])
        how-to-ns-opts (or how-to-ns-opts default-how-to-ns-opts)
        cljfmt-opts (or cljfmt-opts (resolve 'cljfmt.main/default-options))
        third-party-indent-specs (or third-party-indent-specs default-third-party-indent-specs)
        cljfmt (resolve 'cljfmt.main/fix)
        how-to-ns (resolve 'com.gfredericks.how-to-ns.main/fix)
        files (->> strategies
                   (mapcat (fn [f]
                             (f)))
                   distinct)
        cljfmt-files (map io/file files)
        how-to-ns-files (remove #(str/ends-with? % ".edn") files)]
    (impl/setup-cider-indents! third-party-indent-specs)
    (impl/setup-cljfmt-indents! third-party-indent-specs)
    (cljfmt cljfmt-files)
    (how-to-ns how-to-ns-files how-to-ns-opts)))
