UNAME := $(shell uname)
CCFLAGS = -o phosphor.o -I${JAVA_HOME}/include -c -fPIC -fpermissive  -std=c++11 -g -O0
ifeq ($(UNAME), Linux)
	CCFLAGS += -I${JAVA_HOME}/include/linux 
	LINKFLAGS = -z defs -static-libgcc -shared -o libphosphor.so -lc 
endif
ifeq ($(UNAME), Darwin)
	CCFLAGS += -I${JAVA_HOME}/include/darwin
	LINKFLAGS += -dynamiclib -o libphosphor.dylib
endif

libphosphor.so:
	gcc ${CCFLAGS} -o target/phosphor.o JVMTIAgent/phosphor.cpp
	g++ ${LINKFLAGS} -o target/libphosphor.so target/phosphor.o
clean:
	rm -rf target/phosphor.o target/libphosphor.so 
