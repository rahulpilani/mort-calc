(ns mort-calc.events
    (:require [re-frame.core :as rf]
              [mort-calc.db :as db]
              [mort-calc.parse-utils :refer [parse-int]]))

(rf/reg-event-db
 :initialize-db
 (fn [_ _]
    db/default-db))
(defn sanitize-integer [n]
  (clojure.string/replace n #"[^0-9]" ""))

(defn sanitize-float [n]
  (clojure.string/replace n #"[^0-9.]" ""))

(defn round [n]
  (.round js/Math n))

(defn is-valid [a] (and (not (js/isNaN a)) (>= a 0)))

(defn handle-amount-changed
  [db [_ amount]]
  (let [sanitized-amount (sanitize-integer amount)
        home-value (parse-int (get-in db [:home :value]))
        parsed-amount (parse-int sanitized-amount)
        down-payment-pct-raw (round (* (/ (- home-value parsed-amount) home-value) 100))
        down-payment-pct (if (is-valid down-payment-pct-raw) down-payment-pct-raw "")
        db (assoc-in db [:borrow-data :amount] sanitized-amount)
        db (assoc-in db [:borrow-data :down-payment-pct] down-payment-pct)]
    db))

(defn handle-down-payment-changed
  [db [_ pct]]
  (let [down-payment-pct-raw (sanitize-integer pct)
        db (assoc-in db [:borrow-data :down-payment-pct] (sanitize-integer down-payment-pct-raw))
        home-value (parse-int (get-in db [:home :value]))
        down-payment-pct (parse-int down-payment-pct-raw)
        amount-raw (round (- home-value (* (/ down-payment-pct 100) home-value)))
        amount (if (is-valid amount-raw) amount-raw "")]
    (assoc-in db [:borrow-data :amount] amount)))

(defn handle-rate-changed
  [db [_ rate]]
  (assoc-in db [:borrow-data :rate] (sanitize-float rate)))

(defn handle-term-changed
  [db [_ term]]
  (assoc-in db [:borrow-data :term] (sanitize-integer term)))

(defn handle-value-changed
  [db [_ value]]
  (let [home-value (sanitize-integer value)
        db (assoc-in db [:home :value] (sanitize-integer value))
        down-payment-pct (parse-int (get-in db [:borrow-data :down-payment-pct]))
        home-value-parsed (parse-int home-value)
        amount-raw (round (- home-value-parsed (* (/ down-payment-pct 100) home-value-parsed)))
        amount (if (is-valid amount-raw) amount-raw "")]
    (assoc-in db [:borrow-data :amount] amount)))


(defn handle-property-tax-changed
  [db [_ property-tax]]
  (assoc-in db [:home :property-tax] (sanitize-float property-tax)))

(defn handle-hoa-changed
  [db [_ hoa]]
  (assoc-in db [:home :hoa] (sanitize-integer hoa)))

(defn handle-additional-payment-changed
  [db [_ additional-payment]]
  (assoc-in db [:borrow-data :additional-payment] (sanitize-integer additional-payment)))

(defn handle-current-month-changed
  [db [_ current-month]]
  (assoc-in db [:borrow-data :current-month] (sanitize-integer current-month)))

(rf/reg-event-db :amount-changed handle-amount-changed)
(rf/reg-event-db :down-payment-pct-changed handle-down-payment-changed)
(rf/reg-event-db :rate-changed handle-rate-changed)
(rf/reg-event-db :term-changed handle-term-changed)
(rf/reg-event-db :value-changed handle-value-changed)
(rf/reg-event-db :property-tax-changed handle-property-tax-changed)
(rf/reg-event-db :hoa-changed handle-hoa-changed)
(rf/reg-event-db :additional-payment-changed handle-additional-payment-changed)
(rf/reg-event-db :current-month-changed handle-current-month-changed)
