(ns saolsen.steveskeys.btree-test
  (:require [taoensso.nippy :as nippy])
  (:use clojure.test
        saolsen.steveskeys.btree))

(def ks [1 2 3 4 5 6 7 8 9 10])
(def vs ["one" "two" "three" "four" "five" "six" "seven" "eight" "nine" "ten"])

;; Manually built tree for testing the search
(def test-nodes
  {
   :root (->BPlusTreeNode [[(nippy/freeze-to-bytes 2) :a]
                           [(nippy/freeze-to-bytes 5) :b]
                           [(nippy/freeze-to-bytes 9) :c]
                           [nil :d]])
   :a (->BPlusTreeLeaf [[(nippy/freeze-to-bytes 1) "one"]
                        [(nippy/freeze-to-bytes 2) "two"]])
   :b (->BPlusTreeLeaf [[(nippy/freeze-to-bytes 3) "three"]
                        [(nippy/freeze-to-bytes 4) "four"]
                        [(nippy/freeze-to-bytes 5) "five"]])
   :c (->BPlusTreeLeaf [[(nippy/freeze-to-bytes 6) "six"]
                        [(nippy/freeze-to-bytes 7) "seven"]
                        [(nippy/freeze-to-bytes 8) "eight"]
                        [(nippy/freeze-to-bytes 9) "nine"]])
   :d (->BPlusTreeLeaf [[(nippy/freeze-to-bytes 10) "ten"]])
   }
  )

(defn test-they-all-exist
  "checks that all ks are mapped to vs in tree, also checks that if the key
   isn't in the tree it returns nil"
  [tree]
  (doseq [[k v] (map #(vector %1 %2) ks vs)]
    (is (= (get tree (nippy/freeze-to-bytes k)) v)))
  (is (= (get tree (nippy/freeze-to-bytes 11)) nil)))

(deftest test-btree-search
  (testing "Search for a key in a premade tree")
  (let [tree (->PersistantBPlusTree (:root test-nodes) #(get test-nodes %) nil)]
    (test-they-all-exist tree)))

;; Tree that stores the nodes in a clojure map, used to test construction.
(deftest test-btree-buildin
  (testing "Build and search a tree, inserting in a bunch of random orders")
  (dotimes [n 50]
    (let [nextid (ref 0)
          nodes (ref {})
          tree (->PersistantBPlusTree (:root @nodes)
                                      #(get @nodes %)
                                      #(dosync
                                        (let [id (alter nextid inc)]
                                          (alter nodes assoc id %))))]
      (apply (partial assoc tree)
             (flatten (shuffle (map #(vector %1 %2) ks vs))))
      (test-they-all-exist tree))))
