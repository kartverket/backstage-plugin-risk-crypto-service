# Crypto-service

The crypto service is used to encrypt and decrypt sops-files. You can read more about
sops [here](https://github.com/getsops/sops).

The current version of sops is bekk/sops, but this can be changed to the official sops
when [this](https://github.com/getsops/sops/pull/1578) has been included.

## sops configuration file

In order to encrypt files, the service is dependent on having a .sops.yaml-configuration file.
Example:

```yaml
creation_rules:
  - shamir_threshold: 2
    path_regex: \.risc\.yaml$  # Path to files for local development - not relevant for the crypto service 
    key_groups:
      - age:
          - "age1hn2ax8m9798srve8f207llr50tyelzyp63k96ufx0ud487q9xveqca6k0r"
      - gcp_kms:
          - resource_id: projects/spire-ros-5lmr/locations/eur4/keyRings/ROS/cryptoKeys/ros-as-code
```

For the crypto service to be able to decrypt files, it has to have access to a set (minimum one key in x groups, where x
is defined by the shamir threshold) of the resources used to encrypt.
When encrypting files, the crypto service has to be able to access all resources in the configuration file. This is
especially important to remember in terms of kms-resources.

> Small note on config files: The crypto service stores the configuration file as a temporary file. This is deleted when
> the sops
> encryption succeeds, however, if an error occurs it might not be. The information in these files are already stored in
> github, and does not contain any secret information.

### Age

[Age](https://github.com/FiloSottile/age) is a simple, modern and secyre encryption tool, format and Go library.
In this service we use asymmetric Age key-pairs to encrypt and decrypt files. The asymmetry of the Age keys makes it
easy to add the public key to the .sops.yaml configuration files.
The private keys are kept secret, and used for decryption of files.

### Google Cloud Key Management Service

The GCP KMS is the only supported KMS in this service. There are other available key management services available
through sops, but they require credentials or personal access tokens.

When using sops, a personal access token is used to access the resources, because of this the access to the resource is
restricted to the user.

This service only support the use of the GCP KMS and not the other kms-es that sops support, unless authentication is
provided.

# Setup

## Download sops

To run the crypto service locally you need to have sops installed.

It is very easy to do with gh

```sh
brew install gh
gh auth login
```

The current version used in crypto service has added functionality with the use of google access tokens to access the
crypto key resources from the gcp kms.

```shell
# download the latest version
gh release download --repo https://github.com/bekk/sops <nyeste versjon> --pattern '*.darwin.arm64'

# rename the file to sops
mv sops-v1.2.darwin.arm64 sops

# make it executable
chmod +x sops

# add sops to your path
export PATH=$PATH:<path to file>
```

## Environment variables

**SOPS_AGE_KEY** is an environment variable necessary to run the application with sops. The sops age key is the private
key of an assymetric Age key-pair.
The cryptoservice assumes that all files are encrypted with the public key of the key-pair(and is present in the
.sops.yaml-config files), and use the private sops age key to decrypt the files.

Sops is configured to read the private key from either a keys.txt-file from your user configuration directory, or from
the environment variable. The keys.txt-file will have precedence.

This can be created by following these steps for mac-users

```shell
# install age
brew install age

# create a key-pair, and add the private part to the sops config directory
age-keygen -o $HOME/Library/Application Support/sops/age/keys.txt
```

## Run it from Intellij

We recommend using IntelliJ for local development. To run the application, simply open the repository locally and
select[`Local Server`](https://github.com/kartverket/backstage-plugin-risk-crypto-service/blob/main/.run/Local%20Server.run.xml)
as your run configuration, then run it.

Change the SOPS_AGE_KEY to your key, but remember to keep your private key safe.

## Run it from the Terminal

```shell
export SOPS_AGE_KEY=<sops Age private key>
./gradlew bootRun --args='--spring.profiles.active=local'
```

