audiences-sha256
================

A copy of the "audiences" example. The main purpose is to show that instead of SHA-1 it is also allowed to provide a payload manifest with
a stronger hashing algorithm. Note, however, that the Data Station will not store these. After verifying the integrity of the bag after transfer
the SHA-1 checksums will be calculated if not present and the other payload manifests will be discarded.