#!/bin/bash
set -x
readonly BUILD_DIR=$1
readonly PHOSPHOR_JAR=$2
readonly DRIVER_JAR=$3
readonly INST_JVM=$4
readonly DACAPO_DIR=$BUILD_DIR/dacapo
readonly INST_DACAPO_DIR=$BUILD_DIR/dacapo-inst
readonly BENCHMARKS=(avrora fop h2 jython luindex pmd sunflow xalan)
#tradebeans tradesoap are disabled in this script because of the difficulty with distributing the openjdk jce.jar file with everything, then switching it in...
#we don't run batik anymore because it's an enormous pain to automatically set up oracle java...
#tomcat started crashing on travis
#if you want to run them, copy the jce.jar file from an openjdk distribution of the same version into your hotspot jvm that you are going to instrument before instrumenting it.
# (export controls are annoying)
# eclipse doesn't work without big hacks on travis
# ..also disable lusearch because travis seems unhappy when we run it there
if [ ! -d "$DACAPO_DIR" ]; then
  echo "Downloading dacapo jar"
  mkdir -p "$DACAPO_DIR"
  cd "$DACAPO_DIR"
  wget --quiet https://www.jonbell.net/dacapo-9.12-MR1-bach.jar
  unzip -qq dacapo-9.12-MR1-bach.jar
fi
cd "$BUILD_DIR"

if [ ! -d "$INST_DACAPO_DIR" ]; then
  echo "Creating data flow instrumented dacapo"
  java -Xmx8g -jar "$DRIVER_JAR" -q -forceUnboxAcmpEq -withEnumsByValue "$DACAPO_DIR" "$INST_DACAPO_DIR"
else
  echo "Not regenerating data flow instrumented dacapo"
fi
errors=0
echo "Trying data flow benchmarks, one iteration only"
for bm in "${BENCHMARKS[@]}"; do
  $INST_JVM -Xmx4g \
    -Xbootclasspath/a:"$PHOSPHOR_JAR" \
    -javaagent:"$PHOSPHOR_JAR" \
    -cp "$INST_DACAPO_DIR" \
    -Declipse.java.home="$JAVA_HOME" \
    -Djava.awt.headless=true \
    Harness "$bm"
  if [ $? -ne 0 ]; then
    errors=var=$((errors + 1))
    echo "ERROR!!!"
  fi
done

echo "Total errors: $errors"
if [ $errors -ne 0 ]; then
  exit 255
fi
