(ns saolsen.steveskeys.btree-test
  (:require [taoensso.nippy :as nippy])
  (:use clojure.test
        saolsen.steveskeys.btree))

(def nodes
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

(deftest test-btree-search
  (testing "Search for a key")
  (let [tree (->PersistantBPlusTree (:root nodes) #(get nodes %) #(%))]
    (is (= (get tree (nippy/freeze-to-bytes 1)) "one"))
    (is (= (get tree (nippy/freeze-to-bytes 2)) "two"))
    (is (= (get tree (nippy/freeze-to-bytes 3)) "three"))
    (is (= (get tree (nippy/freeze-to-bytes 4)) "four"))
    (is (= (get tree (nippy/freeze-to-bytes 5)) "five"))
    (is (= (get tree (nippy/freeze-to-bytes 6)) "six"))
    (is (= (get tree (nippy/freeze-to-bytes 7)) "seven"))
    (is (= (get tree (nippy/freeze-to-bytes 8)) "eight"))
    (is (= (get tree (nippy/freeze-to-bytes 9)) "nine"))
    (is (= (get tree (nippy/freeze-to-bytes 10)) "ten"))
    (is (= (get tree (nippy/freeze-to-bytes 11)) nil))
))

;(deftest test-btree-search
;  (let [nodes {:1 (bplus-tree-node [[(nippy/freeze-to-bytes "1") :2]])}])
