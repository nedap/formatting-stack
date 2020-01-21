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
(defn print-report [{:keys [max-msg-length]} reports]
  (->> (filter (fn [{:keys [level]}] (#{:error :warning} level)) reports)
       (group-by :filename)
       (into (sorted-map-by compare)) ;; sort filenames for consistent output
       (run! (fn [[title warnings]]
               (println (colorize title :cyan))
               (doseq [{:keys [msg column line linter level]} (sort-by :line warnings)]
                 (println (case level
                            :error   (colorize "ˣ" :red)
                            :warning (colorize "⚠" :yellow))
                          (colorize (format "%3d:%-3d" line column) :grey)
                          (format (str "%-" max-msg-length "." max-msg-length "s") msg)
                          (colorize (str "  " linter) :grey)))
               (println))))

  (->> (filter (fn [{:keys [level]}] (#{:exception} level)) reports)
       (group-by :level)
       (run! (fn [[_ exceptions]]
               (println (colorize (str (count exceptions) " exceptions occurred") :red))
               (doseq [{:keys [msg]} exceptions]
                 (println msg)
                 (println)))))

  (doseq [[level amount] (select-keys (->> (group-by :level reports)
                                           (map-vals count))
                                      #{:error, :warning})]
    (when-not (zero? amount)
      (println (colorize (str (count reports) " " (name level) "s found")
                         (case level
                           :error :red
                           :warning :yellow))))))

(defn new [{:keys [max-msg-length]
            :or {max-msg-length 120}}]
  (implement {:max-msg-length max-msg-length}
    reporter/--report print-report))
