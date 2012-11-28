(ns saolsen.steveskeys.file-test
  (:use clojure.test
        saolsen.steveskeys.file))

;; goodfirst has a root node of 0 as the first header and shit for the second
(deftest test-recovering
  (testing "Making sure that it can recover from one of the header being bad")
  (let [goodfirst (file-manager "testdata/goodfirst")
        goodsecond (file-manager "testdata/goodsecond")]
    (is (= 0 (initialize goodfirst)))
    (is (= 1 (initialize goodsecond)))))

(deftest test-persistance
  (testing "Create a couple 'nodes', commit, create a new filemanager and see
            that they are there still")
  (let [node1 [1 2 3 4 5]
        node2 "This is mah node brah"
        node3 {:we "are" :having "fun!"}
        fm (file-manager "testdata/testfile")
        _ (initialize fm)
        loc1 (write-node fm node1)
        loc2 (write-node fm node2)
        _ (is (= node1 (read-node fm loc1)))
        _ (is (= node2 (read-node fm loc2)))
        loc3 (write-node fm node3)
        fm2 (commit fm loc2)
        _ (is (= node3 (read-node fm2 loc3)))
        _ (.close (:raf fm2))
        fm3 (file-manager "testdata/testfile")
        p (initialize fm3)]
    (is (= node2 (read-node fm3 p)))
    (is (= node1 (read-node fm3 loc1)))
    (is (= node3 (read-node fm3 loc3)))
    (.close (:raf fm3))
    (.delete (java.io.File. "testdata/testfile"))
))
