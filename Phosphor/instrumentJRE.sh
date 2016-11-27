#!/bin/sh
if [ -z "$INST_HOME" ]; then
	INST_HOME=$JAVA_HOME;
fi
if [ -z "$JAVA_HOME" ]; then
	echo "Error: Please set \$JAVA_HOME";
else
	echo "Ensuring instrumented JREs exist for tests... to refresh, do mvn clean\n";
	if [ ! -d "target/jre-inst-int" ]; then
		echo "Creating int tag instrumented JRE\n";
		java -Xmx6g -jar target/Phosphor-0.0.3-SNAPSHOT.jar -forceUnboxAcmpEq -withEnumsByValue $INST_HOME target/jre-inst-int;
		chmod +x target/jre-inst-int/bin/*;
		chmod +x target/jre-inst-int/lib/*;
		chmod +x target/jre-inst-int/jre/bin/*;
		chmod +x target/jre-inst-int/jre/lib/*;
	else
		echo "Not regenerating int tag instrumented JRE\n";
	fi
	if [ ! -d "target/jre-inst-obj" ]; then
			echo "Creating obj tag instrumented JRE\n";
		java -Xmx6g -jar target/Phosphor-0.0.3-SNAPSHOT.jar -multiTaint -forceUnboxAcmpEq -withEnumsByValue $INST_HOME target/jre-inst-obj;
		chmod +x target/jre-inst-obj/bin/*;
		chmod +x target/jre-inst-obj/lib/*;
		chmod +x target/jre-inst-obj/jre/bin/*;
		chmod +x target/jre-inst-obj/jre/lib/*;
	else
		echo "Not regenerating obj tag instrumented JRE\n";
	fi
	if [ ! -d "target/jre-inst-implicit" ]; then
		echo "Creating obj tag + implicit flow instrumented JRE\n";
		java -Xmx6g -jar target/Phosphor-0.0.3-SNAPSHOT.jar -controlTrack -multiTaint -forceUnboxAcmpEq -withEnumsByValue $INST_HOME target/jre-inst-implicit;
		chmod +x target/jre-inst-implicit/bin/*;
		chmod +x target/jre-inst-implicit/lib/*;
		chmod +x target/jre-inst-implicit/jre/bin/*;
		chmod +x target/jre-inst-implicit/jre/lib/*;
	else
		echo "Not regenerating implicit flow instrumented JRE\n";
	fi
fi
