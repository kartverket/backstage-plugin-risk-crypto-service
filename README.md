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

To run the crypto service locally you can either run it through IntelliJ or as a docker-image with docker-compose.
We recommend running it with docker-compose as this do not require downloading a custom configured sops on your local machine.

## Local setup with docker-compose

To run locally with docker-compose, you first need to create the git-ignored file `.env` with the following contents:
```
spring_profiles_active=local
SOPS_AGE_KEY=<AGE SECRET KEY USED TO ENCRYPT AND DECRYPT RISCS>
SECURITY_TEAM_PUBLIC_KEY=<ORGNIZATION SPESIFIC PUBLIC KEY>
SECURITY_PLATFORM_PUBLIC_KEY=<ORGNIZATION SPESIFIC PUBLIC KEY2>
BACKEND_PUBLIC_KEY=<ORGNIZATION SPESIFIC PUBLIC KEY FOR BACKEND>
```

If you need access to environment-variables, ask a colleage.

You can then build and run the application with 
```shell
docker-compose up
```
which starts the crypto service on port 8084.

## Local setup with IntelliJ
### Download sops

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

### Oppdatere Bekk sin sops-versjon før versjon 3.10 (NB! IKKE KVALITETSSIKRET!)
Det finnes en [PR fra Maren Ringsby](https://github.com/getsops/sops/pull/1578) for å legge til støtte for bruk av GCP-tokens mot GCP KMS. 
Etter planen blir den med i 3.10-versjon av sops. 

Inntil det må Bekk sin sops versjon holdes i sync med utviklingen i sops. Her er skrittene:

#### Oppdater Maren sin branch (lokalt)
1. `git clone https://github.com/marensofier/sops.git maren-sops` // Last ned maren sitt repo
2. `cd maren-sops` 
3. `git checkout add_access_token` //Bytt til branchen med endringen
3. `git remote add upstream https://github.com/getsops/sops.git` // Lag remote til sops
4. `git fetch upstream` // Hent ned siste versjon av sops
5. `git pull upstream main` // Merge inn endringer fra originalen. Her er det sikkert konflikter i avhengigheter som må fikses

#### Oppdater Bekk sin versjon til å bli maken til den oppdaterte versjonen til Maren    
1. `cd ..` // Gå et hakk opp 
2. `git clone https://github.com/bekk/sops.git bekk-sops` // Last ned Bekk sin versjon av sops
3. `cd bekk-sops`
4. `git remote add maren-sops ../maren-sops/.git` // Lag en remote til vår lokale maren-sops
5. `git fetch maren-sops` // Hent alle brancher
5. `git branch --track add_access_token maren-sops/add_access_token` // Lag en lokal branch av maren-sops sin main
6. `git checkout add_access_token` // Svitsje til branchen som har riktigst sops nå
7. `git merge -s ours main` // merge med bekk-sops sin main, men behold alt fra maren-sops-main
8. `git checkout main` // tilbake til bekk-sops sin main
9. `git merge add_access_token` // få main til å se ut som maren-sops-main
10. `git push` // direkte push på main, som KAN stoppes av github-regler (i så fall gå via en PR)

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

