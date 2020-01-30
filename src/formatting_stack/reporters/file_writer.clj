(ns formatting-stack.reporters.file-writer
  "writes the output to a file which can be tailed with `watch --color -n 1 cat .fs-output.txt`"
  (:require
   [formatting-stack.protocols.reporter :as reporter]
   [formatting-stack.reporters.pretty-printer :as pretty-printer]
   [nedap.utils.modular.api :refer [implement]]))

(defn write-report [{:keys [printer filename]} reports]
  (->> (with-out-str (reporter/report printer reports))
       (spit filename)))

(defn new [{:keys [printer filename]
            :or {printer (pretty-printer/new {})
                 filename ".fs-output.txt"}}]
  (implement {:printer printer
              :filename filename}
    reporter/--report write-report))
