(ns mort-calc.subs
    (:require-macros [reagent.ratom :refer [reaction]])
    (:require
      [re-frame.core :as rf]
      [cljs-time.core :as t]))

;;
(defn parse-num [f a]
  (if (not= a "")
    (let [converted (f a)]
      (if (js/isNaN converted)
        0
        converted))
    ""))

(defn parse-int [a]
  (parse-num js/parseInt a))

(defn parse-float [a]
  (parse-num js/parseFloat a))

(defn log-and-return [l x & m]
  (do
    (.log js/console (str l ": ") x m)
    x))
(defn exp [a b] (.pow js/Math a b))

(defn is-valid [a] (and (not (js/isNaN a)) (>= a 0)))

(defn round [n]
  (/ (.round js/Math (* n 100)) 100))

(defn taxes-and-fees [home-value hoa property-tax-pct]
  (if
    (and (is-valid hoa) (is-valid property-tax-pct) (is-valid home-value))
    (round (+ hoa (* home-value property-tax-pct)))
    ""))

(defn payment [amount term r]
  (if
    (and (is-valid amount) (is-valid r) (is-valid term))
    (let [rp1 (+ r 1) ;; (1 + r)
          n (* term 12);; Term in months
          rp1en (exp rp1 n) ;; (1 + r)^n
          L amount]
      (do
        (.log js/console amount r term rp1 n rp1en L)
        (round (* L (/ (* r rp1en) (- rp1en 1)))))) ;; L(r(1 + r)^n / ((1 + r)^n - 1))
    ""))

(defn total-payment [taxes-and-fees payment additional-payment]
  (if
    (and (is-valid taxes-and-fees) (is-valid payment))
    (let [a (if (is-valid additional-payment) additional-payment 0)]
      (round (+ taxes-and-fees payment a)))
    ""))

(defn remaining-amount [P r c N] ;; P(1 + r)^n - c((1 + r)^n - 1 / r)
  (let [rp1 (+ 1 r) rp1en (exp rp1 N)]
    (- (* P rp1en) (* c (/ (- rp1en 1) r)))))

(defn gt-zero [x]
  (> x 0))


(rf/reg-sub
  :amount
  (fn [db _]
    (parse-int (get-in db [:borrow-data :amount]))))

(rf/reg-sub
  :rate
  (fn [db [_]]
    (parse-float (get-in db [:borrow-data :rate]))))

(rf/reg-sub
  :rate-str
  (fn [db [_]]
    (get-in db [:borrow-data :rate])))


(rf/reg-sub
  :term
  (fn [db [_]]
    (parse-int (get-in db [:borrow-data :term]))))

(rf/reg-sub
  :current-month
  (fn [db [_]]
    (parse-int (get-in db [:borrow-data :current-month]))))

(rf/reg-sub
  :additional-payment
  (fn [db [_]]
    (parse-int (get-in db [:borrow-data :additional-payment]))))

(rf/reg-sub
  :home-value
  (fn [db [_]]
    (parse-int (get-in db [:home :value]))))

(rf/reg-sub
  :property-tax
  (fn [db [_]]
    (parse-float (get-in db [:home :property-tax]))))

(rf/reg-sub
  :property-tax-str
  (fn [db [_]]
    (get-in db [:home :property-tax])))


(rf/reg-sub
  :hoa
  (fn [db [_]]
    (parse-int (get-in db [:home :hoa]))))

(rf/reg-sub
  :start-date
  (fn [db [_]]
    (get-in db [:home :start-date])))

(rf/reg-sub
  :property-tax-pct
  :<- [:property-tax]
  (fn [property-tax [_]]
    (/ property-tax 1200)))

(rf/reg-sub
  :rate-pct
  :<- [:rate]
  (fn [rate [_]]
    (/ rate 1200)))

(rf/reg-sub
  :taxes-and-fees
  :<- [:home-value]
  :<- [:hoa]
  :<- [:property-tax-pct]
  (fn [[home-value hoa property-tax-pct] [_]]
    (let [taxes-and-fees (taxes-and-fees home-value hoa property-tax-pct)]
      (log-and-return "Taxes and Fees" taxes-and-fees))))

(rf/reg-sub
  :total-payment
  :<- [:taxes-and-fees]
  :<- [:payment]
  :<- [:additional-payment]
  (fn [[taxes-and-fees payment additional-payment] [_]]
    (let [total-payment (total-payment taxes-and-fees payment additional-payment)]
      (log-and-return "Total Payment" total-payment))))



(rf/reg-sub
  :payment
  :<- [:amount]
  :<- [:term]
  :<- [:rate-pct]
  (fn [[amount term r] [_]]
    (let [payment (payment amount term r)]
      (log-and-return "Payment" payment))))

(rf/reg-sub
  :payment-breakdown
  :<- [:amount]
  :<- [:payment]
  :<- [:current-month]
  :<- [:rate-pct]
  :<- [:additional-payment]
  (fn [P c N r additional-payment]
    (if
      (and (is-valid P) (is-valid r) (is-valid c) (is-valid N))
      (let [months (range 1 (+ 1 N))
            a (if (is-valid additional-payment) additional-payment 0)
            t (+ c a)
            dates (vec (map #(t/plus (t/today) (t/months %)) months))
            remaining-amounts (vec (filter gt-zero (map round (map #(remaining-amount P r t %) months))))
            principal-amounts (vec (filter gt-zero (map round (map #(- (first %) (second %)) (partition 2 1 (cons P remaining-amounts))))))
            interest-amounts (vec (filter gt-zero (map round (map #(- t %) principal-amounts))))
            total-interest (reduce + interest-amounts)]
        {
          :date (last (take (count remaining-amounts) dates))
          :remaining-amount (last remaining-amounts)
          :principal (- (last principal-amounts) a)
          :interest (last interest-amounts)
          :total-interest total-interest}))))
