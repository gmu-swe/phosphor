## Tips

To skip a set of tests add the option `-P-<TYPE>` to Maven commands.
Where &lt;TYPE&gt; is one of "data-flow" or "instrument".
This option will disable the profile which is used to run tests of the specified type.
This is useful if you wish to rerun and debug a single test class.
For example, if you wanted to run and debug only `ArrayLengthObjTagITCase`, you would run:

```bash
mvn -P-instrument -Dmaven.failsafe.debug -Dit.test=ArrayLengthObjTagITCase verify
```

If an instrumented Java installation has already been created, and you want to run a minimal amount of plugins when 
running a test you have to invoke the maven-dependency-plugin before failsafe.
For example,
```bash
mvn -P-instrument -Dit.test=ArrayLengthObjTagITCase dependency:properties failsafe:integration-test@data-flow
```

Add the option `-Dphosphor.forceCreation` to Maven commands to force the phosphor-instrument-maven-plugin to create a
new instrumented Java installation.
This will also delete associated caches of dynamically instrumented classes.