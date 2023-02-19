restricted-files-with-access-request
====================================

Demonstrates how to set the accessibility of files. This is done by setting the accessibleToRights element in files.xml to one
of the following values:

* ANONYMOUS - can be downloaded without log-in or access request
* RESTRICTED_REQUEST - can be downloaded after a granted access request
* NONE - can never be downloaded; note that access request is a dataset level attribute in Dataverse, so if one or more file in
  the dataset have this permission, files with RESTRICTED_REQUEST are also no longer requestable.