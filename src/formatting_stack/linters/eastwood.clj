(ns formatting-stack.linters.eastwood
  (:require
   [clojure.string :as str]
   [eastwood.lint]
   [eastwood.util]
   [formatting-stack.protocols.linter :as linter]
   [formatting-stack.util :refer [ns-name-from-filename]]
   [medley.core :refer [deep-merge]]
   [nedap.utils.modular.api :refer [implement]]))

(def default-eastwood-options
  ;; Avoid false positives or more-annoying-than-useful checks:
  (let [linters (remove #{:suspicious-test :unused-ret-vals :constant-test :wrong-tag}
                        eastwood.lint/default-linters)]
    (-> eastwood.lint/default-opts
        (assoc :linters linters))))

(def default-warnings-to-silence
  [#"== Eastwood"
   #"^dbg "
   #"Warning: protocol .* is overwriting function" ;; False positive with nedap.speced.def
   #"Directories scanned"
   #"Entering directory"
   #".*wrong-pre-post.*\*.*\*" ;; False positives for dynamic vars https://git.io/fhQTx
   #"== Warnings"
   #"== Linting done"])

(defn lint! [{:keys [options warnings-to-silence]} filenames]
  (reset! eastwood.util/warning-enable-config-atom []) ;; https://github.com/jonase/eastwood/issues/317
  (let [namespaces (->> filenames
                        (remove #(str/ends-with? % ".edn"))
                        (keep ns-name-from-filename))
        result (->> (with-out-str
                      (binding [*warn-on-reflection* true]
                        (eastwood.lint/eastwood (-> options
                                                    (assoc :namespaces namespaces)))))
                    (str/split-lines)
                    (remove (fn [line]
                              (or (str/blank? line)
                                  (some (fn [re]
                                          (re-find re line))
                                        warnings-to-silence)))))]
    (when-not (every? (fn [line]
                        (str/starts-with? line "== Linting"))
                      result)
      (->> result (str/join "\n") println))))

(defn new [{:keys [eastwood-options warnings-to-silence]
            :or {warnings-to-silence default-warnings-to-silence
                 eastwood-options {}}}]
  (implement {:options (deep-merge default-eastwood-options eastwood-options)
              :warnings-to-silence warnings-to-silence}
    linter/--lint! lint!))
