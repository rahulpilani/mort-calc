(ns mort-calc.views
    (:require
      [re-frame.core :as rf]
      [mort-calc.slider :as s]
      [cljs-time.core :as t]
      [cljs-time.format :as f])
    (:require-macros [reagent.ratom :refer [reaction]]))

(defn clj->json [ds]
  (.stringify js/JSON
    (clj->js ds)))

(defn target-value [t]
  (-> t .-target .-value))

(defn is-valid [validators value]
  (reduce #(and %1 %2) true (map #(% value) validators)))

(defn annotation-class [is-valid]
  (if is-valid
    ""
    "error"))

(defn dispatch [key]
  (fn [t]
    (rf/dispatch [key (target-value t)])))

(def formatter (.NumberFormat. js/Intl "en-US" (js-obj {"style" "currency" "currency" "USD" "maximumFractionDigits" 1})))
(defn format [num]
  (if (= num "") "" (.format formatter num)))

(def percentage-validator #(<= (js/parseFloat %) 100))
(def not-empty-validator #(and (not= % "") (not (js/isNaN %))))
(def not-zero-validator #(not= 0 (js/parseInt %)))

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
      [:div.ui.statistic
        [:div.value
          [:span (format @sub)]]
        [:div.label label]])))


(defn interest-rate []
  (right-labeled-field :rate-str "Interest" "Interest Rate" :rate-changed "%" percentage-validator not-empty-validator not-zero-validator))

(defn property-tax []
  (right-labeled-field :property-tax-str "Property Tax" "Property Tax" :property-tax-changed "%" percentage-validator))

(defn hoa []
  (right-labeled-field :hoa "HOA" "HOA" :hoa-changed "$/Month"))

(defn loan-term []
  (right-labeled-field :term "Term" "Loan Term" :term-changed "Years" not-empty-validator not-zero-validator))

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

(defn to-radians [fraction total]
  (* (/ fraction total) (* 2 js/Math.PI)))

(defn payment-slice [index increment start total fill]
  (if (> increment 0)
    ^{:key index}[:path {:d ((.arc js/d3) (clj->js
                                            {
                                              :innerRadius 40
                                              :outerRadius 75
                                              :startAngle (to-radians start total)
                                              :endAngle (to-radians (+ start increment) total)}))
                         :fill fill}]))

(def payment-fills ["#98abc5", "#8a89a6", "#7b6888", "#6b486b", "#a05d56", "#d0743c", "#ff8c00"])
(def payment-labels ["Principal" "Interest" "Property Tax" "HOA" "Additional Payment"])
(def year-month-formatter (f/formatter "MMM yyyy"))


(defn payment-slices [payment-info]
  (let [values (get payment-info :payment-breakdown)
        cumulative (reductions + 0 values)
        zipped (map vector values cumulative)
        indexed (map-indexed vector zipped)
        total (reduce + values)]
    (.log js/console (clj->js zipped))
    [:g {:transform "translate(75, 75)"}
      (for [item indexed]
        (let [[index [increment start]] item]
          (payment-slice index increment start total (get payment-fills index))))]))

(defn payment-legend [payment-info]
  (let [values (get payment-info :payment-breakdown)
        total (reduce + values)
        indexed (map-indexed vector values)]
      [:g {:transform "translate(180, 20)"}
        (for [item indexed]
          (let [[index value] item]
            ^{:key index}[:g
                          [:rect {:height "0.7em" :width "0.7em" :x "0em" :y (str (- (* index 1) 0.7) "em") :fill (get payment-fills index)}]
                          [:text {:y (str (* index 1) "em") :x "1em"}
                            (get payment-labels index)]]))]))

(defn payment-drilldown []
  (let [all-payments (rf/subscribe [:all-payments])
        current-month (rf/subscribe [:current-month])]
    (fn []
      (let [payment-info (get @all-payments @current-month)]
        [:div.ui.segment
          [:div.ui.dividing.header
            "Forecast"]
;;(f/unparse-local-date year-month-formatter (get payment-info :date))
          [:div.ui.grid
            [:div.two.column.row
              [:div.column
                [:div
                  [:h2.ui.center.aligned.header
                    (f/unparse-local-date year-month-formatter (get payment-info :date))  ]
                  [:table.ui.definition.table
                    [:tbody
                      [:tr
                        [:td "Remaining Amount"]
                        [:td (format (get payment-info :remaining-amount))]]
                      [:tr
                        [:td "Interest Paid"]
                        [:td (format (get payment-info :total-interest))]]]]]]
              [:div.column
                [:div.chart
                 [:svg {:width 350 :height 150}
                  [payment-slices payment-info]
                  [payment-legend payment-info]]]]]]]))))







(defn main-panel []
   (fn []
     [:div
       [:div.ui.grid
         [:div.two.column.centered.row
           [:div.column.five.wide
             [:div.ui.segment
               [loan-form]]]
           [:div.column.eleven.wide
             [:div.one.column.row
               [:div.column
                 [:div.ui.segment
                   [:div.ui.dividing.header "Payment"]
                   [:div.ui.grid
                     [:div.one.column.row
                      [:div.column.center.aligned
                        [statistics]]]]]]]
             [:div.one.column.row
               [:div.column]]
             [:div.one.column.row
               [:div.column
                 [payment-drilldown]]]]]]]))
