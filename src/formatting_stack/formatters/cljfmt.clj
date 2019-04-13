(ns formatting-stack.formatters.cljfmt
  (:require
   [cljfmt.core]
   [cljfmt.main]
   [clojure.java.io :as io]
   [formatting-stack.formatters.cljfmt.impl :as impl]
   [formatting-stack.protocols.formatter]
   [formatting-stack.util :refer [process-in-parallel!]]
   [medley.core :refer [deep-merge]]))

(defrecord Formatter [options third-party-indent-specs]
  formatting-stack.protocols.formatter/Formatter
  (format! [this files]
    (let [cljfmt-opts (deep-merge cljfmt.main/default-options
                                  (or options {}))
          cljfmt-files (map io/file files)]

      (->> files
           (process-in-parallel! (fn [filename]
                                   (with-redefs [cljfmt.core/default-indents (impl/cljfmt-indents-for filename
                                                                                                      third-party-indent-specs)]
                                     (cljfmt.main/fix [filename]))))))))
