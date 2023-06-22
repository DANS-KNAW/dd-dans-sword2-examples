Development
===========

When testing the SWORD2 service on a vagrant box with a self-signed certificate you need to ensure that Java trusts that
certificate, otherwise you will get a security exception when the example programs try to connect to the SWORD2 service.
The `run-deposit.sh` and `run-validation.sh` scripts facilitate this. If a custom keystore is present at `~/.keystore` then
this will be configured to be the trusted keystore for Java. The password for this keystore will be read from the environment
variable KEYSTORE_PASSWORD and will default to 'changeit'.
