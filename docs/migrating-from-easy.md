Migrating from EASY to a Data Station or Vault as a Service
===========================================================

The Data Station SWORD2 service is mostly compatible with the legacy EASY SWORD2 service. However, a couple of things
have changed that will require a
change to the client code or configuration. Also, there are some added features. What follows is a list of all the
changes. Optional ones are marked as
such.

!!! note "Data Station vs Vault as a Service"

    Clients can deposit to either a Data Station or the Vault as a Service (Vaas). The protocol is largely the same. 
    The differences are highlighted in the text with the notes **(VaaS)** and **(Data Station)**.


Service URL
-----------

The service URL has changed from `https://easy.dans.knaw.nl/sword2/collection/1` to a Data Station specific URL:

* `https://sword2.archaeology.datastations.nl/collection/1`
* `https://sword2.ssh.datastations.nl/collection/1`
* `https://sword2.lifesciences.datastations.nl/collection/1`
* `https://sword2.phys-techsciences.datastations.nl/collection/1`

For the Vault as a Service the URL is:

* `https://sword2.vault.datastations.nl/collection/<client_name>`

The client_name is provided by DANS and is unique for each client.

Test deposits must be sent to the demo-server, which is hosted at the "demo" subdomain of the Data Station, e.g.,
`https://demo.sword2.archaeology.datastations.nl/collection/1`. Since this is a test environment, this server is
protected with an extra authentication header which is provided to our customers via a different channel.

DDM schema changes
------------------

### New DANS Dataset Metadata schema and URI

A new version of DANS Dataset Metadata has been introduced. To indicate that it is using the new schema
the `dataset.xml` file in the deposit must use the new namespace: `http://schemas.dans.knaw.nl/dataset/ddm-v2/`.
Deposits containing a legacy-version `dataset.xml` will be rejected.

### New profile element: `ddm:personalData`

The `profile` element has a new, required element, that provides a statement about the presence of personal data in the
dataset. It must be added as the last sub-element of `profile`. It has one required attribute `present`, which takes one
of the following values: `Yes`, `No`, `Unknown`.

Example:

```xml

<ddm:profile>
    <!-- other profile elements -->
    <ddm:personalData present="No"/>
</ddm:profile>
```

### Exactly one `license` element with `xsi:type="dcterms:URI"`

There must be exactly one element with an `xsi:type` attribute set to `dcterms:URI`.

#### Supported licenses **(Data Station)**

The element text containing the URI of one of the licenses supported by the Data Station. The supported licenses can be
retrieved from the Data Station API for example:

```bash
curl https://archaeology.datastations.nl/api/licenses | jq '.data[].uri' 
```

