package edu.columbia.cs.psl.phosphor;

import edu.columbia.cs.psl.phosphor.control.ControlFlowManager;
import edu.columbia.cs.psl.phosphor.control.standard.StandardControlFlowManager;
import edu.columbia.cs.psl.phosphor.instrumenter.TaintTagFactory;
import edu.columbia.cs.psl.phosphor.runtime.TaintSourceWrapper;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.EnumMap;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.StringBuilder;
import org.apache.commons.cli.*;
import org.objectweb.asm.ClassVisitor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public enum PhosphorOption {

    CONTROL_TRACK(new PhosphorOptionBuilder("Enable taint tracking through control flow", true, false)
            .group(PhosphorOptionGroup.CONTROL_PROPAGATION)) {
        @Override
        public void configure(boolean forRuntimeInst, boolean isPresent, CommandLine commandLine) {
            if(forRuntimeInst && isPresent) {
                Configuration.IMPLICIT_TRACKING = true;
            } else if(!forRuntimeInst) {
                Configuration.IMPLICIT_TRACKING = isPresent;
            }
        }
    },
    LIGHT_CONTROL_TRACK(new PhosphorOptionBuilder("Enable taint tracking through control flow, but does not " +
            "propagate control dependencies between methods", true, true)
            .alternativeName("lightImplicit")
            .group(PhosphorOptionGroup.CONTROL_PROPAGATION)) {
        @Override
        public void configure(boolean forRuntimeInst, boolean isPresent, CommandLine commandLine) {
            Configuration.IMPLICIT_LIGHT_TRACKING = isPresent;
        }
    },
    CONTROL_TRACK_EXCEPTIONS(new PhosphorOptionBuilder("Enable taint tracking through exceptional control flow",
            true, true)
            .alternativeName("implicitExceptions")) {
        @Override
        public void configure(boolean forRuntimeInst, boolean isPresent, CommandLine commandLine) {
            if(forRuntimeInst && isPresent) {
                Configuration.IMPLICIT_EXCEPTION_FLOW = true;
            } else if(!forRuntimeInst) {
                Configuration.IMPLICIT_EXCEPTION_FLOW = isPresent;
            }
        }
    },
    WITHOUT_BRANCH_NOT_TAKEN(new PhosphorOptionBuilder("Disable branch not taken analysis in control tracking",
            true, true)) {
        @Override
        public void configure(boolean forRuntimeInst, boolean isPresent, CommandLine commandLine) {
            Configuration.WITHOUT_BRANCH_NOT_TAKEN = isPresent;
        }
    },
    WITH_ARRAY_INDEX_TAGS(new PhosphorOptionBuilder("Propagates taint tags from array indices to values get/set",
            true, true)
            .alternativeName("arrayindex")) {
        @Override
        public void configure(boolean forRuntimeInst, boolean isPresent, CommandLine commandLine) {
            Configuration.ARRAY_INDEX_TRACKING = isPresent;
        }
    },
    WITH_ENUMS_BY_VALUE(new PhosphorOptionBuilder("Propagate tags to enums as if each enum were a value (not a reference) " +
            "through the Enum.valueOf method", true, true).alternativeName("enum")) {
        @Override
        public void configure(boolean forRuntimeInst, boolean isPresent, CommandLine commandLine) {
            Configuration.WITH_ENUM_BY_VAL = isPresent;
        }
    },
    FORCE_UNBOX_ACMP_EQ(new PhosphorOptionBuilder("At each object equality comparison, ensure that all operands are unboxed " +
            "(and not boxed types, which may not pass the test)", true, true).alternativeName("acmpeq")) {
        @Override
        public void configure(boolean forRuntimeInst, boolean isPresent, CommandLine commandLine) {
            Configuration.WITH_UNBOX_ACMPEQ = isPresent;
        }
    },
    READ_AND_SAVE_BCI(new PhosphorOptionBuilder("Read in and track the byte code index of every instruction during instrumentation",
            true, false)) {
        @Override
        public void configure(boolean forRuntimeInst, boolean isPresent, CommandLine commandLine) {
            Configuration.READ_AND_SAVE_BCI = isPresent;
        }
    },
    SERIALIZATION(new PhosphorOptionBuilder("Read and write taint tags through Java Serialization", true, true)) {
        @Override
        public void configure(boolean forRuntimeInst, boolean isPresent, CommandLine commandLine) {
            if(forRuntimeInst && isPresent) {
                Configuration.TAINT_THROUGH_SERIALIZATION = true;
            }
        }
    },
    SKIP_LOCALS(new PhosphorOptionBuilder("Do not output local variable debug tables for generated local variables " +
            "(useful for avoiding warnings from D8)", true, false)) {
        @Override
        public void configure(boolean forRuntimeInst, boolean isPresent, CommandLine commandLine) {
            Configuration.SKIP_LOCAL_VARIABLE_TABLE = isPresent;
        }
    },
    ALWAYS_CHECK_FOR_FRAMES(new PhosphorOptionBuilder("Always check to ensure that class files with version > Java 8 ACTUALLY have " +
            "frames - useful for instrumenting android-targeting code that is compiled with Java 8 but without frames",
            true, false)) {
        @Override
        public void configure(boolean forRuntimeInst, boolean isPresent, CommandLine commandLine) {
            Configuration.ALWAYS_CHECK_FOR_FRAMES = isPresent;
        }
    },
    REENABLE_CACHES(new PhosphorOptionBuilder("Prevent Phosphor from disabling caches.", true, true)) {
        @Override
        public void configure(boolean forRuntimeInst, boolean isPresent, CommandLine commandLine) {
            Configuration.REENABLE_CACHES = isPresent;
        }
    },
    IMPLICIT_HEADERS_NO_TRACKING(new PhosphorOptionBuilder("Add method headers for doing implicit tracking, but " +
            "don't actually propagate them", true, false)
            .group(PhosphorOptionGroup.CONTROL_PROPAGATION)) {
        @Override
        public void configure(boolean forRuntimeInst, boolean isPresent, CommandLine commandLine) {
            Configuration.IMPLICIT_HEADERS_NO_TRACKING = isPresent;
        }
    },
    QUIET(new PhosphorOptionBuilder("Reduces the amount of command line output produced by Phosphor.", true, true)
            .alternativeName("q")) {
        @Override
        public void configure(boolean forRuntimeInst, boolean isPresent, CommandLine commandLine) {
            Configuration.QUIET_MODE = isPresent;
        }
    },
    PRIOR_CLASS_VISITOR(new PhosphorOptionBuilder("Specify the class name for a ClassVisitor class to be added to Phosphor's visitor " +
            "chain before taint tracking is added to the class.", true, true).argType(Class.class)) {
        @Override
        public void configure(boolean forRuntimeInst, boolean isPresent, CommandLine commandLine) {
            if(isPresent) {
                try {
                    @SuppressWarnings("unchecked")
                    Class<? extends ClassVisitor> clazz = (Class<? extends ClassVisitor>) commandLine.getParsedOptionValue(optionName);
                    Configuration.PRIOR_CLASS_VISITOR = clazz;
                } catch(ParseException e) {
                    System.err.println("Failed to create specified prior class visitor: " + optionName);
                }
            } else {
                Configuration.PRIOR_CLASS_VISITOR = null;
            }
        }
    },
    CONTROL_FLOW_MANAGER(new PhosphorOptionBuilder("Can be used to specify the name of a class to be used as the ControlFlowManager " +
            "during instrumentation. This class must implement ControlFlowManager.", true, true)
            .argType(Class.class)
            .group(PhosphorOptionGroup.CONTROL_PROPAGATION)) {
        @Override
        public void configure(boolean forRuntimeInst, boolean isPresent, CommandLine commandLine) {
            if(isPresent) {
                try {
                    @SuppressWarnings("unchecked")
                    Class<? extends ControlFlowManager> clazz = (Class<? extends ControlFlowManager>) commandLine.getParsedOptionValue(optionName);
                    if(clazz != null) {
                        Configuration.controlFlowManager = clazz.newInstance();
                        Configuration.IMPLICIT_TRACKING = true;
                    }
                } catch(Exception e) {
                    System.err.println("Failed to create control propagation manager: " + commandLine.getOptionValue(optionName));
                }
            } else {
                Configuration.controlFlowManager = new StandardControlFlowManager();
            }
        }
    },
    CACHE_DIR(new PhosphorOptionBuilder("Directory for caching generated files", false, true)
            .argType(String.class)) {
        @Override
        public void configure(boolean forRuntimeInst, boolean isPresent, CommandLine commandLine) {
            if(isPresent) {
                Configuration.CACHE_DIR = commandLine.getOptionValue(optionName);
                File f = new File(Configuration.CACHE_DIR);
                if(!f.exists()) {
                    if(!f.mkdir()) {
                        // The cache directory did not exist and the attempt to create it failed
                        System.err.printf("Failed to create cache directory: %s. Generated files are not being cached.\n", Configuration.CACHE_DIR);
                        Configuration.CACHE_DIR = null;
                    }
                }
            } else {
                Configuration.CACHE_DIR = null;
            }
        }
    },
    WITH_HEAVY_OBJ_EQUALS_HASHCODE(new PhosphorOptionBuilder(null, true, true).alternativeName("objmethods")) {
        @Override
        public void configure(boolean forRuntimeInst, boolean isPresent, CommandLine commandLine) {
            if(isPresent) {
                Configuration.WITH_HEAVY_OBJ_EQUALS_HASHCODE = true;
            }
        }
    },
    TAINT_SOURCES(new PhosphorOptionBuilder(null, false, true).argType(String.class)) {
        @Override
        public void configure(boolean forRuntimeInst, boolean isPresent, CommandLine commandLine) {
            if(isPresent) {
                String value = commandLine.getOptionValue(optionName);
                try {
                    Instrumenter.sourcesFile = new FileInputStream(value);
                } catch(FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    },
    TAINT_SINKS(new PhosphorOptionBuilder(null, false, true).argType(String.class)) {
        @Override
        public void configure(boolean forRuntimeInst, boolean isPresent, CommandLine commandLine) {
            if(isPresent) {
                String value = commandLine.getOptionValue(optionName);
                try {
                    Instrumenter.sinksFile = new FileInputStream(value);
                } catch(FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    },
    TAINT_THROUGH(new PhosphorOptionBuilder(null, false, true).argType(String.class)) {
        @Override
        public void configure(boolean forRuntimeInst, boolean isPresent, CommandLine commandLine) {
            if(isPresent) {
                String value = commandLine.getOptionValue(optionName);
                try {
                    Instrumenter.taintThroughFile = new FileInputStream(value);
                } catch(FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    },
    TAINT_SOURCE_WRAPPER(new PhosphorOptionBuilder(null, false, true).argType(Class.class)) {
        @Override
        public void configure(boolean forRuntimeInst, boolean isPresent, CommandLine commandLine) {
            if(isPresent) {
                try {
                    @SuppressWarnings("unchecked")
                    Class<? extends TaintSourceWrapper<?>> clazz = (Class<? extends TaintSourceWrapper<?>>) commandLine.getParsedOptionValue(optionName);
                    if(clazz != null) {
                        Configuration.autoTainter = clazz.newInstance();
                    }
                } catch(Exception e) {
                    System.err.println("Failed to create taint source wrapper: " + commandLine.getOptionValue(optionName));
                }
            }
        }
    },
    TAINT_TAG_FACTORY(new PhosphorOptionBuilder(null, false, true).argType(Class.class)) {
        @Override
        public void configure(boolean forRuntimeInst, boolean isPresent, CommandLine commandLine) {
            if(isPresent) {
                try {
                    @SuppressWarnings("unchecked")
                    Class<? extends TaintTagFactory> clazz = (Class<? extends TaintTagFactory>) commandLine.getParsedOptionValue(optionName);
                    if(clazz != null) {
                        Configuration.taintTagFactory = clazz.newInstance();
                    }
                } catch(Exception e) {
                    System.err.println("Failed to create taint tag factory: " + commandLine.getOptionValue(optionName));
                }
            }
        }
    },
    IGNORE(new PhosphorOptionBuilder(null, false, true).argType(String.class)) {
        @Override
        public void configure(boolean forRuntimeInst, boolean isPresent, CommandLine commandLine) {
            if(isPresent) {
                Configuration.ADDL_IGNORE = commandLine.getOptionValue(optionName);
            }
        }
    },
    IGNORED_METHOD(new PhosphorOptionBuilder(null, false, true).argType(String.class)) {
        @Override
        public void configure(boolean forRuntimeInst, boolean isPresent, CommandLine commandLine) {
            if(isPresent) {
                Configuration.ignoredMethods.add(commandLine.getOptionValue(optionName));
            }
        }
    };

    final String optionName;
    private final Option.Builder builder;
    private final PhosphorOptionGroup group;
    private final boolean dynamicOption;
    private final boolean staticOption;

    PhosphorOption(PhosphorOptionBuilder phosphorBuilder) {
        String name = createName(this);
        if(phosphorBuilder.alternativeName != null) {
            builder = Option.builder(phosphorBuilder.alternativeName).longOpt(name);
            optionName = phosphorBuilder.alternativeName;
        } else {
            builder = Option.builder(name);
            optionName = name;
        }
        if(phosphorBuilder.desc != null) {
            builder.desc(phosphorBuilder.desc);
        }
        if(phosphorBuilder.argType != null) {
            builder.type(phosphorBuilder.argType).hasArg();
        }
        group = phosphorBuilder.group;
        dynamicOption = phosphorBuilder.dynamicOption;
        staticOption = phosphorBuilder.staticOption;
    }

    public abstract void configure(boolean forRuntimeInst, boolean isPresent, CommandLine commandLine);

    public Option createOption() {
        return builder.build();
    }

    private static String createName(PhosphorOption option) {
        StringBuilder builder = new StringBuilder();
        char[] charArray = option.toString().toCharArray();
        boolean capitalizeNext = false;
        for(char c : charArray) {
            if(c == '_') {
                capitalizeNext = true;
            } else if(capitalizeNext) {
                builder.append(c);
                capitalizeNext = false;
            } else {
                char lower = (char) (c + ('a' - 'A'));
                builder.append(lower);
            }
        }
        return builder.toString();
    }

    public static Options createOptions(boolean forRuntimeInst) {
        Options options = new Options();
        if(!forRuntimeInst) {
            options.addOption(new Option("help", "Prints this message"));
        }
        EnumMap<PhosphorOptionGroup, OptionGroup> groupMap = new EnumMap<>(PhosphorOptionGroup.class);
        for(PhosphorOption phosphorOption : values()) {
            boolean enabled = forRuntimeInst ? phosphorOption.dynamicOption : phosphorOption.staticOption;
            if(enabled) {
                if(phosphorOption.group == PhosphorOptionGroup.GENERAL) {
                    options.addOption(phosphorOption.createOption());
                } else {
                    if(!groupMap.containsKey(phosphorOption.group)) {
                        groupMap.put(phosphorOption.group, new OptionGroup());
                    }
                    groupMap.get(phosphorOption.group).addOption(phosphorOption.createOption());
                }
            }
        }
        for(OptionGroup group : groupMap.values()) {
            options.addOptionGroup(group);
        }
        return options;
    }

    public static CommandLine configure(boolean forRuntimeInst, String[] args) {
        CommandLineParser parser = new DefaultParser();
        Options options = createOptions(forRuntimeInst);
        CommandLine line;
        try {
            line = parser.parse(options, args);
        } catch(org.apache.commons.cli.ParseException exp) {
            if(forRuntimeInst) {
                System.err.println(exp.getMessage());
                return null;
            }
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar phosphor.jar [OPTIONS] [input] [output]", options);
            System.err.println(exp.getMessage());
            if(exp.getMessage().contains("-multiTaint")) {
                System.err.println("Note: the -multiTaint option has been removed, and is now enabled by default (int tags no longer exist)");
            }
            return null;
        }
        if(!forRuntimeInst && (line.hasOption("help") || line.getArgs().length != 2)) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar phosphor.jar [OPTIONS] [input] [output]", options);
            return null;
        }
        for(PhosphorOption phosphorOption : values()) {
            phosphorOption.configure(forRuntimeInst, line.hasOption(phosphorOption.optionName), line);
        }
        return line;
    }

    private enum PhosphorOptionGroup {
        GENERAL, CONTROL_PROPAGATION
    }

    private static final class PhosphorOptionBuilder {
        String desc;
        PhosphorOptionGroup group = PhosphorOptionGroup.GENERAL;
        boolean dynamicOption;
        boolean staticOption;
        String alternativeName = null;
        Class<?> argType = null;

        PhosphorOptionBuilder(String desc, boolean staticOption, boolean dynamicOption) {
            this.desc = desc;
            this.staticOption = staticOption;
            this.dynamicOption = dynamicOption;
        }

        PhosphorOptionBuilder group(PhosphorOptionGroup group) {
            this.group = group;
            return this;
        }

        PhosphorOptionBuilder alternativeName(String alternativeName) {
            this.alternativeName = alternativeName;
            return this;
        }

        PhosphorOptionBuilder argType(Class<?> argType) {
            this.argType = argType;
            return this;
        }
    }
}
