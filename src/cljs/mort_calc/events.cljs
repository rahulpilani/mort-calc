(ns mort-calc.events
    (:require [re-frame.core :as rf]
              [mort-calc.db :as db]))

(rf/reg-event-db
 :initialize-db
 (fn [_ _]
    db/default-db))

(defn handle-amount-changed
  [db [_ amount]]
  (assoc-in db [:borrow-data :amount] amount))

(defn handle-rate-changed
  [db [_ rate]]
  (assoc-in db [:borrow-data :rate] rate))

(defn handle-term-changed
  [db [_ term]]
  (assoc-in db [:borrow-data :term] term))

(defn handle-value-changed
  [db [_ value]]
  (assoc-in db [:home :value] value))


(defn handle-property-tax-changed
  [db [_ property-tax]]
  (assoc-in db [:home :property-tax] property-tax))

(defn handle-hoa-changed
  [db [_ hoa]]
  (assoc-in db [:home :hoa] hoa))

(rf/reg-event-db :amount-changed handle-amount-changed)
(rf/reg-event-db :rate-changed handle-rate-changed)
(rf/reg-event-db :term-changed handle-term-changed)
(rf/reg-event-db :value-changed handle-value-changed)
(rf/reg-event-db :property-tax-changed handle-property-tax-changed)
(rf/reg-event-db :hoa-changed handle-hoa-changed)
