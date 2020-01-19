(ns formatting-stack.reporter)

(def ansi-colors
  {:reset "[0m"
   :red   "[031m"
   :green "[032m"
   :cyan  "[036m"
   :grey  "[037m"})

(defn colorize [s color]
  (str \u001b (ansi-colors color) s \u001b (ansi-colors :reset)))

(defn print-report [report]
  (->> (group-by :filename report)
       (into (sorted-map-by compare)) ;; sort filenames for consistent output
       (run! (fn [[title warnings]]
               (println (colorize title :cyan))
               (doseq [{:keys [msg column line linter] :as warn} (sort-by :line warnings)]
                 (println (colorize (format "%3d:%-3d" line column) :grey)
                          (format "%-120.120s" msg) ;; fixme configure 120
                          (colorize (str "  " linter) :grey)))
               (println))))

  (when (seq report)
    (println (colorize (str (count report) " errors found") :red))))
