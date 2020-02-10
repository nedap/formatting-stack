(ns formatting-stack.linters.loc-per-ns
  (:require
   [clojure.string :as string]
   [formatting-stack.protocols.linter :as linter]
   [formatting-stack.util :refer [process-in-parallel!]]
   [nedap.utils.modular.api :refer [implement]]))

(defn count-lines [filename]
  (-> filename
      slurp
      (string/split #"\n")
      (count)))

(defn lint! [{:keys [max-lines-per-ns]} filenames]
  (->> filenames
       (process-in-parallel! (fn [filename]
                               (let [lines (count-lines filename)]
                                 (when (> lines max-lines-per-ns)
                                   {:filename filename
                                    :source   :formatting-stack/loc-per-ns
                                    :level    :warning
                                    :msg      (str "Longer than " max-lines-per-ns " LOC. Consider refactoring.")
                                    :line     lines
                                    :column   0}))))
       (remove nil?)))

(defn new [{:keys [max-lines-per-ns]
            :or   {max-lines-per-ns 350}}]
  (implement {:max-lines-per-ns max-lines-per-ns}
    linter/--lint! lint!))
