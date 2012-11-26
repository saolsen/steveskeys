(ns saolsen.steveskeys.file)
;; File access for steveskeys
;; Used to manage a db file

;; File access will be based on that couchdb b+ tree article.

;; When the diskstore is initialized, the file is read from the end. If the
;; first 2k are corrupt then it is replaced by the second footer, if the second
;; footer is corrupt then the first 2k is copied over.

;; What is in the footer, I know it needs the file's length and a checksum to
;; help with checking if it's corrupt. I think that the location of the root
;; node also needs to be written in there.