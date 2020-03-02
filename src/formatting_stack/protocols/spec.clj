(ns formatting-stack.protocols.spec
  (:require
   [clojure.spec.alpha :as spec]
   [nedap.speced.def :as speced]
   [nedap.utils.spec.api :refer [check!]]
   [nedap.utils.spec.predicates :refer [present-string?]]))

(speced/def-with-doc :formatting-stack.protocols.spec.member/id
  "Members (formatters, linters, etc) identify themselves,
so that final users can locate them and configure them."
  keyword?)

(spec/def ::reify (spec/and some?
                            (complement map?)))

(speced/def-with-doc ::member
  "A 'member' of the stack that does something useful: a formatter, linter or processor.

'Strategies' and 'Reporters' are not members - instead they help members accomplish their purpose."
  (fn [x]
    (if-not (map? x)
      ;; we are facing a `reify`, which means that formatting-stack is being customized
      ;; In those cases, an :id is practically useless (since the point of :id is overriding f-s), so no validation needed:
      true
      (check! (spec/keys :req-un [:formatting-stack.protocols.spec.member/id])
              x))))

(spec/def ::members (spec/and (spec/coll-of ::member)
                              (fn [xs]
                                (let [ids (->> xs
                                               (map (fn ^{::speced/spec (rcomp count #{0 1})} [x]
                                                      (when-not (spec/valid? ::reify x)
                                                        (let [ks (->> x
                                                                      keys ;; fails if (reify)
                                                                      (filter keyword?)
                                                                      (filter (fn [x]
                                                                                (-> x name #{"id"}))))]
                                                          (map (partial get x) ks)))))
                                               (apply concat))]
                                  (when (seq ids)
                                    (assert (apply distinct? ids)
                                            "Members should have unique ids"))
                                  true))))

(spec/def ::filename present-string?)

(spec/def ::filenames (spec/coll-of ::filename))

(spec/def ::msg present-string?)

(spec/def ::msg-extra-data (spec/coll-of present-string?))

(spec/def ::source qualified-keyword?)

(spec/def ::column nat-int?)

(spec/def ::line ::column)

(spec/def ::warning-details-url present-string?)

(spec/def ::level #{:warning :error :exception})

(defmulti reportmm :level)

(defmethod reportmm :exception [_]
  (spec/keys :req-un [::msg
                      ::exception
                      ::source
                      ::level]
             :opt-un [::filename]))

(defmethod reportmm :default [_]
  (spec/keys :req-un [::filename
                      ::source
                      ::msg
                      ::level
                      ::column
                      ::line]
             :opt-un [::msg-extra-data
                      ::warning-details-url]))

(spec/def ::report
  (spec/multi-spec reportmm :level))

(spec/def ::reports (spec/coll-of ::report))
