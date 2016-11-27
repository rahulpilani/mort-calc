(ns mort-calc.views
    (:require [re-frame.core :as rf])
    (:require-macros [reagent.ratom :refer [reaction]]))
(defn clj->json [ds]
  (.stringify js/JSON
    (clj->js ds)))
(defn main-panel []
  (let [amount (rf/subscribe [:amount])
        rate (rf/subscribe [:rate])
        payment (rf/subscribe [:payment])
        term (rf/subscribe [:term])
        remaining-amounts (rf/subscribe [:remaining-amounts])]
    (fn []
      (.log js/console remaining-amounts)
      (js/redrawChart (clj->js @@remaining-amounts))
      [:div.ui.grid
        [:div.two.column.centered.row
          [:div.column
            [:div.ui.segment
              [:form.ui.form
                [:div.ui.dividing.header "Property"]
                [:div.ui.dividing.header "Loan"]
                [:div.two.fields
                  [:div.field
                    [:label "Amount"]
                    [:div.ui.labeled.input
                      [:div.ui.label "$"]
                      [:input
                        {:placeholder "Loan Amount" :id :amount :value @@amount :on-change #(rf/dispatch [:amount-changed (-> % .-target .-value)])}]]]
                  [:div.field
                    [:label "Interest"]
                    [:div.ui.right.labeled.input
                      [:input
                        {:placeholder "Interest Rate %" :id :rate :value @@rate :on-change #(rf/dispatch [:rate-changed (-> % .-target .-value)])}]
                      [:div.ui.label "%"]]]]
                [:div.one.fields
                  [:div.field
                    [:label "Term"]
                    [:div.ui.right.labeled.input
                      [:input
                        {:placeholder "Loan Term" :id :term :value @@term :on-change #(rf/dispatch [:term-changed (-> % .-target .-value)])}]
                      [:div.ui.label "Years"]]]]]]]
          [:div.column
            [:div.ui.segment
              [:div.ui.dividing.header "Payment"]
              [:div.ui.grid
                [:div.one.column.row
                  [:div.column
                    [:div.ui.one.statistics
                      [:div.statistic
                        [:div.value
                          [:span @@payment]]
                        [:div.label "Monthly Mortgage"]]]]]]]]]])))
