package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.control.ControlFlowPropagationPolicy;
import edu.columbia.cs.psl.phosphor.control.ControlFlowStack;
import edu.columbia.cs.psl.phosphor.control.ControlStackInitializingMV;
import edu.columbia.cs.psl.phosphor.control.ControlStackRestoringMV;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import edu.columbia.cs.psl.phosphor.runtime.PhosphorStackFrame;
import edu.columbia.cs.psl.phosphor.runtime.TaintInstrumented;
import edu.columbia.cs.psl.phosphor.runtime.TaintSourceWrapper;
import edu.columbia.cs.psl.phosphor.struct.TaintedWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.*;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static edu.columbia.cs.psl.phosphor.Configuration.controlFlowManager;
import static org.objectweb.asm.Opcodes.ALOAD;

/**
 * CV responsibilities: Add a field to classes to track each instance's taint
 * Add a method for each primitive returning method to return the taint of that
 * return Add a field to hold temporarily the return taint of each primitive
 *
 * @author jon
 */
public class TaintTrackingClassVisitor extends ClassVisitor {

    public static final String CONTROL_STACK_DESC = Type.getDescriptor(ControlFlowStack.class);
    public static final String CONTROL_STACK_INTERNAL_NAME = Type.getInternalName(ControlFlowStack.class);
    public static final Type CONTROL_STACK_TYPE = Type.getType(ControlFlowStack.class);

    private static final boolean NATIVE_BOX_UNBOX = true;
    public static boolean IS_RUNTIME_INST = true;
    private static boolean FIELDS_ONLY = false;
    private static boolean DO_OPT = false;

    static {
        if (!DO_OPT && !IS_RUNTIME_INST) {
            System.err.println("WARN: OPT DISABLED");
        }
    }

    private List<FieldNode> fields;
    private boolean generateHashCode = false;
    private boolean generateEquals = false;
    private Map<MethodNode, MethodNode> forMore = new HashMap<>();
    private boolean hasSerialUID = false;
    private boolean addTaintField = false;
    private boolean ignoreFrames;
    private boolean generateExtraLVDebug;
    private Map<MethodNode, Type> methodsToAddWrappersForWithReturnType = new HashMap<>();
    private List<MethodNode> methodsToAddWrappersFor = new LinkedList<>();
    private List<MethodNode> methodsToAddNameOnlyWrappersFor = new LinkedList<>();
    private List<MethodNode> methodsToAddUnWrappersFor = new LinkedList<>();
    private Set<MethodNode> methodsToAddLambdaUnWrappersFor = new HashSet<>();
    private String className;
    private boolean isNormalClass;
    private boolean isInterface;
    private boolean addTaintMethod;
    private boolean isAnnotation;
    private boolean isAbstractClass;
    private boolean implementsComparable;
    private boolean implementsSerializable;
    private boolean fixLdcClass;
    private boolean isEnum;
    private boolean isUninstMethods;
    private String classSource;
    private String classDebug;
    private Set<String> aggressivelyReduceMethodSize;
    private String superName;
    private boolean isLambda;
    private HashMap<String, Method> superMethodsToOverride = new HashMap<>();
    private HashMap<String, Method> methodsWithErasedTypesToAddWrappersFor = new HashMap<>();
    private LinkedList<MethodNode> wrapperMethodsToAdd = new LinkedList<>();
    private List<FieldNode> extraFieldsToVisit = new LinkedList<>();
    private List<FieldNode> myFields = new LinkedList<>();
    private Set<String> myMethods = new HashSet<>();

    public TaintTrackingClassVisitor(ClassVisitor cv, boolean skipFrames, List<FieldNode> fields) {
        super(Configuration.ASM_VERSION, cv);
        DO_OPT = DO_OPT && !IS_RUNTIME_INST;
        this.ignoreFrames = skipFrames;
        this.fields = fields;
    }

    public TaintTrackingClassVisitor(ClassVisitor cv, boolean skipFrames, List<FieldNode> fields,
            Set<String> aggressivelyReduceMethodSize) {
        this(cv, skipFrames, fields);
        this.aggressivelyReduceMethodSize = aggressivelyReduceMethodSize;
    }

