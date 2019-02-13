#!/bin/sh

gpg2 --keyring=$TRAVIS_BUILD_DIR/pubring.gpg --no-default-keyring --import $TRAVIS_BUILD_DIR/deployment/signingkey.asc;
gpg2 --allow-secret-key-import --keyring=$TRAVIS_BUILD_DIR/secring.gpg --no-default-keyring --import $TRAVIS_BUILD_DIR/deployment/signingkey.asc;
cd $TRAVIS_BUILD_DIR/Phosphor;
mvn -DskipTests deploy --settings $TRAVIS_BUILD_DIR/deployment/settings.xml -Dgpg.executable=gpg2 -Dgpg.keyname=DFB8D3EB -Dgpg.passphrase=$PASSPHRASE -Dgpg.publicKeyring=$TRAVIS_BUILD_DIR/pubring.gpg -Dgpg.secretKeyring=$TRAVIS_BUILD_DIR/secring.gpg
