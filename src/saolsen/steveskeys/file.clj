(ns saolsen.steveskeys.file
  (:require [taoensso.nippy :as nippy]))

;; File access for steveskeys
;; The top of the file is a 2 part header. The header tells you the index of
;; the root node of the tree, with 0 meaning that there is no root node.
;; 
;; {:root pointer}
;;
;; This when serialized by nippy takes up 23 bytes so the first 46 bytes of the
;; file are the two headers. The second header is written to first. If the
;; program fails during this write the first header will still contain the last
;; flush's root. Then the first header is written to, if the program fails during
;; this write the second header will still be correct.
;; If the program ever crashes on a read or write of a node the headers will
;; still let us recover the root node at time of the last flush so only data
;; since that flush can be lost (which is our guarantee).

;; RandomAccessFile is probably not the best choice (at least for the writer)
;; I plan to split out the writer and the reader.

(defprotocol IFileManager
  "Manages reading and writing to the database file"
  (read-node [this pointer]
    "returns the node at the pointer")
  (write-node [this node]
    "writes the node to the file and returns its location")
  (commit [this root-pointer]
    "writes the headers and flushes output streams, returns a new FileManager")
  (initialize [this]
    "initializes the manager from an existing file, returns the root node"))

(defn write-header-and-close
  "writes the header and closes the RandomAccessFile"
  [raf index header]
  (doto raf
    (.seek index)
    (.write header)
    (.close)))

(defn try-thaw
  [bytes]
  (try
    (nippy/thaw-from-bytes bytes)
    (catch Exception e nil)))

(defn read-headers
  "reads the two headers"
  [raf]
  (let [b1 (byte-array 23)
        b2 (byte-array 23)]
    (doto raf
      (.seek 0)
      (.read b1)
      (.read b2))
    (let [head1 (try-thaw b1)
          head2 (try-thaw b2)]
      {:head1 head1 :head2 head2})))

;; Seperate and control reading and writing for performance.
(defrecord FileManager [filename raf]
  IFileManager
  (read-node [_ pointer]
    (.seek raf pointer)
    (let [b1 (byte-array 11)]
      (.read raf b1)
      (let [size (nippy/thaw-from-bytes b1)
            b2 (byte-array size)]
        (.read raf b2)
        (nippy/thaw-from-bytes b2))))

  (write-node [_ node]
    (let [frozen (nippy/freeze-to-bytes node)
          len (nippy/freeze-to-bytes (long (count frozen)))
          tail (.length raf)]
      (assert (= (count len) 11))
      (doto raf
        (.seek tail)
        (.write len)
        (.write frozen))
      tail))

  (commit [_ root-pointer]
    (let [header {:root (long root-pointer)}
          b (nippy/freeze-to-bytes header)]
      (assert (= (count b) 23))
      (write-header-and-close raf 23 b)
      (write-header-and-close (java.io.RandomAccessFile. filename "rw") 0 b)
      (FileManager. filename (java.io.RandomAccessFile. filename "rw"))))

  (initialize [_]
    (let [{:keys [head1 head2]} (read-headers raf)]
      (if head1
        (if head2
          ;; return the root node of head1
          (:root head1)
          ;; copy head1 to head2, return head1
          (do
            (.seek raf 23)
            (.write raf (nippy/freeze-to-bytes head1))
            (:root head1)))
        (if head2
          ;; copy head2 to head1, return head2
          (do
            (.seek raf 0)
            (.write raf (nippy/freeze-to-bytes head2))
            (:root head2))
          ;; database isn't initialized, write nil heads
          (let [n (nippy/freeze-to-bytes {:root (long 0)})]
            (assert (= (count n) 23))
            (.seek raf 0)
            (.write raf n)
            (.write raf n)
            0)))))
)

(defn file-manager
  [filename]
  (let [f (java.io.File. filename)]
    (when (not (.exists f))
      (.createNewFile f)))
  (let [raf (java.io.RandomAccessFile. filename "rw")]
    (FileManager. filename raf)))