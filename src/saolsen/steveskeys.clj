(ns saolsen.steveskeys
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:require [taoensso.nippy :as nippy])
  (:import java.util.Arrays
           com.google.common.primitives.UnsignedBytes))

;; Helpers for working with byte arrays.

(defn bequals
  "Equality operator for byte arrays"
  [a b]
  (java.util.Arrays/equals a b))

;; If somebody has a pure java implementation of this I'd love to not include
;; guava.
(def byte-array-comparator
  (com.google.common.primitives.UnsignedBytes/lexicographicalComparator))

(defn bcompare
  "Comparison operator for byte arrays"
  [a b]
  (.compare byte-array-comparator a b))

;; Protocols

(defprotocol DiskStore
  "key value store persisted to disk"
  (put! [this key value] "associates a key to value")
  (get! [this key option] "gets the value associated to the key")
  (flush! [this] "flushes data to disk")
  (traverse [this start end]
    "returns a range of values from the start to end key"))

;; A store that doesn't match any of the functional requirments but should be
;; api complete.
(defrecord NotAStore [map]
  DiskStore
  (put! [_ key value] (swap! map assoc (str key) (str value)))
  (get! [_ key option] (read-string (get @map (str key))))
  (flush! [_] nil)
  (traverse [_ start end] nil))

(defn new-notastore [] (NotAStore. (atom {})))

(defn -main [& args] (println "Hello World"))
