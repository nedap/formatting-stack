(ns formatting-stack.reporters.blacklist
  "Filters output before passing to provided reporter." ;; FIXME
  (:require
   [formatting-stack.protocols.reporter :as protocols.reporter]
   [formatting-stack.reporters.pretty-printer :as pretty-printer]
   [nedap.utils.modular.api :refer [implement]]
   [nedap.speced.def :as speced]
   [formatting-stack.protocols.spec :as protocols.spec]
   [clojure.spec.alpha :as spec]))

(spec/def ::ignored-report
  (spec/and
   (spec/keys :opt-un [::protocols.spec/source
                       ::protocols.spec/filename
                       ::protocols.spec/level
                       ::protocols.spec/column
                       ::protocols.spec/line])
   #_(fn [{::protocols.spec/keys [source filename]}]
       (or source filename))))

(spec/def ::ignored-reports (spec/coll-of ::ignored-report))

(speced/defn ignored-reports->predicate [^::ignored-reports ignored-reports]
  (fn [^::protocols.spec/report report]
    (->> ignored-reports
         (some (fn [ignored-report]
                (= ignored-report
                   (->> (keys ignored-report)
                        (select-keys report)))))
         (boolean))))

(defn filtering-report [{::keys [reporter blacklist]} reports]
  (->> (remove blacklist reports)
       (protocols.reporter/report reporter)))

(speced/defn new [{:keys [^some? reporter
                          ^::ignored-reports ignored-reports] ;; fixme explain
                   :or   {reporter        (pretty-printer/new {})
                          ignored-reports []}}]
  (implement {::printer  reporter
              ::blacklist (ignored-reports->predicate ignored-reports)}
    protocols.reporter/--report filtering-report))