    @Override
    public void visitSource(String source, String debug) {
        super.visitSource(source, debug);
        this.classSource = source;
        this.classDebug = debug;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        addTaintField = true;
        addTaintMethod = true;
        this.generateExtraLVDebug = name.equals("java/lang/invoke/MethodType");
        this.fixLdcClass = (version & 0xFFFF) < Opcodes.V1_5;
        if ((access & Opcodes.ACC_ABSTRACT) != 0) {
            isAbstractClass = true;
        }
        if ((access & Opcodes.ACC_INTERFACE) != 0) {
            addTaintField = false;
            isInterface = true;
        }
        if ((access & Opcodes.ACC_ENUM) != 0) {
            isEnum = true;
            addTaintField = false;
        }

        if ((access & Opcodes.ACC_ANNOTATION) != 0) {
            isAnnotation = true;
        }

        // Debugging - no more package-protected
        if ((access & Opcodes.ACC_PRIVATE) == 0) {
            access = access | Opcodes.ACC_PUBLIC;
        }

        if (!superName.equals("java/lang/Object") && !Instrumenter.isIgnoredClass(superName)) {
            addTaintField = false;
            addTaintMethod = true;
        }
        if (name.equals("java/awt/image/BufferedImage") || name.equals("java/awt/image/Image")) {
            addTaintField = false;
        }
        if (addTaintField) {
            addTaintMethod = true;
        }
        if ((superName.equals("java/lang/Object") || Instrumenter.isIgnoredClass(superName)) && !isInterface
                && !isAnnotation) {
            generateEquals = true;
            generateHashCode = true;
        }
        isLambda = name.contains("$$Lambda$");

        isNormalClass = (access & Opcodes.ACC_ENUM) == 0 && (access & Opcodes.ACC_INTERFACE) == 0;

        if ((isEnum || name.equals("java/lang/Enum")) && Configuration.WITH_ENUM_BY_VAL) {
            boolean alreadyHas = false;
            for (String s : interfaces) {
                if (s.equals("java/lang/Cloneable")) {
                    alreadyHas = true;
                    break;
                }
            }
            if (!alreadyHas) {
                String[] newIntfcs = new String[interfaces.length + 1];
                System.arraycopy(interfaces, 0, newIntfcs, 0, interfaces.length);
                newIntfcs[interfaces.length] = "java/lang/Cloneable";
                interfaces = newIntfcs;
                if (signature != null) {
                    signature = signature + "Ljava/lang/Cloneable;";
                }
            }
        }
        if (isNormalClass && !Instrumenter.isIgnoredClass(name) && !FIELDS_ONLY) {
            String[] newIntfcs = new String[interfaces.length + 1];
            System.arraycopy(interfaces, 0, newIntfcs, 0, interfaces.length);
            newIntfcs[interfaces.length] = Type.getInternalName(TaintedWithObjTag.class);
            interfaces = newIntfcs;
            if (signature != null) {
                signature = signature + Type.getDescriptor(TaintedWithObjTag.class);
            }
        }
        for (String s : interfaces) {
            if (s.equals(Type.getInternalName(Comparable.class))) {
                implementsComparable = true;
            } else if (s.equals(Type.getInternalName(Serializable.class))) {
                implementsSerializable = true;
            }
        }
        super.visit(version, access, name, signature, superName, interfaces);
        this.visitAnnotation(Type.getDescriptor(TaintInstrumented.class), false);
        this.className = name;
        this.superName = superName;

        //this.isUninstMethods = Instrumenter.isIgnoredClassWithStubsButNoTracking(className);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (Configuration.taintTagFactory.isIgnoredMethod(className, name, desc)) {
            return super.visitMethod(access, name, desc, signature, exceptions);
        }
        if (name.equals("hashCode") && desc.equals("()I")) {
            generateHashCode = false;
        }
        if (name.equals("equals") && desc.equals("(Ljava/lang/Object;)Z")) {
            generateEquals = false;
        }
        superMethodsToOverride.remove(name + desc);
        if (name.equals("compareTo")) {
            implementsComparable = false;
        }
        this.myMethods.add(name + desc);

