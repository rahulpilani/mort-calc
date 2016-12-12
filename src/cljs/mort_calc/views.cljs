(ns mort-calc.views
    (:require
      [re-frame.core :as rf]
      [mort-calc.slider :as s])
    (:require-macros [reagent.ratom :refer [reaction]]))

(defn clj->json [ds]
  (.stringify js/JSON
    (clj->js ds)))

(defn target-value [t]
  (-> t .-target .-value))

(defn is-valid [validators value]
  (reduce #(and %1 %2) true (map validators #(% value))))

(defn annotation-class [is-valid]
  (if is-valid
    "error"
    ""))

(defn dispatch [key]
  (fn [t]
    (rf/dispatch [key (target-value t)])))

(def formatter (.NumberFormat. js/Intl "en-US" (js-obj {"style" "currency" "currency" "USD" "maximumFractionDigits" 1})))
(defn format [num]
  (if (= num "") "" (.format formatter num)))

(def percentage-validator #(<= (js/parseFloat %) 100))

(defn left-labeled-field [id label place-holder event-key left-label & validators]
  (let [sub (rf/subscribe [id])]
    (fn []
      [:div.field {:class (annotation-class (is-valid validators @sub))}
        [:label label]
        [:div.ui.labeled.input
          [:div.ui.label left-label]
          [:input
            {:placeholder place-holder :id id :value (format @sub) :on-change (dispatch event-key)}]]])))

(defn right-labeled-field [id label place-holder event-key right-label & validators]
  (let [sub (rf/subscribe [id])]
    (fn []
      [:div.field {:class (annotation-class (is-valid validators @sub))}
        [:label label]
        [:div.ui.right.labeled.input
          [:input
            {:placeholder place-holder :id id :value @sub :on-change (dispatch event-key)}]
          [:div.ui.label right-label]]])))

(defn statistic [id label]
  (let [sub (rf/subscribe [id])]
    (fn []
      (.log js/console @sub)
      [:div.ui.statistic
        [:div.value
          [:span (format @sub)]]
        [:div.label label]])))


(defn interest-rate []
  (right-labeled-field :rate-str "Interest" "Interest Rate" :rate-changed "%"))

(defn property-tax []
  (right-labeled-field :property-tax-str "Property Tax" "Property Tax" :property-tax-changed "%"))

(defn hoa []
  (right-labeled-field :hoa "HOA" "HOA" :hoa-changed "$/Month"))

(defn loan-term []
  (right-labeled-field :term "Term" "Loan Term" :term-changed "Years"))

(defn loan-amount []
  (left-labeled-field :amount "Loan Amount" "Loan Amount" :amount-changed "$"))

(defn home-value []
  (left-labeled-field :home-value "Home Value" "Home Value" :value-changed "$"))

(defn additional-payment []
  (left-labeled-field :additional-payment "Extra Payment" "Extra Payment" :additional-payment-changed "$"))

(defn slider []
  (let [value (rf/subscribe [:amount])]
    (fn []
      [:input {:type "range" :value @value :min 1000 :max 3000000 :step 10000 :on-change (dispatch :amount-changed)}])))

(defn loan-form []
  (fn []
    [:form.ui.form
      [:div.ui.dividing.header "Loan"]
      [:div.two.fields
        [home-value]
        [loan-amount]]
      [:div.two.fields
        [interest-rate]
        [loan-term]]
      [:div.ui.dividing.header "Additional Expenses"]
      [:div.two.fields
        [property-tax]
        [hoa]]
      [:div.one.fields
        [additional-payment]]
      [slider]]))

(defn monthly-mortgage-payment []
  (statistic :payment "Monthly Mortgage"))

(defn taxes-and-fees []
  (statistic :taxes-and-fees "Taxes & Fees"))

(defn total-payment []
  (statistic :total-payment "Total Payment"))

(defn statistics []
  (fn []
    ; [:div.ui.three.statistics]
    [:div.ui.three.small.statistics
      [monthly-mortgage-payment]
      [taxes-and-fees]
      [total-payment]]))

; (defn main-panel []
;   (let [amount (rf/subscribe [:amount])]
;     (fn []
;       (.log js/console (get-in amount [:borrow-data :amount]))
;       [:div (get-in amount [:borrow-data :amount])])))


(defn main-panel []
   (fn []
     [:div
       [:div.ui.grid
         [:div.two.column.centered.row
           [:div.column.five.wide
             [:div.ui.segment
               [loan-form]]]
           [:div.column.eleven.wide
             [:div.ui.segment
               [:div.ui.dividing.header "Payment"]
               [:div.ui.grid
                 [:div.one.column.row
                   [:div.column.center.aligned
                     [statistics]]]]]]]]]))
