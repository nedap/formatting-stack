(ns functional.formatting-stack.formatters.clean-ns.should-not-be-partially-cleaned
   (:require
    ;; ideally [as-file] would get removed, but with the current approach that's impossible.
    [clojure.java.io :as io :refer [as-file]]))

^{:foo ^{:foo io/copy} []} []
