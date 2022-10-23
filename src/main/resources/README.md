Example Resources
-----------------

These example resources consist of a number of bags that can be used to become more familiar with the SWORD2 depositing system.
They are sorted based on the type of flow that the user may be configured for, please refer to your DANS account manager to determine which flow your account has been configured for.

The distinction between flows is made because No-Access flow expects the depositor to provide their own DOI in the dataset metadata. Every other flow only allows for a DOI with the DANS prefix to be submitted and that is minted by DANS. If no such DOI is provided in the `metadata/dataset.xml` the service will mint and register a new DOI with the DANS prefix for the dataset.

[Ingest-flow specs]: https://github.com/DANS-KNAW/easy-specs/blob/master/easy-ingest-flow/easy-ingest-flow.md

### Resource descriptions

For both types of flow the following resources are available.

```
flow
├── invalid
│   ├── invalid-flow
│   ├── no-available-date
│   └── two-available-dates
└── valid
    ├── audiences
    ├── embargoed
    ├── file-accessibilities
    ├── inaccessible
    ├── invisible
    ├── multisurface
    ├── revision01
    ├── revision02
    └── revision03
```

A short summary of each of the resources follows:

* invalid: all of the  bags in this directory will be rejected, these can be used to see the error messaging that is provided by the service. They may also be used as simple cases to learn how to modify a bag in a way it is accepted by the client as valid.
  - invalid-flow: This bag is configured for the wrong flow, which means it will be rejected with the message that a DOI is/isn't present contrary to what was expected.
  - no-available date: This bag contains an error in the `bag/metadata/dataset.xml` file. It is missing a `<ddm:available>yyyy-mm-dd</ddm:available>` entry within `<ddm:profile>`.
  - no-available date: This bag contains an error in the `bag/metadata/dataset.xml` file. It has a duplicate entry of `<ddm:available>yyyy-mm-dd</ddm:available>` within `<ddm:profile>`.
* valid: all of the bags in this directory are configured to ingest without any errors. These may be used to see how to make use of different features of the client.
  - audiences: This bag contains multiple `<ddm:audience>` entries, each audience is represented by a code which can be found in the [NARCIS classification].
  - embargoed: This bag makes use of the <ddm:available> field to put all files under embargo until a specified date.
  - file-accessibilities: This dataset can be used to test the different file visibility options that are available, these can be found under `metadata/files.xml`.
  - inaccessible: This dataset can be used to test with all files accessible to nobody, these can be found under `metadata/files.xml`.
  - invisible: This dataset can be used to test with all files visible to nobody, these can be found under `metadata/files.xml`.
  - multisurface: this bag makes use of the http://www.opengis.net/gml xml namespace to allow the user to include polygons which may be used to describe geographical regions. These can be accessed by downloading the dataset metadata after the dataset has been uploaded.
  - revision01-03: These bags are different revisions of the same bag. They may be used to test the sequence deposit classes.


[NARCIS classification]: https://www.narcis.nl/content/pdf/classification_en.pdf

## Modifying The Examples

The SWORD2 client only accepts bags that adhere to the [DANS BagIt profile], it is recommended to make use of the bagit command-line tool to manage and update the different bagit-files which must be present.
This tool can be downloaded through homebrew on MacOS.
Some useful commands provided by this tool are:
```
bagit makecomplete <source> <dest> --payloadmanifestalgorithm SHA1
bagit verifyvalid <bag>
```

The first of these commands will update the payload and tagmanifests, as well as the `bagit.txt` and `bag-info.txt` files if they already exist. If they do not yet exist they will be created. 
The second command can be used to verify that the bag is valid according to the bagit specifications,  this is a fast way to find out if the manifests contain the correct checksums.

It is important to keep in mind that `metadata/files.xml` is not updated automatically by this tool, and must be updated manually if any files are added or removed from the bag.

[DANS BagIt profile]: https://doi.org/10.17026/dans-z52-ybfe

