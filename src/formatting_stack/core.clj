(ns formatting-stack.core
  (:require
   [formatting-stack.formatters.cider]
   [formatting-stack.formatters.cljfmt]
   [formatting-stack.formatters.how-to-ns]
   [formatting-stack.indent-specs :refer [default-third-party-indent-specs]]
   [formatting-stack.linters.eastwood]
   [formatting-stack.protocols.compiler :as protocols.compiler]
   [formatting-stack.protocols.formatter :as protocols.formatter]
   [formatting-stack.protocols.linter :as protocols.linter]
   [formatting-stack.strategies :as strategies]))

(def default-strategies [strategies/git-completely-staged])

(def extended-strategies [strategies/git-completely-staged
                          strategies/git-not-completely-staged])

(defn default-formatters [third-party-indent-specs]
  (let [opts {:third-party-indent-specs third-party-indent-specs}]
    [(formatting-stack.formatters.cider/map->Formatter (assoc opts :strategies extended-strategies))
     (formatting-stack.formatters.cljfmt/map->Formatter opts)
     (formatting-stack.formatters.how-to-ns/map->Formatter opts)]))

(def default-linters [(formatting-stack.linters.eastwood/map->Eastwood {:strategies extended-strategies})])

(def default-compilers [])

(defn files-from-strategies [strategies]
  (->> strategies
       (mapcat (fn [f]
                 (f)))
       distinct))

(defn format! [& {:keys [strategies
                         third-party-indent-specs
                         formatters
                         linters
                         compilers]}]
  ;; the following `or` clauses ensure that Components don't pass nil values
  (let [strategies               (or strategies default-strategies)
        third-party-indent-specs (or third-party-indent-specs default-third-party-indent-specs)
        formatters               (or formatters (default-formatters third-party-indent-specs))
        linters                  (or linters default-linters)
        compilers                (or compilers default-compilers)
        {formatters-strategies
         :formatters
         linters-strategies
         :linters
         compilers-strategies
         :compilers
         default-strategies
         :default}               strategies]

    (doseq [formatter formatters]
      (let [{specific-strategies :strategies} formatter
            strategies (or specific-strategies formatters-strategies strategies)
            files (files-from-strategies strategies)]
        (protocols.formatter/format! formatter files)))

    (doseq [linter linters]
      (let [{specific-strategies :strategies} linter
            strategies (or specific-strategies linters-strategies strategies)
            files (files-from-strategies strategies)]
        (protocols.linter/lint! linter files)))

    (doseq [compiler compilers]
      (let [{specific-strategies :strategies} compiler
            strategies (or specific-strategies compilers-strategies strategies)
            files (files-from-strategies strategies)]
        (protocols.compiler/compile! compiler files)))))
