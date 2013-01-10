# steveskeys [![Build Status](https://travis-ci.org/saolsen/steveskeys.png?branch=master)](https://travis-ci.org/saolsen/steveskeys)

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
* Both the values and the index are stored in b+trees. The b+tree I have implemented
  is persistant and immutable. Since the tree nodes are immutable, having a valid root
  node implies having a valid tree. When something new is added to a tree the
  nodes from the node it's inserted to up to the root are all expired and new
  nodes are created. The new root is then swapped into the root atom. Writes
  do not block or change reads so even if a read were to take a long time and
  the root changed before it's done the version of the tree the read is operating
  on is still safe and unchanged. This allows reads and writes to happen without blocking
  eachother and without any locking. (persistant data structures are awesome)
* The nodes themselves are serialized and stored in a file. First the length of the serialized
  node is written to the file (this is an int that serializes to 7 bytes) then the byte data of
  the node is written. The location in the file is returned (and stored in other nodes as pointers)
  When a node is read from the file first 7 bytes are read from the location to get the size and then
  that many bytes are read and deserialized to get the node. The reader and the writer each use a different
  file object so the random access jumping around of reads does not hinder the high throughput that linear
  writes to the end of the file has.
* When flush is called the locations of the root nodes for the two btrees is written to the top of the file twice.
  It is written once, the writer is flushed to disk, then it is written again just below that and the writer is flushed.
  This allows us to recover the file if there is a failure on either of those writes. If the program crashes during
  the first write, the second one is still valid as of the time of the prevous flush. If it crashes during the second write
  the first one was already done and we can recover from it. This way all data as of the last successful flush is acounted for.

## License

Copyright © 2012 Stephen Olsen

Distributed under the Eclipse Public License, the same as Clojure.
