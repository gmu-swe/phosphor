UNAME := $(shell uname)
CCFLAGS = -o phosphor.o -I${JAVA_HOME}/include -c -fPIC -fpermissive
ifeq ($(UNAME), Linux)
	CCFLAGS += -I${JAVA_HOME}/include/linux 
	LINKFLAGS = -z defs -static-libgcc -shared -o libtracker.so -lc 
endif
ifeq ($(UNAME), Darwin)
	CCFLAGS += -I${JAVA_HOME}/include/darwin
	LINKFLAGS += -dynamiclib -o libphosphor.dylib
endif

tracker.dylib:
	gcc ${CCFLAGS} phosphor.cpp
	g++ ${LINKFLAGS} phosphor.o
clean:
	rm -rf phosphor.o libphosphor.dylib libphosphor.so
