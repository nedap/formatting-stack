(ns formatting-stack.reporters.pretty-printer
  "Prints an optionally colorized, indented, possibly truncated output of the reports."
  (:require
   [clojure.stacktrace :refer [print-stack-trace]]
   [formatting-stack.protocols.reporter :as reporter]
   [formatting-stack.protocols.spec :as protocols.spec]
   [formatting-stack.reporters.impl :refer [truncate-line-wise]]
   [formatting-stack.util :refer [colorize colorize-diff]]
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
                                     ^boolean print-diff?
                                     ^boolean? colorize?]}
                             ^::protocols.spec/reports reports]
  (->> reports
       (filter (speced/fn [{:keys [^::protocols.spec/level level]}]
                 (#{:error :warning} level)))
       (group-by :filename)
       (into (sorted-map-by compare)) ;; sort filenames for consistent output
       (run! (fn [[title reports]]
               (println (cond-> title
                          colorize? (colorize :cyan)))
               (doseq [[source-group group-entries] (->> reports
                                                         (group-by :source))
                       :let [_ (println " " (cond-> source-group
                                              colorize? (colorize (case (-> group-entries first :level)
                                                                    :error   :red
                                                                    :warning :yellow))))
                             _ (when-let [url (->> group-entries
                                                   (keep :warning-details-url)
                                                   first)]
                                 (cond-> (str "    See: " url)
                                   colorize? (colorize :grey)
                                   true      println))]
                       {:keys [msg column line msg-extra-data msg-extra-data warning-details-url diff]
                        :or {column "?", line "?"}} (->> group-entries
                                                         (sort-by :line))]

                 (println (cond-> (str "    " line ":" column)
                            colorize? (colorize :grey))
                          (truncate-line-wise msg max-msg-length))
                 (when (and diff print-diff?)
                   (println (cond-> diff
                              colorize? colorize-diff)))
                 (doseq [entry msg-extra-data]
                   (println "       "
                            (truncate-line-wise entry max-msg-length))))
               (println)))))

(defn print-report [this reports]
  (print-exceptions this reports)
  (print-warnings this reports)
  (print-summary this reports))

(defn new [{:keys [max-msg-length print-diff? print-stacktraces? summary? colorize?]
            :or   {max-msg-length     200
                   print-diff?        false
                   print-stacktraces? true
                   summary?           true
                   colorize?          true}}]
  (implement {:max-msg-length max-msg-length
              :print-diff? print-diff?
              :print-stacktraces? print-stacktraces?
              :summary? summary?
              :colorize? colorize?}
    reporter/--report print-report))
