package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import edu.columbia.cs.psl.phosphor.runtime.*;
import edu.columbia.cs.psl.phosphor.struct.*;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.LinkedList;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.StringBuilder;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.*;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArrayWithObjTag;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.tree.*;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.*;

/**
 * CV responsibilities: Add a field to classes to track each instance's taint
 * Add a method for each primitive returning method to return the taint of that
 * return Add a field to hold temporarily the return taint of each primitive
 *
 * @author jon
 */
public class TaintTrackingClassVisitor extends ClassVisitor {
    private static final boolean NATIVE_BOX_UNBOX = true;
    public static boolean IS_RUNTIME_INST = true;
    private static boolean FIELDS_ONLY = false;
    private static boolean DO_OPT = false;

    static {
        if(!DO_OPT && !IS_RUNTIME_INST) {
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
    private String classSource;
    private String classDebug;
    private boolean aggressivelyReduceMethodSize;
    private String superName;
    private boolean isLambda;
    private HashMap<String, Method> superMethodsToOverride = new HashMap<>();
    private HashSet<MethodNode> wrapperMethodsToAdd = new HashSet<>();
    private List<FieldNode> extraFieldsToVisit = new LinkedList<>();
    private List<FieldNode> myFields = new LinkedList<>();
    private List<MethodNode> myMethods = new LinkedList<>();

    public TaintTrackingClassVisitor(ClassVisitor cv, boolean skipFrames, List<FieldNode> fields) {
        super(Configuration.ASM_VERSION, cv);
        DO_OPT = DO_OPT && !IS_RUNTIME_INST;
        this.ignoreFrames = skipFrames;
        this.fields = fields;
    }

    public TaintTrackingClassVisitor(ClassVisitor cv, boolean skipFrames, List<FieldNode> fields, boolean aggressivelyReduceMethodSize) {
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
        if(Instrumenter.IS_KAFFE_INST && name.endsWith("java/lang/VMSystem")) {
            access = access | Opcodes.ACC_PUBLIC;
        } else if(Instrumenter.IS_HARMONY_INST && name.endsWith("java/lang/VMMemoryManager")) {
            access = access & ~Opcodes.ACC_PRIVATE;
            access = access | Opcodes.ACC_PUBLIC;
        }
        if((access & Opcodes.ACC_ABSTRACT) != 0) {
            isAbstractClass = true;
        }
        if((access & Opcodes.ACC_INTERFACE) != 0) {
            addTaintField = false;
            isInterface = true;
        }
        if((access & Opcodes.ACC_ENUM) != 0) {
            isEnum = true;
            addTaintField = false;
        }

        if((access & Opcodes.ACC_ANNOTATION) != 0) {
            isAnnotation = true;
        }

        if(!superName.equals("java/lang/Object") && !Instrumenter.isIgnoredClass(superName)) {
            addTaintField = false;
            addTaintMethod = true;
        }
        if(name.equals("java/awt/image/BufferedImage") || name.equals("java/awt/image/Image")) {
            addTaintField = false;
        }
        if(addTaintField) {
            addTaintMethod = true;
        }
        if((superName.equals("java/lang/Object") || Instrumenter.isIgnoredClass(superName)) && !isInterface && !isAnnotation) {
            generateEquals = true;
            generateHashCode = true;
        }
        isLambda = name.contains("$$Lambda$");

        isNormalClass = (access & Opcodes.ACC_ENUM) == 0 && (access & Opcodes.ACC_INTERFACE) == 0;

        if((isEnum || name.equals("java/lang/Enum")) && Configuration.WITH_ENUM_BY_VAL) {
            boolean alreadyHas = false;
            for(String s : interfaces) {
                if(s.equals("java/lang/Cloneable")) {
                    alreadyHas = true;
                    break;
                }
            }
            if(!alreadyHas) {
                String[] newIntfcs = new String[interfaces.length + 1];
                System.arraycopy(interfaces, 0, newIntfcs, 0, interfaces.length);
                newIntfcs[interfaces.length] = "java/lang/Cloneable";
                interfaces = newIntfcs;
                if(signature != null) {
                    signature = signature + "Ljava/lang/Cloneable;";
                }
            }
        }
        if(isNormalClass && !Instrumenter.isIgnoredClass(name) && !FIELDS_ONLY) {
            String[] newIntfcs = new String[interfaces.length + 1];
            System.arraycopy(interfaces, 0, newIntfcs, 0, interfaces.length);
            newIntfcs[interfaces.length] = Type.getInternalName(TaintedWithObjTag.class);
            interfaces = newIntfcs;
            if(signature != null) {
                signature = signature + Type.getDescriptor(TaintedWithObjTag.class);
            }
            if(generateEquals && Configuration.WITH_HEAVY_OBJ_EQUALS_HASHCODE) {
                newIntfcs = new String[interfaces.length + 1];
                System.arraycopy(interfaces, 0, newIntfcs, 0, interfaces.length);
                Class<?> iface;
                if((Configuration.IMPLICIT_HEADERS_NO_TRACKING || Configuration.IMPLICIT_TRACKING)) {
                    iface = TaintedObjectWithObjCtrlTag.class;
                } else {
                    iface = TaintedObjectWithObjTag.class;
                }
                newIntfcs[interfaces.length] = Type.getInternalName(iface);
                interfaces = newIntfcs;
                if(signature != null) {
                    signature = signature + Type.getDescriptor(iface);
                }
            }
        }
        for(String s : interfaces) {
            if(s.equals(Type.getInternalName(Comparable.class))) {
                implementsComparable = true;
            } else if(s.equals(Type.getInternalName(Serializable.class))) {
                implementsSerializable = true;
            }
        }
        super.visit(version, access, name, signature, superName, interfaces);
        this.visitAnnotation(Type.getDescriptor(TaintInstrumented.class), false);
        if(Instrumenter.isIgnoredClass(superName)) {
            //Might need to override stuff.
            Class<?> c;
            try {
                c = Class.forName(superName.replace("/", "."));
                for(Method m : c.getMethods()) {
                    superMethodsToOverride.put(m.getName() + Type.getMethodDescriptor(m), m);
                }
            } catch(ClassNotFoundException e) {
                if(!superName.startsWith("sun/reflect/")) {
                    e.printStackTrace();
                }
            }
        }
        this.className = name;
        this.superName = superName;
    }

    private void collectUninstrumentedInterfaceMethods(String[] interfaces) {
        String superToCheck;
        if(interfaces != null) {
            for(String itfc : interfaces) {
                superToCheck = itfc;
                try {
                    ClassNode cn = Instrumenter.classes.get(superToCheck);
                    if(cn != null) {
                        String[] s = new String[cn.interfaces.size()];
                        s = cn.interfaces.toArray(s);
                        collectUninstrumentedInterfaceMethods(s);
                        continue;
                    }
                    Class<?> c = Class.forName(superToCheck.replace("/", "."), false, Instrumenter.loader);
                    if(Instrumenter.isIgnoredClass(superToCheck)) {
                        for(Method m : c.getDeclaredMethods()) {
                            if(!Modifier.isPrivate(m.getModifiers())) {
                                superMethodsToOverride.put(m.getName() + Type.getMethodDescriptor(m), m);
                            }
                        }
                    }
                    Class<?>[] in = c.getInterfaces();
                    if(in != null && in.length > 0) {
                        String[] s = new String[in.length];
                        for(int i = 0; i < in.length; i++) {
                            s[i] = Type.getInternalName(in[i]);
                        }
                        collectUninstrumentedInterfaceMethods(s);
                    }
                } catch(Exception ex) {
                    break;
                }

            }
        }
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if(name.equals("hashCode") && desc.equals("()I")) {
            generateHashCode = false;
        }
        if(name.equals("equals") && desc.equals("(Ljava/lang/Object;)Z")) {
            generateEquals = false;
        }
        superMethodsToOverride.remove(name + desc);
        if(name.equals("compareTo")) {
            implementsComparable = false;
        }

        boolean isImplicitLightTrackingMethod = Configuration.autoTainter.shouldInstrumentMethodForImplicitLightTracking(className, name, desc);
        // Hack needed for java 7 + integer->tostring fixes
        if((className.equals("java/lang/Integer") || className.equals("java/lang/Long")) && name.equals("toUnsignedString")) {
            access = (access & ~Opcodes.ACC_PRIVATE) | Opcodes.ACC_PUBLIC;
        }

        if(name.contains(TaintUtils.METHOD_SUFFIX) || (Type.getReturnType(desc).getSort() == Type.VOID && desc.contains("phosphor/struct/Tainted"))) {
            if(isLambda) {
                //lambda wrapper method
                methodsToAddUnWrappersFor.add(new MethodNode(access, name, desc, signature, exceptions));
                return super.visitMethod(access, name, desc, signature, exceptions);
            }
            //Some dynamic stuff might result in there being weird stuff here
            return new MethodVisitor(Configuration.ASM_VERSION) {
            };
        }
        if(Configuration.WITH_ENUM_BY_VAL && className.equals("java/lang/Enum") && name.equals("clone")) {
            return null;
        }
        if(TaintUtils.DEBUG_CALLS || TaintUtils.DEBUG_FIELDS || TaintUtils.DEBUG_FRAMES || TaintUtils.DEBUG_LOCAL) {
            System.out.println("Instrumenting " + name + "\n\n\n\n\n\n");
        }

        if(Instrumenter.IS_KAFFE_INST && className.equals("java/lang/VMSystem")) {
            access = access | Opcodes.ACC_PUBLIC;
        } else if(Instrumenter.IS_HARMONY_INST && className.endsWith("java/lang/VMMemoryManager")) {
            access = access & ~Opcodes.ACC_PRIVATE;
            access = access | Opcodes.ACC_PUBLIC;
        } else if((className.equals("java/lang/Integer") || className.equals("java/lang/Long")) && name.equals("getChars")) {
            access = access | Opcodes.ACC_PUBLIC;
        }
        String originalName = name;
        if(FIELDS_ONLY) { // || isAnnotation
            return super.visitMethod(access, name, desc, signature, exceptions);
        }

        if(originalName.contains("$$INVIVO")) {
            name = name + "_orig";
        }


        //We will need to add shadow args for each parameter that is a primitive. Because that worked so well last time.
        Type[] argTypes = Type.getArgumentTypes(desc);
        LinkedList<Type> newArgTypes = new LinkedList<>();
        boolean isRewrittenDesc = false;
        if(isLambda) {
            for(Type t : argTypes) {
                newArgTypes.add(t);
                if(t.getDescriptor().contains("ControlTaintTagStack") || t.getDescriptor().contains("edu/columbia/cs/psl/phosphor/struct") || t.getDescriptor().contains("Ledu/columbia/cs/psl/phosphor/runtime/Taint") || TaintUtils.isPrimitiveType(t)) {
                    final MethodVisitor cmv = super.visitMethod(access, name, desc, signature, exceptions);
                    MethodNode fullMethod = new MethodNode(Configuration.ASM_VERSION, access, name, desc, signature, exceptions) {
                        @Override
                        public void visitEnd() {
                            super.visitEnd();
                            this.accept(cmv);
                        }
                    };
                    methodsToAddNameOnlyWrappersFor.add(fullMethod);
                    return fullMethod;
                }
            }
        } else {
            for (Type t : argTypes) {
                if (TaintUtils.isShadowedType(t)) {
                    newArgTypes.add(Type.getType(TaintUtils.getShadowTaintType(t.getDescriptor())));
                    isRewrittenDesc = true;
                }
                if (TaintUtils.isWrappedType(t)) {
                    newArgTypes.add(TaintUtils.getWrapperType(t));
                    isRewrittenDesc = true;
                } else {
                    newArgTypes.add(t);
                }
            }
        }
        if((Configuration.IMPLICIT_HEADERS_NO_TRACKING || Configuration.IMPLICIT_TRACKING) && !name.equals("<clinit>")) {
            isRewrittenDesc = true;
            newArgTypes.add(Type.getType(ControlTaintTagStack.class));
        }
        if((isRewrittenDesc) && name.equals("<init>")) {
            newArgTypes.add(Type.getType(TaintSentinel.class));
        }
        //If we are rewriting the return type, also add a param to pass for pre-alloc
        Type oldReturnType = Type.getReturnType(desc);
        Type newReturnType = TaintUtils.getContainerReturnType(Type.getReturnType(desc));
        if((oldReturnType.getSort() != Type.VOID && oldReturnType.getSort() != Type.OBJECT && oldReturnType.getSort() != Type.ARRAY)) {
            newArgTypes.add(newReturnType);
        }
        Type[] newArgs = new Type[newArgTypes.size()];
        newArgs = newArgTypes.toArray(newArgs);

        boolean requiresNoChange = !isRewrittenDesc && newReturnType.equals(Type.getReturnType(desc));
        MethodNode wrapper = new MethodNode(access, name, desc, signature, exceptions);
        if(!requiresNoChange && !name.equals("<clinit>") && !(name.equals("<init>") && !isRewrittenDesc)) {
            methodsToAddWrappersFor.add(wrapper);
        }
        String newDesc = Type.getMethodDescriptor(newReturnType, newArgs);
        if((access & Opcodes.ACC_NATIVE) == 0) {
            //not a native method
            LinkedList<String> addToSig = new LinkedList<>();
            if(Configuration.IMPLICIT_TRACKING) {
                addToSig.add(Type.getInternalName(ControlTaintTagStack.class));
            }
            if(name.equals("<init>") && isRewrittenDesc) {
                addToSig.add(Type.getInternalName(TaintSentinel.class));
            }
            if((oldReturnType.getSort() != Type.VOID && oldReturnType.getSort() != Type.OBJECT && oldReturnType.getSort() != Type.ARRAY)) {
                addToSig.add(newReturnType.getInternalName());
            }
            signature = TaintUtils.remapSignature(signature, addToSig);
            if(!name.contains("<") && !requiresNoChange) {
                name = name + TaintUtils.METHOD_SUFFIX;
            }

            MethodVisitor mv = super.visitMethod(access, name, newDesc, signature, exceptions);
            mv = new TaintTagFieldCastMV(mv, name);

            MethodVisitor rootmV = mv;
            MethodVisitor optimizer;
            optimizer = mv;
            SpecialOpcodeRemovingMV somv = new SpecialOpcodeRemovingMV(optimizer, ignoreFrames, access, className, newDesc, fixLdcClass);
            mv = somv;

            NeverNullArgAnalyzerAdapter analyzer = new NeverNullArgAnalyzerAdapter(className, access, name, newDesc, mv);
            mv = new StringTaintVerifyingMV(analyzer, (implementsSerializable || className.startsWith("java/nio/") || className.startsWith("java/io/BUfferedInputStream") || className.startsWith("sun/nio")), analyzer); //TODO - how do we handle directbytebuffers?
            ControlStackInitializingMV controlStackInitializingMV = null;
            ControlStackRestoringMV controlStackRestoringMV = null;
            MethodVisitor next = mv;
            if(ControlStackInitializingMV.isApplicable(isImplicitLightTrackingMethod)) {
                controlStackInitializingMV = new ControlStackInitializingMV(next, isImplicitLightTrackingMethod);
                next = controlStackInitializingMV;
            }
            if(ControlStackRestoringMV.isApplicable(name)) {
                controlStackRestoringMV = new ControlStackRestoringMV(next, rootmV, className, name);
                next = controlStackRestoringMV;
            }
            ReflectionHidingMV reflectionMasker = new ReflectionHidingMV(next, className, name, analyzer);
            PrimitiveBoxingFixer boxFixer = new PrimitiveBoxingFixer(access, className, name, desc, signature, exceptions, reflectionMasker, analyzer);

            TaintPassingMV tmv = new TaintPassingMV(boxFixer, access, className, name, newDesc, signature, exceptions, desc, analyzer, rootmV, wrapperMethodsToAdd, isImplicitLightTrackingMethod);
            tmv.setFields(fields);
            UninstrumentedCompatMV umv = new UninstrumentedCompatMV(access, className, name, newDesc, oldReturnType, signature, exceptions, boxFixer, analyzer, ignoreFrames);
            InstOrUninstChoosingMV instOrUninstChoosingMV = new InstOrUninstChoosingMV(tmv, umv);
            LocalVariableManager lvs = new LocalVariableManager(access, newDesc, instOrUninstChoosingMV, analyzer, mv, generateExtraLVDebug);
            umv.setLocalVariableSorter(lvs);
            boolean isDisabled = Configuration.ignoredMethods.contains(className + "." + originalName + desc);

            somv.setLVS(lvs);
            MethodArgReindexer mar = new MethodArgReindexer(lvs, access, name, newDesc, desc, wrapper, isLambda);
            TaintLoadCoercer tlc = new TaintLoadCoercer(className, access, name, desc, signature, exceptions, mar, ignoreFrames, instOrUninstChoosingMV, aggressivelyReduceMethodSize | isDisabled, isImplicitLightTrackingMethod);

            PrimitiveArrayAnalyzer primitiveArrayFixer = new PrimitiveArrayAnalyzer(className, access, name, desc, signature, exceptions, tlc, isImplicitLightTrackingMethod);
            NeverNullArgAnalyzerAdapter preAnalyzer = new NeverNullArgAnalyzerAdapter(className, access, name, desc, primitiveArrayFixer);

            primitiveArrayFixer.setAnalyzer(preAnalyzer);
            boxFixer.setLocalVariableSorter(lvs);
            tmv.setArrayAnalyzer(primitiveArrayFixer);
            tmv.setLocalVariableSorter(lvs);
            if(controlStackInitializingMV != null) {
                controlStackInitializingMV.setArrayAnalyzer(primitiveArrayFixer);
                controlStackInitializingMV.setLocalVariableManager(lvs);
            }
            if(controlStackRestoringMV != null) {
                controlStackRestoringMV.setArrayAnalyzer(primitiveArrayFixer);
                controlStackRestoringMV.setLocalVariableManager(lvs);
            }
            lvs.setPrimitiveArrayAnalyzer(primitiveArrayFixer); // this guy will tell the LVS what return types to prealloc
            reflectionMasker.setLvs(lvs);
            final MethodVisitor prev = preAnalyzer;
            MethodNode rawMethod = new MethodNode(Configuration.ASM_VERSION, access, name, desc, signature, exceptions) {
                @Override
                protected LabelNode getLabelNode(Label l) {
                    if(!Configuration.READ_AND_SAVE_BCI) {
                        return super.getLabelNode(l);
                    }
                    if(!(l.info instanceof LabelNode)) {
                        l.info = new LabelNode(l);
                    }
                    return (LabelNode) l.info;
                }

                @Override
                public void visitEnd() {
                    super.visitEnd();
                    this.accept(prev);
                }

                @Override
                public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
                    if(className.equals("com/sleepycat/je/log/FileManager$LogEndFileDescriptor") && name.startsWith("enqueueWrite1") && local.length > 6 && "java/lang/Object".equals(local[6])) {
                        local[6] = "[B";
                    }
                    super.visitFrame(type, nLocal, local, nStack, stack);
                }
            };
            if(!isInterface && !originalName.contains("$$INVIVO")) {
                this.myMethods.add(rawMethod);
            }
            forMore.put(wrapper, rawMethod);

            if(Configuration.extensionMethodVisitor != null) {
                try {
                    TaintAdapter custom = Configuration.extensionMethodVisitor.getConstructor(Integer.TYPE, String.class, String.class, String.class, String.class, String[].class, MethodVisitor.class,
                            NeverNullArgAnalyzerAdapter.class, String.class, String.class).newInstance(access, className, name, desc, signature, exceptions, rawMethod, null, classSource, classDebug);
                    custom.setFields(fields);
                    custom.setSuperName(superName);
                    return custom;
                } catch(InstantiationException | SecurityException | NoSuchMethodException | InvocationTargetException | IllegalArgumentException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            return rawMethod;
        } else {
            // this is a native method. we want here to make a $taint method that will call the original one.
            final MethodVisitor prev = super.visitMethod(access, name, desc, signature, exceptions);
            MethodNode rawMethod = new MethodNode(Configuration.ASM_VERSION, access, name, desc, signature, exceptions) {
                @Override
                public void visitEnd() {
                    super.visitEnd();
                    this.accept(prev);
                }
            };
            forMore.put(wrapper, rawMethod);
            return rawMethod;
        }
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        if(shouldMakeFieldPublic(className, name)) {
            access = access & ~Opcodes.ACC_PRIVATE;
            access = access & ~Opcodes.ACC_PROTECTED;
            access = access | Opcodes.ACC_PUBLIC;
        }
        Type fieldType = Type.getType(desc);
        if(TaintUtils.isShadowedType(fieldType)) {
            if(TaintAdapter.canRawTaintAccess(className)) {
                extraFieldsToVisit.add(new FieldNode(access, name + TaintUtils.TAINT_FIELD, TaintUtils.getShadowTaintType(desc), null, null));
            } else {
                extraFieldsToVisit.add(new FieldNode(access, name + TaintUtils.TAINT_FIELD, "I", null, null));
            }
        } else if(!FIELDS_ONLY && fieldType.getSort() == Type.ARRAY && fieldType.getElementType().getSort() != Type.OBJECT && fieldType.getDimensions() > 1) {
            desc = MultiDTaintedArray.getTypeForType(fieldType).getDescriptor();
        }
        if (TaintUtils.isWrappedTypeWithSeparateField(fieldType)) {
            extraFieldsToVisit.add(new FieldNode(access, name + TaintUtils.TAINT_WRAPPER_FIELD, TaintUtils.getWrapperType(fieldType).getDescriptor(), null, null));
            access = access | Opcodes.ACC_TRANSIENT;
        }
        if (!hasSerialUID && name.equals("serialVersionUID")) {
            hasSerialUID = true;
        }
        if((access & Opcodes.ACC_STATIC) == 0) {
            myFields.add(new FieldNode(access, name, desc, signature, value));
        }
        return super.visitField(access, name, desc, signature, value);
    }

    private boolean shouldMakeFieldPublic(String className, String name) {
        return className.equals("java/lang/String") && (name.equals("value"));
    }

    @Override
    public void visitEnd() {

        for(MethodNode mn : wrapperMethodsToAdd) {
            mn.accept(this);
        }

        if((isEnum || className.equals("java/lang/Enum")) && Configuration.WITH_ENUM_BY_VAL) {
            MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC, "clone", "()Ljava/lang/Object;", null, new String[]{"java/lang/CloneNotSupportedException"});
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "clone", "()Ljava/lang/Object;", false);

            mv.visitInsn(Opcodes.ARETURN);
            mv.visitEnd();
            mv.visitMaxs(0, 0);
        }
        boolean goLightOnGeneratedStuff = className.equals("java/lang/Byte");// || isLambda;
        if(!hasSerialUID && !isInterface && !goLightOnGeneratedStuff) {
            super.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "serialVersionUIDPHOSPHOR_TAG", Configuration.TAINT_TAG_DESC, null, null);
        }
        //Add a field to track the instance's taint
        if(addTaintField && !goLightOnGeneratedStuff) {
            super.visitField(Opcodes.ACC_PUBLIC, TaintUtils.TAINT_FIELD, TaintAdapter.getTagType(className).getDescriptor(), null, null);
            // Add an int field that can be used to mark an instance as visited when searching
            super.visitField(Opcodes.ACC_PUBLIC, TaintUtils.MARK_FIELD, "I", null, Integer.MIN_VALUE);
        }
        if(this.className.equals("java/lang/reflect/Method")) {
            super.visitField(Opcodes.ACC_PUBLIC, TaintUtils.TAINT_FIELD + "marked", "Z", null, 0);
            super.visitField(Opcodes.ACC_PUBLIC, TaintUtils.TAINT_FIELD + "method", "Ljava/lang/reflect/Method;", null, 0);
        } else if(this.className.equals("java/lang/Class")) {
            super.visitField(Opcodes.ACC_PUBLIC, TaintUtils.TAINT_FIELD + "marked", "Z", null, 0);
            super.visitField(Opcodes.ACC_PUBLIC, TaintUtils.TAINT_FIELD + "class", "Ljava/lang/Class;", null, 0);
            super.visitField(Opcodes.ACC_PUBLIC, TaintUtils.CLASS_OFFSET_CACHE_ADDED_FIELD, "Ledu/columbia/cs/psl/phosphor/struct/SinglyLinkedList;", null, 0);
        }
        for(FieldNode fn : extraFieldsToVisit) {
            if(className.equals("java/lang/Byte") && !fn.name.startsWith("value")) {
                continue;
            }
            if(isNormalClass) {
                fn.access = fn.access & ~Opcodes.ACC_FINAL;
                fn.access = fn.access & ~Opcodes.ACC_PRIVATE;
                fn.access = fn.access & ~Opcodes.ACC_PROTECTED;
                fn.access = fn.access | Opcodes.ACC_PUBLIC;
            }
            if((fn.access & Opcodes.ACC_STATIC) != 0) {
                if(fn.desc.equals("I")) {
                    super.visitField(fn.access, fn.name, fn.desc, fn.signature, 0);
                } else {
                    super.visitField(fn.access, fn.name, fn.desc, fn.signature, null);
                }
            } else {
                super.visitField(fn.access, fn.name, fn.desc, fn.signature, null);
            }
        }
        if(FIELDS_ONLY) {
            return;
        }
        if((isAbstractClass || isInterface) && implementsComparable && !goLightOnGeneratedStuff) {
            //Need to add this to interfaces so that we can call it on the interface
            if((Configuration.IMPLICIT_HEADERS_NO_TRACKING || Configuration.IMPLICIT_TRACKING)) {
                super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, "compareTo$$PHOSPHORTAGGED", "(Ljava/lang/Object;" + Type.getDescriptor(ControlTaintTagStack.class) + Configuration.TAINTED_INT_DESC + ")" + Configuration.TAINTED_INT_DESC, null, null);
            } else {
                super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, "compareTo$$PHOSPHORTAGGED", "(Ljava/lang/Object;" + Configuration.TAINTED_INT_DESC + ")" + Configuration.TAINTED_INT_DESC, null, null);
            }
        }

        if(generateEquals && !goLightOnGeneratedStuff) {

            superMethodsToOverride.remove("equals(Ljava/lang/Object;)Z");
            methodsToAddWrappersFor.add(new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_NATIVE, "equals", "(Ljava/lang/Object;)Z", null, null));
            MethodVisitor mv;
            mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, "equals", "(Ljava/lang/Object;)Z", null, null);
            mv.visitCode();
            Label start = new Label();
            Label end = new Label();
            mv.visitLabel(start);
            if(isLambda) {
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                Label eq = new Label();
                mv.visitJumpInsn(Opcodes.IF_ACMPEQ, eq);
                mv.visitInsn(Opcodes.ICONST_0);
                mv.visitInsn(Opcodes.IRETURN);
                mv.visitLabel(eq);
                mv.visitFrame(Opcodes.F_NEW, 2, new Object[]{className, "java/lang/Object"}, 0, new Object[]{});
                mv.visitInsn(Opcodes.ICONST_1);
            } else {
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "equals", "(Ljava/lang/Object;)Z", false);
            }
            mv.visitLabel(end);
            mv.visitInsn(Opcodes.IRETURN);
            mv.visitLocalVariable("this", "L" + className + ";", null, start, end, 0);
            mv.visitLocalVariable("other", "Ljava/lang/Object;", null, start, end, 1);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        if(generateHashCode && !goLightOnGeneratedStuff) {
            superMethodsToOverride.remove("hashCode()I");
            methodsToAddWrappersFor.add(new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_NATIVE, "hashCode", "()I", null, null));
            MethodVisitor mv;
            mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, "hashCode", "()I", null, null);
            mv.visitCode();
            Label start = new Label();
            Label end = new Label();
            mv.visitLabel(start);
            if(isLambda) {
                mv.visitInsn(Opcodes.ICONST_0);
            } else {
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "hashCode", "()I", false);
            }
            mv.visitLabel(end);
            mv.visitInsn(Opcodes.IRETURN);
            mv.visitLocalVariable("this", "L" + className + ";", null, start, end, 0);

            mv.visitMaxs(0, 0);
            mv.visitEnd();

        }
        if(addTaintMethod) {
            if(isInterface) {
                super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, "get" + TaintUtils.TAINT_FIELD, "()Ljava/lang/Object;", null, null);
                super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, "set" + TaintUtils.TAINT_FIELD, "(Ljava/lang/Object;)V", null, null);
            } else {
                MethodVisitor mv;
                mv = super.visitMethod(Opcodes.ACC_PUBLIC, "get" + TaintUtils.TAINT_FIELD, "()Ljava/lang/Object;", null, null);
                mv = new TaintTagFieldCastMV(mv, "get" + TaintUtils.TAINT_FIELD);
                mv.visitCode();
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitFieldInsn(Opcodes.GETFIELD, className, TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_DESC);

                mv.visitInsn(Opcodes.ARETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();

                mv = super.visitMethod(Opcodes.ACC_PUBLIC, "set" + TaintUtils.TAINT_FIELD, "(Ljava/lang/Object;)V", null, null);
                mv = new TaintTagFieldCastMV(mv, "set" + TaintUtils.TAINT_FIELD);

                mv.visitCode();
                Configuration.taintTagFactory.generateSetTag(mv, className);
                if(className.equals("java/lang/String")) {
                    //Also overwrite the taint tag of all of the chars behind this string

                    Type taintType = MultiDTaintedArray.getTypeForType(Type.getType(char[].class));
                    mv.visitFieldInsn(Opcodes.GETSTATIC, Type.getInternalName(Configuration.class), "autoTainter", Type.getDescriptor(TaintSourceWrapper.class));
                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                    mv.visitInsn(Opcodes.DUP);
                    mv.visitFieldInsn(Opcodes.GETFIELD, className, "value", "[C");
                    //A
                    mv.visitTypeInsn(Opcodes.NEW, taintType.getInternalName());
                    //A T
                    mv.visitInsn(Opcodes.DUP_X1);
                    //T A T
                    mv.visitInsn(Opcodes.SWAP);
                    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, taintType.getInternalName(), "<init>", "([C)V", false);
                    //T
                    mv.visitInsn(Opcodes.DUP_X1);
                    mv.visitFieldInsn(Opcodes.PUTFIELD, className, "value" + TaintUtils.TAINT_WRAPPER_FIELD, taintType.getDescriptor());

                    mv.visitVarInsn(Opcodes.ALOAD, 1);
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(TaintSourceWrapper.class), "combineTaintsOnArray", "(Ljava/lang/Object;" + Configuration.TAINT_TAG_DESC + ")V", false);
                } else if((className.equals(TaintPassingMV.INTEGER_NAME) || className.equals(TaintPassingMV.LONG_NAME)
                        || className.equals(TaintPassingMV.FLOAT_NAME) || className.equals(TaintPassingMV.DOUBLE_NAME))) {
                    //For primitive types, also set the "value" field
                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                    mv.visitVarInsn(Opcodes.ALOAD, 1);
                    mv.visitFieldInsn(Opcodes.PUTFIELD, className, "value" + TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_DESC);
                }
                mv.visitInsn(Opcodes.RETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }
        }

        //For weird lambdas
        for(MethodNode m : methodsToAddNameOnlyWrappersFor) {
            if((m.access & Opcodes.ACC_ABSTRACT) == 0 && !m.name.contains("<")) {
                String[] exceptions = new String[m.exceptions.size()];
                exceptions = m.exceptions.toArray(exceptions);

                MethodVisitor mv = super.visitMethod(m.access, m.name + TaintUtils.METHOD_SUFFIX, m.desc, m.signature, exceptions);
                GeneratorAdapter ga = new GeneratorAdapter(mv, m.access, m.name + TaintUtils.METHOD_SUFFIX, m.desc);

                m.accept(ga);

            }

        }
        for(MethodNode m : methodsToAddUnWrappersFor) {
            if((m.access & Opcodes.ACC_ABSTRACT) == 0 && !m.name.contains("<")) {
                String[] exceptions = new String[m.exceptions.size()];
                exceptions = m.exceptions.toArray(exceptions);

                LinkedList<Type> newArgs = new LinkedList<>();
                for(Type t : Type.getArgumentTypes(m.desc)) {
                    if(TaintUtils.isPrimitiveOrPrimitiveArrayType(t)) {
                        newArgs.removeLast();
                    }
                    if(t.getDescriptor().startsWith("Ledu/columbia/cs/psl/phosphor/struct/Lazy")) {
                        newArgs.add(MultiDTaintedArray.getPrimitiveTypeForWrapper(t.getInternalName()));
                    } else {
                        newArgs.add(t);
                    }
                }
                Type returnType = Type.getReturnType(m.desc);
                String t = returnType.getDescriptor();
                Type returnTypeToHackOnLambda = null;
                if(returnType.getSort() == Type.VOID) {
                    //Check to see if we are one of those annoying lambdas with a void return but pre-allocated return obj passed
                    if(!newArgs.isEmpty() && newArgs.getLast().getDescriptor().startsWith("Ledu/columbia/cs/psl/phosphor/struct/Tainted")) {
                        returnTypeToHackOnLambda = newArgs.removeLast();
                    }
                }
                if(returnType.getDescriptor().startsWith("Ledu/columbia/cs/psl/phosphor/struct/Tainted")) {
                    newArgs.removeLast();
                    //Need to change return type...
                    if(returnType.getDescriptor().contains("edu/columbia/cs/psl/phosphor/struct/Tainted")) {
                        t = returnType.getDescriptor().replace("Ledu/columbia/cs/psl/phosphor/struct/Tainted", "");
                        t = t.replace("WithIntTag", "");
                        t = t.replace("WithObjTag", "");
                    }
                    t = t.replace(";", "").replace("Int", "I")
                            .replace("Byte", "B")
                            .replace("Short", "S")
                            .replace("Long", "J")
                            .replace("Boolean", "Z")
                            .replace("Float", "F")
                            .replace("Double", "D");

                }
                if(Configuration.IMPLICIT_TRACKING && !newArgs.isEmpty()) {
                    newArgs.removeLast(); //remove control taint tag
                }
                StringBuilder newDesc = new StringBuilder("(");
                for(Type _t : newArgs) {
                    newDesc.append(_t.getDescriptor());
                }
                newDesc.append(")");
                newDesc.append(t);

                MethodNode z = new MethodNode(m.access | Opcodes.ACC_MODULE, m.name.replace(TaintUtils.METHOD_SUFFIX, ""), newDesc.toString(), m.signature, exceptions);
                methodsToAddWrappersFor.add(z);
                methodsToAddLambdaUnWrappersFor.add(z);
                if(returnTypeToHackOnLambda != null) {
                    methodsToAddWrappersForWithReturnType.put(z, returnTypeToHackOnLambda);
                }
            }
            if((m.access & Opcodes.ACC_ABSTRACT) == 0 && !m.name.contains("<")) {
                String[] exceptions = new String[m.exceptions.size()];
                exceptions = m.exceptions.toArray(exceptions);

                LinkedList<Type> newArgs = new LinkedList<>();
                for(Type t : Type.getArgumentTypes(m.desc)) {
                    if(TaintUtils.isPrimitiveOrPrimitiveArrayType(t)) {
                        newArgs.removeLast();
                    }
                    if(t.getDescriptor().startsWith("Ledu/columbia/cs/psl/phosphor/struct/Lazy")) {
                        newArgs.add(MultiDTaintedArray.getPrimitiveTypeForWrapper(t.getInternalName()));
                    } else {
                        newArgs.add(t);
                    }
                }
                Type returnType = Type.getReturnType(m.desc);
                String t = returnType.getDescriptor();
                if(returnType.getDescriptor().startsWith("Ledu/columbia/cs/psl/phosphor/struct/Tainted")) {
                    newArgs.removeLast();
                    //Need to change return type...
                    if(returnType.getDescriptor().contains("edu/columbia/cs/psl/phosphor/struct/Tainted")) {
                        t = returnType.getDescriptor().replace("Ledu/columbia/cs/psl/phosphor/struct/Tainted", "");
                        t = t.replace("WithIntTag", "");
                        t = t.replace("WithObjTag", "");
                    }
                    t = t.replace(";", "").replace("Int", "I")
                            .replace("Byte", "B")
                            .replace("Short", "S")
                            .replace("Long", "J")
                            .replace("Boolean", "Z")
                            .replace("Float", "F")
                            .replace("Double", "D");
                }
                if(returnType.getSort() == Type.VOID) {
                    //Check to see if we are one of those annoying lambdas with a void return but pre-allocated return obj passed
                    if(!newArgs.isEmpty() && newArgs.getLast().getDescriptor().startsWith("Ledu/columbia/cs/psl/phosphor/struct/Tainted")) {
                        newArgs.removeLast();
                    }
                }
                if(Configuration.IMPLICIT_TRACKING && !newArgs.isEmpty()) {
                    newArgs.removeLast();//remove control taint tag
                }
                StringBuilder newDesc = new StringBuilder("(");
                for(Type _t : newArgs) {
                    newDesc.append(_t.getDescriptor());
                }
                newDesc.append(")");
                newDesc.append(t);

                newDesc = new StringBuilder(TaintUtils.remapMethodDescAndIncludeReturnHolder(newDesc.toString()));
                if(newDesc.toString().equals(m.desc)) {
                    continue;
                }
                MethodVisitor mv = super.visitMethod(m.access, m.name, newDesc.toString(), m.signature, exceptions);
                GeneratorAdapter ga = new GeneratorAdapter(mv, m.access, m.name, newDesc.toString());

                Label startLabel = new Label();
                ga.visitCode();
                ga.visitLabel(startLabel);
                ga.visitLineNumber(0, startLabel);

                switch(returnType.getSort()) {
                    case Type.INT:
                    case Type.SHORT:
                    case Type.BOOLEAN:
                    case Type.BYTE:
                    case Type.CHAR:
                        ga.visitInsn(Opcodes.ICONST_0);
                        break;
                    case Type.ARRAY:
                    case Type.OBJECT:
                        ga.visitInsn(Opcodes.ACONST_NULL);
                        break;
                    case Type.LONG:
                        ga.visitInsn(Opcodes.LCONST_0);
                        break;
                    case Type.DOUBLE:
                        ga.visitInsn(Opcodes.DCONST_0);
                        break;
                    case Type.FLOAT:
                        ga.visitInsn(Opcodes.FCONST_0);
                        break;
                }
                Label endLabel = new Label();
                ga.visitLabel(endLabel);
                ga.returnValue();

                for(Object o : m.localVariables) {
                    LocalVariableNode n = (LocalVariableNode) o;
                    ga.visitLocalVariable(n.name, n.desc, n.signature, startLabel, endLabel, n.index);
                }
                ga.visitMaxs(0, 0);
                ga.visitEnd();
            }
        }

        if(!goLightOnGeneratedStuff) {
            for(MethodNode m : methodsToAddWrappersFor) {
                if(this.className.equals("java/lang/String") && (m.name.equals("hashCode") && m.desc.equals("()I") || (m.name.equals("equals") && m.desc.equals("(Ljava/lang/Object;)Z")))) {
                    MethodNode fullMethod = forMore.get(m);

                    MethodVisitor mv = super.visitMethod(m.access, m.name, m.desc, m.signature, null);
                    fullMethod.accept(mv);
                    continue;
                }
                if((m.access & Opcodes.ACC_NATIVE) == 0) {
                    if((m.access & Opcodes.ACC_ABSTRACT) == 0) {
                        //not native
                        MethodNode fullMethod = forMore.get(m);

                        Type origReturn = Type.getReturnType(m.desc);
                        Type newReturn = TaintUtils.getContainerReturnType(origReturn);

                        Type returnTypeToHackOnLambda = methodsToAddWrappersForWithReturnType.get(m);
                        boolean needToPrealloc = TaintUtils.isPreAllocReturnType(m.desc) || returnTypeToHackOnLambda != null;
                        boolean useSuffixName = !TaintUtils.remapMethodDescAndIncludeReturnHolder(m.desc).equals(m.desc) || Type.getReturnType(m.desc).getDescriptor().equals("Ljava/lang/Object;") || methodsToAddLambdaUnWrappersFor.contains(m);
                        String[] exceptions = new String[m.exceptions.size()];
                        exceptions = m.exceptions.toArray(exceptions);

                        boolean useInvokeVirtual = (m.access & Opcodes.ACC_MODULE) != 0 || (isLambda && (m.access & Opcodes.ACC_STATIC) == 0);
                        m.access = m.access & ~Opcodes.ACC_MODULE;
                        MethodVisitor mv = super.visitMethod(m.access, m.name, m.desc, m.signature, exceptions);
                        mv = new TaintTagFieldCastMV(mv, m.name);

                        if(fullMethod != null) {
                            visitAnnotations(mv, fullMethod);
                        }

                        MethodVisitor soc = new SpecialOpcodeRemovingMV(mv, ignoreFrames, m.access, className, m.desc, fixLdcClass);
                        NeverNullArgAnalyzerAdapter an = new NeverNullArgAnalyzerAdapter(className, m.access, m.name, m.desc, soc);
                        LocalVariableManager lvs = new LocalVariableManager(m.access, m.desc, an, an, mv, generateExtraLVDebug);
                        lvs.setPrimitiveArrayAnalyzer(new PrimitiveArrayAnalyzer(returnTypeToHackOnLambda == null ? newReturn : returnTypeToHackOnLambda));
                        GeneratorAdapter ga = new GeneratorAdapter(lvs, m.access, m.name, m.desc);

                        Label startLabel = new Label();
                        ga.visitCode();
                        ga.visitLabel(startLabel);
                        ga.visitLineNumber(0, startLabel);

                        Type[] argTypes = Type.getArgumentTypes(m.desc);
                        int idx = 0;
                        if((m.access & Opcodes.ACC_STATIC) == 0) {
                            ga.visitVarInsn(Opcodes.ALOAD, 0);
                            idx++;
                        }

                        String newDesc = "(";
                        for (Type t : argTypes) {
                            boolean loaded = false;
                            boolean needToBoxMultiD = false;
                            if (TaintUtils.isShadowedType(t)) {
                                newDesc += Configuration.TAINT_TAG_DESC;
                                Configuration.taintTagFactory.generateEmptyTaint(ga);
                            }
                            if (TaintUtils.isWrappedType(t)) {
                                if (t.getSort() == Type.ARRAY && t.getDimensions() == 1) {
                                    newDesc += TaintUtils.getWrapperType(t);
                                    ga.visitVarInsn(Opcodes.ALOAD, idx);
                                    TaintAdapter.createNewTaintArray(t.getDescriptor(), an, lvs, lvs);
                                    loaded = true;
                                } else {
                                    newDesc += TaintUtils.getWrapperType(t);
                                    needToBoxMultiD = true;
                                }
                            } else {
                                newDesc += t.getDescriptor();
                            }
                            if (!loaded) {
                                ga.visitVarInsn(t.getOpcode(Opcodes.ILOAD), idx);
                            }
                            if (NATIVE_BOX_UNBOX && t.getSort() == Type.OBJECT && Instrumenter.isCollection(t.getInternalName())) {
                                ga.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(NativeHelper.class), "ensureIsBoxedObjTags", "(Ljava/util/Collection;)Ljava/util/Collection;", false);
                                ga.visitTypeInsn(Opcodes.CHECKCAST, t.getInternalName());
                            }
                            if (t.getDescriptor().endsWith("java/lang/Object;") && !className.contains("MethodAccessorImpl") && !m.name.startsWith("invoke") && !className.contains("ConstructorAccessorImpl")) {
                                ga.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArray.class), "boxIfNecessary", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
                                ga.visitTypeInsn(Opcodes.CHECKCAST, t.getInternalName());
                            }
                            if (needToBoxMultiD) {
                                Label isDone = new Label();
                                ga.visitInsn(Opcodes.DUP);
                                ga.visitJumpInsn(Opcodes.IFNULL, isDone);
                                ga.visitIntInsn(Opcodes.BIPUSH, t.getElementType().getSort());
                                ga.visitIntInsn(Opcodes.BIPUSH, t.getDimensions());
                                ga.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArrayWithObjTag.class), "initWithEmptyTaints", "([Ljava/lang/Object;II)Ljava/lang/Object;", false);
                                FrameNode fn = TaintAdapter.getCurrentFrameNode(an);
                                fn.stack.set(fn.stack.size() - 1, "java/lang/Object");
                                ga.visitLabel(isDone);
                                TaintAdapter.acceptFn(fn, lvs);
                                ga.visitTypeInsn(Opcodes.CHECKCAST, MultiDTaintedArray.getTypeForType(t).getDescriptor());

                            }
                            idx += t.getSize();
                        }
                        if(Configuration.IMPLICIT_TRACKING) {
                            newDesc.append(Type.getDescriptor(ControlTaintTagStack.class));
                            CONTROL_STACK_POOL_INSTANCE.delegateVisit(ga);
                        } else if(Configuration.IMPLICIT_HEADERS_NO_TRACKING) {
                            newDesc.append(Type.getDescriptor(ControlTaintTagStack.class));
                            CONTROL_STACK_FACTORY.delegateVisit(ga);
                        }
                        if(m.name.equals("<init>")) {
                            newDesc.append(Type.getDescriptor(TaintSentinel.class));
                            ga.visitInsn(Opcodes.ACONST_NULL);
                        }
                        if(needToPrealloc) {
                            newDesc.append(returnTypeToHackOnLambda == null ? newReturn.getDescriptor() : returnTypeToHackOnLambda.getDescriptor());
                            an.visitVarInsn(Opcodes.ALOAD, lvs.getPreAllocatedReturnTypeVar(returnTypeToHackOnLambda == null ? newReturn : returnTypeToHackOnLambda));
                        }
                        newDesc.append(")").append(newReturn.getDescriptor());

                        int opcode;
                        if(useInvokeVirtual) {
                            opcode = Opcodes.INVOKEVIRTUAL;
                        } else if((m.access & Opcodes.ACC_STATIC) == 0) {
                            opcode = Opcodes.INVOKESPECIAL;
                        } else {
                            opcode = Opcodes.INVOKESTATIC;
                        }
                        if(m.name.equals("<init>")) {
                            ga.visitMethodInsn(Opcodes.INVOKESPECIAL, className, m.name, newDesc.toString(), false);
                        } else {
                            ga.visitMethodInsn(opcode, className, m.name + (useSuffixName ? TaintUtils.METHOD_SUFFIX : ""), newDesc.toString(), false);
                        }
                        //unbox collections
                        idx = 0;
                        if((m.access & Opcodes.ACC_STATIC) == 0) {
                            idx++;
                        }

                        for(Type t : argTypes) {
                            if(NATIVE_BOX_UNBOX && t.getSort() == Type.OBJECT && Instrumenter.isCollection(t.getInternalName())) {
                                ga.visitVarInsn(t.getOpcode(Opcodes.ILOAD), idx);
                                ga.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(NativeHelper.class), "ensureIsUnBoxedObjTags", "(Ljava/util/Collection;)Ljava/util/Collection;", false);
                                ga.visitInsn(Opcodes.POP);
                            }
                            idx += t.getSize();
                        }
                        if(origReturn != newReturn) {
                            String taintType = TaintUtils.getShadowTaintType(origReturn.getDescriptor());
                            if(taintType != null) {
                                if(origReturn.getSort() == Type.ARRAY) {
                                    FrameNode fn2 = TaintAdapter.getCurrentFrameNode(an);
                                    Label isNull = new Label();
                                    ga.visitInsn(Opcodes.DUP);

                                    Label isDone = new Label();
                                    ga.visitJumpInsn(Opcodes.IFNULL, isNull);
                                    ga.visitFieldInsn(Opcodes.GETFIELD, newReturn.getInternalName(), "val", origReturn.getDescriptor());
                                    FrameNode fn = TaintAdapter.getCurrentFrameNode(an);
                                    ga.visitJumpInsn(Opcodes.GOTO, isDone);
                                    fn.type = Opcodes.F_NEW;
                                    fn2.type = Opcodes.F_NEW;
                                    ga.visitLabel(isNull);
                                    TaintAdapter.acceptFn(fn2, ga);
                                    ga.visitInsn(Opcodes.POP);
                                    ga.visitInsn(Opcodes.ACONST_NULL);
                                    ga.visitLabel(isDone);
                                    TaintAdapter.acceptFn(fn, ga);

                                } else {
                                    ga.visitFieldInsn(Opcodes.GETFIELD, newReturn.getInternalName(), "val", origReturn.getDescriptor());
                                }
                            } else {
                                //Need to convert from [[WrapperForCArray to [[[C

                                Label isDone = new Label();
                                ga.visitInsn(Opcodes.DUP);
                                ga.visitJumpInsn(Opcodes.IFNULL, isDone);
                                ga.visitIntInsn(Opcodes.BIPUSH, origReturn.getElementType().getSort());
                                ga.visitIntInsn(Opcodes.BIPUSH, origReturn.getDimensions() - 1);
                                ga.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArrayWithObjTag.class), "unboxVal", "(Ljava/lang/Object;II)Ljava/lang/Object;", false);
                                FrameNode fn = TaintAdapter.getCurrentFrameNode(an);
                                fn.stack.set(fn.stack.size() - 1, "java/lang/Object");
                                ga.visitLabel(isDone);
                                TaintAdapter.acceptFn(fn, lvs);
                                ga.visitTypeInsn(Opcodes.CHECKCAST, origReturn.getInternalName());

                            }
                        }
                        Label endLabel = new Label();
                        ga.visitLabel(endLabel);
                        ga.returnValue();
                        for(Object o : m.localVariables) {
                            LocalVariableNode n = (LocalVariableNode) o;
                            ga.visitLocalVariable(n.name, n.desc, n.signature, startLabel, endLabel, n.index);
                        }
                        ga.visitMaxs(0, 0);
                        ga.visitEnd();
                    } else {
                        String[] exceptions = new String[m.exceptions.size()];
                        exceptions = m.exceptions.toArray(exceptions);
                        MethodNode fullMethod = forMore.get(m);

                        MethodVisitor mv = super.visitMethod(m.access, m.name, m.desc, m.signature, exceptions);
                        visitAnnotations(mv, fullMethod);
                        m.accept(mv);
                    }
                } else {
                    // Generate wrapper for native method - a native wrapper
                    generateNativeWrapper(m, m.name, false);
                    if(className.equals("sun/misc/Unsafe")) {
                        generateNativeWrapper(m, m.name, true);
                    }
                }
            }
        }
        superMethodsToOverride.remove("wait(JI)V");
        superMethodsToOverride.remove("wait(J)V");
        superMethodsToOverride.remove("wait()V");
        superMethodsToOverride.remove("notify()V");
        superMethodsToOverride.remove("notifyAll()V");
        for(Method m : superMethodsToOverride.values()) {
            int acc = Opcodes.ACC_PUBLIC;
            if(Modifier.isProtected(m.getModifiers()) && isInterface) {
                continue;
            } else if(Modifier.isPrivate(m.getModifiers())) {
                continue;
            }
            if(Modifier.isStatic(m.getModifiers())) {
                acc = acc | Opcodes.ACC_STATIC;
            }
            if(isInterface) {
                acc = acc | Opcodes.ACC_ABSTRACT;
            } else {
                acc = acc & ~Opcodes.ACC_ABSTRACT;
            }
            MethodNode mn = new MethodNode(Configuration.ASM_VERSION, acc, m.getName(), Type.getMethodDescriptor(m), null, null);
            generateNativeWrapper(mn, mn.name, false);
        }
        super.visitEnd();
    }

    private void visitAnnotations(MethodVisitor mv, MethodNode fullMethod) {
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

    private void generateNativeWrapper(MethodNode m, String methodNameToCall, boolean skipUnboxing) {
        String[] exceptions = new String[m.exceptions.size()];
        exceptions = m.exceptions.toArray(exceptions);
        Type[] argTypes = Type.getArgumentTypes(m.desc);

        boolean isPreAllocReturnType = TaintUtils.isPreAllocReturnType(m.desc);
        StringBuilder newDesc = new StringBuilder("(");
        LinkedList<LocalVariableNode> lvsToVisit = new LinkedList<>();
        LabelNode start = new LabelNode(new Label());
        LabelNode end = new LabelNode(new Label());
        for (Type t : argTypes) {
            if (TaintUtils.isShadowedType(t)) {
                newDesc += TaintUtils.getShadowTaintType(t.getDescriptor());
            }
            if (TaintUtils.isWrappedType(t)) {
                newDesc += TaintUtils.getWrapperType(t);
            } else {
                newDesc.append(t.getDescriptor());
            }
        }
        Type origReturn = Type.getReturnType(m.desc);
        Type newReturn = TaintUtils.getContainerReturnType(origReturn);
        if((Configuration.IMPLICIT_HEADERS_NO_TRACKING || Configuration.IMPLICIT_TRACKING)) {
            newDesc.append(Type.getDescriptor(ControlTaintTagStack.class));
        }
        if(m.name.equals("<init>")) {
            newDesc.append(Type.getDescriptor(TaintSentinel.class));
        }
        if(isPreAllocReturnType) {
            newDesc.append(newReturn.getDescriptor());
        }
        newDesc.append(")").append(newReturn.getDescriptor());

        MethodVisitor mv;
        if(m.name.equals("<init>")) {
            mv = super.visitMethod(m.access & ~Opcodes.ACC_NATIVE, m.name, newDesc.toString(), m.signature, exceptions);
        } else {
            mv = super.visitMethod(m.access & ~Opcodes.ACC_NATIVE, m.name + TaintUtils.METHOD_SUFFIX + (skipUnboxing ? "$$NOUNBOX" : ""), newDesc.toString(), m.signature, exceptions);
        }
        NeverNullArgAnalyzerAdapter an = new NeverNullArgAnalyzerAdapter(className, m.access, m.name, newDesc.toString(), mv);
        MethodVisitor soc = new SpecialOpcodeRemovingMV(an, ignoreFrames, m.access, className, newDesc.toString(), fixLdcClass);
        LocalVariableManager lvs = new LocalVariableManager(m.access, newDesc.toString(), soc, an, mv, generateExtraLVDebug);
        lvs.setPrimitiveArrayAnalyzer(new PrimitiveArrayAnalyzer(newReturn));
        GeneratorAdapter ga = new GeneratorAdapter(lvs, m.access, m.name + TaintUtils.METHOD_SUFFIX, newDesc.toString());
        if(isInterface) {
            ga.visitEnd();
            return;
        }
        ga.visitCode();
        ga.visitLabel(start.getLabel());
        if(isLambda) {
            if(m.name.equals("equals")) {
                int retVar = (Configuration.IMPLICIT_TRACKING ? 3 : 2);
                ga.visitVarInsn(Opcodes.ALOAD, retVar);
                NEW_EMPTY_TAINT.delegateVisit(ga);
                ga.visitFieldInsn(Opcodes.PUTFIELD, newReturn.getInternalName(), "taint", Configuration.TAINT_TAG_DESC);
                ga.visitVarInsn(Opcodes.ALOAD, 0);
                ga.visitVarInsn(Opcodes.ALOAD, 1);
                Label eq = new Label();
                ga.visitJumpInsn(Opcodes.IF_ACMPEQ, eq);
                ga.visitVarInsn(Opcodes.ALOAD, retVar);
                ga.visitInsn(Opcodes.DUP);
                ga.visitInsn(Opcodes.ICONST_0);
                ga.visitFieldInsn(Opcodes.PUTFIELD, newReturn.getInternalName(), "val", "Z");
                ga.visitInsn(Opcodes.ARETURN);
                ga.visitLabel(eq);
                if(Configuration.IMPLICIT_TRACKING) {
                    ga.visitFrame(Opcodes.F_NEW, 4, new Object[]{className, "java/lang/Object", Type.getInternalName(ControlTaintTagStack.class), newReturn.getInternalName()}, 0, new Object[]{});
                } else {
                    ga.visitFrame(Opcodes.F_NEW, 3, new Object[]{className, "java/lang/Object", newReturn.getInternalName()}, 0, new Object[]{});
                }
                ga.visitVarInsn(Opcodes.ALOAD, retVar);
                ga.visitInsn(Opcodes.DUP);
                ga.visitInsn(Opcodes.ICONST_1);
                ga.visitFieldInsn(Opcodes.PUTFIELD, newReturn.getInternalName(), "val", "Z");
            } else if(m.name.equals("hashCode")) {
                int retVar = (Configuration.IMPLICIT_TRACKING ? 2 : 1);
                ga.visitVarInsn(Opcodes.ALOAD, retVar);
                ga.visitInsn(Opcodes.DUP);
                ga.visitInsn(Opcodes.DUP);
                NEW_EMPTY_TAINT.delegateVisit(ga);
                ga.visitFieldInsn(Opcodes.PUTFIELD, newReturn.getInternalName(), "taint", Configuration.TAINT_TAG_DESC);
                ga.visitInsn(Opcodes.ICONST_0);
                ga.visitFieldInsn(Opcodes.PUTFIELD, newReturn.getInternalName(), "val", "I");
            } else {
                switch(Type.getReturnType(newDesc.toString()).getSort()) {
                    case Type.INT:
                    case Type.SHORT:
                    case Type.BOOLEAN:
                    case Type.BYTE:
                    case Type.CHAR:
                        ga.visitInsn(Opcodes.ICONST_0);
                        break;
                    case Type.ARRAY:
                    case Type.OBJECT:
                        ga.visitInsn(Opcodes.ACONST_NULL);
                        break;
                    case Type.LONG:
                        ga.visitInsn(Opcodes.LCONST_0);
                        break;
                    case Type.DOUBLE:
                        ga.visitInsn(Opcodes.DCONST_0);
                        break;
                    case Type.FLOAT:
                        ga.visitInsn(Opcodes.FCONST_0);
                        break;
                }
            }
        } else {
            String descToCall = m.desc;
            boolean isUntaggedCall = false;
            if(methodNameToCall.contains(TaintUtils.METHOD_SUFFIX_UNINST)) {
                descToCall = TaintUtils.remapMethodDescForUninst(descToCall);
                isUntaggedCall = true;
            }
            int idx = 0;
            if((m.access & Opcodes.ACC_STATIC) == 0) {
                ga.visitVarInsn(Opcodes.ALOAD, 0);
                lvsToVisit.add(new LocalVariableNode("this", "L" + className + ";", null, start, end, idx));
                idx++;
            }
            for(Type t : argTypes) {
                if (TaintUtils.isShadowedType(t)) {
                    lvsToVisit.add(new LocalVariableNode("phosphorNativeWrapArg" + idx, TaintUtils.getShadowTaintType(t.getDescriptor()), null, start, end, idx));
                    idx++;
                }
                ga.visitVarInsn(t.getOpcode(Opcodes.ILOAD), idx);
                if (TaintUtils.isWrappedType(t)) {
                    Type wrapper = TaintUtils.getWrapperType(t);
                    ga.visitMethodInsn(Opcodes.INVOKESTATIC, wrapper.getInternalName(),"unwrap","("+wrapper.getDescriptor()+")"+t.getDescriptor(),false);
                    lvsToVisit.add(new LocalVariableNode("phosphorNativeWrapArg" + idx, TaintUtils.getWrapperType(t).getDescriptor(), null, start, end, idx));
                } else {
                    lvsToVisit.add(new LocalVariableNode("phosphorNativeWrapArg" + idx, t.getDescriptor(), null, start, end, idx));
                }
                if (!skipUnboxing) {
                    if(t.getDescriptor().equals("[Lsun/security/pkcs11/wrapper/CK_ATTRIBUTE;")) {
                        ga.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArray.class), "unboxCK_ATTRIBUTE", "([Lsun/security/pkcs11/wrapper/CK_ATTRIBUTE;)[Lsun/security/pkcs11/wrapper/CK_ATTRIBUTE;", false);
                    } else if(t.getDescriptor().equals("Ljava/lang/Object;") || (t.getSort() == Type.ARRAY && t.getElementType().getDescriptor().equals("Ljava/lang/Object;"))) {
                        // Need to make sure that it's not a boxed primitive array
                        ga.visitInsn(Opcodes.DUP);
                        ga.visitInsn(Opcodes.DUP);
                        Label isOK = new Label();
                        ga.visitTypeInsn(Opcodes.INSTANCEOF, "[" + Type.getDescriptor(LazyArrayObjTags.class));
                        ga.visitInsn(Opcodes.SWAP);
                        ga.visitTypeInsn(Opcodes.INSTANCEOF, Type.getInternalName(LazyArrayObjTags.class));
                        ga.visitInsn(Opcodes.IOR);
                        ga.visitJumpInsn(Opcodes.IFEQ, isOK);
                        if(isUntaggedCall) {
                            ga.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArray.class), "unbox1D", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
                        } else {
                            if(className.equals("sun/misc/Unsafe")) {
                                ga.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArrayWithObjTag.class), "unboxRawOnly1D", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
                            } else if(className.equals("sun/reflect/NativeMethodAccessorImpl") && "invoke0".equals(methodNameToCall) && Type.getType(Object.class).equals(t)) {
                                ga.loadArg(0);
                                ga.visitInsn(Opcodes.SWAP);
                                ga.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArray.class), "unboxMethodReceiverIfNecessary", "(Ljava/lang/reflect/Method;Ljava/lang/Object;)Ljava/lang/Object;", false);
                            } else {
                                ga.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArrayWithObjTag.class), "unboxRaw", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
                            }
                        }
                        if(t.getSort() == Type.ARRAY) {
                            ga.visitTypeInsn(Opcodes.CHECKCAST, t.getInternalName());
                        }
                        FrameNode fn = TaintAdapter.getCurrentFrameNode(an);
                        ga.visitLabel(isOK);
                        TaintAdapter.acceptFn(fn, lvs);
                    } else if(!isUntaggedCall && t.getSort() == Type.ARRAY && t.getDimensions() > 1 && t.getElementType().getSort() != Type.OBJECT) {
                        // Need to unbox it!!
                        ga.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArrayWithObjTag.class), "unboxRaw", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
                        ga.visitTypeInsn(Opcodes.CHECKCAST, t.getInternalName());
                    }
                }
                idx += t.getSize();
            }
            int opcode;
            if((m.access & Opcodes.ACC_STATIC) == 0) {
                opcode = Opcodes.INVOKESPECIAL;
            } else {
                opcode = Opcodes.INVOKESTATIC;
            }
            if(m.name.equals("<init>") && methodNameToCall.contains(TaintUtils.METHOD_SUFFIX_UNINST)) {
                //call with uninst sentinel
                descToCall = descToCall.substring(0, descToCall.indexOf(')')) + Type.getDescriptor(UninstrumentedTaintSentinel.class) + ")" + descToCall.substring(descToCall.indexOf(')') + 1);
                ga.visitInsn(Opcodes.ACONST_NULL);
                ga.visitMethodInsn(opcode, className, m.name, descToCall, false);
            } else {
                ga.visitMethodInsn(opcode, className, methodNameToCall, descToCall, false);
            }
            if(origReturn != newReturn) {
                if(origReturn.getSort() == Type.ARRAY) {
                    if(origReturn.getDimensions() > 1) {
                        Label isOK = new Label();
                        ga.visitInsn(Opcodes.DUP);
                        ga.visitJumpInsn(Opcodes.IFNULL, isOK);
                        ga.visitTypeInsn(Opcodes.CHECKCAST, "[Ljava/lang/Object;");
                        ga.visitIntInsn(Opcodes.BIPUSH, origReturn.getElementType().getSort());
                        ga.visitIntInsn(Opcodes.BIPUSH, origReturn.getDimensions());
                        ga.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArrayWithObjTag.class), "initWithEmptyTaints", "([Ljava/lang/Object;II)Ljava/lang/Object;", false);
                        FrameNode fn = TaintAdapter.getCurrentFrameNode(an);
                        fn.stack.set(fn.stack.size() - 1, "java/lang/Object");
                        ga.visitLabel(isOK);
                        TaintAdapter.acceptFn(fn, lvs);
                        ga.visitTypeInsn(Opcodes.CHECKCAST, newReturn.getDescriptor());
                    } else {
                        TaintAdapter.createNewTaintArray(origReturn.getDescriptor(), an, lvs, lvs);
                        //                        ga.visitInsn(Opcodes.SWAP);
                    }
                } else {
                    if(origReturn.getSize() == 1) {
                        int retIdx = lvs.getPreAllocatedReturnTypeVar(newReturn);
                        an.visitVarInsn(Opcodes.ALOAD, retIdx);
                        ga.visitInsn(Opcodes.SWAP);
                        ga.visitFieldInsn(Opcodes.PUTFIELD, newReturn.getInternalName(), "val", origReturn.getDescriptor());
                        an.visitVarInsn(Opcodes.ALOAD, retIdx);
                        Configuration.taintTagFactory.generateEmptyTaint(ga);
                        Configuration.taintTagFactory.propagateTagNative(className, m.access, m.name, m.desc, mv);
                        ga.visitFieldInsn(Opcodes.PUTFIELD, newReturn.getInternalName(), "taint", Configuration.TAINT_TAG_DESC);
                        an.visitVarInsn(Opcodes.ALOAD, retIdx);
                    } else {
                        int retIdx = lvs.getPreAllocatedReturnTypeVar(newReturn);
                        an.visitVarInsn(Opcodes.ALOAD, retIdx);
                        ga.visitInsn(Opcodes.DUP_X2);
                        ga.visitInsn(Opcodes.POP);
                        ga.visitFieldInsn(Opcodes.PUTFIELD, newReturn.getInternalName(), "val", origReturn.getDescriptor());
                        an.visitVarInsn(Opcodes.ALOAD, retIdx);
                        Configuration.taintTagFactory.generateEmptyTaint(ga);
                        Configuration.taintTagFactory.propagateTagNative(className, m.access, m.name, m.desc, mv);
                        ga.visitFieldInsn(Opcodes.PUTFIELD, newReturn.getInternalName(), "taint", Configuration.TAINT_TAG_DESC);
                        an.visitVarInsn(Opcodes.ALOAD, retIdx);
                    }
                }
            } else if(origReturn.getSort() != Type.VOID && (origReturn.getDescriptor().equals("Ljava/lang/Object;") || origReturn.getDescriptor().equals("[Ljava/lang/Object;"))) {
                //Check to see if the top of the stack is a primitive array, adn if so, box it.
                if(!isUntaggedCall) {
                    ga.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArrayWithObjTag.class),
                            "boxIfNecessary", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
                    if(origReturn.getSort() == Type.ARRAY) {
                        ga.visitTypeInsn(Opcodes.CHECKCAST, "[Ljava/lang/Object;");
                    }
                }
            }
        }
        ga.visitLabel(end.getLabel());
        ga.returnValue();
        if(isPreAllocReturnType) {
            lvsToVisit.add(new LocalVariableNode("phosphorReturnHolder", newReturn.getDescriptor(), null, start, end, lvs.getPreAllocatedReturnTypeVar(newReturn)));
        }
        for(LocalVariableNode n : lvsToVisit) {
            n.accept(ga);
        }
        ga.visitMaxs(0, 0);
        ga.visitEnd();
    }

    private static void acceptAnnotationRaw(final AnnotationVisitor av, final String name, final Object value) {
        if(av != null) {
            if(value instanceof String[]) {
                String[] typeConst = (String[]) value;
                av.visitEnum(name, typeConst[0], typeConst[1]);
            } else if(value instanceof AnnotationNode) {
                AnnotationNode an = (AnnotationNode) value;
                an.accept(av.visitAnnotation(name, an.desc));
            } else if(value instanceof java.util.List) {
                AnnotationVisitor v = av.visitArray(name);
                java.util.List<?> array = (java.util.List<?>) value;
                for(Object o : array) {
                    acceptAnnotationRaw(v, null, o);
                }
                v.visitEnd();
            } else {
                av.visit(name, value);
            }
        }
    }
}
