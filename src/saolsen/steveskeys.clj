(ns saolsen.steveskeys)
;; This is a diskstore modeled after the requirments of the precog challenge
;; problem. The problem is to create a disk backed key value store that
;; implements some scala traits. This disk backed key value store is instead
;; written in clojure and implements a similar intervace but in a clojure
;; idiomatic way.

;; The functional constraints of this implementation match those of the scala
;; requirments.

;; * The write performance must approach the linear write proformance of the
;;   hard disk as measured by raw Java IO.
;; * Duplicate values cannot be stored more than once. That is, if you add 10000
;;   keys all with the value "John Doe", then the text "John Doe" must be stored
;;   only a single time.
;; * flush! must obey the obvious constraint.
;; * If the program is forcibly terminated at any point during writing to a disk
;;   store, then retrieving the disk store may not fail and must preserve all
;;   all information prior to the most recent call to flush().
;; * Assume the number of unique values (and the number of keys) is too great to
;;  fit in memory.

;; Ways this differs from a scala implementation is that instead of traverse
;; returning a Reader it will return a lazy sequence. This is not meant to be
;; an exact implementation of the precog challenge (as I am not interviewing
;; with them) it is just meant to be a similar artifact that's useable from
;; clojure. (and to be a great systems programming challenge)

;; Get is used in clojure and I didn't want to override it here so even though
;; it doesn't mutate the DiskStore I still named the get method get!. I'm open
;; to better suggestions.

(defprotocol DiskStore
  "key value store persisted to disk, keys can be "
  (put! [this key value] "associates a key to value")
  (get! [this key option] "gets the value associated to the key")
  (flush! [this] "flushes data to disk")
  (traverse [this start end]
    "returns a range of values from the start to end key"))

;; A store that doesn't match any of the functional requirments but should be
;; api complete.
(defrecord NotAStore [map]
  DiskStore
  (put! [_ key value] (swap! map assoc key value))
  (get! [_ key option] (get @map key))
  (flush! [_] nil)
  (traverse [_ start end] nil))

(defn new-notastore [] (NotAStore. (atom {})))

(defn -main [& args] (println "Hello World"))
