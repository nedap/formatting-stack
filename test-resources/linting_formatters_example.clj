(ns linting-formatters-example
  "Documents how formatters can be used as linters for a non-mutative experience.

  Not a part of the test suite.

  See also:

    * https://github.com/nedap/formatting-stack/blob/master/README.md
    * https://github.com/nedap/formatting-stack/wiki/FAQ"
  (:require
   [formatting-stack.core]
   [formatting-stack.defaults :refer [default-linters]]
   [formatting-stack.formatters.clean-ns :as formatters.clean-ns]
   [formatting-stack.formatters.cljfmt :as formatters.cljfmt]
   [formatting-stack.formatters.how-to-ns :as formatters.how-to-ns]
   [formatting-stack.formatters.newlines :as formatters.newlines]
   [formatting-stack.formatters.no-extra-blank-lines :as formatters.no-extra-blank-lines]
   [formatting-stack.formatters.trivial-ns-duplicates :as formatters.trivial-ns-duplicates]
   [formatting-stack.indent-specs :refer [default-third-party-indent-specs]]
   [formatting-stack.strategies :as strategies]))

(def formatters
  (->> [(-> (formatters.cljfmt/new {:third-party-indent-specs default-third-party-indent-specs})
            (assoc :strategies [strategies/all-files]))
        (-> (formatters.how-to-ns/new {})
            (assoc :strategies [strategies/all-files]))
        (formatters.no-extra-blank-lines/new)
        (formatters.newlines/new {})
        (-> (formatters.trivial-ns-duplicates/new {})
            (assoc :strategies [strategies/all-files
                                strategies/files-with-a-namespace
                                strategies/exclude-edn]))
        (when (strategies/refactor-nrepl-available?)
          (-> (formatters.clean-ns/new {})
              (assoc :strategies [strategies/all-files
                                  strategies/files-with-a-namespace
                                  strategies/exclude-cljc
                                  strategies/exclude-cljs
                                  strategies/exclude-edn
                                  strategies/namespaces-within-refresh-dirs-only
                                  strategies/do-not-use-cached-results!])))]
       (filterv some?)))

(comment
  (formatting-stack.core/format! :linters (into default-linters formatters)
                                 :formatters [] ;; disable all formatters
                                 :in-background? false))
