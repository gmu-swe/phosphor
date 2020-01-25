#!/bin/bash
BUILD_DIR=$1
PHOSPHOR_JAR=$2
INST_JVM=$3
DACAPO_DIR=$BUILD_DIR/dacapo
INST_DACAPO_DIR=$BUILD_DIR/dacapo-inst
BENCHMARKS=(avrora batik fop h2 jython luindex pmd sunflow xalan) #tradebeans tradesoap are disabled in this script because of the PITA with distributing the openjdk jce.jar file with everything, then switching it in...
#tomcat started crashing on travis
#if you want to run them, copy the jce.jar file from an openjdk distribution of the same version into your hotspot jvm that you are going to instrument before instrumenting it.
# (export controls are annoying)
# eclipse doesn't work wihtout big hacks on travis
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

echo "Trying data flow benchmarks. Note: these numbers will not be super accurate from travis!"
for bm in "${BENCHMARKS[@]}"; do
  echo "$JAVA_HOME/bin/java -cp $DACAPO_DIR Harness $bm"
  echo "$INST_JVM -Xbootclasspath/p:$PHOSPHOR_JAR -javaagent:$PHOSPHOR_JAR -cp $INST_DACAPO_DIR -Declipse.java.home=$JAVA_HOME -Djava.awt.headless=true Harness $bm"
  $INST_JVM -Xmx1g -Xbootclasspath/p:"$PHOSPHOR_JAR" -javaagent:"$PHOSPHOR_JAR" -cp "$INST_DACAPO_DIR" -Declipse.java.home="$JAVA_HOME" -Djava.awt.headless=true Harness "$bm"
  if [ $? -ne 0 ]; then
    HAD_ERROR=$(expr $HAD_ERROR + 1)
    echo "ERROR!!!"
  fi
done

echo "Total errors: $HAD_ERROR"
if [ $HAD_ERROR -ne 0 ]; then
  exit 255
fi
