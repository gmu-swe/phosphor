#!/bin/sh
openssl aes-256-cbc -K $encrypted_d58958f32407_key -iv $encrypted_d58958f32407_iv -in deployment/signingkey.asc.enc -out deployment/signingkey.asc -d

gpg2 --keyring=$TRAVIS_BUILD_DIR/pubring.gpg --no-default-keyring --import $TRAVIS_BUILD_DIR/deployment/signingkey.asc;
gpg2 --allow-secret-key-import --keyring=$TRAVIS_BUILD_DIR/secring.gpg --no-default-keyring --import $TRAVIS_BUILD_DIR/deployment/signingkey.asc;
cd $TRAVIS_BUILD_DIR/Phosphor;
mvn -DskipTests deploy --settings $TRAVIS_BUILD_DIR/deployment/settings.xml -Dgpg.executable=gpg2 -Dgpg.keyname=77787D71ED65A50488D41B82E876C482DFB8D3EB -Dgpg.passphrase=$PASSPHRASE -Dgpg.publicKeyring=$TRAVIS_BUILD_DIR/pubring.gpg -Dgpg.secretKeyring=$TRAVIS_BUILD_DIR/secring.gpg
cd $TRAVIS_BUILD_DIR/phosphor-instrument-maven-plugin;
mvn -DskipTests deploy --settings $TRAVIS_BUILD_DIR/deployment/settings.xml -Dgpg.executable=gpg2 -Dgpg.keyname=77787D71ED65A50488D41B82E876C482DFB8D3EB -Dgpg.passphrase=$PASSPHRASE -Dgpg.publicKeyring=$TRAVIS_BUILD_DIR/pubring.gpg -Dgpg.secretKeyring=$TRAVIS_BUILD_DIR/secring.gpg
