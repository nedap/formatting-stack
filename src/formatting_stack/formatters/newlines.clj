(ns formatting-stack.formatters.newlines
  (:require
   [clojure.string :as str]
   [com.gfredericks.all-my-files-should-end-with-exactly-one-newline-character :as impl]
   [formatting-stack.protocols.formatter :as formatter]
   [formatting-stack.protocols.linter :as linter]
   [formatting-stack.util :refer [process-in-parallel! silence]]
   [nedap.speced.def :as speced]
   [nedap.utils.modular.api :refer [implement]]))

(defn format! [{:keys [expected-newline-count]} files]
  (->> files
       (process-in-parallel! (fn [filename]
                               (silence ;; Supress "All newlines are good, nothing to fix."
                                 (impl/so-fix-them [filename] :expected-newline-count expected-newline-count)))))
  nil)

(defn lint! [{:keys [expected-newline-count]} files]
  (->> files
       (process-in-parallel! (fn [filename]
                               (when-not (zero?
                                          (silence (impl/but-do-they? [filename] :expected-newline-count expected-newline-count)))
                                 {:filename filename
                                  :source   :formatting-stack/newlines
                                  :level    :warning
                                  :line     (-> filename slurp str/split-lines count)
                                  :msg      (str "File should end in " expected-newline-count " newlines")
                                  :column   1})))
       (filterv some?)))

(speced/defn new [{:keys [^{::speced/spec #{0 1}} expected-newline-count]
                   :or {expected-newline-count 1}}]
  (implement {:id ::id
              :expected-newline-count expected-newline-count}
    linter/--lint! lint!
    formatter/--format! format!))
