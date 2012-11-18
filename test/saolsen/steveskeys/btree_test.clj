(ns saolsen.steveskeys.btree-test
  (:require [taoensso.nippy :as nippy])
  (:use clojure.test
        saolsen.steveskeys.btree))

(def ks [1 2 3 4 5 6 7 8 9 10])
(def vs ["one" "two" "three" "four" "five" "six" "seven" "eight" "nine" "ten"])

;; Manually built tree for testing the search
(def test-nodes
  {
   :root (->BPlusTreeNode [{:key (nippy/freeze-to-bytes 2) :val :a}
                           {:key (nippy/freeze-to-bytes 5) :val :b}
                           {:key (nippy/freeze-to-bytes 9) :val :c}
                           {:key nil :val :d}])
   :a (->BPlusTreeLeaf [{:key (nippy/freeze-to-bytes 1) :val :one}
                        {:key (nippy/freeze-to-bytes 2) :val :two}])
   :b (->BPlusTreeLeaf [{:key (nippy/freeze-to-bytes 3) :val :three}
                        {:key (nippy/freeze-to-bytes 4) :val :four}
                        {:key (nippy/freeze-to-bytes 5) :val :five}])
   :c (->BPlusTreeLeaf [{:key (nippy/freeze-to-bytes 6) :val :six}
                        {:key (nippy/freeze-to-bytes 7) :val :seven}
                        {:key (nippy/freeze-to-bytes 8) :val :eight}
                        {:key (nippy/freeze-to-bytes 9) :val :nine}])
   :d (->BPlusTreeLeaf [{:key (nippy/freeze-to-bytes 10) :val :ten}])
   :one "one"
   :two "two"
   :three "three"
   :four "four"
   :five "five"
   :six "six"
   :seven "seven"
   :eight "eight"
   :nine "nine"
   :ten "ten"
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
  (let [tree
        (->PersistantBPlusTree (:root test-nodes) #(get test-nodes %) nil 1)]
    (test-they-all-exist tree)))

;; Tree that stores the nodes in a clojure map, used to test construction.
(deftest test-btree-buildin
  (testing "Build and search a tree, inserting in a bunch of random orders")
  (dotimes [n 50]
    (let [nextid (ref 0)
          nodes (ref {:root (->BPlusTreeLeaf [])})
          tree (->PersistantBPlusTree (:root @nodes)
                                      #(get @nodes %)
                                      #(dosync
                                        (let [id (alter nextid inc)]
                                          (alter nodes assoc id %)
                                          id))
                                      12)
          added
          (apply (partial assoc tree)
                 (flatten (shuffle
                           (map
                            #(vector (nippy/freeze-to-bytes %1) %2) ks vs))))]
      (test-they-all-exist added))))
