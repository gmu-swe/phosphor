# Phosphor: Dynamic Taint Tracking for the JVM

Phosphor is a system for performing dynamic taint analysis in the JVM, on commodity JVMs (e.g. Oracle's HotSpot or
OpenJDK's IcedTea).
Phosphor associates labels, which we referred to as taint tags, with program data and propagates these
labels along information "flows" at runtime.
This repository contains the source code for Phosphor.
For more information about how Phosphor works and its uses, please refer to
our [OOPSLA 2014 paper](http://jonbell.net/publications/phosphor),
[ISSTA 2015 Tool Demo](http://mice.cs.columbia.edu/getTechreport.php?techreportID=1601)
or email [Jonathan Bell](mailto:jbell@cs.columbia.edu).
José Cambronero also maintains
a [series of examples on using Phosphor](https://github.com/josepablocam/phosphor-examples/).

Phosphor has been extensively developed since its original publication, and now includes many features and options not
described in the OOPSLA 2014 paper.
If you are looking to replicate our OOPSLA 2014 experiments, the easiest way to get
the same version with certainty is to use
this [VM Image with all relevant files here](http://academiccommons.columbia.edu/catalog/ac%3A182689), and to follow the
[README with instructions for doing so here](https://www.dropbox.com/s/dmebj6k8izams6p/artifact-63-phosphor.pdf?dl=0).

Phosphor currently requires Java 9+ to build, but it can also be used on Java 8.

## Refactoring Status and Roadmap

This branch contains what is nearly a complete rewrite of Phosphor, using a less fragile (but slower) approach to pass
taint tags between methods.
It also is the first version of Phosphor to support Java 9+.

Remaining tasks for this branch before promotion:

* Implement control tracking semantics (currently entirely unimplemented)
* Performance optimization
* Improve documentation
* Consider simplifying distribution of the now multiple jars, or alternatively remove the phosphor-distribution module
* Consider removing the PHOSPHOR_TAG field from objects, and remove `MultiTainter.taintedObject`

## Building

### Requirements

* Java Development Kit (JDK) 9+
* [Apache Maven](https://maven.apache.org/) 3.6.0+

### Steps

1. Clone or download this repository.
2. Ensure that some version of the JDK 9+ is installed.
   A JDK can be downloads from [Oracle](https://www.oracle.com/java/technologies/downloads/) or
   the [Adoptium Working Group](https://adoptium.net/temurin/releases/).
3. Set the JAVA_HOME environmental variable to the path of this JDK installation.
   On Linux and Mac this can be done by running `export JAVA_HOME=<PATH-TO-JDK>`, where &lt;PATH-TO-JDK&gt; is the path
   of the JDK installation.
4. Ensure that you have installed Apache Maven 3.6.0+.
   Directions for [downloading](https://maven.apache.org/download.cgi)
   and [installing](https://maven.apache.org/install.html) Maven are available on the project page for Maven.
5. In the root directory of this project (the one where this README file is located), run `mvn -DskipTests install`.

## Running Phosphor's Tests

Once you have built Phosphor according to the directions described above in the
Section ["Building Phosphor"](#Building-Phosphor), you can run Phosphor's tests and examples.
Although Phosphor currently requires Java 9+ to build, it can also be used on Java 8.
If you would like to run Phosphor's tests on Java 8, build Phosphor using Java 9+, then change the
JAVA_HOME environmental variable to the path of a JDK 8 installation before running the tests.

TODO what command should you run and what will this command do.
TODO Describe some of the tests

## Running the DaCapo Benchmarks

## Creating an Instrumented Java Installation

In order to associate labels with data and to propagate these labels, Phosphor uses Java bytecode instrumentation to
modify Java classes.
In order to track the flow of information through classes in the Java Class Library (JCL), such as `java.lang.String`
and `java.util.List`, Phosphor must also instrument the bytecode of JCL classes.
Therefore, the first step when using Phosphor is to create an instrumented Java installation
(i.e., Java Development Kit or Java Runtime Environment).
A Java installation can be downloaded from [Oracle](https://www.oracle.com/java/technologies/downloads/) or
the [Adoptium Working Group](https://adoptium.net/temurin/releases/).
Once you have obtained a Java installation, it can be instrumented either using Phosphor's [driver JAR](#Driver-Jar) or
[Maven plugin](#Maven-Plugin).
We discuss both options and how to configure Phosphor below.

### Driver JAR

### Maven Plugin

### Configuring Phosphor

## Running an Application with Phosphor

## Extending Phosphor

## Running

Phosphor works by modifying your application's bytecode to perform data flow tracking.
To be complete, Phosphor also modifies the bytecode of JRE-provided classes, too.
The first step to using Phosphor is generating an instrumented version of your runtime environment.
We have tested Phosphor with Oracle and OpenJDK Java 8 runtimes.

The instrumenter takes two primary arguments: first a path containing the classes to instrument, and then a destination
for the instrumented classes.
You can also specify to track taint tags through control flow, to use objects as tags (
instead of integers), or to automatically perform taint marking in particular methods using the various options as shown
by invoking Phosphor with the "-help" option.

```
usage: java -jar phosphor.jar [OPTIONS] [input] [output]
 -controlTrack                  Enable taint tracking through control flow
 -help                          print this message
 -withoutDataTrack              Disable taint tracking through data flow
                                (on by default)
```

Phosphor now should be configured to correctly run JUnit tests (with taint tracking) in most environments (Mac + Linux +
Windows).
Running `mvn verify` should cause Phosphor to generate several different instrumented JRE's (for multitaint
use, int-tag taint use, and control track use) into the project's `target` directory, then run unit tests in that JRE
that are automatically tracked.
You take a look at the test cases to see some example usage.
Tests that end
in `ObjTagITCase` are executed with Phosphor configured for object tags (multi tainting), and `ImplicitITCase` tests run
in the control tracking mode.

### Instrumenting a Java 9+ VM

We'll assume that in all of the code examples below, we're in the same directory as this README.md, that you have
already run `mvn package`, and that the JVM is located
here: `/Library/Java/JavaVirtualMachines/adoptopenjdk-16.jdk/Contents/Home` (modify this path in the commands below to
match your environment).

For Java >= 9, we generate an instrumented JVM by using the `jlink` tool. We have created a jar (
in `phosphor-instrument-jigsaw`) that wraps this entire process, invoking these tools automatically. To instrument the
JVM, run this command:
`java -jar phosphor-instrument-jigsaw/target/phosphor-instrument-jigsaw-0.1.0-SNAPSHOT.jar /Library/Java/JavaVirtualMachines/adoptopenjdk-16.jdk/Contents/Home jre-inst`

The next step is to instrument the code which you would like to track. This time, we will use Phosphor's normal
instrumenter (not the jigsaw JVM instrumenter), and will pass your entire (compiled) code base to Phosphor for
instrumentation, and specify an output folder for that.
Example: `java -jar Phosphor/target/Phosphor-0.1.0-SNAPSHOT.jar path-to-application output-path-for-instrumented-code`

We can now run the instrumented code using our instrumented JRE, as such:

```
jre-inst/bin/java -javaagent:phosphor-jigsaw-javaagent/target/phosphor-jigsaw-javaagent-0.1.0-SNAPSHOT.jar -cp path-to-instrumented-code your.main.class
````

### Instrumenting a Java 8 VM

We'll assume that in all of the code examples below, we're in the same directory (which has a copy of
Phosphor-0.0.5-SNAPSHOT.jar, which you generated by downloading Phosphor, and running `mvn package`), and that the JRE
is located here: `/Library/Java/JavaVirtualMachines/jdk1.8.0_222.jdk/Contents/Home` (modify this path in the commands
below to match your environment).

*Important note on OpenJDK vs Oracle's JDK:* Oracle's JVM requires that the jar that contains all of the cryptography
routines (`jce.jar`) be signed by Oracle, for export control purposes. OpenJDK does not. When Phosphor instruments the
JVM, it will break these signatures. Hence, it is not possible to use Phosphor with Oracle's JDK *and* use the
cryptography functionality of the JVM without some extra work. The first option is (if you have been granted a signing
key by Oracle), you can sign the resulting jar. The more attainable option is to instead use the OpenJDK `jce.jar` file
with your Oracle JVM, which will not have the signature checks, and will work just fine. Alternatively, if this is too
complicated (and you need cryptography support), you can just choose to use OpenJDK entirely (which does not have this
check).

Then, to instrument the JRE we'll run:
`java -jar Phosphor-0.1.0-SNAPSHOT.jar /Library/Java/JavaVirtualMachines/jdk1.8.0_222.jdk/Contents/Home jre-inst`

After you do this, make sure to chmod +x the binaries in the new folder, e.g. `chmod +x jre-inst/bin/*`

The next step is to instrument the code which you would like to track. This time when you run the instrumenter, pass
your entire (compiled) code base to Phosphor for instrumentation, and specify an output folder for that.

We can now run the instrumented code using our instrumented JRE, as such:

```
export JAVA_HOME=jre-inst/
$JAVA_HOME/bin/java  -Xbootclasspath/a:Phosphor-0.1.0-SNAPSHOT.jar -javaagent:Phosphor-0.1.0-SNAPSHOT.jar -cp path-to-instrumented-code your.main.class
````

Note: It is not 100% necessary to instrument your application/library code in advance - the javaagent will detect any
uninstrumented class files as they are being loaded into the JVM and instrument them as necessary. If you want to do
this, then you may want to add the flag `-javaagent:Phosphor-0.1.0-SNAPSHOT.jar=cacheDir=someCacheFolder` and Phosphor
will cache the generated files in `someCacheFolder` so they aren't regenerated every run. If you take a look at the
execution of Phosphor's JUnit tests, you'll notice that this is how they are instrumented. It's always necessary to
instrument the JRE in advance though for bootstrapping.

New 2/27/19: You can no longer specify auto taint methods (what were sources/sinks/taint through methods) for the static
instrumenter. Instead, ALL autotaint instrumentation happens via the java agent (this makes it possible to detect
child-classes of auto taint classes). You can specify the files to the java agent using the
syntax `-javaagent:Phosphor-0.0.4-SNAPSHOT.jar=taintSources={taintSourceFile},taintSinks={taintSinksFile},taintThrough={taintThroughFile}`

## Interacting with Phosphor

Phosphor exposes a simple API to allow to marking data with tags, and to retrieve those tags. Key functionality is
implemented in ``edu.columbia.cs.psl.phosphor.runtime.MultiTainter``. To get or set the taint tag of a primitive type,
developers call the taintedX or getTaint(X) method (replacing X with each of the primitive types, e.g. taintedByte,
etc.).
Ignore the methods ending with the suffix $$PHOSPHOR, they are used internally.
To get or set the taint tag of an object, first cast that object to the interface TaintedWithObjTag (Phosphor changes
all classes to implement this interface), and use the get and set methods.

You can determine if a variable is derived from a particular tainted source by examining the labels on that
variable's `Taint` object.

You *can* detaint variables with Phosphor - to do so, simply use the `MultiTainter` interface to set the taint on a
value to `0` (or `null`).

## Notes on control tracking

Please note that the control tracking functionality can impose SIGNIFICANT overhead (we've observed > 10x slowdown)
depending on the structure of the code you are instrumenting and the amount of tainted data flowing around. This is
incredibly un-optimized at this point. This also can make it difficult to apply Phosphor with control tracking to very
large methods (since it causes them to grow beyond the maximum size permitted). Nonetheless, we have had great success
applying it in various projects --- it works fine on the JDK (perhaps a few internal classes will be too large, but they
were not needed in our workloads) and on projects like Tomcat. There are quite a few paths to improving this
functionality. If you are interested in helping, please contact us.

## Contact

Please email [Jonathan Bell](mailto:bellj@gmu.edu) with any feedback or questions.
This project is still under development and we welcome any feedback.

## License

This software is released under the MIT license.

Copyright (c) 2013, by The Trustees of Columbia University in the City of New York.

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit
persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

## Acknowledgements

Phosphor makes use of the following libraries:

* [ASM](http://asm.ow2.org/license.html), (c) 2000-2011 INRIA, France
  Telecom, [license](http://asm.ow2.org/license.html)
* [Apache Harmony](https://harmony.apache.org), (c) The Apache Software
  Foundation, [license](http://www.apache.org/licenses/LICENSE-2.0)

Phosphor's performance tuning is made possible
by [JProfiler, the java profiler](https://www.ej-technologies.com/products/jprofiler/overview.html).

The authors of this software are Katherine Hough, [Jonathan Bell](http://jonbell.net)
and [Gail Kaiser](http://www.cs.columbia.edu/~kaiser/). Jonathan Bell and Katherine Hough are funded in part by NSF
CCF-1763822 and CCF-1844880. Gail Kaiser directs the [Programming Systems Laboratory](http://www.psl.cs.columbia.edu/),
funded in part by NSF CCF-1161079, NSF CNS-0905246, and NIH U54 CA121852.
