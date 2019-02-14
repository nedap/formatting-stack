(ns formatting-stack.formatters.newlines
  (:require
   [com.gfredericks.all-my-files-should-end-with-exactly-one-newline-character :as impl]
   [formatting-stack.protocols.formatter]))

(defrecord Formatter [expected-newline-count]
  formatting-stack.protocols.formatter/Formatter
  (format! [this files]
    (let [expected-newline-count (or expected-newline-count 1)]
      (with-out-str ;; Supress "All newlines are good, nothing to fix."
        (impl/so-fix-them files :expected-newline-count expected-newline-count)))))
