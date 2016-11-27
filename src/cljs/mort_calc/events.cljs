(ns mort-calc.events
    (:require [re-frame.core :as rf]
              [mort-calc.db :as db]))

(rf/reg-event-db
 :initialize-db
 (fn  [_ _]
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

(rf/reg-event-db :amount-changed handle-amount-changed)
(rf/reg-event-db :rate-changed handle-rate-changed)
(rf/reg-event-db :term-changed handle-term-changed)
