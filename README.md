# Crypto-service

The crypto service is used to encrypt and decrypt SOPS-files. You can read more about
SOPS [here](https://github.com/getsops/sops).

<br>

## Update SOPS version

To update the SOPS version

1. Find the latest version available from https://github.com/getsops/sops.
2. Change the `SOPS_VERSION_ARG` argument in `Dockerfile` to the latest version.

## SOPS configuration file

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
> the SOPS
> encryption succeeds, however, if an error occurs it might not be. The information in these files are already stored in
> GitHub, and does not contain any secret information.

### Age

[Age](https://github.com/FiloSottile/age) is a simple, modern and secure encryption tool, format and Go library.
In this service we use asymmetric Age key-pairs to encrypt and decrypt files. The asymmetry of the Age keys makes it
easy to add the public key to the .sops.yaml configuration files.
The private keys are kept secret, and used for decryption of files.



### Google Cloud Key Management Service

The GCP KMS is the only supported KMS in this service. There are other available key management services available
through sops, but they require credentials or personal access tokens.

When using SOPS, a personal access token is used to access the resources, because of this the access to the resource is
restricted to the user.

This service only support the use of the GCP KMS and not the other KMS-es that SOPS support, unless authentication is
provided.

<br>

# Setup

To run the crypto service locally you can either run it through IntelliJ or as a docker-image with docker-compose.
We recommend running it with docker-compose as this do not require downloading a custom configured SOPS installation on
your local machine.

## Local setup with docker-compose

To run locally with docker-compose, you first need to create the git-ignored file `.env`.
```
cp .env.example .env
```

> If you are on M4 Mac, uncomment IMAGE and BUILD_IMAGE.

If you need access to environment-variables, ask a colleague.

You can then build and run the application with 
```shell
docker-compose up
```
which starts the crypto service on port 8084.

## Local setup with IntelliJ

To run the crypto service locally you need to have SOPS installed:

```shell
# Download SOPS using curl (replace <version> with the preferred version, e.g., v3.10.1. If not on MacOS, replace
# `darwin` with `linux` or similar, depending on your system.
curl -o sops -L "https://github.com/getsops/sops/releases/download/<version>/sops-<version>.darwin.arm64"

# Make the file executable
chmod +x sops

# Add sops to your path. To permanently add sops to your path, add this to your shell config file (`.bashrc`,
# `.zshrc` or the equivalent in your shell of choice).
export PATH=$PATH:<path to file>
```

## Environment variables

**SOPS_AGE_KEY** is an environment variable necessary to run the application with SOPS. The SOPS age key is the private
key of an assymetric Age key-pair.
The cryptoservice assumes that all files are encrypted with the public key of the key-pair(and is present in the
.sops.yaml-config files), and use the private sops age key to decrypt the files.

SOPS is configured to read the private key from either a keys.txt-file from your user configuration directory, or from
the environment variable. The keys.txt-file will have precedence.

This can be created by following these steps for mac-users

```shell
# install age
brew install age

# create a key-pair, and add the private part to the SOPS config directory
age-keygen -o $HOME/Library/Application Support/sops/age/keys.txt
```

**SOPS_VERSION**, for example `3.10.2`, is used to check the health of the app using the `actuator/health` endpoint. 
The app will be deemed unhealthy if the indicated version is not the same as returned by `sops -v`.
The image created from Dockerfile will have this environment variable set.

## Run it from Intellij

We recommend using IntelliJ for local development. To run the application, simply open the repository locally and
select[`Local Server`](https://github.com/kartverket/backstage-plugin-risk-crypto-service/blob/main/.run/Local%20Server.run.xml)
as your run configuration, then run it.

Change the SOPS_AGE_KEY to your key, but remember to keep your private key safe.


## Run it from the Terminal

```shell
export SOPS_AGE_KEY=<sops Age private key>
export SOPS_VERSION=<sops version running locally>
./gradlew bootRun --args='--spring.profiles.active=local'
```

## Run it from image

```bash
docker buildx build .
```

which eventually will end with
```
=> => writing image  <image id>
```

Then run:
```bash
docker run -p 8081:8081 -p 8080:8080 -e SOPS_AGE_KEY=etc/bin -e MANAGEMENT.ENDPOINT.HEALTH.SHOW-DETAILS=always <image id>
```
The argument `-e MANAGEMENT.ENDPOINT.HEALTH.SHOW-DETAILS=always` will display details 
when GETing `http://localhost:8081/actuator/health`. Can be useful if health is reported as DOWN.