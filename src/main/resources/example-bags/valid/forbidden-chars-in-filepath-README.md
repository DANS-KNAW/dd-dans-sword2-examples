forbidden-chars-in-filepath.zip
===============================

`forbidden-chars-in-filepath.zip` is zipped because some of the characters forbidden by Dataverse are also forbidden by some file systems, which would make it
impossible to clone this repository on such a file system. If your file system does support these characters, you can unzip the file and use the unzipped
version.

## Rules

### label (file name)

The file name must not contain any of the following characters:

* colon `:`
* asterisk `*`
* question mark `?`
* double quote `"`
* lower than `<`
* greater than `>`
* pipe `|`
* semi-colon `;`
* hash `#`

### directoryLabel (path)

The path can contain **only the following characters**:

* alphanumeric characters `(a-z, A-Z, 0-9)`
* underscore `_`
* hyphen `-`
* dot `.`
* backslash `\`
* forward slash `/`
* space ` `


