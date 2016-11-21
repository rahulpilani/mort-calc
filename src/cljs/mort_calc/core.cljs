(ns mort-calc.core
    (:require [reagent.core :as reagent]
              [re-frame.core :as re-frame]
              [mort-calc.events]
              [mort-calc.subs]
              [mort-calc.views :as views]
              [mort-calc.config :as config]))


(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (re-frame/dispatch-sync [:initialize-db])
  (dev-setup)
  (mount-root))
