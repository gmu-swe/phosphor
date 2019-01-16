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
		java -Xmx6g -Dphosphor.verify=true -jar target/Phosphor-0.0.4-SNAPSHOT.jar -serialization -forceUnboxAcmpEq -withEnumsByValue $INST_HOME target/jre-inst-int;
	else
		echo "Not regenerating int tag instrumented JRE\n";
	fi
	if [ ! -d "target/jre-inst-obj" ]; then
			echo "Creating obj tag instrumented JRE\n";
		java -Xmx6g -Dphosphor.verify=true -jar target/Phosphor-0.0.4-SNAPSHOT.jar -serialization -multiTaint -forceUnboxAcmpEq -withEnumsByValue $INST_HOME target/jre-inst-obj;
	else
		echo "Not regenerating obj tag instrumented JRE\n";
	fi
	#if [ ! -d "target/jre-inst-obj-alen" ]; then
	#		echo "Creating obj-alen tag instrumented JRE\n";
	#	java -Xmx6g -jar target/Phosphor-0.0.3-SNAPSHOT.jar -serialization -multiTaint -forceUnboxAcmpEq -withEnumsByValue -withArrayLengthTags -withArrayIndexTags $INST_HOME target/jre-inst-obj-alen;
	#	chmod +x target/jre-inst-obj-alen/bin/*;
	#	chmod +x target/jre-inst-obj-alen/lib/*;
	#	chmod +x target/jre-inst-obj-alen/jre/bin/*;
	#	chmod +x target/jre-inst-obj-alen/jre/lib/*;
	#else
	#	echo "Not regenerating obj-alen tag instrumented JRE\n";
	#fi
	if [ ! -d "target/jre-inst-implicit" ]; then
		echo "Creating obj tag + implicit flow instrumented JRE\n";
		java -Xmx6g -Dphosphor.verify=true -jar target/Phosphor-0.0.4-SNAPSHOT.jar -controlTrack -multiTaint -forceUnboxAcmpEq -withEnumsByValue $INST_HOME target/jre-inst-implicit;
	else
		echo "Not regenerating implicit flow instrumented JRE\n";
	fi
fi
