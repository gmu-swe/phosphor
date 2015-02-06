Phosphor: Dynamic Taint Tracking for the JVM
========

Phosphor is a system for performing dynamic taint analysis in the JVM, on commodity JVMs (e.g. Oracle's HotSpot or OpenJDK's IcedTea). This repository contains the source for Phosphor. For more information about how Phosphor works and what it could be useful for, please refer to our [Technical Report](https://mice.cs.columbia.edu/getTechreport.php?techreportID=1569) or email [Jonathan Bell](mailto:jbell@cs.columbia.edu).

JVMTI Fork Information
-------
This branch contains a different version of Phosphor than was described in our OOPSLA paper. While the previously discussed version of Phosphor handled multi-dimension arrays by wrapping each inner dimension in an object (with one field of the object storing the contents for that array and another field storing the taint tags), this version maintains taint tags for multi-dimension arrays by using [JVMTI object tagging](http://docs.oracle.com/javase/7/docs/platform/jvmti/jvmti.html). Please note that the instructions for building and running this branch are slightly different than the main branch as it also includes a native component.

This implementation re-balances some tradeoffs. JVMTI is a standard, but several of the JVMs that we evaluated do not support it (mainly the old ones: OpenJDK and Oracle's JVM both do); the Dalvik VM definitely does not support it. However, it greatly increases Phosphor's resilience to native interoperability. A key limitation of Phosphor previously was that if native code accessed a field of an object which was a multi-dimension array, the application would crash (because Phosphor removed all multi-dimnsion arrays, replacing them with those wrappers and because the native code couldn't be modified to be aware of this). In our experiments (i.e. with the applications in DaCapo) this was not a problem. Nonetheless, supporting these applications would be nice. In this implementation, if native code directly accessed these fields, taints would not be propogated, but the application wouldn't crash. We haven't yet, but it would be possible to add a warning feature to Phosphor to detect when native code is directly accessing fields of objects (and not propogating taints), again, using JVMTI.

We have performed some cursory evaluations of the runtime overhead of the two approaches, finding that each approach has its ups and downs in terms of performance. The object wrapping approach puts much more pressure on the garbage collector (since these wrapper objects are created and discarded frequently), but the JVMTI approach can lead to slowdowns every time that the JVM-JNI barrier is crossed to get/set a taint tag. Something of a hybrid approach may be most effective, but unfortunately we do not have time to investigate this further right now.

Running
-------
Phosphor works by modifying your application's bytecode to perform data flow tracking. To be complete, Phosphor also modifies the bytecode of JRE-provided classes, too. The first step to using Phosphor is generating an instrumented version of your runtime environment. We have tested Phosphor with versions 7 and 8 of both Oracle's HotSpot JVM and OpenJDK's IcedTea JVM.

We'll assume that in all of the code examples below, we're in the same directory (which has a copy of [phosphor.jar](https://github.com/Programming-Systems-Lab/phosphor/raw/master/phosphor.jar)), and that the JRE is located here: `/Library/Java/JavaVirtualMachines/jdk1.7.0_45.jdk/Contents/Home/jre` (modify this path in the commands below to match your environment).

First, you'll need to build the native tracking library. Check out the Phosphor source (or at least, the `TrackerJVMTI` folder). Set `JAVA_HOME` to point to your JDK (preferably the same one that you plan to use for an execution environment), then run `make` in the `TrackerJVMTI` folder. You should get a libphosphor.so or libphosphor.dylib (for linux or mac, respectively).

Then, to instrument the JRE we'll run:
`java -jar phosphor.jar /Library/Java/JavaVirtualMachines/jdk1.7.0_45.jdk/Contents/Home/jre jre-inst`

The instrumenter takes two primary arguments: first a path containing the classes to instrument, and then a destination for the instrumented classes. Optionally, you can specify additional classpath dependencies referenced from your code (may or may not be necessary, and explained in further below).

The next step is to instrument the code which you would like to track. We'll start off by instrumenting the demo suite provided under the PhosphorTests project. This suite includes a slightly modified version of [DroidBench](http://sseblog.ec-spride.de/tools/droidbench/), a test suite that simulates application data leaks. We'll instrument the [phosphortests.jar](https://github.com/Programming-Systems-Lab/phosphor/raw/master/phosphortests.jar) file:
`java -jar phosphor.jar phosphortests.jar inst`

This will create the folder inst, and place in it the instrumented version of the demo suite jar.

We can now run the instrumented demo suite using our instrumented JRE, as such (note: you will likely need to make the binaries in your generated JAVA_HOME/bin executable, and in some cases, there may also be binaries in JAVA_HOME/lib/):
`JAVA_HOME=jre-inst/ $JAVA_HOME/bin/java  -Xbootclasspath/a:phosphor.jar -cp inst/phosphortests.jar -agentpath:libphosphor.so -ea phosphor.test.DroidBenchTest`
The result should be a list of test cases, with assertion errors for each "testImplicitFlow" test case. Note the `agentpath` argument, which was not needed in previous versions of phosphor, which must point to the compiled JVMTI library from the first step.

If you are running an application that might be defining its own classes, you can also use Phosphor as a just-in-time instrumenter by passing it as a java agent to the JVM when you run your application (e.g. `java -javaagent:phosphor.jar`).

Note that the JVMTI version of Phosphor does *not* require classpath information for instrumentation purposes - its stack frame map modifications are much less invasive and does not need to completely recalculate them.

Interacting with Phosphor
-----
Phosphor exposes a simple API to allow you to mark data with tags, and to retrieve those tags. The class ``edu.columbia.cs.psl.phosphor.runtime.Tainter`` contains all relevant methods (ignore the methods ending with the suffix $$INVIVO_PC, they are used internally), namely, getTaint(...) and taintedX(...) (with one X for each data type: taintedByte, taintedBoolean, etc).

Building from Source
-----
Phosphor consists of a Java project and a C++ project.

We suggest that you build the Java project in Eclipse (it contains an eclipse project file), and use the included Eclipse jar export wizard file (jar-descriptor.jardesc) to export a jar for Phosphor. If you would like to export the jar manually, please be sure to use the included MANFIEST.MF file (which specifies both the main class and information for the java agent).

To build the native agent, we have included simple makefiles for both Mac OS X and Linux. Before running the makefile, please be sure that `JAVA_HOME` is set to point to your JDK. I strongly recommend building the library against the same JDK that you plan to instrument and run.

Support for Android
----
If you are interested in applying Phosphor to Android, please look at the other branch (not the JVMTI one).

Questions, concerns, comments
----
Please email [Jonathan Bell](mailto:jbell@cs.columbia.edu) with any feedback. This project is still under heavy development, and we are working on many extensions (for example, tagging data with arbitrary types of tags, rather than just integer tags), and would very much welcome any feedback.

License
-------
This software is released under the MIT license.

Copyright (c) 2013, by The Trustees of Columbia University in the City of New York.

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Acknowledgements
--------
This project makes use of the following libraries:
* [ASM](http://asm.ow2.org/license.html), (c) 2000-2011 INRIA, France Telecom, [license](http://asm.ow2.org/license.html)

The authors of this software are [Jonathan Bell](http://jonbell.net) and [Gail Kaiser](http://www.cs.columbia.edu/~kaiser/). The authors are members of the [Programming Systems Laboratory](http://www.psl.cs.columbia.edu/), funded in part by NSF CCF-1161079, NSF CNS-0905246, and NIH U54 CA121852.

[![githalytics.com alpha](https://cruel-carlota.pagodabox.com/ae2f03ebde27be607b8ffe5a9911293d "githalytics.com")](http://githalytics.com/Programming-Systems-Lab/phosphor)
