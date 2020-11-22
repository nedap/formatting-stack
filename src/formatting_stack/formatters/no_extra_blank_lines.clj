(ns formatting-stack.formatters.no-extra-blank-lines
  "Ensures that no three consecutive newlines happen in a given file.

  That can happen naturally, or because of other formatters' intricacies."
  (:require
   [clojure.string :as string]
   [formatting-stack.protocols.formatter :as formatter]
   [formatting-stack.protocols.linter :as linter]
   [formatting-stack.util :refer [ensure-sequential process-in-parallel! rcomp]]
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
                                   (spit filename formatted))))))
  nil)

(defn lint! [this files]
  (->> files
       (process-in-parallel! (fn [filename]
                               (->> (slurp filename)
                                    (string/split-lines)
                                    (map-indexed (fn [idx line] {:lineNumber idx :line line}))
                                    (partition 2 1)
                                    (filter (fn [[{left :line} {right :line}]] (= "" left right)))
                                    (map (rcomp first :lineNumber inc))
                                    (reduce (fn [ret line-number]
                                              (if (= (:line (last ret))
                                                     (dec line-number))
                                                ret ;; group consecutive warnings
                                                (conj ret {:filename filename
                                                           :msg      "File has two or more consecutive blank lines"
                                                           :column   1
                                                           :line     line-number
                                                           :level    :warning
                                                           :source   :formatting-stack/no-extra-blank-lines})))
                                            []))))
       (mapcat ensure-sequential)))

(defn new []
  (implement {:id ::id}
    linter/--lint! lint!
    formatter/--format! format!))
