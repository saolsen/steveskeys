(ns saolsen.steveskeys
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:require [taoensso.nippy :as nippy]))

;; Protocol

(defprotocol DiskStore
  "key value store persisted to disk"
  (put! [this key value] "associates a key to value")
  (get! [this key option] "gets the value associated to the key")
  (flush! [this] "flushes data to disk")
  (traverse [this start end]
    "returns a range of values from the start to end key"))

(defn -main [& args] (println "Hello World"))
