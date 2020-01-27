(ns formatting-stack.reporters.pretty-printer
  (:require
   [nedap.utils.modular.api :refer [implement]]
   [medley.core :refer [map-vals]]
   [formatting-stack.protocols.reporter :as reporter]))

;; fixme move to util
(def ansi-colors
  {:reset  "[0m"
   :red    "[031m"
   :green  "[032m"
   :yellow "[033m"
   :cyan   "[036m"
   :grey   "[037m"})

(defn colorize [s color]
  (str \u001b (ansi-colors color) s \u001b (ansi-colors :reset)))

;; fixme split into separate parts
(defn print-report [{:keys [max-msg-length print-stacktraces?]} reports]
  (->> (filter (fn [{:keys [level]}] (#{:exception} level)) reports)
       (group-by :filename)
       (run! (fn [[title reports]]
               (println (colorize title :cyan))
               (doseq [{:keys [^Throwable exception]} reports]
                 (if print-stacktraces?
                  (clojure.stacktrace/print-stack-trace exception)
                  (println (ex-message exception)))
                 (println)))))

  (->> (filter (fn [{:keys [level]}] (#{:error :warning} level)) reports)
       (group-by :filename)
       (into (sorted-map-by compare)) ;; sort filenames for consistent output
       (run! (fn [[title reports]]
               (println (colorize title :cyan))
               (doseq [{:keys [msg column line linter level]} (sort-by :line reports)]
                 (println (case level
                            :error   (colorize "ˣ" :red)
                            :warning (colorize "⚠" :yellow))
                          (colorize (format "%3d:%-3d" line column) :grey)
                          (format (str "%-" max-msg-length "." max-msg-length "s") msg)
                          (colorize (str "  " linter) :grey)))
               (println))))

  ;; fixme dedupe
  (let [summary (->> (group-by :level reports)
                     (map-vals count))]
    (when-let [exceptions (:exception summary)]
      (println (colorize (str exceptions " exceptions occurred") :red)))
    (when-let [error (:error summary)]
      (println (colorize (str error " errors found") :red)))
    (when-let [warning (:warning summary)]
      (println (colorize (str warning " warnings found") :yellow)))))

(defn new [{:keys [max-msg-length print-stacktraces?]
            :or {max-msg-length 120
                 print-stacktraces? true}}]
  (implement {:max-msg-length max-msg-length
              :print-stacktraces? print-stacktraces?}
    reporter/--report print-report))