        boolean isImplicitLightTrackingMethod = isImplicitLightMethod(className, name, desc);
        // Hack needed for java 7 + integer->tostring fixes
        if ((className.equals("java/lang/Integer") || className.equals("java/lang/Long"))
                && name.equals("toUnsignedString")) {
            access = (access & ~Opcodes.ACC_PRIVATE) | Opcodes.ACC_PUBLIC;
        }
        if (className.equals("java/lang/reflect/Array") && name.equals("newArray")) {
            access = (access & ~Opcodes.ACC_PRIVATE) | Opcodes.ACC_PUBLIC;
        }

        if (Configuration.WITH_ENUM_BY_VAL && className.equals("java/lang/Enum") && name.equals("clone")) {
            return null;
        }
        if ((className.equals("java/lang/Integer") || className.equals("java/lang/Long"))
                && name.equals("getChars")) {
            access = access | Opcodes.ACC_PUBLIC;
        }
        String originalName = name;
        if (FIELDS_ONLY) { // || isAnnotation
            return super.visitMethod(access, name, desc, signature, exceptions);
        }

        if (!isUninstMethods && (access & Opcodes.ACC_NATIVE) == 0) {
            // not a native method


            //TODO: We used to do this, for reasons related to wrappers. do we still need to?
            // TODO
            if (className.equals("java/lang/invoke/StringConcatFactory")) {
                access = access & ~Opcodes.ACC_VARARGS;
            }

            access = access & ~Opcodes.ACC_FINAL;

            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            MethodVisitor rootmV = new TaintTagFieldCastMV(mv, name);
            mv = rootmV;
            SpecialOpcodeRemovingMV specialOpcodeRemovingMV = new SpecialOpcodeRemovingMV(mv, ignoreFrames, access,
                    className, desc, fixLdcClass);
            mv = specialOpcodeRemovingMV;
            NeverNullArgAnalyzerAdapter analyzer = new NeverNullArgAnalyzerAdapter(className, access, name, desc, mv);
            mv = new DefaultTaintCheckingMethodVisitor(analyzer, access, className, name, desc,
                    (implementsSerializable || className.startsWith("java/nio/")
                            || className.startsWith("java/io/BUfferedInputStream") || className.startsWith("sun/nio")),
                    analyzer, fields); // TODO - how do we handle directbytebuffers?

            ControlFlowPropagationPolicy controlFlowPolicy = controlFlowManager.createPropagationPolicy(access,
                    className, name, desc);

            ControlStackInitializingMV controlStackInitializingMV = null;
            ControlStackRestoringMV controlStackRestoringMV = null;
            MethodVisitor next = mv;
            if (ControlStackInitializingMV.isApplicable(isImplicitLightTrackingMethod)) {
                controlStackInitializingMV = new ControlStackInitializingMV(next, controlFlowPolicy);
                next = controlStackInitializingMV;
            }
            if (ControlStackRestoringMV.isApplicable(name)) {
                controlStackRestoringMV = new ControlStackRestoringMV(next, rootmV, className, name, controlFlowPolicy);
                next = controlStackRestoringMV;
            }
            ReflectionHidingMV reflectionMasker = new ReflectionHidingMV(next, className, name, isEnum);
            PrimitiveBoxingFixer boxFixer = new PrimitiveBoxingFixer(access, className, name, desc, signature,
                    exceptions, reflectionMasker, analyzer);

            TaintPassingMV tmv = new TaintPassingMV(boxFixer, access, className, name, desc, signature, exceptions,
                    analyzer, rootmV, wrapperMethodsToAdd, controlFlowPolicy);
            tmv.setFields(fields);

            UninstrumentedCompatMV umv = new UninstrumentedCompatMV(access, className, name, desc, signature,
                    exceptions, mv, analyzer);
            InstOrUninstChoosingMV instOrUninstChoosingMV = new InstOrUninstChoosingMV(tmv, umv);

            LocalVariableManager lvs = new LocalVariableManager(access, desc, instOrUninstChoosingMV, analyzer, mv,
                    generateExtraLVDebug);
            umv.setLocalVariableSorter(lvs);
            boolean isDisabled = Configuration.ignoredMethods.contains(className + "." + originalName + desc);

            boolean reduceThisMethodSize = aggressivelyReduceMethodSize != null
                    && aggressivelyReduceMethodSize.contains(name + desc);

            final LocalVariableAdder localVariableAdder = new LocalVariableAdder(lvs, (access & Opcodes.ACC_STATIC) != 0, desc);

            lvs.setLocalVariableAdder(localVariableAdder);
            specialOpcodeRemovingMV.setLVS(lvs);
            TaintLoadCoercer tlc = new TaintLoadCoercer(className, access, name, desc, signature, exceptions,
                    localVariableAdder, instOrUninstChoosingMV, reduceThisMethodSize | isDisabled,
                    isImplicitLightTrackingMethod);

            PrimitiveArrayAnalyzer primitiveArrayFixer = new PrimitiveArrayAnalyzer(className, access, name, desc,
                    signature, exceptions, tlc, isImplicitLightTrackingMethod, fixLdcClass,
                    controlFlowPolicy.getFlowAnalyzer());
            NeverNullArgAnalyzerAdapter preAnalyzer = new NeverNullArgAnalyzerAdapter(className, access, name, desc,
                    primitiveArrayFixer);

            controlFlowPolicy.initialize(boxFixer, lvs, analyzer);

            primitiveArrayFixer.setAnalyzer(preAnalyzer);
            boxFixer.setLocalVariableSorter(lvs);
            tmv.setLocalVariableSorter(lvs);
            if (controlStackInitializingMV != null) {
                controlStackInitializingMV.setLocalVariableManager(lvs);
            }
            if (controlStackRestoringMV != null) {
                controlStackRestoringMV.setArrayAnalyzer(primitiveArrayFixer);
                controlStackRestoringMV.setLocalVariableManager(lvs);
            }
            lvs.setPrimitiveArrayAnalyzer(primitiveArrayFixer);
            reflectionMasker.setLvs(lvs);
            final MethodVisitor prev = preAnalyzer;
            MethodNode rawMethod = new MethodNode(Configuration.ASM_VERSION, access, name, desc, signature,
                    exceptions) {
                @Override
                protected LabelNode getLabelNode(Label l) {
                    if (!Configuration.READ_AND_SAVE_BCI) {
                        return super.getLabelNode(l);
                    }
                    if (!(l.info instanceof LabelNode)) {
                        l.info = new LabelNode(l);
                    }
                    return (LabelNode) l.info;
                }

                @Override
                public void visitMaxs(int maxStack, int maxLocals) {
                    super.visitMaxs(maxStack, maxLocals);
                    localVariableAdder.setMaxStack(maxStack);
                    lvs.setNumLocalVariablesAddedAfterArgs(localVariableAdder.getNumLocalVariablesAddedAfterArgs());
                }

                @Override
                public void visitEnd() {
                    super.visitEnd();
                    this.accept(prev);
                }

                @Override
                public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
                    if (className.equals("com/sleepycat/je/log/FileManager$LogEndFileDescriptor")
                            && name.startsWith("enqueueWrite1") && local.length > 6
                            && "java/lang/Object".equals(local[6])) {
                        local[6] = "[B";
                    }
                    super.visitFrame(type, nLocal, local, nStack, stack);
                }
            };

