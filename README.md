# steveskeys

File backed key value store, based on the precog challenge.

http://precog.com/blog-precog-2/entry/do-you-have-what-it-takes-to-be-a-precog-engineer

## Useage

Steveskeys is available on clojars, simply add it as a dependency in
project.clj

```clojure
[steveskeys "0.1.0"]
```

Example Use

```clojure
(use 'saolsen.steveskeys)

;; create a store with get-store
(def store (get-store "filename"))
(put! store "a" "apple")
(put! store "b" "baby")
(put! store "c" {:see ["works" "for" "any" "clojure" "values"]})
(get! store "b" nil)
;; => "baby"

(traverse store "a" "c")
;; => ["a" "apple" "b" "baby" "c" {:see ["you" "can" "use" "any" "clojure" "datatype"]}]

;; flush! guarantees data is saved to disk
(flush! store)

;; then, later or in another session if you open a store with the same
;; filename the data will still be there
(def store2 (get-store "filename"))
(get! store2 "a" nil)
;; => "apple"
```

## Requirments

This is a key value store modeled after the requirments of the precog
challenge problem. The problem is to create a disk backed key value
store that implements some scala traits. This disk backed key value
store is instead written in clojure and implements a similar interface
but in a clojure idiomatic way.

The functional constraints of this implementation match the scala
requirments.

* The write performance must approach the linear write performance of the
  hard disk as measured by raw Java IO.
* Duplicate values cannot be stored more than once. That is, if you add 10000
  keys all with the value "John Doe", then the text "John Doe" must be stored
  only a single time.
* flush! must obey the obvious constraint.
* If the program is forcibly terminated at any point during writing to a disk
  store, then retrieving the disk store may not fail and must preserve all
  information prior to the most recent call to flush().
* Assume the number of unique values (and the number of keys) is too
  great to fit in memory.

Ways this differs from a scala requirments.

* instead of traverse returning a Reader it will return a lazy
  sequence.
* get is used in clojure and I didn't want to override it here so even
  though it doesn't mutate the DiskStore I still named the get method
  get!. I'm open to better suggestions.

## Implementation Details

* I'm using nippy for serialization of clojure data structures to byte
  arrays so anything serializable by nippy is supported as a key or
  value.
* I'm using google guava's byte-array comparator because java doesn't
  have one built in.

## License

Copyright © 2012 Stephen Olsen

Distributed under the Eclipse Public License, the same as Clojure.
