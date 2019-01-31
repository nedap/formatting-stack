(ns formatting-stack.formatters.cljfmt
  (:require
   [clojure.java.io :as io]
   [formatting-stack.formatters.cljfmt.impl :as impl]
   [formatting-stack.protocols.formatter]
   [medley.core :refer [deep-merge]]))

(defrecord Formatter [options third-party-indent-specs]
  formatting-stack.protocols.formatter/Formatter
  (format! [this files]
    (require 'cljfmt.main)
    (let [cljfmt @(resolve 'cljfmt.main/fix)
          cljfmt-opts (deep-merge @(resolve 'cljfmt.main/default-options)
                                  (or options {}))
          cljfmt-files (map io/file files)]
      (impl/setup-cljfmt-indents! third-party-indent-specs)
      (cljfmt cljfmt-files))))
