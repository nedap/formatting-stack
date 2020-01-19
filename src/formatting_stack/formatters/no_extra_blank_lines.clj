(ns formatting-stack.formatters.no-extra-blank-lines
  "Ensures that no three consecutive newlines happen in a given file.

  That can happen naturally, or because of other formatters' intricacies."
  (:require
   [clojure.string :as string]
   [formatting-stack.protocols.formatter :as formatter]
   [formatting-stack.util :refer [process-in-parallel!]]
   [nedap.utils.modular.api :refer [implement]]))

(defn without-extra-newlines [s]
  (-> s (string/replace #"(\n\n)(\n)+" "$1")))

(defn format! [this files]
  (->> files
       (process-in-parallel! (fn [filename]
                               (let [contents (-> filename slurp)
                                     formatted (without-extra-newlines contents)]
                                 (when-not (= contents formatted)
                                   (println "Removing extra blank lines:" filename)
                                   (spit filename formatted)))))))

(defn new []
  (implement {}
    formatter/--format! format!))
