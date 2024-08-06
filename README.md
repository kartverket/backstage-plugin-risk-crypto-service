#Dette er en test
hehe
# backstage-plugin-risk-crypto-service


## Setup

We recommend using IntelliJ for local development. To run the application, simply open the repository locally and select[`Local Server`](https://github.com/kartverket/backstage-plugin-risk-crypto-service/blob/main/.run/Local%20Server.run.xml) as your run configuration, then run it. 

## Encryption and decryption key setup

### age keys

Age keys are asymmetrical so they include a private and a public part. The public part can then be put in the `.sops.yaml` so that files will be encrypted with that public key. 
The private part of the key can then be used to decrypt the file. Sops will retrieve the the key from either the environmental variable `SOPS_AGE_KEY` or from a `keys.txt` file located at a subdirectory of your user configuration directory. [See SOPS documentation for the directory path](https://github.com/getsops/sops?tab=readme-ov-file#22encrypting-using-age).

* Make sure that [age](https://github.com/FiloSottile/age) is installed, i.e. `brew install age`.

### Generate new master key
* Create a new age key by running `age-keygen -o keys.txt`. Copy the public key, from now on called `<AGE_PUBLIC_KEY>`
* In OS X `keys.txt` must be present in `$HOME/Library/Application Support/sops/age/keys.txt`,
* The private key (inside `keys.txt`) should be distributed in a secure way, i.e by 1Password or Dashlane.

### GCP-keys

GCP-keys are symmetrical, meaning that the same key is used to both encrypt and decrypt content. The key itself is stored in GCP and SOPS connects to GCP and uses the GCP-key when encrypting and decrypting. Access to a GCP-key is goverened by IAM-policies in GCP.

### Configure GCP KMS
* Make sure Google-CLI, `gcloud` is installed, i.e. `brew install --cask google-cloud-sdk`
* In your favourite terminal run `gcloud auth application-default login`. This will log you in to GCP (via a browser) and save the login information that will be used by the GCP-libraries that SOPS uses.

### Generate new master key

* Navigate to [GCP KMS](https://console.cloud.google.com/security/kms/keyrings) with a Kartverket-user
* Choose the correct project (or create a new one if yoy can; it is recommended to have keys in separate projects)
* Create "Key ring", call it `ROS` (if you do not have one allready). Choose `Multi-region` and `eur4 (Netherlands and Finland)`
* Create a key, i.e `ROS-as-code`
* Under `Actions` in the key-list, choose `Copy resource name`. That will copy the `<GCP_KEY_ID>` to the clipboard. Den vil se noe slikt ut: `projects/<prosjekt-id>/locations/eur4/keyRings/ROS/cryptoKeys/ROS-as-code`
* Update `./security/.sops.yaml`:

```yaml
creation_rules:
  - path_regex: \.ros\.yaml$
    gcp_kms: <GCP_KEY_ID>
```

### Access to master keys

Everybody that should update the ROS-files must have access to encrypt/decrypt via the master-key. This can be done achieved in two ways:

* Being in the AD/Entra-group for Team Leads
* **or** by explicit access to the key in [GCP KMS](https://console.cloud.google.com/security/kms/keyrings).

## Rotate SOPS data-key

It is considered good practise to rotate the data key regularely. 

* In your favorite shell, navigate to `.security/ros` in the  repository .
* Kjør `sops -r <name>.ros.yaml`
