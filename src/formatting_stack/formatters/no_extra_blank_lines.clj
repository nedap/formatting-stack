(ns formatting-stack.formatters.no-extra-blank-lines
  "Ensures that no three consecutive newlines happen in a given file.

  That can happen naturally, or because of other formatters' intricacies."
  (:require
   [clojure.string :as string]
   [formatting-stack.protocols.formatter :as formatter]
   [formatting-stack.protocols.linter :as linter]
   [formatting-stack.util :refer [ensure-sequential process-in-parallel!]]
   [nedap.utils.modular.api :refer [implement]]))

(def ^:const extra-newline-pattern
  #"(\n\n)(\n)+")

(defn without-extra-newlines [s]
  (-> s (string/replace extra-newline-pattern "$1")))

(defn format! [this files]
  (->> files
       (process-in-parallel! (fn [filename]
                               (let [contents (-> filename slurp)
                                     formatted (without-extra-newlines contents)]
                                 (when-not (= contents formatted)
                                   (println "Removing extra blank lines:" filename)
                                   (spit filename formatted))))))
  nil)

(defn lint! [this files]
  (letfn [(extra-line-seq [content]
            (let [m (re-matcher extra-newline-pattern content)]
              ((fn step []
                 (when (. m (find)) ;; fixme might need some cleanup
                   (cons (+ 2 (count (re-seq #"\n" (subs content 0 (.start m))))) (lazy-seq (step))))))))]
    (->> files
         (process-in-parallel! (fn [filename]
                                 (->> (extra-line-seq (slurp filename))
                                      (mapv (fn [line]
                                              {:filename filename
                                               :msg      "File has extra blank lines"
                                               :column   1
                                               :line     line
                                               :level    :warning
                                               :source   :formatting-stack/no-extra-blank-lines})))))
         (mapcat ensure-sequential))))

(defn new []
  (implement {:id ::id}
    linter/--lint! lint!
    formatter/--format! format!))
