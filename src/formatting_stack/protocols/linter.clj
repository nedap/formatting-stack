(ns formatting-stack.protocols.linter
  (:require
   [clojure.spec.alpha :as spec]
   [formatting-stack.protocols.spec :as protocols.spec]
   [nedap.speced.def :as speced]))

(speced/defprotocol Linter
  "A specific member of the \"stack\", that lints files.

Normally it's a wrapper around a linting library, with extra configuration, performance improvements, etc."

  (^::protocols.spec/reports lint! [^::protocols.spec/member this
                                    ^::protocols.spec/filenames filenames]
    "Lints `filenames` according to a linter of your choice: e.g. Eastwood, or Kibit, lein-dependency-check, etc."))

(spec/def ::linters (spec/coll-of (partial speced/satisfies? Linter)))

(speced/defprotocol LinterFactory
  ""
  (^::linters linters [this
                       ^vector? default-strategies]
    ""))

(spec/def ::linter-factory (partial speced/satisfies? LinterFactory))
