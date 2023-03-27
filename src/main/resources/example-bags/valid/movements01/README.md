movements
=========

The bags movements01 and movement02 demonstrate how some special scenarios with combined moves, replacements, additions and
deletions are handled.

Movements01
-----------

* `originals/file1.txt` and `published/file1.txt` have different contents.
* `fileA.txt` and `fileB.txt` have the same contents.
* `multimove1.txt` and `multimove2.txt` have the same contents.

Movements02
-----------

* `originals/file1.txt` overwrites `published/file1.txt`. This is implemented as:
    * `published/file1.txt` is deleted (to make room for `originals/file1.txt`)
    * `originals/file1.txt` is moved to `published/file1.txt`
* `fileA.txt` is replaced with different contents. The fact that `fileB.txt` has the same checksum does not matter, so:
    * `fileA.txt` is replaced with new contents
    * `fileB.txt` is not changed
* Both `multimove1.txt` and `multimove2.txt` are moved from the folder `original` to `published`. Since both have the same
  checksum, it is not possible for the service to determine which is moved where. (It could theoretically use the label for that,
  but it is not hat sophisticated.) Therefore, it will implement this as:
    * Remove both files from `original` and add both files to `published`.
