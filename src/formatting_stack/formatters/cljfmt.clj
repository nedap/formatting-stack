(ns formatting-stack.formatters.cljfmt
  (:require
   [cljfmt.main]
   [clojure.java.io :as io]
   [formatting-stack.formatters.cljfmt.impl :as impl]
   [formatting-stack.protocols.formatter]
   [medley.core :refer [deep-merge]]))

(defrecord Formatter [options third-party-indent-specs]
  formatting-stack.protocols.formatter/Formatter
  (format! [this files]
    (let [cljfmt-opts (deep-merge cljfmt.main/default-options
                                  (or options {}))
          cljfmt-files (map io/file files)]
      (impl/setup-cljfmt-indents! third-party-indent-specs)
      (cljfmt.main/fix cljfmt-files))))
