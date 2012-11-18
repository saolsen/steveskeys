(ns saolsen.steveskeys.btree
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]]
        [clojure.math.numeric-tower])
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

(defprotocol IReplace
  "protocol for nodes to change correctly"
  (add-kvps [node new-kvps] "adds or replaces one or more key value pairs")
  (split [node] "splits the node in two")
  (greatest-key [node] "greatest key"))

;; Each node has a list of key value pairs.
;; {:kvps [{:key key1 :val node-id1} {:key key2 :val node-id2}] }
(defrecord BPlusTreeNode [kvps]
  IReplace
  (add-kvps [_ new-kvps]
    (let [new-keys (map :key new-kvps)
          old (filter #(not (or (bequals (:key %) (first new-keys))
                                (bequals (:key %) (second new-keys))))
                      kvps)
          nil-replace? (not (and new-keys))
          new (sort-by #(:key %) bcompare (into (butlast old) new-kvps))]
      (if nil-replace?
        (BPlusTreeNode. new)
        (BPlusTreeNode. (conj new (last kvps))))))
  (split [_]
    (let [s (count kvps)
          half (floor (/ s 2))]
      [(BPlusTreeNode. (take half kvps))
       (BPlusTreeNode. (drop half kvps))]))
  (greatest-key [_] (or (:key (last kvps))
                        (:key (last (butlast kvps))))))

;; {:kvps [[key1 val1] [key2 val2]] }
(defrecord BPlusTreeLeaf [kvps]
  IReplace
  (add-kvps [_ new-kvps]
    (let [new-keys (map :key new-kvps)
          old (filter #(not (or (bequals (:key %) (first new-keys))
                                (bequals (:key %) (second new-keys))))
                      kvps)
          new (sort-by #(:key %) bcompare (into old new-kvps))]
      (BPlusTreeLeaf. new)))
  (split [_]
    (let [s (count kvps)
          half (floor (/ s 2))]
      [(BPlusTreeLeaf. (take half kvps))
       (BPlusTreeLeaf. (drop half kvps))]))
  (greatest-key [_] (:key (last kvps))))

(defn- search-step-reducer
  "reduce function over the key value pairs"
  [search-key result kvp]
  (or result
      (let [{:keys [key val]} kvp
            c (bcompare search-key key)]
        ;; if the key is in the correct range for this node return the value
        (if (<= c 0)
          kvp
          nil))))

(defn- search-step
  "returns the next node id"
  [key node]
  (let [kvps (:kvps node)
        next (reduce (partial search-step-reducer key) nil (butlast kvps))]
    (or next
        (last kvps))))

(defn- path-to-leaf
  "returns the path to the leaf node that the key would go (or be) in, as well
   as the leaf node"
  [key root get-node-fn]
    (loop [node root
           path []]
      (let [next (search-step key node)
            next-node (get-node-fn (:val next))
            next-path (conj path {:node node :key (:key next)})]
        (if (instance? BPlusTreeLeaf next-node)
          {:path next-path :node next-node}
          (recur next-node next-path)))))

(deftype PersistantBPlusTree [root get-node-or-record add-node-or-record bf]
  clojure.lang.Associative
  ;; assoc
  (assoc [_ key value]
    (let [new-record-id (add-node-or-record value)
          {:keys [path node]} (path-to-leaf key root get-node-or-record)
;          new-leaf (add-nodes node [{:key key :val new-record-id}])
;          leafs (if )
          nodes (reverse path)]
      (debug "new record: " new-record-id)
      (debug "path: " path)
      (debug "node: " node)
      (loop [kvps [{:key key :val new-record-id}]
             n {:node node :key key}
             remaining nodes]
        (debug kvps n remaining)
        (let [new-node (add-kvps (:node n) kvps)
              new-nodes (sort-by greatest-key bcompare
                                 (if (> (count (:kvps new-node)) bf)
                                   (split new-node)
                                   [new-node]))
              ids (map add-node-or-record new-nodes)
              new-kvps (if (= (count ids) 1)
                         [{:key (:key n)
                          :val (first ids)}]
                         [{:key (greatest-key (first new-nodes))
                           :val (first ids)}
                          {:key (:key n)
                           :val (second ids)}])]
          (if remaining
            (recur new-kvps (first remaining) (next remaining))
            (if (= (count new-kvps) 1)
              (PersistantBPlusTree. (:val (first new-kvps))
                                    get-node-or-record add-node-or-record bf)
              (recur new-kvps {:node (BPlusTreeNode. []) :key nil} nil)))))))

  ;; get
  (valAt [_ key]
    ;; Recursively search down the tree for the key, returns it's value.
    (let [search (path-to-leaf key root get-node-or-record)
          node (:node search)]
      (get-node-or-record
       (:val (first (filter #(bequals key (:key %)) (:kvps node))))))
))
