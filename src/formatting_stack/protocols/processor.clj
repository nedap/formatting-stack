(ns formatting-stack.protocols.processor
  (:require
   [clojure.spec.alpha :as spec]
   [formatting-stack.protocols.spec :as protocols.spec]
   [nedap.speced.def :as speced]))

(speced/defprotocol Processor
  "Any file-processing component that isn't a formatter or a linter."

  (^nil? process! [^::protocols.spec/member this
                   ^::protocols.spec/filenames filenames]
    "Performs a compilation according to a processor of your choice: e.g. the ClojureScript processor, or Garden, Stefon, etc.
You are free to ignore `filenames`, compiling the whole project instead."))

(spec/def ::processors (spec/coll-of (partial speced/satisfies? Processor)))

(speced/defprotocol ProcessorFactory
  ""
  (^::processors processors [this
                             ^vector? processors-strategies
                             ^map? third-party-indent-specs]
    ""))

(spec/def ::processor-factory (partial speced/satisfies? ProcessorFactory))
