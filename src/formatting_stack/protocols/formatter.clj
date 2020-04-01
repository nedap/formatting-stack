(ns formatting-stack.protocols.formatter
  (:require
   [clojure.spec.alpha :as spec]
   [formatting-stack.protocols.spec :as protocols.spec]
   [nedap.speced.def :as speced]))

(speced/defprotocol Formatter
  "A specific member of the \"stack\", that format files.

Normally it's a wrapper around a formatting library, with extra configuration, performance improvements, etc."

  (^nil? format! [^::protocols.spec/member this
                  ^::protocols.spec/filenames filenames]
    "Formats `filenames` according to a formatter of your choice."))

(spec/def ::formatters (spec/coll-of (partial speced/satisfies? Formatter)))

(speced/defprotocol FormatterFactory
  ""
  (^::formatters formatters [this
                             ^vector? default-strategies
                             ^map? third-party-indent-specs]
    ""))

(spec/def ::formatter-factory (partial speced/satisfies? FormatterFactory))
