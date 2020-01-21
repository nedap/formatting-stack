(ns formatting-stack.linters.loc-per-ns
  (:require
   [clojure.string :as string]
   [formatting-stack.protocols.linter :as linter]
   [formatting-stack.util :refer [process-in-parallel!]]
   [nedap.utils.modular.api :refer [implement]]))

(defn overly-long-ns? [filename threshold]
  (-> filename
      slurp
      (string/split #"\n")
      (count)
      (> threshold)))

(defn lint! [{:keys [max-lines-per-ns]} filenames]
  (->> filenames
       (process-in-parallel! (fn [filename]
                               (when (overly-long-ns? filename max-lines-per-ns)
                                 {:filename filename
                                  :linter :formatting-stack/loc-per-ns
                                  :level :warning
                                  :msg (str "Longer than " max-lines-per-ns " LOC. consider refactoring")
                                  :line (+ 1 max-lines-per-ns) ;; first line after limit is the issue
                                  :column 1})))
       (remove nil?)))

(defn new [{:keys [max-lines-per-ns]
            :or {max-lines-per-ns 350}}]
  (implement {:max-lines-per-ns max-lines-per-ns}
    linter/--lint! lint!))
