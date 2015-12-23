(ns update-only.core
  (:use [clojure.string :only (replace-first)])
  (:require [cheshire.core :refer [parse-string]]
            [clojure.reflect :refer [reflect]]
            [clojure.set :as set]))

;; (defn is-class? [x]
;;   (instance? (class x) Class))

;; (defmulti instantiate is-class?)

;; (defmethod instantiate true [cls]
;;   (clojure.lang.Reflector/invokeConstructor
;;    (resolve (symbol (replace-first (str cls) #".* " "")))
;;    (to-array [])))

;; (defmethod instantiate false [x] x)


;; (defn setter-name [property]
;;   (let [x (subs property 0 1)
;;         xs (subs property 1)]
;;     (str "set" (clojure.string/upper-case x) xs)))


;; (defn getter-name [property]
;;   (let [x (subs property 0 1)
;;         xs (subs property 1)]
;;     (str "get" (clojure.string/upper-case x) xs)))


;; (defn fetch-method [reflector method-name]
;;   (first (set/select #(= (:name %) (symbol method-name)) (:members reflector))))


;; (defn get-nested-object [obj reflector property]
;;   (let [cur-val (clojure.lang.Reflector/invokeInstanceMethod obj
;;                                                              (getter-name property)
;;                                                              (to-array []))]
;;     (or cur-val
;;         (let [member-type (:type (fetch-method reflector property))
;;               member (clojure.lang.Reflector/invokeConstructor
;;                       (resolve member-type)
;;                       (to-array []))]))))

;; (declare update-with)

;; (defn set-val [obj reflector prop value]
;;   (let [current-value (clojure.lang.Reflector/invokeInstanceMethod obj
;;                                                                    (getter-name prop)
;;                                                                    (to-array []))]
;;     (cond
;;      (vector? value) nil
     
;;      (map? value)
;;      (let [nested-object (get-nested-object obj reflector prop)]
;;        (update-with nested-object value)
;;        (clojure.lang.Reflector/invokeInstanceMethod obj
;;                                                   (setter-name prop)
;;                                                   (to-array [value])))
;;      :else
;;      (clojure.lang.Reflector/invokeInstanceMethod obj
;;                                                   (setter-name prop)
;;                                                   (to-array [value]))))
;;   obj)


;; (defn update-with [obj hash]
;;   (let [obj (instantiate obj)
;;         r (reflect obj)]
;;     (doseq [[k v] hash]
;;       (set-val obj r k v))
;;     obj))


;; (gen-class
;;  :methods [^:static [updateWith [Object String] Object]])


;; (def ^:private -updateWith update-with)
