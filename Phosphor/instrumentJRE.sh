#!/bin/sh
if [ -z "$JAVA_HOME" ]; then
	echo "Error: Please set \$JAVA_HOME";
else
	echo "Ensuring instrumented JREs exist for tests... to refresh, do mvn clean\n";
	if [ ! -d "target/jre-inst-int" ]; then
		echo "Creating int tag instrumented JRE\n";
		java -Xmx6g -XX:MaxPermSize=512m -jar target/Phosphor-0.0.2-SNAPSHOT.jar -forceUnboxAcmpEq -withEnumsByValue $JAVA_HOME target/jre-inst-int;
		chmod +x target/jre-inst-int/bin/*;
		chmod +x target/jre-inst-int/lib/*;
	else
		echo "Not regenerating int tag instrumented JRE\n";
	fi
	if [ ! -d "target/jre-inst-obj" ]; then
			echo "Creating obj tag instrumented JRE\n";
		java -Xmx6g -XX:MaxPermSize=512m  -jar target/Phosphor-0.0.2-SNAPSHOT.jar -multiTaint -forceUnboxAcmpEq -withEnumsByValue $JAVA_HOME target/jre-inst-obj;
		chmod +x target/jre-inst-obj/bin/*;
		chmod +x target/jre-inst-obj/lib/*;
	else
		echo "Not regenerating obj tag instrumented JRE\n";
	fi
	if [ ! -d "target/jre-inst-implicit" ]; then
		echo "Creating obj tag + implicit flow instrumented JRE\n";
		java -Xmx6g -XX:MaxPermSize=512m -jar target/Phosphor-0.0.2-SNAPSHOT.jar -controlTrack -multiTaint -forceUnboxAcmpEq -withEnumsByValue $JAVA_HOME target/jre-inst-implicit;
		chmod +x target/jre-inst-implicit/bin/*;
		chmod +x target/jre-inst-implicit/lib/*;
	else
		echo "Not regenerating implicit flow instrumented JRE\n";
	fi
fi
