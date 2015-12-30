(ns clojure-play-clj.core.desktop-launcher
  (:require [clojure-play-clj.core :refer :all])
  (:import [com.badlogic.gdx.backends.lwjgl LwjglApplication]
           [org.lwjgl.input Keyboard])
  (:gen-class))

(defn -main
  []
  (LwjglApplication. clojure-play-clj-game "clojure-play-clj" 800 600)
  (Keyboard/enableRepeatEvents true))
