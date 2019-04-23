(ns formatting-stack.indent-specs)

(def default-third-party-indent-specs
  '{cats.core/mlet                                        {:style/indent 1}
    clojure.core/delay                                    {:style/indent 0}
    clojure.core/time                                     {:style/indent 0}
    clojure.test.check.properties/for-all                 {:style/indent 1}
    datomic.client.api/q                                  {:style/indent 1}
    datomic.api/q                                         {:style/indent 1}
    hiccup.form/form-to                                   {:style/indent 1}
    hiccup.page/html5                                     {:style/indent 1}
    fulcrologic.semantic-ui.factories/ui-button           {:style/indent 1}
    fulcrologic.semantic-ui.factories/ui-card-content     {:style/indent 1}
    fulcrologic.semantic-ui.factories/ui-dropdown         {:style/indent 1}
    fulcrologic.semantic-ui.factories/ui-dropdown-menu    {:style/indent 1}
    fulcrologic.semantic-ui.factories/ui-form             {:style/indent 1}
    fulcrologic.semantic-ui.factories/ui-form-group       {:style/indent 1}
    fulcrologic.semantic-ui.factories/ui-grid             {:style/indent 1}
    fulcrologic.semantic-ui.factories/ui-grid-column      {:style/indent 1}
    fulcrologic.semantic-ui.factories/ui-header           {:style/indent 1}
    fulcrologic.semantic-ui.factories/ui-item-content     {:style/indent 1}
    fulcrologic.semantic-ui.factories/ui-item-group       {:style/indent 1}
    fulcrologic.semantic-ui.factories/ui-label            {:style/indent 1}
    fulcrologic.semantic-ui.factories/ui-label-group      {:style/indent 1}
    fulcrologic.semantic-ui.factories/ui-list             {:style/indent 1}
    fulcrologic.semantic-ui.factories/ui-list-content     {:style/indent 1}
    fulcrologic.semantic-ui.factories/ui-list-item        {:style/indent 1}
    fulcrologic.semantic-ui.factories/ui-menu-item        {:style/indent 1}
    fulcrologic.semantic-ui.factories/ui-modal            {:style/indent 1}
    fulcrologic.semantic-ui.factories/ui-modal-actions    {:style/indent 1}
    fulcrologic.semantic-ui.factories/ui-modal-content    {:style/indent 1}
    fulcrologic.semantic-ui.factories/ui-modal-header     {:style/indent 1}
    fulcrologic.semantic-ui.factories/ui-segment          {:style/indent 1}
    fulcrologic.semantic-ui.factories/ui-segment-group    {:style/indent 1}
    fulcrologic.semantic-ui.factories/ui-sidebar          {:style/indent 1}
    fulcrologic.semantic-ui.factories/ui-sidebar-pushable {:style/indent 1}
    fulcrologic.semantic-ui.factories/ui-sidebar-pusher   {:style/indent 1}
    fulcrologic.semantic-ui.factories/ui-table            {:style/indent 1}
    fulcrologic.semantic-ui.factories/ui-table-body       {:style/indent 1}
    fulcrologic.semantic-ui.factories/ui-table-cell       {:style/indent 1}
    fulcrologic.semantic-ui.factories/ui-table-row        {:style/indent 1}
    fulcro.client.dom/div                                 {:style/indent 1}
    fulcro.client.dom/span                                {:style/indent 1}
    fulcro-spec.core/assertions                           {:style/indent      0
                                                           :style.cljfmt/type :inner}
    fulcro-spec.core/behavior                             {:style/indent 1}
    fulcro-spec.core/component                            {:style/indent 1}
    fulcro-spec.core/specification                        {:style/indent 1}
    fulcro-spec.core/when-mocking!                        {:style/indent      0
                                                           :style.cljfmt/type :inner}
    fulcro-spec.core/when-mocking                         {:style/indent      0
                                                           :style.cljfmt/type :inner}
    garden.stylesheet/at-media                            {:style/indent 1}})

(def magic-symbol-mappings
  "If a given ns requires e.g. `fulcro.server`, then `action` will have the specified indent rule.

  Apt for macros such as `fulcro.server/defmutation` which inject `action` into the local environment."
  '{fulcro.server           {action {:style/indent      0
                                     :style.cljfmt/type :inner}}
    fulcro.client.mutations {action {:style/indent      0
                                     :style.cljfmt/type :inner}}})