            if (Configuration.extensionMethodVisitor != null) {
                try {
                    TaintAdapter custom = Configuration.extensionMethodVisitor.getConstructor(Integer.TYPE,
                            String.class, String.class, String.class, String.class, String[].class, MethodVisitor.class,
                            NeverNullArgAnalyzerAdapter.class, String.class, String.class).newInstance(access,
                                    className, name, desc, signature, exceptions, rawMethod, null, classSource,
                                    classDebug);
                    custom.setFields(fields);
                    custom.setSuperName(superName);
                    return custom;
                } catch (InstantiationException | SecurityException | NoSuchMethodException | InvocationTargetException
                        | IllegalArgumentException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            return rawMethod;
        } else {
            // this is a native method. we want here to make a $taint method that will call
            // the original one.
            final MethodVisitor prev = super.visitMethod(access, name, desc, signature, exceptions);
            MethodNode rawMethod = new MethodNode(Configuration.ASM_VERSION, access, name, desc, signature,
                    exceptions) {
                @Override
                public void visitEnd() {
                    super.visitEnd();
                    this.accept(prev);
                }
            };
            return rawMethod;
        }
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        //if (Instrumenter.isIgnoredClassWithStubsButNoTracking(className)) {
        //    return super.visitField(access, name, desc, signature, value);
        //}
        if (shouldMakeFieldPublic(className, name)) {
            access = access & ~Opcodes.ACC_PRIVATE;
            access = access & ~Opcodes.ACC_PROTECTED;
            access = access | Opcodes.ACC_PUBLIC;
        }
        Type fieldType = Type.getType(desc);
        if (TaintUtils.isShadowedType(fieldType)) {
            if (TaintAdapter.canRawTaintAccess(className)) {
                extraFieldsToVisit.add(new FieldNode(access, name + TaintUtils.TAINT_FIELD,
                        TaintUtils.getShadowTaintType(desc), null, null));
            } else {
                extraFieldsToVisit.add(new FieldNode(access, name + TaintUtils.TAINT_FIELD, "I", null, null));
            }
        }
        if (TaintUtils.isWrappedTypeWithSeparateField(fieldType)) {
            extraFieldsToVisit.add(new FieldNode(access, name + TaintUtils.TAINT_WRAPPER_FIELD,
                    TaintUtils.getWrapperType(fieldType).getDescriptor(), null, null));
            if ((access & Opcodes.ACC_STATIC) == 0) {
                access = access | Opcodes.ACC_TRANSIENT;
            }
        }
        if (!hasSerialUID && name.equals("serialVersionUID")) {
            hasSerialUID = true;
        }
        if ((access & Opcodes.ACC_STATIC) == 0) {
            myFields.add(new FieldNode(access, name, desc, signature, value));
        }
        return super.visitField(access, name, desc, signature, value);
    }