The use of [jq](https://stedolan.github.io/jq/){:target=_blank} to extract the URIs from the resulting JSON is optional,
of course.

Note, that in EASY SWORD2 some deviations from the license URI were allowed (for example both `http` and `https` as URI
scheme were accepted). In Data Station SWORD2 you must provide the license URI identical character by character as in
the list of supported licenses.

#### Supported licenses **(VaaS)**

The Vault as a Service only requires a license to be specified. It does not have a list of supported licenses, so any
license URI is accepted.

### New element `ddm:datesOfCollection` (optional)

The `ddm:dcmiMetadata` section of DDM supports a new element `ddm:datesOfCollection` that allows you to specify the
start and end dates for the date of collection. This will then be mapped to the Dataverse Citation Metadata field "Date
of Collection" which is a structured field with Start and End subfields.

In `ddm:datesOfCollection` the start and end dates must be entered in the element text, separated by a slash. The dates
must be formatted as YYYY-MM-DD

Example:

```xml

<ddm:dcmiMetadata>
    <!-- other elements -->
    <ddm:datesOfCollection>2018-01-02/2018-05-01</ddm:datesOfCollection>
    <!-- other elements -->
</ddm:dcmiMetadata>
```

### New element `ddm:language` (optional)

The `ddm:dcmiMetadata` section of DDM supports a new element `ddm:language` that allows you to specify the language of the
dataset in the attribute `code`. This will then be mapped to the Dataverse Citation Metadata field "Language". The supported
encoding schemes are `ISO639-1` and `ISO639-2`; which one is used must be specified in the attribute `encodingScheme`. For
the list of supported codes see the wikipedia page on [ISO 639]{:target=_blank}.

Note, that the element text can be specified, but is not checked against the code, nor is it mapped to any Dataverse field.

Example:

```xml

<ddm:dcmiMetadata>
    <!-- other elements -->
    <ddm:language encodingScheme="ISO639-1" code="fy">West-Fries</ddm:language>
    <ddm:language encodingScheme="ISO639-2" code="kal">Groenlands</ddm:language>
    <ddm:language encodingScheme="ISO639-2" code="baq">Baskisch</ddm:language>
    <!-- other elements -->
</ddm:dcmiMetadata>  
```

!!! warn "dc:language not mapped (Data Station)"

    In EASY SWORD2 the element `dc:language` was mapped to the EASY metadata field "Language" for a very limited set of
    languages. This is no longer the case. If you want to specify the language of the dataset, you must use the new
    element `ddm:language`.

[ISO 639]: https://en.wikipedia.org/wiki/List_of_ISO_639_language_codes



File and directory names are sanitized (no action required) **(Data Station only)**
----------------------------------------------------------------------------------

Dataverse has specific rules about what characters it allows in file names and directory names.

* file names: all allowed **except**: `:`, `*`, `?`, `"`, `<`, `>`, `|`, `;`, `#`;
* directory names: **only the following allowed**: alphanumeric characters from ASCII, `_`, `-`, `.`, `\`, ` ` (space)

All forbidden characters are replaced with an underscore by Ingest Flow, so it is not necessary for the client to change
any file or directory names. The original file and directory name are recorded in the file's description metadata in
Dataverse, e.g. `original_filepath: path/with/strænge/<chars>`

Organizational identifier (optional)
------------------------------------

The client now has the option to provide an organizational identifier. The Depositor Organization most likely assign its
own identifier to its datasets. It is **highly recommended** to store this identifier with the dataset deposited in the
Data Station, so that it can be more easily correlated with the information in the client's repository or database. This
is done using two elements in `bag-info.txt`: `Has-Organizational-Identifier` and, if
available, `Has-Organizational-Identifier-Version`. For example:

```text
Has-Organizational-Identifier: REPO1:1234
Has-Organizational-Identifier-Version: 1
```

`REPO1` is an organization specific prefix that must be agreed on with DANS beforehand. In this case `1234` would be the
unique identifier that the REPO1 organization uses to reference the corresponding dataset. The version has no
constraints and can be left out, if no such information is kept by the Depositor Organization.

See also [DANS BagIt Profile, Section 4]{:target=_blank}.


Final deposit state changed to `PUBLISHED` **(Data Station)** and `ACCEPTED` **(VaaS)**
---------------------------------------------------------------------------------------

The final state of a deposit in EASY was `ARCHIVED`. This has changed to the state `PUBLISHED` for the Data Stations and
`ACCEPTED` for the Vault as a Service. The meaning is still that the client can stop tracking the deposit and rest
assured that archiving in the [DANS Data Vault]{:target=_blank} will occur in due course.

Original metadata is stored as `original-metadata.zip` **(Data Station only)**
------------------------------------------------------------------------------

The DDM schema allows you to include any DCTERMS element you wish in the section `ddm:dcmiMetadata` section. However,
not everything is mapped to Dataverse metadata. To ensure that the originally deposited metadata is not lost, it is
saved in an uncompressed ZIP-file in the dataset, called `original-metadata.zip`. It includes `dataset.xml`
and `files.xml`.

Files outside the `data` folder are not archived
------------------------------------------------

The EASY SWORD2 Service stored any files it did not process in the archive "as is". The Data Station SWORD2 Service will
not store any files outside the `data` folder of the bag (except for the original metadata: see previous section).

[DANS Data Vault]: {{ dans_data_vault }}
[DANS BagIt Profile, Section 4]: {{ dans_bag_profile_section_4 }}
