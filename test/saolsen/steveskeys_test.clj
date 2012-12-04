(ns saolsen.steveskeys-test
  (:use clojure.test
        saolsen.steveskeys))

(deftest test-filestore
  (testing "gotta make sure it all runs smoothly")
  (let [store (get-store "testdata/teststore")]
    (put! store "a" "alpha")
    (put! store "d" "delta")
    (put! store "b" "beta")
    (is (= ["a" "alpha" "b" "beta" "d" "delta"] (traverse store "a" "d")))
    (put! store "c" "alpha")
    (flush! store))
  (let [store2 (get-store "testdata/teststore")]
    (is (= ["a" "alpha" "b" "beta" "c" "alpha" "d" "delta"]
           (traverse store2 "a" "d"))))
  (.delete (java.io.File. "testdata/teststore")))
