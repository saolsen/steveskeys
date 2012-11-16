(ns saolsen.steveskeys
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:require [taoensso.nippy :as nippy]))

;; Protocol

;; The keys are stored in a btree to support the traverse function. I'm thinking
;; of also storing the values in a btree to conform to the restraint that each
;; unique value can only be stored once on disk.

(defprotocol IDiskStore
  "key value store persisted to disk"
  (put! [this key value] "associates a key to value")
  (get! [this key option] "gets the value associated to the key")
  (flush! [this] "flushes data to disk")
  (traverse [this start end]
    "returns a range of values from the start to end key"))

(defrecord DiskStore [keys vals]
  IDiskStore
  (put! [_ key value] ;add value, add key
    ))

(defn -main [& args] (println "Hello World"))
