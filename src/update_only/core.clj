(ns update-only.core
  (:use [clojure.string :only (replace-first)]))

(defn is-class? [x]
  (instance? (class x) Class))

(defmulti instantiate is-class?)

(defmethod instantiate true [cls]
  (clojure.lang.Reflector/invokeConstructor
   (resolve (symbol (replace-first (str cls) #".* " "")))
   (to-array [])))

(defmethod instantiate false [x] x)


