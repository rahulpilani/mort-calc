(ns mort-calc.dropdown
  (:require
    [re-frame.core :as rf]
    [reagent.core :as r]))

(defn dispatch [key]
  (fn [t]
    (rf/dispatch [key t])))

(defn dropdown-markup [id list-id place-holder event-key]
  (let [sub (rf/subscribe [id])
        list (rf/subscribe [list-id])]
    (fn []
      [:div.ui.dropdown.selection.fluid
        [:input {:type "hidden" :name id :value @sub}]
        [:i.dropdown.icon]
        [:div.default.text place-holder]
        [:div.menu
          (for [[id text] @list]
            ^{:key id}[:div.item {:data-value id} text])]])))

(defn dropdown-did-mount [id event-key]
  (let [sub (rf/subscribe [id])]
    (fn [this]
      (.dropdown (js/$ (r/dom-node this)) (clj->js {:onChange (dispatch event-key)})))))

(defn dropdown-inner [id list-id place-holder event-key]
  (r/create-class
    {
      :reagent-render (dropdown-markup id list-id place-holder event-key)
      :component-did-mount (dropdown-did-mount id event-key)}))

(defn dropdown [id list-id label place-holder event-key]
  [:div.field
    [:label label]
    [dropdown-inner id list-id place-holder event-key]])
