(ns formatting-stack.linters.bikeshed
  (:require
   [bikeshed.core :as bikeshed]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [formatting-stack.protocols.linter :as linter]
   [nedap.utils.modular.api :refer [implement]]))

(defn lint! [{:keys [max-line-length]} filenames]
  (let [files (map io/file filenames)
        output (->> (with-out-str
                      (bikeshed/long-lines files :max-line-length max-line-length))
                    (str/split-lines)
                    (remove (fn [line]
                              (or (str/blank? line)
                                  (some (fn [re]
                                          (re-find re line))
                                        [#"^Checking for lines"
                                         #":refer \["
                                         #"^No lines found"])))))]
    (when (-> output count (> 1))
      (let [[head & tail] output
            replacement (str "Lines exceeding " max-line-length " columns")
            output (-> head
                       (str/replace #"^Badly formatted files" replacement)
                       (cons tail))]
        (->> output (str/join "\n") println)))))

(defn new [{:keys [max-line-length]
            :or {max-line-length 130}}]
  (implement {:max-line-length max-line-length}
    linter/--lint! lint!))
