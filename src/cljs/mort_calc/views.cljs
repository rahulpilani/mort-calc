(ns mort-calc.views
    (:require
      [re-frame.core :as rf]
      [mort-calc.dropdown :refer [dropdown]]
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
  (dropdown :term :loan-terms "Term" "Loan Term" :term-changed))

(defn loan-amount []
  (left-labeled-field :amount "Loan Amount" "Loan Amount" :amount-changed "$"))

(defn down-payment-pct []
  (right-labeled-field :down-payment-pct "Down Payment" "Down Payment %" :down-payment-pct-changed "%"))

(defn home-value []
  (left-labeled-field :home-value "Home Value" "Home Value" :value-changed "$"))

(defn additional-payment []
  (left-labeled-field :additional-payment "Extra Payment" "Extra Payment" :additional-payment-changed "$"))

(defn loan-form []
  (fn []
    [:form.ui.form
      [:div.ui.dividing.header "Loan"]
      [:div.one.fields
        [home-value]]
      [:div.two.fields
        [loan-amount]
        [down-payment-pct]]
      [:div.two.fields
        [interest-rate]
        [loan-term]]
      [:div.ui.dividing.header "Additional Expenses"]
      [:div.two.fields
        [property-tax]
        [hoa]]
      [:div.one.fields
        [additional-payment]]]))

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

(defn arc-radians [start increment total]
  [(to-radians start total) (to-radians (+ start increment) total)])

(defn arc-args [start increment total inner-radius outer-radius]
  (let [[start-radians end-radians] (arc-radians start increment total)]
    (clj->js {
              :outerRadius outer-radius
              :startAngle start-radians
              :endAngle end-radians
              :innerRadius inner-radius})))


(defn payment-pct [index increment start total]
  (if (> increment 0)
    (let [[x y] (.centroid (.arc js/d3) (arc-args start increment total 0 185))]
      ^{:key (str "pct-" index)}[:text {:x x :y y :text-anchor "middle" :alignment-baseline "middle"}
                                  (str (.round js/Math (* 100 (/ increment total))) "%")])))


(defn payment-slice [index increment start total fill]
  (if (> increment 0)
    ^{:key (str "slice-" index)}[:path {:d ((.arc js/d3) (arc-args start increment total 0 75))
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
    [:g {:transform "translate(110, 105)"}
      (for [item indexed]
        (let [[index [increment start]] item]
          ^{:key index}[:g
                        [payment-slice index increment start total (get payment-fills index)]
                        [payment-pct index increment start total]]))]))


(defn payment-legend [payment-info]
  (let [values (get payment-info :payment-breakdown)
        total (reduce + values)
        indexed (map-indexed vector values)]
      [:g {:transform "translate(180, 20)"}
        (for [item indexed]
          (let [[index value] item]
            ^{:key index}[:g {:transform "translate(45,0)"}
                          [:rect {:height "0.7em" :width "0.7em" :x "0em" :y (str (- (* index 1) 0.7) "em") :fill (get payment-fills index)}]
                          [:text {:y (str (* index 1) "em") :x "1em"}
                            (get payment-labels index)]]))]))

(defn format-date-mmm-yyyy [date]
  (if (not (nil? date))
      (f/unparse-local-date year-month-formatter date)
      ""))

(defn slider-scale []
  (let [all-payments (rf/subscribe [:all-payments])]
    (fn []
      (let [months (count @all-payments)
            domain (clj->js [1 (+ 1 months)])
            rng (clj->js [6 700])]
        [:svg {:width 710 :height 20}
          (for [month (cons 1 (range 6 (+ 2 months) 6))]
            (let [scale (.range (.domain (.scaleLinear js/d3) domain) rng)
                  height (cond (= 0 (mod month 60)) 11 :else 13)]
              ^{:key month}[:g
                            ^{:key (str "tick-" month)} [:line {:x1 (scale month) :y1 20 :x2 (scale month) :y2 height :stroke-width 1 :stroke "black"}]
                            (if (or (= 1 month) (= 0 (mod month 60)))
                              ^{:key (str "label-" month)} [:text {:x (scale month) :y 10 :text-anchor "middle" :font-size "0.7em"} (.round js/Math (/ month 12))])]))]))))

(defn forecast-slider []
  (let [all-payments (rf/subscribe [:all-payments])
        current-month (rf/subscribe [:limited-current-month])]
    (fn []
      [:div.slider.forecast
        [slider-scale]
        [:input {:id "forecast-slider" :type "range" :default-value @current-month :min 1 :max (count @all-payments) :step 1 :on-change (dispatch :current-month-changed)}]])))


(defn payment-drilldown []
  (let [all-payments (rf/subscribe [:all-payments])
        current-month (rf/subscribe [:limited-current-month])]
    (fn []
      (let [payment-info (get @all-payments @current-month)
            months (keys @all-payments)
            max-month (apply max months)
            [principal interest] (get payment-info :payment-breakdown)
            {:keys [remaining-amount total-interest]} payment-info
            adjusted-principal (if (< remaining-amount principal) remaining-amount principal)
            payoff-date (get-in @all-payments [max-month :date])
            stats [
                    ["Principal" adjusted-principal format]
                    ["Interest" interest format]
                    ["Remaining Amount" remaining-amount format]
                    ["Interest Paid" total-interest format]
                    ["Pay-off Date" payoff-date format-date-mmm-yyyy]]]
        [:div.ui.segment.forecast
          [:div.ui.dividing.header
            "Forecast"]
          [:div.ui.grid
            [:div.column.center.aligned.sixteen.wide
              [:h2.header
                (format-date-mmm-yyyy (get payment-info :date))]]
            [:div.two.column.row
              [:div.column
                [:table.ui.definition.table
                  [:tbody
                    (for [[name value formatter] stats]
                      ^{:key name}[:tr
                                    [:td.seven.wide name]
                                    [:td (formatter value)]])]]]
              [:div.column
                [:div.chart
                 [:svg {:width 380 :height 210}
                  [payment-slices payment-info]
                  [payment-legend payment-info]]]]]

            [:div.column.sixteen.wide.center.aligned
              [forecast-slider]]]]))))

(defn main-panel []
   (fn []
     [:div
       [:div.ui.grid
         [:div.column.five.wide
           [:div.ui.segment.form
             [loan-form]]]
         [:div.column.eleven.wide
           [:div.ui.segment.payment
             [:div.ui.dividing.header "Payment"]
             [statistics]]
          [payment-drilldown]]]]))
