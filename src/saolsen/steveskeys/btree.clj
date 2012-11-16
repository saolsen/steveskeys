(ns saolsen.steveskeys.btree
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
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

;; Persistant B+ tree implementation.
;; An immutable and persistant B+ tree implementation that can be used anywhere
;; immutable structures are required (eg, atoms)
;;
;; Assumptions:
;; - keys and values are byte arrays.
;; - get-node is a referentially transparent function.
;; - (= node (get-node (add-node node)))

;; Each node has a list of key value pairs.
;; {:kvps [[key1 node-id1] [key2 node-id2]] }
(defrecord BPlusTreeNode [kvps])
;; {:kvps [[key1 val1] [key2 val2]] }
(defrecord BPlusTreeLeaf [kvps])

(defn- search-step-reducer
  "reduce function over the key value pairs"
  [search-key result kvp]
  (or result
      (let [[key value] kvp
            c (bcompare search-key key)]
        ;; if the key is in the correct range for this node return the value
        (if (<= c 0)
          value
          nil))))

(defn- search-step
  "returns the value or the next node id, {:value nil} means it's not found"
  [key node]
  (if (instance? BPlusTreeNode node)
    ;; node
    (let [kvps (:kvps node)
          next (reduce (partial search-step-reducer key) nil (butlast kvps))]
      {:next (or next (second (last kvps)))})
    ;; leaf
    {:value (second (first (filter #(bequals key (first %)) (:kvps node))))}))

(deftype PersistantBPlusTree [root get-node add-node]
  clojure.lang.Associative
  ;; assoc
  (assoc [_ key value] {:it 1}) ;woohoo, this has to return an associative!

  ;; get
  (valAt [_ key]
    ;; Recursively search down the tree for the key, returns it's value.
    (loop [node root]
      (let [step (search-step key node)
            next (:next step)]
        (or (:value step)
            (when next (recur (get-node next)))))))
)