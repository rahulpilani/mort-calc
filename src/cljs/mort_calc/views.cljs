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
        remaining-amounts (rf/subscribe [:remaining-amounts])]
    (fn []
      (.log js/console remaining-amounts)
      (js/redrawChart (clj->js @@remaining-amounts))
      [:div
        [:div
          [:input.form-control
            {:id :amount :value @@amount :on-change #(rf/dispatch [:amount-changed (-> % .-target .-value)])}]]
        [:div
          [:input.form-control
            {:id :rate :value @@rate :on-change #(rf/dispatch [:rate-changed (-> % .-target .-value)])}]]
        [:div
          [:span (str "Monthly Mortgage: " @@payment)]]])))
