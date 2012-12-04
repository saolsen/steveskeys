(ns saolsen.steveskeys.file
  (:require [taoensso.nippy :as nippy]))

;; File access for steveskeys
;; The top of the file is a 2 part header. The header tells you the index of
;; the root nodes of the trees, with 0 meaning that there is no root node.
;;
;; {:keys pointer :vals pointer}
;;
;; This when serialized by nippy takes up 36 bytes so the first 72 bytes of the
;; file are the two headers. The second header is written to first. If the
;; program fails during this write the first header will still contain the last
;; flush's root. Then the first header is written to, if the program fails
;; during this write the second header will still be correct.
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
  (commit [this keys-root vals-root]
    "writes the headers and flushes output streams, returns a new FileManager")
  (initialize [this]
    "initializes the manager from an existing file, returns the root node"))

(defn write-header-and-close
  "writes the header and closes the RandomAccessFile"
  [f index header]
  (doto f
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
  [f]
  (let [b1 (byte-array 31)
        b2 (byte-array 31)]
    (doto f
      (.seek 0)
      (.read b1)
      (.read b2))
    (let [head1 (try-thaw b1)
          head2 (try-thaw b2)]
      {:head1 head1 :head2 head2})))

(defrecord FileManager [filename reader writer]
  IFileManager
  (read-node [_ pointer]
    (.seek reader pointer)
    (let [b1 (byte-array 7)]
      (.read reader b1)
      (let [size (nippy/thaw-from-bytes b1)
            b2 (byte-array size)]
        (.read reader b2)
        (nippy/thaw-from-bytes b2))))

  (write-node [_ node]
    (let [frozen (nippy/freeze-to-bytes node)
          len (nippy/freeze-to-bytes (int (count frozen)))
          tail (.length writer)]
      (assert (= (count len) 7))
      (doto writer
        (.seek tail)
        (.write len)
        (.write frozen))
      tail))

  (commit [_ keys-root vals-root]
    (let [header {:keys (int keys-root) :vals (int vals-root)}
          b (nippy/freeze-to-bytes header)]
      (assert (= (count b) 31))
      (.close reader)
      (write-header-and-close writer 31 b)
      (write-header-and-close (java.io.RandomAccessFile. filename "rws") 0 b)
      (FileManager. filename
                    (java.io.RandomAccessFile. filename "rws")
                    (java.io.RandomAccessFile. filename "rws"))))

  (initialize [_]
    (let [{:keys [head1 head2]} (read-headers reader)]
      (if head1
        (if head2
          ;; return the root node of head1
          head1
          ;; copy head1 to head2, return head1
          (do
            (.seek writer 31)
            (.write writer (nippy/freeze-to-bytes head1))
            head1))
        (if head2
          ;; copy head2 to head1, return head2
          (do
            (.seek writer 0)
            (.write writer (nippy/freeze-to-bytes head2))
            head2)
          ;; database isn't initialized, write nil heads
          (let [n (nippy/freeze-to-bytes {:keys (int 0) :vals (int 0)})]
            (assert (= (count n) 31))
            (.seek writer 0)
            (.write writer n)
            (.write writer n)
            {:keys (int 0) :vals (int 0)})))))
)

(defn file-manager
  [filename]
  (let [f (java.io.File. filename)]
    (when (not (.exists f))
      (.createNewFile f)))
  (let [reader (java.io.RandomAccessFile. filename "rws")
        writer (java.io.RandomAccessFile. filename "rws")]
    (FileManager. filename reader writer)))