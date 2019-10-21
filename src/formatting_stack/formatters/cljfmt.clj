(ns formatting-stack.formatters.cljfmt
  (:require
   [cljfmt.main]
   [clojure.java.io :as io]
   [formatting-stack.formatters.cljfmt.impl :as impl]
   [formatting-stack.protocols.formatter]
   [formatting-stack.protocols.linter]
   [formatting-stack.util :refer [process-in-parallel!]]
   [medley.core :refer [deep-merge]]))

(defrecord Formatter [options third-party-indent-specs]
  formatting-stack.protocols.linter/Linter
  (lint! [this files]
    (->> files
         (process-in-parallel! (fn [filename]
                                 (let [indents (impl/cljfmt-indents-for filename third-party-indent-specs)]
                                   (#'cljfmt.main/check-one {:indents indents} filename))))
         (remove (fn [{{:keys [incorrect error]} :counts}] (zero? (+ incorrect error))))
         (map :file)))

  formatting-stack.protocols.formatter/Formatter
  (format! [this files]
    (let [cljfmt-opts (deep-merge cljfmt.main/default-options
                                  (or options {}))
          cljfmt-files (map io/file files)]

      (->> files
           (process-in-parallel! (fn [filename]
                                   (let [indents (impl/cljfmt-indents-for filename third-party-indent-specs)]
                                     (cljfmt.main/fix [filename] {:indents indents}))))))))
