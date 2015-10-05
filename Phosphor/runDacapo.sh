#!/bin/sh
DACAPO_DIR=dacapo
PHOSPHOR_JAR=`pwd`/target/Phosphor-0.0.2-SNAPSHOT.jar
BENCHMARKS=(avrora batik eclipse fop h2 jython luindex lusearch pmd sunflow tomcat xalan) #tradebeans tradesoap are disabled in this script because of the PITA with distributing the openjdk jce.jar file with everything, then switching it in...
#if you want to run them, copy the jce.jar file from an openjdk distribution of the same version into your hotspot jvm that you are going to instrument before instrumenting it.
# (export controls are annoying)

HAD_ERROR=0
if [ ! -d "$DACAPO_DIR" ]; then
echo "Downloading dacapo jar";
mkdir dacapo;
cd dacapo;
wget http://ase.cs.columbia.edu:8282/repository/internal/org/dacapo/dacapo/9.12bach/dacapo-9.12bach.jar;
unzip dacapo-9.12bach.jar;
cd ..;
fi
    echo "Ensuring instrumented dacapo exist for tests... to refresh, do mvn clean\n";
    if [ ! -d "target/dacapo-inst-int" ]; then
    echo "Creating int tag instrumented dacapo";
    java -Xmx6g -XX:MaxPermSize=512m -jar $PHOSPHOR_JAR -forceUnboxAcmpEq -withEnumsByValue $DACAPO_DIR target/dacapo-inst-int;
    else
    echo "Not regenerating int tag instrumented dacapo";
    fi
    if [ ! -d "target/dacapo-inst-obj" ]; then
    echo "Creating obj tag instrumented dacapo";
    java -Xmx6g -XX:MaxPermSize=512m  -jar $PHOSPHOR_JAR -multiTaint -forceUnboxAcmpEq -withEnumsByValue $DACAPO_DIR target/dacapo-inst-obj;
    else
    echo "Not regenerating obj tag instrumented dacapo";
    fi
#    if [ ! -d "target/dacapo-inst-implicit" ]; then
#    echo "Creating obj tag + implicit flow instrumented dacapo\n";
#    java -Xmx6g -XX:MaxPermSize=512m -jar $PHOSPHOR_JAR -controlTrack -multiTaint $DACAPO_DIR target/dacapo-inst-implicit;
#    else
#    echo "Not regenerating implicit flow instrumented dacapo\n";
#    fi
echo "Trying int tag benchmarks. Note: NOT doing any warmup or control - these numbers will not be super accurate!";
for bm in "${BENCHMARKS[@]}"
do
echo "target/jre-inst-int/bin/java -Xbootclasspath/p:$PHOSPHOR_JAR -javaagent:$PHOSPHOR_JAR -cp target/dacapo-inst-int/ -Declipse.java.home=$JAVA_HOME Harness $bm"
target/jre-inst-int/bin/java -Xbootclasspath/p:$PHOSPHOR_JAR -javaagent:$PHOSPHOR_JAR -cp target/dacapo-inst-int/ -Declipse.java.home=$JAVA_HOME Harness $bm
echo "target/jre-inst-obj/bin/java -Xbootclasspath/p:$PHOSPHOR_JAR -javaagent:$PHOSPHOR_JAR -cp target/dacapo-inst-obj/ -Declipse.java.home=$JAVA_HOME Harness $bm"
target/jre-inst-obj/bin/java -Xbootclasspath/p:$PHOSPHOR_JAR -javaagent:$PHOSPHOR_JAR -cp target/dacapo-inst-obj/ -Declipse.java.home=$JAVA_HOME Harness $bm
if [ $? -ne 0 ]; then
HAD_ERROR=`expr $HAD_ERROR + 1`
fi

done
echo "Total errors: $HAD_ERROR"
if [ $HAD_ERROR -ne 0 ]; then
exit 255
fi