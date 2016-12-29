(ns mort-calc.events
    (:require [re-frame.core :as rf]
              [mort-calc.db :as db]
              [mort-calc.parse-utils :refer [parse-int parse-float]]))

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
  (assoc-in db [:borrow-data :amount] amount))
  ; (let [home-value (parse-int (get-in db [:home :value]))
  ;       clean-amount (sanitize-integer amount)
  ;       int-amount (parse-int clean-amount)
  ;       down-payment-pct (if (and (is-valid home-value) (is-valid int-amount)) (round (* (/ (- home-value int-amount) home-value) 100)) "")]
  ;   (do
  ;     (assoc-in db [:borrow-data :amount] clean-amount)
  ;     (assoc-in db [:borrow-data :down-payment-pct] down-payment-pct))))


(defn handle-down-payment-changed
  [db [_ down-payment-pct]]
  (assoc-in db [:borrow-data :down-payment-pct] (sanitize-integer down-payment-pct)))

(defn handle-rate-changed
  [db [_ rate]]
  (assoc-in db [:borrow-data :rate] (sanitize-float rate)))

(defn handle-term-changed
  [db [_ term]]
  (assoc-in db [:borrow-data :term] (sanitize-integer term)))

(defn handle-value-changed
  [db [_ value]]
  (assoc-in db [:home :value] (sanitize-integer value)))


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
