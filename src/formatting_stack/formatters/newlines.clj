(ns formatting-stack.formatters.newlines
  (:require
   [com.gfredericks.all-my-files-should-end-with-exactly-one-newline-character :as impl]
   [formatting-stack.protocols.formatter]
   [formatting-stack.util :refer [process-in-parallel!]]))

(defrecord Formatter [expected-newline-count]
  formatting-stack.protocols.formatter/Formatter
  (format! [this files]
    (let [expected-newline-count (or expected-newline-count 1)]
      (with-out-str ;; Supress "All newlines are good, nothing to fix."
        (->> files
             (process-in-parallel! (fn [filename]
                                     (impl/so-fix-them [filename] :expected-newline-count expected-newline-count))))))))
