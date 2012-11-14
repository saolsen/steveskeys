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

;; Protocol

(defprotocol DiskStore
  "key value store persisted to disk"
  (put! [this key value] "associates a key to value")
  (get! [this key option] "gets the value associated to the key")
  (flush! [this] "flushes data to disk")
  (traverse [this start end]
    "returns a range of values from the start to end key"))

;; Testing

;; A store that doesn't match any of the functional requirments but should be
;; api complete,
;; TODO: The keys must be ordered.

;; Shit, can't even use a map because it doesn't use the equlity operator above.
(defrecord NotAStore [map]
  DiskStore
  (put! [_ key value] (swap! map assoc
                             (nippy/freeze-to-bytes key)
                             (nippy/freeze-to-bytes value)))
  (get! [_ key option] (nippy/thaw-from-bytes
                        (get @map (nippy/freeze-to-bytes key))))
  (flush! [_] nil)
  (traverse [_ start end]
    (let [state @map
          ks (keys state)
          sorted (sort bcompare ks)
          bstart (nippy/freeze-to-bytes start)
          bend (nippy/freeze-to-bytes end)
          sindex (.indexOf sorted bstart)
          eindex (.indexOf sorted bend)]
      (assert (not= sindex -1) "No value exists for start key.")
      (assert (not= eindex -1) "No value exists for end key.")
      (map #(get state (nth % sorted)) (range sindex (+ 1 eindex))))))

(defn new-notastore []
  (NotAStore. (atom {})))

(defn -main [& args] (println "Hello World"))
