(ns customization-example
  "Documents how users can customize formatting-stack, as a runnable example.

  Not a part of the test suite.

  See also:

    * https://github.com/nedap/formatting-stack/blob/master/README.md
    * https://github.com/nedap/formatting-stack/wiki/FAQ"
  (:require
   [formatting-stack.core]
   [formatting-stack.linters.kondo :as kondo]
   [formatting-stack.linters.line-length :as line-length]
   [formatting-stack.linters.ns-aliases :as ns-aliases]))

;; You an implement your own linters:
(def custom-linter
  (reify formatting-stack.protocols.linter/Linter
    (--lint! [this filenames]
      [{:source   ::my-linter
        :level    :warning
        :column   40
        :line     6
        :msg      "Hello, I am a sample linter!"
        :filename "path.clj"}])))

;; You can tweak the default linters' configuration:
(def linter-overrides
  {;; add a linter:
   ::id             custom-linter

   ;; remove an undesired linter:
   ::ns-aliases/id  nil

   ;; change :max-line-length from 130 to 80:
   ::line-length/id {:max-line-length 80}

   ;; override some kondo defaults. They will be deep-merged against formatting-stack's kondo config:
   ::kondo/id       {:kondo-clj-options  {:linters {:cond-else {:level :warning}}}
                     ;; remember there are different options, for clj and cljs.
                     :kondo-cljs-options {:linters {:duplicate-require {:level :warning}}}}})

;; XXX a key customizable thing should be the strategies.
(comment
  (formatting-stack.core/format! :formatters [] ;; disable all formatters (as an example of how to do that)

                                 :in-background? false

                                 ;; `:overrides` will be deep-merged against formatting-stack's defauts.
                                 ;; a `nil` value at any depth has the special meaning "remove this entry":
                                 :overrides {:linters    linter-overrides
                                             ;; tentative API:
                                             :strategies {:formatters []
                                                          :linters    []}}))
