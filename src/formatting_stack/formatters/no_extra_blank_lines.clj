(ns formatting-stack.formatters.no-extra-blank-lines
  "Ensures that no three consecutive newlines happen in a given file.

  That can happen naturally, or because of other formatters' intricacies."
  (:require
   [clojure.string :as string]
   [formatting-stack.protocols.formatter :as formatter]
   [formatting-stack.protocols.linter :as linter]
   [formatting-stack.util :refer [ensure-sequential process-in-parallel!]]
   [nedap.utils.modular.api :refer [implement]]))

(def extra-newline-pattern
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
            (let [matcher        (re-matcher extra-newline-pattern content)
                  current-offset (fn [] (->> (.start matcher)
                                             (subs content 0) ;; remove content after cursor
                                             (re-seq #"\n")
                                             (count)  ;; count all newlines from 0 -> cursor
                                             (+ 2)))] ;; account for offset
              ((fn next-offset []
                 (when (.find matcher)
                   (cons (current-offset) (lazy-seq (next-offset))))))))]
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
