(ns update-only.core
  (:use [clojure.string :only (replace-first)])
  (:require [cheshire.core :refer [parse-string]]
            [clojure.reflect :refer [reflect]]
            [clojure.set :as set]))

(defn is-class? [x]
  (instance? (class x) Class))

(defmulti instantiate is-class?)

(defmethod instantiate true [cls]
  (clojure.lang.Reflector/invokeConstructor
   (resolve (symbol (replace-first (str cls) #".* " "")))
   (to-array [])))

(defmethod instantiate false [x] x)


(defn setter-name [property]
  (let [x (subs property 0 1)
        xs (subs property 1)]
    (str "set" (clojure.string/upper-case x) xs)))


(defn getter-name [property]
  (let [x (subs property 0 1)
        xs (subs property 1)]
    (str "get" (clojure.string/upper-case x) xs)))


(defn fetch-method [reflector method-name]
  (first (set/select #(= (:name %) (symbol method-name)) (:members reflector))))

(defn first-param [method]
  (first (:parameter-types method)))

(defn get-first-arugument [reflector method-name]
  (first-param (fetch-method reflector method-name)))


(defn get-nested-object [obj property]
  (let [cur-val (clojure.lang.Reflector/invokeInstanceMethod obj
                                                             (getter-name property)
                                                             (to-array []))]
    (or cur-val
        (let [r (reflect obj)
              member-type (:type (fetch-method r property))
              member (clojure.lang.Reflector/invokeConstructor
                      (resolve member-type)
                      (to-array []))]))))

(declare update-with)

(defn set-val [obj prop value]
  (let [current-value (clojure.lang.Reflector/invokeInstanceMethod obj
                                                                   (getter-name prop)
                                                                   (to-array []))]
    (cond
     (vector? value) nil
     
     (map? value)
     (let [nested-object (get-nested-object obj prop)]
       (update-with nested-object value)
       (clojure.lang.Reflector/invokeInstanceMethod obj
                                                  (setter-name prop)
                                                  (to-array [value])))
     :else
     (clojure.lang.Reflector/invokeInstanceMethod obj
                                                  (setter-name prop)
                                                  (to-array [value]))))
  obj)


(defn update-with [obj hash]
  (let [obj (instantiate obj)]
    (doseq [[k v] hash]
      (set-val obj k v))
    obj))
