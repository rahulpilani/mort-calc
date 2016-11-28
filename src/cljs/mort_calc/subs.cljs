(ns mort-calc.subs
    (:require-macros [reagent.ratom :refer [reaction]])
    (:require
      [re-frame.core :as rf]
      [cljs-time.core :as t]))

;;

(rf/reg-sub
  :amount
 (fn [db [_]]
     (reaction (get-in db [:borrow-data :amount]))))

(rf/reg-sub
  :rate
  (fn [db [_]]
      (reaction (get-in db [:borrow-data :rate]))))

(rf/reg-sub
  :term
  (fn [db [_]]
      (reaction (get-in db [:borrow-data :term]))))

(rf/reg-sub
  :home-value
  (fn [db [_]]
      (reaction (get-in db [:home :value]))))

(rf/reg-sub
  :property-tax
  (fn [db [_]]
      (reaction (get-in db [:home :property-tax]))))

(rf/reg-sub
  :hoa
  (fn [db [_]]
      (reaction (get-in db [:home :hoa]))))

(rf/reg-sub
  :start-date
  (fn [db [_]]
      (reaction (get-in db [:home :start-date]))))

(rf/reg-sub :int-amount (fn [db [_]] (let [amount (rf/subscribe  [:amount])](reaction (js/parseInt @@amount)))))
(rf/reg-sub :float-rate (fn [db [_]] (let [rate (rf/subscribe [:rate])](reaction (js/parseFloat @@rate)))))
(rf/reg-sub :int-term (fn [db [_]] (let [term (rf/subscribe [:term])](reaction (js/parseInt @@term)))))
(rf/reg-sub :int-hoa (fn [db [_]] (let [hoa (rf/subscribe [:hoa])](reaction (js/parseInt @@hoa)))))
(rf/reg-sub :int-home-value (fn [db [_]] (let [home-value (rf/subscribe [:home-value])](reaction (js/parseInt @@home-value)))))
(rf/reg-sub :float-property-tax (fn [db [_]] (let [property-tax (rf/subscribe [:property-tax])](reaction (js/parseFloat @@property-tax)))))

(defn exp [a b] (.pow js/Math a b))

(defn is-valid [a] (and (not (js/isNaN a)) (>= a 0)))

(defn round [n]
  (/ (.round js/Math (* n 100)) 100))

(rf/reg-sub
  :taxes-and-fees
  (fn [db [_]]
    (let [hoa (rf/subscribe [:int-hoa])
          property-tax (rf/subscribe [:float-property-tax])
          home-value (rf/subscribe [:home-value])
          property-tax-pct (/ @@property-tax 1200)]
      (do
        (.log js/console (str "HOA: " @@hoa " Tax: " property-tax-pct " Home Value: " @@home-value))
        (reaction
          (if
            (and (is-valid @@hoa) (is-valid property-tax-pct) (is-valid @@home-value))
            (round (+ @@hoa (* @@home-value property-tax-pct)))
            ""))))))

(rf/reg-sub
  :total-payment
  (fn [db [_]]
    (let [taxes-and-fees (rf/subscribe [:taxes-and-fees])
          payment (rf/subscribe [:payment])]
      (reaction
        (if
          (and (is-valid @@taxes-and-fees) (is-valid @@payment))
          (round (+ @@taxes-and-fees @@payment))
          "")))))

(rf/reg-sub
  :payment
  (fn [db [_]]
    (let [amount (rf/subscribe [:int-amount])
          rate (rf/subscribe [:float-rate])
          term (rf/subscribe [:int-term])
          rate-pct (reaction (/ @@rate 100))]
         (reaction
           (if
             (and (is-valid @@amount) (is-valid @rate-pct) (is-valid @@term))
             (let [r (/ @rate-pct 12);; Monthly rate
                   rp1 (+ r 1) ;; (1 + r)
                   n (* @@term 12);; Term in months
                   rp1en (exp rp1 n) ;; (1 + r)^n
                   L @@amount]
               (round (* L (/ (* r rp1en) (- rp1en 1))))) ;; L(r(1 + r)^n / ((1 + r)^n - 1))
             "")))))

(defn remaining-amount [P r c N] ;; P(1 + r)^n - c((1 + r)^n - 1 / r)
  (let [rp1 (+ 1 r) rp1en (exp rp1 N)]
    (- (* P rp1en) (* c (/ (- rp1en 1) r)))))

(rf/reg-sub
  :remaining-amounts
  (fn [db [_]]
    (let [P (rf/subscribe [:int-amount])
          rate (rf/subscribe [:float-rate])
          c (rf/subscribe [:payment])
          term (rf/subscribe [:int-term])]
        (reaction
            (if
              (and (is-valid @@P) (is-valid @@rate) (is-valid @@c))
              (let [months (range 1 (* 12 @@term))
                    r (/ @@rate 1200)
                    dates (map #(t/plus (t/today) (t/months %)) months)
                    remaining-amounts (map round (map #(remaining-amount @@P r @@c %) months))
                    principal-amounts (map round (map #(- (first %) (second %)) (partition 2 1 (cons @@P remaining-amounts))))
                    interest-amounts (map round (map #(- @@c %) principal-amounts))]
                  (vec
                    (take-nth 6
                      (map
                        #(zipmap [:months :principal :interest :remaining] %)
                        (map vector dates principal-amounts interest-amounts remaining-amounts)))))
              [])))))
