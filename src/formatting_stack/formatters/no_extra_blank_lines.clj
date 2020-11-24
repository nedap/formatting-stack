(ns formatting-stack.formatters.no-extra-blank-lines
  "Ensures that no three consecutive newlines happen in a given file.

  That can happen naturally, or because of other formatters' intricacies."
  (:require
   [clojure.spec.alpha :as spec]
   [clojure.string :as string]
   [formatting-stack.protocols.formatter :as formatter]
   [formatting-stack.protocols.linter :as linter]
   [formatting-stack.util :refer [ensure-sequential process-in-parallel! rcomp]]
   [nedap.speced.def :as speced]
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

(speced/defn ^{::speced/spec (spec/coll-of pos-int?)} report-extra-newlines [^string? s]
  (->> (string/split-lines s)
       (map-indexed (fn [idx line] {:lineNumber idx :line line}))
       (partition 2 1)
       (filter (fn [[{left :line} {right :line}]] (= "" left right)))
       (map (rcomp first :lineNumber inc))
       (reduce (fn [ret line-number]
                 (if (= (last ret)
                        (dec line-number))
                   ret ;; group consecutive warnings
                   (conj ret line-number)))
               [])))

(defn lint! [this files]
  (->> files
       (process-in-parallel! (fn [filename]
                               (->> (slurp filename)
                                    (report-extra-newlines)
                                    (mapv (fn [line-number]
                                            {:column   1
                                             :line     line-number
                                             :filename filename
                                             :msg      "File has two or more consecutive blank lines"
                                             :level    :warning
                                             :source   :formatting-stack/no-extra-blank-lines})))))
       (mapcat ensure-sequential)))

(defn new []
  (implement {:id ::id}
    linter/--lint! lint!
    formatter/--format! format!))
