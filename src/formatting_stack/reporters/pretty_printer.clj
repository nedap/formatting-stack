(ns formatting-stack.reporters.pretty-printer
  "Prints a colorized output of the reports"
  (:require
   [clojure.stacktrace :refer [print-stack-trace]]
   [formatting-stack.protocols.reporter :as reporter]
   [formatting-stack.util :refer [colorize]]
   [medley.core :refer [map-vals]]
   [nedap.utils.modular.api :refer [implement]]))

(defn print-summary [{:keys [summary?]} reports]
  (when summary?
    (->> (group-by :level reports)
         (map-vals count)
         (into (sorted-map-by compare)) ;; print summary in order
         (run! (fn [[type n]]
                 (-> (str n (case type
                              :exception " exceptions occurred"
                              :error " errors found"
                              :warning " warnings found"))
                     (colorize (case type
                                 :exception :red
                                 :error :red
                                 :warning :yellow))
                     (println)))))))

(defn print-exceptions [{:keys [print-stacktraces?]} reports]
  (->> (filter (fn [{:keys [level]}] (#{:exception} level)) reports)
       (group-by :filename)
       (run! (fn [[title reports]]
               (println (colorize title :cyan))
               (doseq [{:keys [^Throwable exception]} reports]
                 (if print-stacktraces?
                   (print-stack-trace exception)
                   (println (ex-message exception)))
                 (println))))))

(defn print-warnings [{:keys [max-msg-length]} reports]
  (->> (filter (fn [{:keys [level]}] (#{:error :warning} level)) reports)
       (group-by :filename)
       (into (sorted-map-by compare)) ;; sort filenames for consistent output
       (run! (fn [[title reports]]
               (println (colorize title :cyan))
               (doseq [{:keys [msg column line source level]} (sort-by :line reports)]
                 (println (case level
                            :error   (colorize "ˣ" :red)
                            :warning (colorize "⚠" :yellow))
                          (colorize (format "%3d:%-3d" line column) :grey)
                          (format (str "%-" max-msg-length "." max-msg-length "s") msg)
                          (colorize (str "  " source) :grey)))
               (println)))))

(defn print-report [this reports]
  (print-exceptions this reports)
  (print-warnings this reports)
  (print-summary this reports))

(defn new [{:keys [max-msg-length print-stacktraces? summary?]
            :or {max-msg-length 120
                 print-stacktraces? true
                 summary? true}}]
  (implement {:max-msg-length max-msg-length
              :print-stacktraces? print-stacktraces?}
    reporter/--report print-report))
