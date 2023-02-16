Migrating from EASY to a Data Station
=====================================

The Data Station SWORD2 service is mostly compatible with the legacy EASY SWORD2 service. However, there are a few required changes to the client code.

Service URL
-----------
The service URL has changed from `https://easy.dans.knaw.nl/sword2/collection/1` to a Data Station specific URL:

* `https://sword2.archaeology.datastations.nl/collection/1`
* `https://sword2.ssh.datastations.nl/collection/1`

Test deposits must be sent to the demo-server, which is hosted at the "demo" subdomain of the Data Station, e.g.,
`https://demo.sword2.archaeology.datastations.nl/collection/1`. Since this is a test environment, this server is protected with an
extra authentication header which is provided to our customers via a different channel.

DDM schema changes
------------------

### New DANS Dataset Metadata schema and URI

A new version of DANS Dataset Metadata has been introduced. To indicate that it is using the new schema the `dataset.xml` file in the deposit
must use the new namespace: `http://schemas.dans.knaw.nl/dataset/ddm-v2/`. Deposits containing a legacy-version `dataset.xml` will be rejected.

### New profile element: `ddm:personalData`

The `profile` element has a new, required element, that provides a statement about the presence of personal data in the dataset.
It must be added as the last sub-element of `profile`. It has one required attribute `present`, which takes one of the following values:
`Yes`, `No`, `Unknown`.

Example:

```xml

<ddm:personalData present="No"/>
```

### Exactly one `license` element with `xsi:type="dcterms:URI"`

There must be exactly one element with an `xsi:type` attribute set to `dcterms:URI` and an element text containing one of the licenses
supported by the Data Station. The supported licenses can be retrieved from the Data Station API for example:

```bash
curl https://archaeology.datastations.nl/api/licenses | jq '.data[].uri' 
```

The use of [jq](https://stedolan.github.io/jq/){:target=_blank} to extract the URIs from the resulting JSON is optional, of course.


Final deposit state changed to `PUBLISHED`
------------------------------------------

The final state of a deposit in EASY was `ARCHIVED`. This has changed to the state `PUBLISHED`. The meaning is still that the client can stop
tracking the deposit and rest assured that archiving in the DANS Data Vault will occur in due course. 