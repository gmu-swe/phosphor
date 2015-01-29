tracker.dylib:
	gcc -o phosphor.o -I${JAVA_HOME}/include -I${JAVA_HOME}/include/darwin -c -fPIC phosphor.cpp
	g++ -dynamiclib -o libphosphor.dylib phosphor.o
clean:
	rm -rf phosphor.o libphosphor.dylib
