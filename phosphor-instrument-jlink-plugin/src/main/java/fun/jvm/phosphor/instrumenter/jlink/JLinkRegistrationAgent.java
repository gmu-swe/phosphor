package fun.jvm.phosphor.instrumenter.jlink;

import java.lang.instrument.Instrumentation;
import java.util.*;


/**
 * Based heavily on hibernate-demos JLinkPluginRegistrationAgent, licensed under Apache License Version 2.0
 * https://github.com/hibernate/hibernate-demos/blob/master/java9/custom-jlink-plugin/agent/src/main/java/org/hibernate/demos/jlink/agent/JLinkPluginRegistrationAgent.java
 */
public class JLinkRegistrationAgent {
    public static void premain(String agentArgs, Instrumentation inst) throws Exception {
        Module jlinkModule = ModuleLayer.boot().findModule("jdk.jlink").get();
        Module phosphorModule = ModuleLayer.boot().findModule("fun.jvm.phosphor.instrumenter.jlink").get();
        Map<String, Set<Module>> extraExports = new HashMap<>();
        extraExports.put("jdk.tools.jlink.plugin", Collections.singleton(phosphorModule));

        // alter jdk.jlink to export its API to the module with our indexing plug-in
        inst.redefineModule(jlinkModule,
                Collections.emptySet(),
                extraExports,
                Collections.emptyMap(),
                Collections.emptySet(),
                Collections.emptyMap()
        );

        Class<?> pluginClass = jlinkModule.getClassLoader().loadClass("jdk.tools.jlink.plugin.Plugin");
        Class<?> addPhosphorPluginClass = phosphorModule.getClassLoader().loadClass("fun.jvm.phosphor.instrumenter.jlink.PhosphorJLinkPlugin");

        Map<Class<?>, List<Class<?>>> extraProvides = new HashMap<>();
        extraProvides.put(pluginClass, Collections.singletonList(addPhosphorPluginClass));

        // alter the module with the phosphor plug-in so it provides the plug-in as a service
        inst.redefineModule(phosphorModule,
                Collections.emptySet(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptySet(),
                extraProvides
        );

    }
}
