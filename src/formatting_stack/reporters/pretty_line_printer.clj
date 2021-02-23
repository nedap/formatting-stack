(ns formatting-stack.reporters.pretty-line-printer
  (:require
   [clojure.stacktrace :refer [print-stack-trace]]
   [formatting-stack.protocols.reporter :as reporter]
   [formatting-stack.protocols.spec :as protocols.spec]
   [formatting-stack.reporters.impl :refer [truncate-line-wise]]
   [formatting-stack.util :refer [colorize]]
   [medley.core :refer [map-vals]]
   [nedap.speced.def :as speced]
   [nedap.utils.modular.api :refer [implement]]))

(speced/defn print-summary [{:keys [^boolean? summary?
                                    ^boolean? colorize?]} reports]
  (when summary?
    (->> reports
         (group-by :level)
         (map-vals count)
         (into (sorted-map-by compare)) ;; print summary in order
         (run! (fn [[report-type n]]
                 (cond-> (str n (case report-type
                                  :exception " exceptions occurred"
                                  :error     " errors found"
                                  :warning   " warnings found"))
                   colorize? (colorize (case report-type
                                         :exception :red
                                         :error     :red
                                         :warning   :yellow))
                   true      println))))))

(speced/defn print-exceptions [{:keys [^boolean? print-stacktraces?]} reports]
  (->> reports
       (filter (speced/fn [{:keys [^::protocols.spec/level level]}]
                 (#{:exception} level)))
       (group-by :filename)
       (run! (fn [[title reports]]
               (println (colorize title :cyan))
               (doseq [{:keys [^Throwable exception]} reports]
                 (if print-stacktraces?
                   (print-stack-trace exception)
                   (println (ex-message exception)))
                 (println))))))

(speced/defn print-warnings [{:keys [max-msg-length
                                     ^boolean? colorize?]}
                             ^::protocols.spec/reports reports]
  (->> (filter (fn [{:keys [level]}] (#{:error :warning} level)) reports)
       (group-by :filename)
       (into (sorted-map-by compare))
       (run! (fn [[title reports]]
               (println (cond-> title colorize? (colorize :cyan)))
               (doseq [{:keys [msg msg-extra-data column line source level]} (sort-by :line reports)]
                 (println (case level
                            :error   (cond-> "❌" colorize? (colorize :red))
                            :warning (cond-> "⚠️" colorize? (colorize :yellow)))
                          (if (or column line)
                            (cond-> (format "%3s:%-3s" (or line "?") (or column "?")) colorize? (colorize :grey))
                            "       ")
                          (format (str "%-" max-msg-length "." max-msg-length "s") msg)
                          (cond-> (str "  " source) colorize? (colorize :grey)))
                 (doseq [entry msg-extra-data]
                   (println "          " (truncate-line-wise entry max-msg-length))))))))

(defn print-report
  [options reports]
  (print-exceptions options reports)
  (print-warnings options reports)
  (print-summary options reports))

(defn new [{:keys [max-msg-length print-stacktraces? summary? colorize?]
            :or   {max-msg-length     100
                   print-stacktraces? true
                   summary?           true
                   colorize?          true}}]
  (implement {:max-msg-length max-msg-length
              :print-stacktraces? print-stacktraces?
              :summary? summary?
              :colorize? colorize?}
    reporter/--report print-report))
