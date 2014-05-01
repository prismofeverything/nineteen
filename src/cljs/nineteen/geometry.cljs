(ns nineteen.geometry
  (:require [clojure.set :as set]))

(def sqrt-three-over-two (/ (Math/sqrt 3.0) 2))

(defn object
  [m]
  (let [out (js-obj)]
    (doall (map #(aset out (name (first %)) (second %)) m))
    out))

(defn random-color
  []
  (let [base (str (.toString (js/Math.random) 16) "000000")]
    (str "#" (.slice base 2 8))))

(defn set-at
  [sphere [x y z]]
  (.set (.-position sphere) x y z))

(defn set-scale
  [sphere [x y z]]
  (.set (.-scale sphere) x y z))

(defn make-sphere
  ([at color radius] (make-sphere at color radius (js/THREE.SphereGeometry. radius 16 16)))
  ([at color radius geometry] (make-sphere at color radius geometry {}))
  ([at color radius geometry options]
     (let [material (js/THREE.MeshPhongMaterial. (object (merge {:color (or color 0xff1493)} options)))
           sphere (js/THREE.Mesh. geometry material)]
       (set-at sphere at)
       sphere)))

(defn subtract-mesh
  [a b]
  (let [a-bsp (js/ThreeBSP. a)
        b-bsp (js/ThreeBSP. b)
        operate (.subtract a-bsp b-bsp)
        remaining (.toMesh operate (.-material a))]
    (.computeFaceNormals (.-geometry remaining))
    (.computeVertexNormals (.-geometry remaining))
    (set! (.-normalsNeedUpdate (.-geometry remaining)) true)
    remaining))

(defn subtract-all
  [mesh subtractions]
  (reduce subtract-mesh mesh subtractions))

(defn key-geometry
  [width height depth]
  (js/THREE.CubeGeometry. width height depth))

(defn cloak-geometry
  ([geometry color] (cloak-geometry geometry color {}))
  ([geometry color options]
     (let [material (js/THREE.MeshPhongMaterial. (object (merge {:color color} options)))
           mesh (js/THREE.Mesh. geometry material)]
       mesh)))

(def white 0xffffff)
(def black 0x333333)
(def grey 0x999999)

(defn make-white-keys
  [start width height depth subtractions]
  (map
   (fn [n]
     (let [geometry (key-geometry width height depth)
           key (cloak-geometry geometry white)
           bottom (* 0.5 height)
           back (* -0.5 depth)]
       (set-at key [(+ start (* width n)) bottom back])
       (subtract-all key subtractions)))
   (range 7)))

(defn make-black-keys
  [start width height depth full-width full-height full-depth subtractions]
  (map
   (fn [n]
     (let [geometry (key-geometry width height depth)
           key (cloak-geometry geometry black)
           bottom (- (* 0.5 full-height) (* 0.5 (- full-height height)))
           back (- (* -0.5 full-depth) (* 0.5 (- full-depth depth)))]
       (set-at key [(+ start (* full-width (+ n 0.5))) bottom back])
       (subtract-all key subtractions)))
   [0 1 3 4 5]))

(defn make-grey-keys
  [start width height depth full-width full-height full-depth subtractions]
  (map
   (fn [n]
     (let [geometry (key-geometry width height depth)
           key (cloak-geometry geometry grey)
           bottom (- (* 0.5 full-height) (* 0.5 (- full-height height)))
           back (- (* -0.5 full-depth) (* 0.5 (- full-depth depth)))]
       (set-at key [(+ start (* full-width (+ n 0.3))) bottom back])
       (subtract-all key subtractions)))
   (range 7)))

(defn make-octave
  [start]
  (let [key-width 2
        key-height 1
        key-depth 15
        grey-keys (make-grey-keys start 0.8 2.3 11 key-width key-height key-depth [])
        black-keys (make-black-keys start 1.1 1.7 13 key-width key-height key-depth grey-keys)
        white-keys (make-white-keys start key-width key-height key-depth (concat grey-keys black-keys))
        all-keys (concat white-keys black-keys grey-keys)]
    ;; (doseq [key all-keys]
    ;;   (set-scale key [0.5 0.5 0.5]))
    all-keys))

(defn build-keyboard
  []
  (let [octaves (concat (make-octave -21) (make-octave -7) (make-octave 7))
        keyboard (js/THREE.Object3D.)]
    (doseq [key octaves]
      (.add keyboard key))
    keyboard))