    private boolean shouldMakeFieldPublic(String className, String name) {
        return className.equals("java/lang/String") && (name.equals("value"));
    }

    @Override
    public void visitEnd() {

        if(className.equals("java/lang/Thread")){
            super.visitField(Opcodes.ACC_PUBLIC, "phosphorStackFrame", PhosphorStackFrame.DESCRIPTOR, null, null);
        }

        if ((isEnum || className.equals("java/lang/Enum")) && Configuration.WITH_ENUM_BY_VAL) {
            MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC, "clone", "()Ljava/lang/Object;", null,
                    new String[] {"java/lang/CloneNotSupportedException"});
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "clone", "()Ljava/lang/Object;", false);

            mv.visitInsn(Opcodes.ARETURN);
            mv.visitEnd();
            mv.visitMaxs(0, 0);
        }
        boolean goLightOnGeneratedStuff = className.equals("java/lang/Byte");// || isLambda;
        if (!hasSerialUID && !isInterface && !goLightOnGeneratedStuff) {
            super.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "serialVersionUIDPHOSPHOR_TAG",
                    Configuration.TAINT_TAG_DESC, null, null);
        }
        // Add a field to track the instance's taint
        if (addTaintField && !goLightOnGeneratedStuff) {
            super.visitField(Opcodes.ACC_PUBLIC, TaintUtils.TAINT_FIELD,
                    TaintAdapter.getTagType(className).getDescriptor(), null, null);
            // Add an int field that can be used to mark an instance as visited when
            // searching
            super.visitField(Opcodes.ACC_PUBLIC, TaintUtils.MARK_FIELD, "I", null, Integer.MIN_VALUE);
        }
        if (this.className.equals("java/lang/reflect/Method")) {
            super.visitField(Opcodes.ACC_PUBLIC, TaintUtils.TAINT_FIELD + "marked", "Z", null, 0);
            super.visitField(Opcodes.ACC_PUBLIC, TaintUtils.TAINT_FIELD + "method", "Ljava/lang/reflect/Method;", null,
                    0);
        } else if (this.className.equals("java/lang/Class")) {
            super.visitField(Opcodes.ACC_PUBLIC, TaintUtils.TAINT_FIELD + "marked", "Z", null, 0);
            super.visitField(Opcodes.ACC_PUBLIC, TaintUtils.TAINT_FIELD + "class", "Ljava/lang/Class;", null, 0);
            super.visitField(Opcodes.ACC_PUBLIC, TaintUtils.CLASS_OFFSET_CACHE_ADDED_FIELD,
                    "Ledu/columbia/cs/psl/phosphor/struct/SinglyLinkedList;", null, 0);
        }
        for (FieldNode fn : extraFieldsToVisit) {
            if (className.equals("java/lang/Byte") && !fn.name.startsWith("value")) {
                continue;
            }
            if (isNormalClass) {
                fn.access = fn.access & ~Opcodes.ACC_FINAL;
                fn.access = fn.access & ~Opcodes.ACC_PRIVATE;
                fn.access = fn.access & ~Opcodes.ACC_PROTECTED;
                fn.access = fn.access | Opcodes.ACC_PUBLIC;
            }
            if ((fn.access & Opcodes.ACC_STATIC) != 0) {
                if (fn.desc.equals("I")) {
                    super.visitField(fn.access, fn.name, fn.desc, fn.signature, 0);
                } else {
                    super.visitField(fn.access, fn.name, fn.desc, fn.signature, null);
                }
            } else {
                super.visitField(fn.access, fn.name, fn.desc, fn.signature, null);
            }
        }
        if (FIELDS_ONLY) {
            return;
        }

        if (addTaintMethod) {
            if (isInterface) {
                super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, "get" + TaintUtils.TAINT_FIELD,
                        "()Ljava/lang/Object;", null, null);
                super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, "set" + TaintUtils.TAINT_FIELD,
                        "(Ljava/lang/Object;)V", null, null);
            } else {
                MethodVisitor mv;
                mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, "get" + TaintUtils.TAINT_FIELD, "()Ljava/lang/Object;", null,
                        null);
                mv = new TaintTagFieldCastMV(mv, "get" + TaintUtils.TAINT_FIELD);
                mv.visitCode();
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(Opcodes.GETFIELD, className, TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_DESC);

                mv.visitInsn(Opcodes.ARETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();

                mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, "set" + TaintUtils.TAINT_FIELD, "(Ljava/lang/Object;)V",
                        null, null);
                mv = new TaintTagFieldCastMV(mv, "set" + TaintUtils.TAINT_FIELD);

                mv.visitCode();
                Configuration.taintTagFactory.generateSetTag(mv, className);
                if (className.equals("java/lang/String")) {
                    // Also overwrite the taint tag of all of the chars behind this string

                    Type taintType = MultiDTaintedArray.getTypeForType(Type.getType(char[].class));
                    mv.visitFieldInsn(Opcodes.GETSTATIC, Type.getInternalName(Configuration.class), "autoTainter",
                            Type.getDescriptor(TaintSourceWrapper.class));
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitVarInsn(ALOAD, 1);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(TaintSourceWrapper.class), "setStringTaintTag", "(Ljava/lang/String;" + Configuration.TAINT_TAG_DESC + ")V", false);
                } else if((className.equals(TaintPassingMV.INTEGER_NAME) || className.equals(TaintPassingMV.LONG_NAME)
                        || className.equals(TaintPassingMV.FLOAT_NAME) || className.equals(TaintPassingMV.DOUBLE_NAME))) {
                    //For primitive types, also set the "value" field
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitVarInsn(ALOAD, 1);
                    mv.visitFieldInsn(Opcodes.PUTFIELD, className, "value" + TaintUtils.TAINT_FIELD,
                            Configuration.TAINT_TAG_DESC);
                }
                mv.visitInsn(Opcodes.RETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }
        }

        super.visitEnd();
    }

    private static void acceptAnnotationRaw(final AnnotationVisitor av, final String name, final Object value) {
        if (av != null) {
            if (value instanceof String[]) {
                String[] typeConst = (String[]) value;
                av.visitEnum(name, typeConst[0], typeConst[1]);
            } else if (value instanceof AnnotationNode) {
                AnnotationNode an = (AnnotationNode) value;
                an.accept(av.visitAnnotation(name, an.desc));
            } else if (value instanceof java.util.List) {
                AnnotationVisitor v = av.visitArray(name);
                java.util.List<?> array = (java.util.List<?>) value;
                for (Object o : array) {
                    acceptAnnotationRaw(v, null, o);
                }
                v.visitEnd();
            } else {
                av.visit(name, value);
            }
        }
    }


    public static boolean isImplicitLightMethod(String owner, String name, String desc) {
        return Configuration.autoTainter.shouldInstrumentMethodForImplicitLightTracking(owner, name, desc);
    }
}
