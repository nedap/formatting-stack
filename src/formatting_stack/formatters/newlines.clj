(ns formatting-stack.formatters.newlines
  (:require
   [com.gfredericks.all-my-files-should-end-with-exactly-one-newline-character :as impl]
   [formatting-stack.protocols.formatter :as formatter]
   [formatting-stack.util :refer [process-in-parallel!]]
   [nedap.speced.def :as speced]
   [nedap.utils.modular.api :refer [implement]]))

(defn format! [{:keys [expected-newline-count]} files]
  (with-out-str ;; Supress "All newlines are good, nothing to fix."
    (->> files
         (process-in-parallel! (fn [filename]
                                 (impl/so-fix-them [filename] :expected-newline-count expected-newline-count))))))

(speced/defn new [{:keys [^pos-int? expected-newline-count]
                   :or {expected-newline-count 1}}]
  (implement {:expected-newline-count expected-newline-count}
    formatter/--format! format!))
