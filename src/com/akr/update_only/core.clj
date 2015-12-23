(ns com.akr.update-only.core
  (:use [clojure.string :only (replace-first)])
  (:require [cheshire.core :refer [parse-string]]
            [clojure.reflect :refer [reflect resolve-class]]
            [clojure.set :as set]
            [clojure.pprint :refer [pprint]])
  (:import [clojure.lang Reflector])
  (:gen-class :name com.pro.akr.UpdateOnly
              :methods [[updateWith [Object String] Object]]))

(defn is-class? [x]
  (let [result (instance? (class x) Class)]
    (println "x = " x " result = " result)    
    result))

(defmulti instantiate is-class?)

(defmethod instantiate true [cls]
  (println "class name = " (symbol (replace-first (str cls) #".* " "")))
  (Reflector/invokeConstructor
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
            (pprint "Created object: ")
            (pprint (reflect x))
            x)))))
          

(declare update-with)

(defn set-val [obj reflector prop value]
  (let [current-value (Reflector/invokeInstanceMethod obj
                                                                   (getter-name prop)
                                                                   (to-array []))]
    (cond
     (nil? value)
     (do
       (let [setter (setter-name prop)
             setter-meta (fetch-method reflector setter)
             param-type (first (:parameter-types setter-meta))]
         (println "Obj = " obj " Prop = " prop)
         (println "Reflection = " (reflect obj))
         (Reflector/invokeInstanceMethod obj
                                       setter
                                       (to-array [nil]))))

     (vector? value) nil
     
     (map? value)
     (let [nested-object (get-nested-object obj reflector prop)
           setter (setter-name prop)
           setter-meta (fetch-method reflector setter)
           param-type (first (:parameter-types setter-meta))]
       (update-with nested-object value)
       (println "Going to call setter: " setter )
       (Reflector/invokeInstanceMethod obj
                                       setter
                                       (to-array [nested-object])))
     :else
     (let [setter (setter-name prop)
           setter-meta (fetch-method reflector setter)
           param-type (first (:parameter-types setter-meta))
           _ (println "casting " (class value) " -> " param-type)]
       (let [cast-value (if (= param-type (symbol "java.lang.Float"))
                          (do
                            (println "Detected float")
                            (float value))
                          (cast (resolve param-type) value))]
         (println "value = " value " valueClass = " (class value))
         (println "cast-value = " cast-value " castValueClass = " (class value))
         (println "setter = " setter)
         (Reflector/invokeInstanceMethod obj
                                         setter
                                         (to-array [cast-value]))))))
  obj)


(defn update-with [obj hash]
  (println "Input object: " obj)
  (println "class(obj) = " (class obj))
  (let [obj (instantiate obj)
        r (reflect obj)]
    ;; (println "\nReflection:\n")
    ;; (pprint r)
    (println "Request to apply values: " hash)
    (doseq [[k v] hash]
      (println "calling setval " k " = " v)
      (set-val obj r k v))
    obj))

(defn -updateWith [self obj string]
  (let [x (parse-string string)]
    (print "x =")
    (pprint x)
    (update-with obj x)
    (println "Called update-with")))
