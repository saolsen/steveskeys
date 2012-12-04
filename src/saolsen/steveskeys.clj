(ns saolsen.steveskeys
  (:require [taoensso.nippy :as n]
            [saolsen.steveskeys.file :as file]
            [saolsen.steveskeys.btree :as btree]))

;; The keys are stored in a btree to support the traverse function. I'm thinking
;; of also storing the values in a btree to conform to the restraint that each
;; unique value can only be stored once on disk.

(defprotocol PDiskStore
  "key value store persisted to disk"
  (put! [this key value] "associates a key to value")
  (get! [this key option] "gets the value associated to the key")
  (flush! [this] "flushes data to disk")
  (traverse [this start end]
    "returns a range of values from the start to end key"))

;; TODO: store the values in a seperate btree to eliminate duplicates
(defrecord DiskStore [fs keys vals]
  PDiskStore
  (put! [_ key value]
    (let [k (n/freeze-to-bytes key)
          v (n/freeze-to-bytes value)]
      (if-let [val (get @vals v)]
        (swap! keys assoc k val)
        (let [new-val (file/write-node fs v)]
          (swap! vals assoc v new-val)
          (swap! keys assoc k new-val)))
      true))

  (get! [_ key option]
    (let [k (n/freeze-to-bytes key)
          v (get @keys k)
          val (file/read-node fs v)]
      (n/thaw-from-bytes val)))

  (flush! [_]
    (file/commit fs (btree/get-root-loc @keys) (btree/get-root-loc @vals))
    true)

  (traverse [_ start end]
    (let [kvps (btree/traverse @keys
                               (n/freeze-to-bytes start)
                               (n/freeze-to-bytes end))
          get-kv (map #(vector (:key %) (file/read-node fs (:val %))) kvps)
          to-vals (map #(map n/thaw-from-bytes %) get-kv)]
      (reduce #(conj %1 (first %2) (second %2)) [] to-vals)))
)

(defn get-store
  [filename]
  (let [file-store (file/file-manager filename)
        root-loc (file/initialize file-store)
        roots (if (not= {:keys (long 0) :vals (long 0)} root-loc)
                (vector
                 (btree/deserialize (file/read-node
                                     file-store (:keys root-loc)))
                 (btree/deserialize (file/read-node
                                     file-store (:vals root-loc))))
                (vector
                 (btree/->BPlusTreeLeaf [])
                 (btree/->BPlusTreeLeaf [])))
        keys (btree/->PersistantBPlusTree
              (first roots)
              (:keys root-loc)
              #(->> %
                    (file/read-node file-store)
                    (btree/deserialize))
              #(->> %
                    (btree/serialize)
                    (file/write-node file-store))
              32)
        vals (btree/->PersistantBPlusTree
              (second roots)
              (:vals root-loc)
              #(->> %
                    (file/read-node file-store)
                    (btree/deserialize))
              #(->> %
                    (btree/serialize)
                    (file/write-node file-store))
              32)]
    (DiskStore. file-store (atom keys) (atom vals))))

(defn -main [& args]
  (println "steveskeys!"))