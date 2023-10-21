#!/bin/sh
export GPG_TTY=$(tty)
echo $DEPLOY_KEY | base64 --decode > $GITHUB_WORKSPACE/deployment/signingkey.asc
gpg --batch --keyring=$GITHUB_WORKSPACE/deployment/pubring.gpg --no-default-keyring --import $GITHUB_WORKSPACE/deployment/signingkey.asc;
gpg --batch --allow-secret-key-import --keyring=$GITHUB_WORKSPACE/deployment/secring.gpg --no-default-keyring --import $GITHUB_WORKSPACE/deployment/signingkey.asc;
mvn -DskipTests deploy --settings $GITHUB_WORKSPACE/deployment/settings.xml -Dgpg.keyname=77787D71ED65A50488D41B82E876C482DFB8D3EB -Dgpg.passphrase=$DEPLOY_KEY_PASSPHRASE -Dgpg.publicKeyring=$GITHUB_WORKSPACE/deployment/pubring.gpg -Dgpg.secretKeyring=$GITHUB_WORKSPACE/deployment/secring.gpg
