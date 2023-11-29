# Phosphor: Dynamic Taint Tracking for the JVM

Phosphor is a system for performing dynamic taint tracking in the Java Virtual Machine (JVM), on commodity JVMs (e.g.
Oracle's HotSpot or OpenJDK's IcedTea).
Phosphor uses Java bytecode instrumentation to associate labels, which we referred to as taint tags, with program data
and propagate these labels along information "flows" at runtime.
This repository contains the source code for Phosphor.
For more information about how Phosphor works and its uses, please refer to
our [OOPSLA 2014 paper](http://jonbell.net/publications/phosphor),
[ISSTA 2015 Tool Demo](http://mice.cs.columbia.edu/getTechreport.php?techreportID=1601),
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
* Consider removing the PHOSPHOR_TAG field from objects, and remove `MultiTainter.taintedObject`

## Building Phosphor

### Requirements

* Java Development Kit (JDK) 9+
* [Apache Maven](https://maven.apache.org/) 3.6.0+

### Steps

1. Clone or download this repository.
2. Ensure that some version of the JDK 9+ is installed.
   A JDK can be downloaded from [Oracle](https://www.oracle.com/java/technologies/downloads/) or
   the [Adoptium Working Group](https://adoptium.net/temurin/releases/).
3. Set the JAVA_HOME environmental variable to the path of this JDK installation.
   On Linux and Mac, this can be done by running `export JAVA_HOME=<PATH-TO-JDK>`, where &lt;PATH-TO-JDK&gt; is the path
   of the JDK installation.
4. Ensure that you have installed Apache Maven 3.6.0+.
   Directions for [downloading](https://maven.apache.org/download.cgi)
   and [installing](https://maven.apache.org/install.html) Maven are available on the project page for Maven.
5. In the root directory of this project (the one where this README file is located), run `mvn -DskipTests install`.

## Running Phosphor's Tests

Once you have built Phosphor according to the directions described above in the
section ["Building Phosphor"](#Building-Phosphor), you can run Phosphor's tests and examples.
Although Phosphor currently requires Java 9+ to build, it can also be used on Java 8.
If you would like to run Phosphor's tests on Java 8, build Phosphor using Java 9+, then change the
`JAVA_HOME` environmental variable to the path of a JDK 8 installation before running the tests.
To run Phosphor the root directory of this project, run `mvn -pl :integration-tests verify`.
The first time you run this command, Maven will invoke the Phosphor Maven plugin to create
Phosphor-instrumented Java installations.
These instrumented Java installation are cached for future use and will not be recreated unless one of the
Phosphor JARs, the configuration used to create them, or the value of `JAVA_HOME` changes.
Once the Phosphor Maven plugin finishes creating the instrumented Java installations the tests will run.
These tests demonstrate how Phosphor can be used and are a good reference when first learning Phosphor.

## Creating an Instrumented Java Installation

In order to track the flow of information through classes in the Java Class Library (JCL), such as `java.lang.String`
and `java.util.List`, Phosphor must instrument the bytecode of JCL classes.
Therefore, the first step when using Phosphor is to create an instrumented Java installation
(i.e., Java Development Kit or Java Runtime Environment).
A Java installation can be downloaded from [Oracle](https://www.oracle.com/java/technologies/downloads/) or
the [Adoptium Working Group](https://adoptium.net/temurin/releases/).
Once you have obtained a Java installation, it can be instrumented either using Phosphor's [driver](#Driver) or
[Maven plugin](#Maven-Plugin).
We discuss both options below.

**Important note on OpenJDK vs Oracle's Java installations:**
Oracle's Java installations requires that the JAR that contains the cryptography routines `jce.jar` be signed by
Oracle for export control purposes.
OpenJDK does not.
Phosphor instrumentation will break these signatures.
Therefore, it is not possible to use Phosphor with Oracle's Java installation *and* use the cryptography functionality.

### Driver

The Phosphor driver can be used apply Phosphor instrumentation to Java classes in a Java installation,
directory, or archive.
If you have built Phosphor according to the directions described above in the
section ["Building Phosphor"](#Building-Phosphor), then the driver JAR will be available at
`phosphor-driver/target/phosphor-driver-0.1.0-SNAPSHOT.jar` relative to the root of this project.
The latest snapshot of the driver JAR is available at the
[Sonatype OSS Repository Hosting (OSSRH)](https://oss.sonatype.org/content/repositories/snapshots/edu/gmu/swe/phosphor/).
The driver JAR can also be acquired using the Maven dependency:

```
<build>
    ...
    <dependencies>
        <dependency>
            <groupId>edu.gmu.swe.phosphor</groupId>
            <artifactId>phosphor-driver</artifactId>
            <version>VERSION</version>
        </dependency>
    </dependencies>
    ...
</build>
```

The driver supports archives of types JAR, WAR, ZIP, and JMOD.
The driver takes a list of options followed by a source location and a destination location:

```
java -jar <phosphor-driver.jar> [OPTIONS] <SOURCE> <DEST>
```

The driver will create a Phosphor-instrumented copy of Java installation, directory, or archive located at the
specified source location at place it at the specified destination location.
The options you specify allow you to control how Phosphor instruments the specified source.
A detailed list of available options can be accessed by running:

```
java -jar <phosphor-driver.jar> -help
```

### Maven Plugin

To create a Phosphor-instrumented Java installation as part of your Maven build, add the
`phosphor-instrument-maven-plugin` in your pom:

```
<build>
    ...
    <plugins>
        ...
        <plugin>
            <groupId>edu.gmu.swe.phosphor</groupId>
            <artifactId>phosphor-instrument-maven-plugin</artifactId>
           <version>VERSION</version>
        </plugin>
        ...
    </plugins>
    ...
</build>
```

See the documentation for the
[InstrumentingMojo](phosphor-instrument-maven-plugin/src/main/java/edu/columbia/cs/psl/phosphor/plugin/InstrumentMojo.java)
for more detailing on how to configure and use the Phosphor Maven plugin.

## Instrumenting Your Application

If you choose, you can also use the Phosphor driver to instrument you application classes before running your
application.
This step is optional;
Phosphor will dynamically instrument any classes not already instrumented at runtime as they are loaded by the JVM.
If you want to Phosphor to cache classes that are dynamically instrumented, then you can add the Java option
`-DphosphorCacheDirectory=<CACHE-DIRECTORY>` when running your application, where &lt;CACHE-DIRECTORY&gt; is the file
path to the directory where Phosphor should store the cached instrumented class files.

## Running Your Application with Phosphor

Once you have created an instrumented Java installation (see the section
["Creating an Instrumented Java Installation"](#Creating-an-Instrumented-Java-Installation), you can run your
application with Phosphor.
Locate the JAR for Phosphor's Java agent.
If you have built Phosphor according to the directions described above in the
section ["Building Phosphor"](#Building-Phosphor), then the agent JAR will be available at
`Phosphor/target/Phosphor-0.1.0-SNAPSHOT.jar` relative to the root of this project.
The latest snapshot of the agent JAR is available at the
[Sonatype OSS Repository Hosting (OSSRH)](https://oss.sonatype.org/content/repositories/snapshots/edu/gmu/swe/phosphor/).
The agent JAR can also be acquired using the Maven dependency:

```
<build>
    ...
    <dependencies>
        <dependency>
            <groupId>edu.gmu.swe.phosphor</groupId>
            <artifactId>Phosphor</artifactId>
            <version>VERSION</version>
        </dependency>
    </dependencies>
    ...
</build>
```

Once you have located the JAR for Phosphor's Java agent, execute the same Java command you would normally to run
your application using the Java executable inside the instrumented Java installation you created and
add `javaagent` and `bootclasspath` Java options for Phosphor.
If running java using the `-jar` option run:

```
<INSTRUMENTED-JAVA-HOME>/bin/java \
    -Xbootclasspath/a:<AGENT-JAR> \
    -javaagent:<AGENT-JAR> \
    [ options ] -jar file.jar [ argument ... ]
```

Otherwise run:

```
<INSTRUMENTED-JAVA-HOME>/bin/java \
    -Xbootclasspath/a:<AGENT-JAR> \
    -javaagent:<AGENT-JAR> \
    [options] class [ argument ... ]
```

Where:

* &lt;<INSTRUMENTED-JAVA-HOME>&gt; is the instrumented Java installation you created.
* &lt;<AGENT-JAR>&gt; is the path to the JAR for Phosphor's Java agent.

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

Please email [Jonathan Bell](mailto:bellj@gmu.edu) with comments, suggestions, or questions.
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
