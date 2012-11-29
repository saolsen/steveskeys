(ns saolsen.steveskeys
  (:require [taoensso.nippy :as n]
            [saolsen.steveskeys.file :as file]
            [saolsen.steveskeys.btree :as btree]))


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

;; TODO: store the values in a seperate btree to eliminate duplicates
(defrecord DiskStore [fs tree]
  IDiskStore
  (put! [_ key value]
    (swap! tree assoc (n/freeze-to-bytes key) (n/freeze-to-bytes value))
    true)

  (get! [_ key option]
    (n/thaw-from-bytes (get @tree (n/freeze-to-bytes key))))

  (flush! [_]
    (file/commit fs (btree/get-root-loc @tree))
    true)

  (traverse [_ start end]
    (let [kvps (btree/traverse @tree
                               (n/freeze-to-bytes start)
                               (n/freeze-to-bytes end))
          get-kv (map #(vector (:key %) (:val %)) kvps)
          to-vals (map #(map n/thaw-from-bytes %) get-kv)]
      (reduce #(conj %1 (first %2) (second %2)) [] to-vals)))
)

(defn get-store
  [filename]
  (let [file-store (file/file-manager filename)
        root-loc (file/initialize file-store)
        root (if (not= 0 root-loc)
               (btree/deserialize (file/read-node file-store root-loc))
               (btree/->BPlusTreeLeaf []))
        tree (btree/->PersistantBPlusTree
              root
              root-loc
              #(->> %
                    (file/read-node file-store)
                    (btree/deserialize))
              #(->> %
                    (btree/serialize)
                    (file/write-node file-store))
              (partial file/read-node file-store)
              (partial file/write-node file-store)
              32)]
    (DiskStore. file-store (atom tree))))
