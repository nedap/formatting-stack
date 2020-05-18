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

(spec/def ::filename present-string?)

(spec/def ::filenames (spec/coll-of ::filename))

(spec/def ::msg present-string?)

(spec/def ::msg-extra-data (spec/coll-of present-string?))

(spec/def ::source qualified-keyword?)

(spec/def ::column nat-int?)

(spec/def ::line nat-int?)

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
                      ::level]
             :opt-un [::column
                      ::line
                      ::msg-extra-data
                      ::warning-details-url]))

(spec/def ::report
  (spec/multi-spec reportmm :level))

(spec/def ::reports (spec/coll-of ::report))
