#!/bin/bash
set -x
BUILD_DIR=$1
PHOSPHOR_JAR=$2
PHOSPHOR_JAVA_AGENT=$3
INST_JVM=$4
IS_JAVA_8=$5
DACAPO_DIR=$BUILD_DIR/dacapo
INST_DACAPO_DIR=$BUILD_DIR/dacapo-inst
BENCHMARKS=(avrora fop h2 jython luindex pmd sunflow xalan)
#tradebeans tradesoap are disabled in this script because of the difficulty with distributing the openjdk jce.jar file with everything, then switching it in...
#we don't run batik anymore because it's an enormous pain to automatically set up oracle java...
#tomcat started crashing on travis
#if you want to run them, copy the jce.jar file from an openjdk distribution of the same version into your hotspot jvm that you are going to instrument before instrumenting it.
# (export controls are annoying)
# eclipse doesn't work without big hacks on travis
# ..also disable lusearch because travis seems unhappy when we run it there
HAD_ERROR=0
if [ ! -d "$DACAPO_DIR" ]; then
  echo "Downloading dacapo jar"
  mkdir -p $DACAPO_DIR
  cd $DACAPO_DIR
  wget --quiet https://www.jonbell.net/dacapo-9.12-MR1-bach.jar
  unzip -qq dacapo-9.12-MR1-bach.jar
fi
cd $BUILD_DIR

if [ ! -d "$INST_DACAPO_DIR" ]; then
  echo "Creating data flow instrumented dacapo"
  java -Xmx6g -XX:MaxPermSize=512m -jar "$PHOSPHOR_JAR" -q -forceUnboxAcmpEq -withEnumsByValue "$DACAPO_DIR" "$INST_DACAPO_DIR"
else
  echo "Not regenerating data flow instrumented dacapo"
fi

echo "Trying data flow benchmarks, one iteration only"
for bm in "${BENCHMARKS[@]}"; do
#  echo "$JAVA_HOME/bin/java -cp $DACAPO_DIR Harness $bm"

  if [ "$IS_JAVA_8" == "true" ]; then
    $INST_JVM -Xmx1g -Xbootclasspath/a:"$PHOSPHOR_JAR" -javaagent:"$PHOSPHOR_JAVA_AGENT" -cp "$INST_DACAPO_DIR" -Declipse.java.home="$JAVA_HOME" -Djava.awt.headless=true Harness "$bm"
  else
    $INST_JVM -Xmx4g -javaagent:"$PHOSPHOR_JAVA_AGENT" -cp "$INST_DACAPO_DIR" -Declipse.java.home="$JAVA_HOME" -Djava.awt.headless=true Harness "$bm"
  fi
  if [ $? -ne 0 ]; then
    HAD_ERROR=$(expr $HAD_ERROR + 1)
    echo "ERROR!!!"
  fi
done

echo "Total errors: $HAD_ERROR"
if [ $HAD_ERROR -ne 0 ]; then
  exit 255
fi
