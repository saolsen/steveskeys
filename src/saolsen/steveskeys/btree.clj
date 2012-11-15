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
;; Assumptions:
;; - keys and values are byte arrays, this is specific for the parameters of
;;   this problem but if this tree implementation is to be used in any other
;;   setting it will need to be genericised.

;; Each node has a list of key value pairs.
;; {:kvps [[key1 node-id1] [key2 node-id2]] }
(defrecord BPlusTreeNode [kvps])
;; {:kvps [[key1 val1] [key2 val2]] }
(defrecord BPlusTreeLeaf [kvps])

(defprotocol IPersistantBPlusTree
  "A persistant B+ tree"
  (getnode [this id]
    "gets a tree node by id")
  (addnode [this value]
    "adds a new node, returns the new node id")
  (search [this key] "searches the tree for the value associated with the key"))

(defn- search-step-reducer
  "reduce function over the key value pairs"
  [search-key result kvp]
  (or result
      (let [[key value] kvp
            c (bcompare search-key key)]
        (if (<= c 0)
          value
          nil))))

(defn- search-step
  "returns the value or the next node id, nil value means it's not found"
  [key node]
  (if (instance? BPlusTreeNode node)
    (let [kvps (:kvps node)
          next (reduce (partial search-step-reducer key) nil (butlast kvps))]
      {:next (or next (second (last kvps)))})
    ;; leaf
    {:value (second (first (filter #(bequals key (first %)) (:kvps node))))}))

;; Getting and setting of nodes, pulling them from the cache etc is not managed
;; here. The tree takes a getter and a setter for retreiving the nodes, this way
;; it can be used independant of how the nodes are stored.
(defrecord PersistantBPlusTree [root get-node add-node]
  IPersistantBPlusTree
  (getnode [_ id] (get-node id))
  (addnode [_ value] (add-node value))
  (search [this key]
    (loop [node root]
      (let [step (search-step key node)
            next (:next step)]
        (or (:value step)
            (when next (recur (getnode this next))))))))
