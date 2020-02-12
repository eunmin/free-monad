(ns clojure-example.core
  (:require [clojure.algo.monads :refer [defmonad domonad]]))

(defrecord FlatMapped [c f])
(defrecord Suspend [a])
(defrecord Pure [a])

(defmonad free-m
  [m-bind (fn [mv f]
            (->FlatMapped mv f))
   m-result (fn [v]
              (->Pure v))])

(defn lift-m [fa]
  (->Suspend fa))

(defn fold-map [nt fm]
  (loop [nt nt
         fm fm]
    (condp instance? fm
      Pure (:a fm)
      Suspend (nt (:a fm))
      FlatMapped (let [inner (:c fm)
                       f (:f fm)]
                   ;; TODO: fold-map to tailrecur
                   (recur nt (f (fold-map nt inner)))))))

(defrecord Put [key value])
(defrecord Get [key])
(defrecord Delete [key])

(defn put' [key value]
  (lift-m (->Put key value)))

(defn get' [key]
  (lift-m (->Get key)))

(defn delete' [key]
  (lift-m (->Delete key)))

(def program (domonad free-m
                      [_ (put' "wild-cats" 2)
                       _ (put' "tame-cats" 5)
                       n (get' "wild-cats")
                       _ (delete' "tame-cats")]
                      n))

(def m (atom {}))

(defn compiler [cmd]
  (condp instance? cmd
    Get
    (let [{:keys [key]} cmd]
      (println ":get" cmd)
      (get @m key))
    Put
    (let [{:keys [key value]} cmd]
      (println ":put" cmd)
      (swap! m #(assoc % key value)))
    Delete
    (let [{:keys [key value]} cmd]
      (swap! m #(dissoc % key))
      (println ":delete" cmd))))

(fold-map compiler program)
