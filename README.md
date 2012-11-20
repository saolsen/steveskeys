note: I think I am at 20 hours of work right now.

# steveskeys

http://precog.com/blog-precog-2/entry/do-you-have-what-it-takes-to-be-a-precog-engineer

Key value storage implementation based on the precog challenge problem
but in clojure instead of scala.

## Requirments

This is a diskstore modeled after the requirments of the precog
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
  all information prior to the most recent call to flush().
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
  
## Other Info

This is really just a toy example and not a useful
library. Specifically ordering by the binary representation of the
values isn't something anyone would want. I may in the future continue
work on this and add some indexes based on the clojure values that
would be much more likely to benefit someone looking to use this.

## License

Copyright © 2012 FIXME

Distributed under the Eclipse Public License, the same as Clojure.
