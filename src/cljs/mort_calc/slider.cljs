(ns mort-calc.slider
  (:require
    [re-frame.core :as rf]
    [reagent.core :as r]))

(defn target-value [t]
  (-> t .-target .-value))

(defn dispatch [key]
  (fn [t]
    (rf/dispatch [key (target-value t)])))

(defn node-id [id]
  (str "slider-" (name id)))
;;<input type="text" id="range" value="" name="range" />
(defn slider-render [id event-key]
  (fn []
    (let [sub (rf/subscribe [id])]
      (fn []
        [:input {:type "text" :value @@sub :id (node-id id) :on-change #(dispatch event-key)}]))))

(defn extract-from [data]
  (.-from data))

(defn update-config [sub event-key config]
  (assoc config :from sub :onChange #(rf/dispatch [event-key (extract-from %)])))

(defn slider-did-update [id event-key]
  (let [sub (rf/subscribe [id])]
    (fn [this]
      (let [node (js/$ (r/dom-node this))
            irs (.data node "ionRangeSlider")]
          (.update irs (clj->js {:from @@sub}))))))


(defn slider-did-mount [id event-key config]
  (let [sub (rf/subscribe [id])]
    (fn [this]
      (.ionRangeSlider (js/$ (r/dom-node this)) (clj->js (update-config @@sub event-key config))))))


(defn slider [id event-key config]
  (r/create-class
    {
      :reagent-render (slider-render id event-key)
      :component-did-mount (slider-did-mount id event-key config)
      :component-did-update (slider-did-update id event-key)}))
