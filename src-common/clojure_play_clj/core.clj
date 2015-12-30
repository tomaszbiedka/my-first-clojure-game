(ns clojure-play-clj.core
  (:require [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]
            [play-clj.ui :refer :all]
            [play-clj.math :refer :all])
  (:import (com.badlogic.gdx.scenes.scene2d.utils ClickListener)))

(declare clojure-play-clj-game main-screen error-screen)

(def player-speed 5)
(def fruit-fall-speed 12)
(def fruit-cycles-to-fall 60)
(def fruit-spawn-speed 1.5)

(defn- get-direction []
  (cond
    (key-pressed? :dpad-left) :left
    (key-pressed? :dpad-right) :right))

(defn- update-score-label [screen {:keys [id] :as entity}]
  (if (= id :score-label)
    (doto entity
      (label! :set-text (str (:score screen)))
      (label! :set-x (- (width screen) 40))
      (label! :set-y (- (height screen) 30)))
    entity))

(defn- update-player-position [{:keys [player?] :as entity}]
  (if player?
    (if (get-direction)
      (let [direction (get-direction)
            new-x (case direction
                    :right (min (- (game :width) (:width entity)) (+ (:x entity) player-speed))
                    :left (max 0 (- (:x entity) player-speed)))]
        (when (not= (:direction entity) direction)
          (texture! entity :flip true false))
        (sound! (:player-sound entity) :resume)
        (assoc entity :x new-x :direction direction))
      (do
        (sound! (:player-sound entity) :pause)
        entity))
    entity))

(defn- update-fruit-position [{:keys [fruit?] :as entity}]
  (if fruit?
    (let [new-cycles-to-fall (if (> (:cycles-to-fall entity) 0) (dec (:cycles-to-fall entity)) 0)
          new-y (if (> new-cycles-to-fall 0) (:y entity) (- (:y entity) fruit-fall-speed))]
      (assoc entity :y new-y :cycles-to-fall new-cycles-to-fall))
    entity))

(defn- update-hit-box [{:keys [player? fruit?] :as entity}]
  (if (or player? fruit?)
    (assoc entity :hit-box (rectangle (:x entity) (:y entity) (:width entity) (:height entity)))
    entity))

(defn- remove-touched-fruits [screen entities]
  (if-let [fruits (filter #(contains? % :fruit?) entities)]
    (let [player (some #(when (:player? %) %) entities)
          touched-fruits (filter #(rectangle! (:hit-box player) :overlaps (:hit-box %)) fruits)
          score-update (count touched-fruits)]
      (update! screen :score (+ (:score screen) score-update))
      (remove (set touched-fruits) entities))
    entities))

(defn- remove-fallen-fruits [entities]
  (if-let [fruits (filter #(contains? % :fruit?) entities)]
    (let [fallen-fruits (filter #(< (:y %) 0) fruits)]
      (remove (set fallen-fruits) entities))
    entities))

(defn- update-entities [screen entities]
  (->> entities
       (map (fn [entity]
              (->> entity
                   (update-score-label screen)
                   (update-player-position)
                   (update-fruit-position)
                   (update-hit-box))))
       (remove-touched-fruits screen)
       (remove-fallen-fruits)))

(defn- spawn-fruit []
  (let [x (+ 50 (rand-int 600))
        y (+ 500 (rand-int 30))]
    (assoc (texture "fruit.png") :fruit? true :x x, :y y, :width 50, :height 61 :cycles-to-fall fruit-cycles-to-fall)))

(defscreen main-screen
           :on-show
           (fn [screen entities]
             (update! screen :camera (orthographic) :renderer (stage) :score 0)
             (add-timer! screen :spawn-fruit 1 fruit-spawn-speed)
             (sound "background.mp3" :loop)
             (let [background (assoc (texture "background.png") :x 0 :y 0)
                   player (assoc (texture "horse.png") :player? true :x 50 :y 0 :width 300 :height 187 :direction :left :player-sound (sound "horse.mp3"))
                   score-label (assoc (label "0" (color :blue)) :id :score-label)]
               (label! score-label :set-font-scale 2)
               (sound! (:player-sound player) :loop)
               [background player score-label]))

           :on-render
           (fn [screen entities]
             (clear!)
             (render! screen entities)
             (update-entities screen entities))

           :on-timer
           (fn [screen entities]
             (case (:id screen)
               :spawn-fruit (conj entities (spawn-fruit))))

           :on-pause
           (fn [screen entities]
             (println "on screen pause"))

           :on-resume
           (fn [screen entities]
             (add-timer! screen :spawn-fruit 1 fruit-spawn-speed)
             (println "on screen resume"))

           :on-resize
           (fn [screen entities]
             (height! screen 600)))

(defscreen error-screen
           :on-show
           (fn [screen entities]
             (let [cb (proxy [ClickListener] []
                        (clicked [evt x y] (set-screen! clojure-play-clj-game main-screen)))
                   errorLabel (assoc (label "Runtime error!" (color :white)) :x 10 :y 10)
                   errorButton (assoc (text-button "Restart game" (style :text-button nil nil nil (bitmap-font)) :add-listener cb) :x 350 :y 300)
                   ]
               (update! screen :camera (orthographic) :renderer (stage))
               [errorLabel, errorButton]))

           :on-render
           (fn [screen entities]
             (clear!)
             (render! screen entities))

           :on-resize
           (fn [screen entities]
             (height! screen 600)))

(set-screen-wrapper! (fn [screen screen-fn]
                       (try (screen-fn)
                            (catch Exception e
                              (.printStackTrace e)
                              (set-screen! clojure-play-clj-game error-screen)))))

(defgame clojure-play-clj-game
         :on-create
         (fn [this]
           (set-screen! this main-screen)))
