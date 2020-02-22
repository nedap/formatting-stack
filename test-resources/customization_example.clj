(ns customization-example
  (:require
   [formatting-stack.core]
   [formatting-stack.defaults]
   [formatting-stack.linters.kondo :as kondo]
   [formatting-stack.linters.line-length :as line-length]
   [formatting-stack.linters.ns-aliases :as ns-aliases]))

;; You an implement your own linters:
(def custom-linters
  [(reify formatting-stack.protocols.linter/Linter
     (--lint! [this filenames]
       [{:source   ::my-linter
         :level    :warning
         :column   40
         :line     6
         :msg      "Hello, I am a sample linter!"
         :filename "path.clj"}]))])

;; You can tweak the default linters' configuration:
(def tweaked-linters
  (->> formatting-stack.defaults/default-linters
       (keep (fn [{:keys [id] :as linter}]
               ;; all formatters and linters have an `:id`.
               (case id
                 ;; change :max-line-length from 130 to 80:
                 ::line-length/id (assoc linter :max-line-length 80)

                 ;; remove an undesired linter:
                 ::ns-aliases/id  nil

                 ;; overrride some kondo defaults. They will be deep-merged against formatting-stack's kondo config:
                 ::kondo/id       (assoc linter
                                         :kondo-clj-options  {:linters {:cond-else {:level :warning}}}
                                         ;; remember there are different options, for clj and cljs.
                                         :kondo-cljs-options {:linters {:duplicate-require {:level :warning}}})
                 linter)))
       (vec)))

(def all-linters
  (into custom-linters tweaked-linters))

(formatting-stack.core/format! :linters all-linters
                               :formatters [] ;; disable all formatters
                               :in-background? false)
