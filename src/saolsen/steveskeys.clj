(ns saolsen.steveskeys
  (:require [saolsen.steveskeys.file :as file]
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
    (swap! tree assoc key value))

  (get! [_ key option]
    (get @tree key))

  (flush! [_]
    (file/commit fs nil)) ;; NEED TO KNOW ROOT POINTER!

  (traverse [_ start end]
    (btree/traverse @tree start end)))

(defn get-store
  [filename]
  (let [file-store (file/file-manager filename)
        root-loc (file/initialize file-store)
        root (if (not= 0 root-loc)
               (file/read-node file-store root-loc)
               (btree/->BPlusTreeLeaf []))
        tree (btree/->PersistantBPlusTree
              root
              (partial file/read-node file-store)
              (partial file/write-node file-store)
              32)]
    (DiskStore. file-store (atom tree))))
