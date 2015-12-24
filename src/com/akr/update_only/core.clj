(ns com.akr.update-only.core
  (:use [clojure.string :only (replace-first)])
  (:require [cheshire.core :refer [parse-string]]
            [clojure.reflect :refer [reflect resolve-class]]
            [clojure.set :as set]
            [clojure.pprint :refer [pprint]]
            [clojure.core.match :refer [match]])
  (:import [clojure.lang Reflector]
           [java.lang.reflect Field ParameterizedType])
  (:gen-class :name com.pro.akr.UpdateOnly
              :methods [[updateWith [Object String] Object]]))


(gen-interface
 :name com.pro.akr.Comparator
 :methods [[isSame [Object] Boolean]])


(defn is-class? [x] (instance? (class x) Class))

(defmulti instantiate is-class?)

(defmethod instantiate true [cls]
  (let [member-type (symbol (replace-first (str cls) #".* " ""))
        new-obj-statement `(new ~member-type)]
    (eval new-obj-statement)))

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


(defn construct [klass & args]
  (.newInstance
    (.getConstructor klass (into-array java.lang.Class (map type args)))
    (object-array args)))


(defn get-nested-object [obj reflector property]
  (let [cur-val (Reflector/invokeInstanceMethod obj
                                                (getter-name property)
                                                (to-array []))]
    (println "get-nested-object: cur-val = " cur-val)
    (or cur-val
        (let [member-type (:type (fetch-method reflector property))
              _ (println "Nested object member-type = " member-type " class = " (class member-type))
              ;; _ (pprint reflector)
              ]
          (let [;x (construct (resolve-class (.getContextClassLoader (Thread/currentThread)) member-type))
                abc `(new ~member-type)
                _ (println "Going to eval: " abc)
                x (eval abc)]
            x)))))
          

(declare update-with)

(defn obj->class-name [obj]
  (symbol (replace-first (str (class obj)) #".* " "")))

(defn set-val [obj reflector prop value]
  (println "\nSetval:")
  (let [current-value (Reflector/invokeInstanceMethod obj
                                                      (getter-name prop)
                                                      (to-array []))
        setter (setter-name prop)
        setter-meta (fetch-method reflector setter)
        property-type (first (:parameter-types setter-meta))]
    (println prop " Value: " current-value " -> " value)
    (println prop " Type : " (class value) " -> " property-type)
    (println "class-name "(obj->class-name value))

    (let [final-val
          (match [value (obj->class-name value) property-type]

                 [nil _ _] nil
                 [_ 'clojure.lang.PersistentArrayMap _] (let [nested-object (get-nested-object obj reflector prop)]
                                                          (update-with nested-object value)
                                                          nested-object)
                 [_ 'clojure.lang.PersistentVector _] (let [klass (.. (class obj)
                                                                      (getDeclaredField prop)
                                                                      (getGenericType)
                                                                      (getActualTypeArguments))
                                                            klass (first klass)
                                                            implements-comparator? (instance? com.pro.akr.Comparator (instantiate klass))]

                                                        (let [existing-array (into [] (or current-value []))
                                                              new-array (for [x value
                                                                              :let [obj (instantiate klass)]]
                                                                          (do
                                                                            (update-with obj x)
                                                                             (if implements-comparator?
                                                                               (if-let [matching-element (some (fn [elem]
                                                                                                                 (and (. elem isSame obj)
                                                                                                                      (or (println "updating existing element") true)
                                                                                                                      elem))
                                                                                                               existing-array)]
                                                                                 (do (update-with matching-element x)
                                                                                     matching-element)
                                                                                 obj)
                                                                               obj)))]
                                                          (java.util.ArrayList. new-array)))
                 [_ 'java.lang.Double  'java.lang.Float]    (float value)
                 [_ 'java.lang.Integer 'java.lang.Double]   (double value)
                 [_ 'java.lang.Float   'java.lang.Double]   (double value)
                 [_ 'java.lang.Long    'java.lang.Integer]  (int value)
                 [_ 'java.lang.Integer 'java.lang.Long]     (long value)

                 :else (-> property-type
                           (resolve)
                           (cast value)))]

      (Reflector/invokeInstanceMethod obj
                                      setter
                                      (to-array [final-val])))
    obj))


(defn update-with [obj hash]
  (let [obj (instantiate obj)
        r (reflect obj)]
    (println "Update obj " obj " with: " hash)
    (doseq [[k v] hash]
      (set-val obj r k v))
    obj))

(defn -updateWith [self obj string]
  (with-out-str
    (update-with obj (parse-string string))))
