(ns mort-calc.subs
    (:require-macros [reagent.ratom :refer [reaction]])
    (:require
      [re-frame.core :as rf]
      [cljs-time.core :as t]))

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
    (.log js/console (str l ": ") x (clj->js m))
    x))
(defn exp [a b] (.pow js/Math a b))

(defn is-valid [a] (and (not (js/isNaN a)) (>= a 0)))

(defn round [n]
  (/ (.round js/Math (* n 100)) 100))

(defn find-property-tax [home-value property-tax-pct]
  (if (and (is-valid home-value) (is-valid property-tax-pct))
    (* home-value property-tax-pct)
    ""))

(defn taxes-and-fees [hoa property-tax-amount]
  (if
    (and (is-valid hoa) (is-valid property-tax-amount))
    (round (+ hoa property-tax-amount))
    ""))

(defn payment [amount term r]
  (if
    (and (is-valid amount) (is-valid r) (is-valid term))
    (let [rp1 (+ r 1) ;; (1 + r)
          n (* term 12);; Term in months
          rp1en (exp rp1 n) ;; (1 + r)^n
          L amount]
        (round (* L (/ (* r rp1en) (- rp1en 1))))) ;; L(r(1 + r)^n / ((1 + r)^n - 1))
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
  :down-payment-pct
  (fn [db _]
    (parse-int (get-in db [:borrow-data :down-payment-pct]))))

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
  :loan-terms
  (fn [db [_]]
    (get-in db [:loan-terms])))

(rf/reg-sub
  :term-months
  :<- [:term]
  (fn [term [_]]
    (if (is-valid term)
      (* 12 term)
      0)))

(rf/reg-sub
  :current-month
  (fn [db [_]]
    (parse-int (get-in db [:borrow-data :current-month]))))

(rf/reg-sub
  :limited-current-month
  :<- [:current-month]
  :<- [:all-payments]
  (fn [[current-month payment-breakdown] [_]]
    (let [max-month (count payment-breakdown)]
      (if (> current-month max-month)
        max-month
        current-month))))


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
  :property-tax-amount
  :<- [:home-value]
  :<- [:property-tax-pct]
  (fn [[home-value property-tax-pct] [_]]
    (let [property-tax-amount (find-property-tax home-value property-tax-pct)]
      property-tax-amount)))

(rf/reg-sub
  :taxes-and-fees
  :<- [:property-tax-amount]
  :<- [:hoa]
  (fn [[property-tax-amount hoa] [_]]
    (let [taxes-and-fees (taxes-and-fees hoa property-tax-amount)]
      taxes-and-fees)))

(rf/reg-sub
  :total-payment
  :<- [:taxes-and-fees]
  :<- [:payment]
  :<- [:additional-payment]
  (fn [[taxes-and-fees payment additional-payment] [_]]
    (let [total-payment (total-payment taxes-and-fees payment additional-payment)]
      total-payment)))



(rf/reg-sub
  :payment
  :<- [:amount]
  :<- [:term]
  :<- [:rate-pct]
  (fn [[amount term r] [_]]
    (let [payment (payment amount term r)]
      payment)))

(defn payment-dict [hoa property-tax additional-payment]
  (fn [values]
    (let [[date month remaining-amount principal interest total-interest] values]
      {:date date
       :month month
       :remaining-amount remaining-amount
       :payment-breakdown [principal interest property-tax hoa additional-payment]
       :total-interest total-interest})))

(rf/reg-sub
  :all-payments
  :<- [:amount]
  :<- [:payment]
  :<- [:term-months]
  :<- [:rate-pct]
  :<- [:additional-payment]
  :<- [:hoa]
  :<- [:property-tax-amount]
  :<- [:current-month]
  (fn [[P c N r additional-payment hoa property-tax-amount current-month] [_]]
    (if
      (and (is-valid P) (is-valid r) (is-valid c) (is-valid N))
      (let [months (range 1 (+ 1 N))
            a (if (is-valid additional-payment) additional-payment 0)
            t (+ c a)
            dates (vec (map #(t/plus (t/today) (t/months %)) months))
            remaining-amounts (vec (filter gt-zero (map round (map #(remaining-amount P r t %) months))))
            principal-amounts (vec (filter gt-zero (map round (map #(- (first %) (second %)) (partition 2 1 (cons P remaining-amounts))))))
            interest-amounts (vec (filter gt-zero (map round (map #(- t %) principal-amounts))))
            total-interests (reductions + interest-amounts)
            uber-vec (map vector dates months remaining-amounts principal-amounts interest-amounts total-interests)
            payment-dict-coll (map (payment-dict hoa property-tax-amount additional-payment) uber-vec)
            payment-dict (zipmap months payment-dict-coll)
            max-month (+ 1 (count payment-dict))]
          payment-dict))))
