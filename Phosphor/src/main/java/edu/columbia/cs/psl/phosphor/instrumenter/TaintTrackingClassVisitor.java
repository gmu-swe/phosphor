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
import org.objectweb.asm.*;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.tree.*;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static edu.columbia.cs.psl.phosphor.Configuration.controlFlowManager;
import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.*;
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
    private HashMap<String, MethodNode> methodsToAddWrappersFor = new HashMap();
    private String className;
    private boolean isNormalClass;
    private boolean isInterface;
    private boolean addTaintMethod;
    private boolean isAnnotation;
    private boolean isLambda;
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
    private HashMap<String, Method> superMethodsToOverride = new HashMap<>();
    private LinkedList<MethodNode> wrapperMethodsToAdd = new LinkedList<>();
    private List<FieldNode> extraFieldsToVisit = new LinkedList<>();
    private List<FieldNode> myFields = new LinkedList<>();
    private Set<String> myMethods = new HashSet<>();
    private boolean isAnonymousClassDefinition; //Anonymous classes can NOT contain references to themselves - can not add wrappers that invoke methods of this or supertype

    public TaintTrackingClassVisitor(ClassVisitor cv, boolean skipFrames, List<FieldNode> fields) {
        super(Configuration.ASM_VERSION, cv);
        DO_OPT = DO_OPT && !IS_RUNTIME_INST;
        this.ignoreFrames = skipFrames;
        this.fields = fields;
    }

    public TaintTrackingClassVisitor(ClassVisitor cv, boolean skipFrames, List<FieldNode> fields,
            Set<String> aggressivelyReduceMethodSize, boolean isAnonymousClassDefinition) {
        this(cv, skipFrames, fields);
        this.aggressivelyReduceMethodSize = aggressivelyReduceMethodSize;
        this.isAnonymousClassDefinition = isAnonymousClassDefinition;
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

        isLambda = name.contains("$$Lambda$");

        // Debugging - no more package-protected
        if ((access & Opcodes.ACC_PRIVATE) == 0) {
            access = access | Opcodes.ACC_PUBLIC;
        }

        if (superName != null && !superName.equals("java/lang/Object") && !Instrumenter.isIgnoredClass(superName)) {
            addTaintField = false;
            addTaintMethod = true;
        }
        if (name.equals("java/awt/image/BufferedImage") || name.equals("java/awt/image/Image")) {
            addTaintField = false;
        }
        if (addTaintField) {
            addTaintMethod = true;
        }
        if (superName != null && (superName.equals("java/lang/Object") || Instrumenter.isIgnoredClass(superName)) && !isInterface
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

        if (superName != null && Instrumenter.isIgnoredClass(superName)) {
            //Might need to override stuff.
            Class c;
            try {
                c = Class.forName(superName.replace("/", "."));
                for (Method m : c.getMethods()) {
                    superMethodsToOverride.put(m.getName() + Type.getMethodDescriptor(m), m);
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }


    private HashMap<String, Integer> maxStackPerMethod = new HashMap<>();
    private MethodVisitor createInstrumentingMVChain(int access, String name, String desc, String signature, String[] _exceptions, boolean fetchStackFrame){
        boolean isImplicitLightTrackingMethod = isImplicitLightMethod(className, name, desc);
        String originalName = name;

        //TODO: We used to do this, for reasons related to wrappers. do we still need to?
        // TODO
        if (className.equals("java/lang/invoke/StringConcatFactory")) {
            access = access & ~Opcodes.ACC_VARARGS;
        }

        access = access & ~Opcodes.ACC_FINAL;

        String instrumentedDesc;
        if(isAbstractMethodToWrap(className, name) || fetchStackFrame){
            instrumentedDesc = desc;
        } else {
            instrumentedDesc = TaintUtils.addPhosphorStackFrameToDesc(desc);
        }
        MethodVisitor mv = super.visitMethod(access, name, instrumentedDesc, signature, _exceptions);
        MethodVisitor rootmV = new TaintTagFieldCastMV(mv, name);
        mv = rootmV;
        SpecialOpcodeRemovingMV specialOpcodeRemovingMV = new SpecialOpcodeRemovingMV(mv, ignoreFrames, access,
                className, instrumentedDesc, fixLdcClass);
        mv = specialOpcodeRemovingMV;
        NeverNullArgAnalyzerAdapter analyzer = new NeverNullArgAnalyzerAdapter(className, access, name, instrumentedDesc, mv);
        mv = new DefaultTaintCheckingMethodVisitor(analyzer, access, className, name, instrumentedDesc,
                (implementsSerializable || className.startsWith("java/nio/")
                        || className.equals("java/lang/String")
                        || className.startsWith("java/io/BufferedInputStream") || className.startsWith("sun/nio")),
                analyzer, fields); // TODO - how do we handle directbytebuffers?

        ControlFlowPropagationPolicy controlFlowPolicy = controlFlowManager.createPropagationPolicy(access,
                className, name, instrumentedDesc);

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
        PrimitiveBoxingFixer boxFixer = new PrimitiveBoxingFixer(access, className, name, instrumentedDesc, signature,
                _exceptions, reflectionMasker, analyzer);

        TaintPassingMV tmv = new TaintPassingMV(boxFixer, access, className, name, instrumentedDesc, signature, _exceptions,
                analyzer, rootmV, wrapperMethodsToAdd, controlFlowPolicy, fetchStackFrame);
        tmv.setFields(fields);

        UninstrumentedCompatMV umv = new UninstrumentedCompatMV(access, className, name, instrumentedDesc, signature,
                _exceptions, mv, analyzer, fetchStackFrame);
        InstOrUninstChoosingMV instOrUninstChoosingMV = new InstOrUninstChoosingMV(tmv, umv);

        LocalVariableManager lvs = new LocalVariableManager(access, name, desc, TaintUtils.addPhosphorStackFrameToDesc(desc), instOrUninstChoosingMV, analyzer, mv,
                generateExtraLVDebug, maxStackPerMethod);
        umv.setLocalVariableSorter(lvs);
        boolean isDisabled = Configuration.ignoredMethods.contains(className + "." + originalName + desc);

        boolean reduceThisMethodSize = aggressivelyReduceMethodSize != null
                && aggressivelyReduceMethodSize.contains(name + instrumentedDesc);

        final LocalVariableAdder localVariableAdder = new LocalVariableAdder(lvs, (access & Opcodes.ACC_STATIC) != 0, desc);

        lvs.setLocalVariableAdder(localVariableAdder);
        specialOpcodeRemovingMV.setLVS(lvs);
        TaintLoadCoercer tlc = new TaintLoadCoercer(className, access, name, desc, signature, _exceptions,
                localVariableAdder, instOrUninstChoosingMV, reduceThisMethodSize | isDisabled,
                isImplicitLightTrackingMethod);

        PrimitiveArrayAnalyzer primitiveArrayFixer = new PrimitiveArrayAnalyzer(className, access, name, desc,
                signature, _exceptions, tlc, isImplicitLightTrackingMethod, fixLdcClass,
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
        return prev;
    }
    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] _exceptions) {
        if (Configuration.taintTagFactory.isIgnoredMethod(className, name, desc)) {
            return super.visitMethod(access, name, desc, signature, _exceptions);
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

        // Hack needed for java 7 + integer->tostring fixes
        if ((className.equals("java/lang/Integer") || className.equals("java/lang/Long"))
                && name.equals("toUnsignedString")) {
            access = (access & ~Opcodes.ACC_PRIVATE) | Opcodes.ACC_PUBLIC;
        }
        if (className.equals("java/lang/reflect/Array") && name.equals("newArray")) {
            access = (access & ~Opcodes.ACC_PRIVATE) | Opcodes.ACC_PUBLIC;
        }
        if (Instrumenter.isUnsafeClass(className)){
            access = (access & ~Opcodes.ACC_PRIVATE) | Opcodes.ACC_PUBLIC;
        }

        if (Configuration.WITH_ENUM_BY_VAL && className.equals("java/lang/Enum") && name.equals("clone")) {
            return null;
        }
        if ((className.equals("java/lang/Integer") || className.equals("java/lang/Long"))
                && name.equals("getChars")) {
            access = access | Opcodes.ACC_PUBLIC;
        }
        if (FIELDS_ONLY) { // || isAnnotation
            return super.visitMethod(access, name, desc, signature, _exceptions);
        }

        if (!isUninstMethods && (access & Opcodes.ACC_NATIVE) == 0) {
            // not a native method
            MethodVisitor prev = createInstrumentingMVChain(access, name, desc, signature, _exceptions, isMethodToRetainDescriptor(className, name));
            MethodNode rawMethod = new MethodNode(Configuration.ASM_VERSION, access, name, desc, signature,
                    _exceptions) {
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
                    maxStackPerMethod.put(name+desc, maxStack);
                }

                @Override
                public void visitEnd() {
                    super.visitEnd();
                    this.accept(prev);
                    if(!name.equals("<clinit>") && !desc.contains(PhosphorStackFrame.DESCRIPTOR)) {
                        methodsToAddWrappersFor.put(name+desc, this);
                    }
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
                                    className, name, desc, signature, _exceptions, rawMethod, null, classSource,
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
            final MethodVisitor prev = super.visitMethod(access, name, desc, signature, _exceptions);
            MethodNode rawMethod = new MethodNode(Configuration.ASM_VERSION, access, name, desc, signature,
                    _exceptions) {
                @Override
                public void visitEnd() {
                    super.visitEnd();
                    this.accept(prev);
                    if(!name.equals("<clinit>") && !desc.contains(PhosphorStackFrame.DESCRIPTOR)) {
                        methodsToAddWrappersFor.put(name + desc, this);
                    }
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
        for (MethodNode mn : this.methodsToAddWrappersFor.values()) {
            this.generateMethodWrapper(mn);
        }
        if(!isAnonymousClassDefinition) {
            for (Method m : superMethodsToOverride.values()) {
                int acc = Opcodes.ACC_PUBLIC;
                if (Modifier.isProtected(m.getModifiers()) && isInterface) {
                    continue;
                } else if (Modifier.isPrivate(m.getModifiers())) {
                    continue;
                }
                if (Modifier.isStatic(m.getModifiers())) {
                    acc = acc | Opcodes.ACC_STATIC;
                }
                if (isInterface) {
                    acc = acc | Opcodes.ACC_ABSTRACT;
                } else {
                    acc = acc & ~Opcodes.ACC_ABSTRACT;
                }
                MethodNode mn = new MethodNode(Configuration.ASM_VERSION, acc | Opcodes.ACC_NATIVE, m.getName(), Type.getMethodDescriptor(m), null, null);

                generateMethodWrapper(mn);
            }
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

    private boolean isAbstractMethodToWrap(String className, String methodName){
        return (className.equals("jdk/internal/reflect/ConstructorAccessorImpl") && methodName.equals("newInstance")) ||
                (className.equals("jdk/internal/reflect/MethodAccessorImpl") && methodName.equals("invoke"));
    }

    public static boolean isMethodToRetainDescriptor(String className, String methodName){
        if(methodName.equals("<clinit>")){
            return true;
        }
        //SEE ALSO ReflectionMasker when changing this
        //TODO can i skip this now that we are always making the uninst version be the full method?
        if(className.startsWith("jdk/internal/reflect/Generated") && (methodName.equals("<init>"))) { //these classes are anonymously defined and we can not directly reference the constructor using an invokespecial?
            return true;
        }
        if(className.startsWith("sun/reflect/Generated") && (methodName.equals("<init>"))) { //these classes are anonymously defined and we can not directly reference the constructor using an invokespecial?
            return true;
        }
        return false;
    }
    private void generateMethodWrapper(MethodNode mn) {
        String descOfWrapper;
        String descToCall;
        boolean fetchStackFrame;
        boolean isAbstractMethodToWrap = isAbstractMethodToWrap(className, mn.name);
        boolean isInvertWrapper = isMethodToRetainDescriptor(className, mn.name);
        boolean isNative = (mn.access & Opcodes.ACC_NATIVE) != 0;
        if (!isNative && !isAbstractMethodToWrap && !isInvertWrapper) {
            //Not native. This wrapper won't receive the stack frame, but will retrieve and pass it
            descOfWrapper = mn.desc;
            fetchStackFrame = true;
            descToCall = TaintUtils.addPhosphorStackFrameToDesc(mn.desc);
            if (isAnonymousClassDefinition) {
                MethodVisitor instrumentingMVChain = createInstrumentingMVChain(mn.access, mn.name, mn.desc, mn.signature, mn.exceptions.toArray(new String[0]), true);
                mn.accept(instrumentingMVChain);
                return;
            }
        } else if (mn.desc.contains(PhosphorStackFrame.DESCRIPTOR)) {
            // Might be instrumenting a lambda or something else generated by the JVM
            descOfWrapper = mn.desc.replace(PhosphorStackFrame.DESCRIPTOR, "");
            fetchStackFrame = true;
            descToCall = mn.desc;
        } else {
            fetchStackFrame = false;
            descOfWrapper = TaintUtils.addPhosphorStackFrameToDesc(mn.desc);
            descToCall = mn.desc;
        }

        int acc = mn.access & ~Opcodes.ACC_NATIVE;
        if(isAbstractMethodToWrap(className, mn.name)){
            acc = acc & ~Opcodes.ACC_ABSTRACT; //When we do instrumentation at runtime, we need to use reflection, and the generated constructor might not get instrumented due to class loading circularities.
        }

        MethodVisitor mv = super.visitMethod(acc, mn.name, descOfWrapper, mn.signature, mn.exceptions.toArray(new String[mn.exceptions.size()]));
        if (!fetchStackFrame) {
            mv = new SpecialOpcodeRemovingMV(mv, false, acc, className, descOfWrapper, fixLdcClass);
        }
        GeneratorAdapter ga = new GeneratorAdapter(mv, acc, mn.name, descOfWrapper);

        if(!isNative) {
            visitAnnotations(mv, mn);
        }
        if ((acc & Opcodes.ACC_ABSTRACT) == 0) {
            //Not an abstract method...
            boolean isStatic = (mn.access & Opcodes.ACC_STATIC) != 0;
            boolean isConstructor = mn.name.equals("<init>");

            mv.visitCode();
            if(!isStatic){
                ga.loadThis();
            }
            int stackFrameLocal;
            int shouldPopLocal;
            if(fetchStackFrame) {
                //There are methods that the JDK will internally resolve to, and prefix the arguments
                //with a handle to the underlying object. That handle won't be passed explicitly by the caller.
                String descForStackFrame = mn.desc;
                if (this.className.startsWith("java/lang/invoke/VarHandleReferences")) {
                    if (mn.desc.contains("java/lang/invoke/VarHandle")) {
                        descForStackFrame = mn.desc.replace("Ljava/lang/invoke/VarHandle;", "");
                    }
                }

                if (Configuration.DEBUG_STACK_FRAME_WRAPPERS) {
                    ga.visitLdcInsn(TaintAdapter.getMethodKeyForStackFrame(mn.name, descForStackFrame, false));
                    STACK_FRAME_FOR_METHOD_DEBUG.delegateVisit(ga);
                } else {
                    ga.push(PhosphorStackFrame.hashForDesc(TaintAdapter.getMethodKeyForStackFrame(mn.name, descForStackFrame, false)));
                    STACK_FRAME_FOR_METHOD_FAST.delegateVisit(ga);
                }
                stackFrameLocal = ga.newLocal(Type.getType(PhosphorStackFrame.class));
                shouldPopLocal = ga.newLocal(Type.BOOLEAN_TYPE);
                ga.dup();
                GET_AND_CLEAR_CLEANUP_FLAG.delegateVisit(ga);
                ga.storeLocal(shouldPopLocal);
                ga.storeLocal(stackFrameLocal);

                /*
                Fix for #188: We should ensure at this point that any java.lang.Object arg is the expected wrapper type.
                We should *not* do that inside of Unsafe though, because Phosphor relies on the ability of getReference/putReference
                to set underlying array fields (getting this wrong can manifest as JVM segfaults).
                 */
                if(!this.className.contains("Unsafe")) {
                    Type[] argTypes = ga.getArgumentTypes();
                    for (int i = 0; i < argTypes.length; i++) {
                        if (argTypes[i].getDescriptor().equals("Ljava/lang/Object;")) {
                            ga.loadLocal(stackFrameLocal);
                            if (this.className.startsWith("java/lang/invoke/VarHandleGuards") && mn.name.startsWith("guard_")) {
                                //Gross fix, we will never have the right stack frame set up, but the prev should be right
                                ga.visitFieldInsn(Opcodes.GETFIELD, PhosphorStackFrame.INTERNAL_NAME, "prevFrame", PhosphorStackFrame.DESCRIPTOR);
                                ga.push(i - 1); //Off by one for the VarHandle :)
                            } else {
                                ga.push(i);
                            }
                            ga.loadArg(i);
                            GET_ARG_WRAPPER_GENERIC.delegateVisit(ga);
                            ga.storeArg(i);
                        }
                    }
                }

                /*
                 Fix for #188: If this method is @CallerSensitive, then call Reflection.getCallerClass HERE, and pass
                 it into the instrumented method so that the callerClass is the true caller, rather than this wrapper.
                 */
                if(mn.visibleAnnotations != null){
                    boolean callerSensitive = false;
                    for (AnnotationNode eachAnnotationNode : mn.visibleAnnotations) {
                        if (eachAnnotationNode.desc.equals("Ljdk/internal/reflect/CallerSensitive;")){
                            callerSensitive = true;
                            break;
                        }
                    }
                    if(callerSensitive) {
                        ga.loadLocal(stackFrameLocal);
                        ga.visitMethodInsn(Opcodes.INVOKESTATIC, "jdk/internal/reflect/Reflection", "getCallerClass", "()Ljava/lang/Class;", false);
                        SET_CALLER_CLASS_WRAPPER.delegateVisit(ga);
                    }
                }

                ga.loadArgs();
                ga.loadLocal(stackFrameLocal);

                if (Configuration.DEBUG_STACK_FRAME_WRAPPERS) {
                    ga.dup();
                    ga.visitLdcInsn(TaintAdapter.getMethodKeyForStackFrame(mn.name, descForStackFrame, false));
                    PREPARE_FOR_CALL_DEBUG.delegateVisit(ga);
                }
            } else {
                ga.loadArgs(0, ga.getArgumentTypes().length - 1);
                if(isInvertWrapper){
                    ga.loadArg(ga.getArgumentTypes().length - 1);
                    if (Configuration.DEBUG_STACK_FRAME_WRAPPERS) {
                        ga.visitLdcInsn(TaintAdapter.getMethodKeyForStackFrame(mn.name, descToCall, false));
                        PREPARE_FOR_CALL_DEBUG.delegateVisit(ga);
                    } else {
                        ga.push(PhosphorStackFrame.hashForDesc(TaintAdapter.getMethodKeyForStackFrame(mn.name, descToCall, false)));
                        PREPARE_FOR_CALL_FAST.delegateVisit(ga);
                    }
                }
                stackFrameLocal = -1;
                shouldPopLocal = -1;
            }

            ga.visitMethodInsn(isStatic ? Opcodes.INVOKESTATIC : isAbstractMethodToWrap ? Opcodes.INVOKEVIRTUAL : Opcodes.INVOKESPECIAL, className, mn.name, descToCall, isInterface);

            if (fetchStackFrame) {
                ga.loadLocal(stackFrameLocal);
                ga.loadLocal(shouldPopLocal);
                POP_STACK_FRAME.delegateVisit(ga);
            }
            ga.returnValue();
            mv.visitMaxs(0, 0);

        }
        mv.visitEnd();

    }

    private static void visitAnnotations(MethodVisitor mv, MethodNode fullMethod) {
        if(fullMethod.annotationDefault != null) {
            AnnotationVisitor av = mv.visitAnnotationDefault();
            acceptAnnotationRaw(av, null, fullMethod.annotationDefault);
            av.visitEnd();
        }
        if(fullMethod.visibleAnnotations != null) {
            for(Object o : fullMethod.visibleAnnotations) {
                AnnotationNode an = (AnnotationNode) o;
                an.accept(mv.visitAnnotation(an.desc, true));
            }
        }
        if(fullMethod.invisibleAnnotations != null) {
            for(Object o : fullMethod.invisibleAnnotations) {
                AnnotationNode an = (AnnotationNode) o;
                an.accept(mv.visitAnnotation(an.desc, false));
            }
        }
        if(fullMethod.visibleTypeAnnotations != null) {
            for(Object o : fullMethod.visibleTypeAnnotations) {
                TypeAnnotationNode an = (TypeAnnotationNode) o;
                an.accept(mv.visitTypeAnnotation(an.typeRef, an.typePath, an.desc, true));
            }
        }
        if(fullMethod.invisibleTypeAnnotations != null) {
            for(Object o : fullMethod.invisibleTypeAnnotations) {
                TypeAnnotationNode an = (TypeAnnotationNode) o;
                an.accept(mv.visitTypeAnnotation(an.typeRef, an.typePath, an.desc, false));
            }
        }
        if(fullMethod.parameters != null) {
            for(Object o : fullMethod.parameters) {
                ParameterNode pn = (ParameterNode) o;
                pn.accept(mv);
            }
        }
        if(fullMethod.visibleParameterAnnotations != null) {
            for(int i = 0; i < fullMethod.visibleParameterAnnotations.length; i++) {
                if(fullMethod.visibleParameterAnnotations[i] != null) {
                    for(Object o : fullMethod.visibleParameterAnnotations[i]) {
                        AnnotationNode an = (AnnotationNode) o;
                        an.accept(mv.visitParameterAnnotation(i, an.desc, true));
                    }
                }
            }
        }
        if(fullMethod.invisibleParameterAnnotations != null) {
            for(int i = 0; i < fullMethod.invisibleParameterAnnotations.length; i++) {
                if(fullMethod.invisibleParameterAnnotations[i] != null) {
                    for(Object o : fullMethod.invisibleParameterAnnotations[i]) {
                        AnnotationNode an = (AnnotationNode) o;
                        an.accept(mv.visitParameterAnnotation(i, an.desc, false));
                    }
                }
            }
        }
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
