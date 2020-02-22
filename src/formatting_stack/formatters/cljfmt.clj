(ns formatting-stack.formatters.cljfmt
  (:require
   [cljfmt.main]
   [formatting-stack.formatters.cljfmt.impl :as impl]
   [formatting-stack.protocols.formatter :as formatter]
   [formatting-stack.util :refer [process-in-parallel!]]
   [nedap.speced.def :as speced]
   [nedap.utils.modular.api :refer [implement]]))

(defn format! [{:keys [third-party-indent-specs]} files]
  (->> files
       (process-in-parallel! (fn [filename]
                               (let [indents (impl/cljfmt-indents-for filename third-party-indent-specs)]
                                 (cljfmt.main/fix [filename] {:indents indents})))))
  nil)

(speced/defn new [{:keys [third-party-indent-specs] :as options}]
  (implement (assoc options :id ::id)
    formatter/--format! format!))
