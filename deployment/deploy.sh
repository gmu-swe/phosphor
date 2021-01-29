#!/bin/sh

TRAVIS_BUILD_DIR=`pwd`

base64 -d $DEPLOY_KEY > $TRAVIS_BUILD_DIR/deployment/signingkey.asc
gpg --batch --keyring=$TRAVIS_BUILD_DIR/pubring.gpg --no-default-keyring --import $TRAVIS_BUILD_DIR/deployment/signingkey.asc;
gpg --batch --allow-secret-key-import --keyring=$TRAVIS_BUILD_DIR/secring.gpg --no-default-keyring --import $TRAVIS_BUILD_DIR/deployment/signingkey.asc;
cd $TRAVIS_BUILD_DIR/Phosphor;
mvn -DskipTests deploy --settings $TRAVIS_BUILD_DIR/deployment/settings.xml -Dgpg.keyname=77787D71ED65A50488D41B82E876C482DFB8D3EB -Dgpg.passphrase=$DEPLOY_KEY_PASSPHRASE -Dgpg.publicKeyring=$TRAVIS_BUILD_DIR/pubring.gpg -Dgpg.secretKeyring=$TRAVIS_BUILD_DIR/secring.gpg
cd $TRAVIS_BUILD_DIR/phosphor-instrument-maven-plugin;
mvn -DskipTests deploy --settings $TRAVIS_BUILD_DIR/deployment/settings.xml -Dgpg.keyname=77787D71ED65A50488D41B82E876C482DFB8D3EB -Dgpg.passphrase=$DEPLOY_KEY_PASSPHRASE -Dgpg.publicKeyring=$TRAVIS_BUILD_DIR/pubring.gpg -Dgpg.secretKeyring=$TRAVIS_BUILD_DIR/secring.gpg
