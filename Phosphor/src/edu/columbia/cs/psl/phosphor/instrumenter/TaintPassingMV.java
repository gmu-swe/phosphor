package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.TaggedValue;
import edu.columbia.cs.psl.phosphor.instrumenter.asm.OffsetPreservingLabel;
import edu.columbia.cs.psl.phosphor.runtime.*;
import edu.columbia.cs.psl.phosphor.struct.*;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.*;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethod.*;

public class TaintPassingMV extends TaintAdapter implements Opcodes {

    // Used to indicate that a call to push should not be made for a particular branch
    static final int DO_NOT_TRACK_BRANCH_STARTING = -1;
    static final String BYTE_NAME = "java/lang/Byte";
    static final String BOOLEAN_NAME = "java/lang/Boolean";
    static final String INTEGER_NAME = "java/lang/Integer";
    static final String FLOAT_NAME = "java/lang/Float";
    static final String LONG_NAME = "java/lang/Long";
    static final String CHARACTER_NAME = "java/lang/Character";
    static final String DOUBLE_NAME = "java/lang/Double";
    static final String SHORT_NAME = "java/lang/Short";
    private static final String BOOLEAN_DESC = Type.getDescriptor(Boolean.class);
    private static final String BYTE_DESC = Type.getDescriptor(Byte.class);
    private static final String CHARACTER_DESC = Type.getDescriptor(Character.class);
    private static final String SHORT_DESC = Type.getDescriptor(Short.class);
    int lastArg;
    Type[] paramTypes;
    int controlTaintArray = -1;
    boolean isImplicitLightTracking;
    boolean isIgnoreAllInstrumenting = false;
    boolean isRawInsns = false;
    private int branchStarting = DO_NOT_TRACK_BRANCH_STARTING;
    private List<Field> forceCtrlStoreFields = new LinkedList<>();
    private boolean isTaintlessArrayStore = false;
    private boolean dontUnboxTaints;
    private boolean isIgnoreEverything = false;
    private Type originalMethodReturnType;
    private Type newReturnType;
    private Label endLabel = new Label();
    private Label popAllLabel = new Label();
    private int idxOfCaughtExceptionTaint;
    private HashMap<Integer, Object> varTypes = new HashMap<>();
    private HashSet<Integer> boxAtNextJump = new HashSet<>();
    private HashSet<Integer> forceCtrlAdd = new HashSet<>();
    private String name;
    private boolean isStatic = true;
    private boolean isSuperUninitialized;
    private int sizeOfControlTaintArray = -1;
    private String className;
    private String desc;
    private MethodVisitor passThroughMV;
    private boolean rewriteLVDebug;
    private boolean isLambda;
    private HashSet<MethodNode> wrapperMethodsToAdd;
    private boolean isExcludedFromControlTrack;
    private Label firstLabel;
    private PrimitiveArrayAnalyzer arrayAnalyzer;
    private boolean nextLoadIsTracked = false;
    private boolean nextDupCopiesTaint0 = false;
    private boolean nextDupCopiesTaint1 = false;
    private boolean nextDupCopiesTaint2 = false;
    private boolean nextDupCopiesTaint3 = false;

    public TaintPassingMV(MethodVisitor mv, int access, String className, String name, String desc, String signature,
                          String[] exceptions, String originalDesc, NeverNullArgAnalyzerAdapter analyzer,
                          MethodVisitor passThroughMV, HashSet<MethodNode> wrapperMethodsToAdd, boolean isImplicitLightTracking) {
        super(access, className, name, desc, signature, exceptions, mv, analyzer);
        Configuration.taintTagFactory.instrumentationStarting(access, name, desc);
        this.isLambda = this.isIgnoreAllInstrumenting = className.contains("$Lambda$");
        this.name = name;
        this.className = className;
        this.wrapperMethodsToAdd = wrapperMethodsToAdd;
        this.isImplicitLightTracking = isImplicitLightTracking;
        this.rewriteLVDebug = className.equals("java/lang/invoke/MethodType");
        this.isSuperUninitialized = name.equals("<init>");
        this.passThroughMV = passThroughMV;
        this.desc = desc;
        this.isExcludedFromControlTrack = Configuration.IMPLICIT_TRACKING && Instrumenter.isIgnoredFromControlTrack(className, name);
        this.isStatic = (access & Opcodes.ACC_STATIC) != 0;
        Type[] newArgTypes = Type.getArgumentTypes(desc);
        lastArg = isStatic ? 0 : 1; // If non-static, then arg[0] = this
        for(Type t : newArgTypes) {
            lastArg += t.getSize();
        }
        originalMethodReturnType = Type.getReturnType(originalDesc);
        newReturnType = Type.getReturnType(desc);
        paramTypes = new Type[lastArg + 1];
        int n = (isStatic ? 0 : 1);
        if(TaintUtils.DEBUG_LOCAL) {
            System.out.println("New desc is " + Arrays.toString(newArgTypes));
        }
        for(Type newArgType : newArgTypes) {
            if(TaintUtils.DEBUG_LOCAL) {
                System.out.println("ARG TYPE: " + newArgType);
            }
            paramTypes[n] = newArgType;
            n += newArgType.getSize();
        }
    }

    /**
     * Returns whether a class with the specified name is used by Phosphor for "internal" tainting. Calls to methods in
     * internal tainting classes from instrumented classes are remapped to the appropriate "$$PHOSPHORTAGGED" version
     * even if the internal tainting class is not instrumented by Phosphor. This requires internal tainting classes to
     * provide instrumented versions of any method that may be invoked by a classes that is instrumented by Phosphor.
     *
     * @param owner the name of class being checking
     * @return true if a class with the specified name is used by Phosphor for internal tainting
     * @see MultiTainter
     */
    private static boolean isInternalTaintingClass(String owner) {
        return owner.startsWith("edu/columbia/cs/psl/phosphor/runtime/")
                || Configuration.taintTagFactory.isInternalTaintingClass(owner)
                || owner.startsWith("edu/gmu/swe/phosphor/ignored/runtime/");
    }

    private static boolean isBoxUnboxMethodToWrap(String owner, String name, String desc) {
        if(name.equals("valueOf") && desc.startsWith("(Ljava/lang/String")) {
            return Instrumenter.isIgnoredClass(owner);//All of these no matter what will get caught by parseXXX
        }
        if((owner.equals(INTEGER_NAME) || owner.equals(BYTE_NAME) || owner.equals(BOOLEAN_NAME) || owner.equals(CHARACTER_NAME) || owner.equals(SHORT_NAME) || owner.equals(LONG_NAME) || owner.equals(FLOAT_NAME) || owner.equals(DOUBLE_NAME))
                && (name.equals("toString") || name.equals("toHexString") || name.equals("toOctalString") || name.equals("toBinaryString") || name.equals("toUnsignedString"))) {
            return true;
        } else if((owner.equals(BOOLEAN_NAME) && (name.equals("parseBoolean")))
                || (owner.equals(BYTE_NAME) && (name.equals("parseByte")))
                || (owner.equals(INTEGER_NAME) && (name.equals("parseInt") || name.equals("parseUnsignedInt")))
                || (owner.equals(SHORT_NAME) && (name.equals("parseShort")))
                || (owner.equals(LONG_NAME) && (name.equals("parseLong") || name.equals("parseUnsignedLong")))
                || (owner.equals(FLOAT_NAME) && (name.equals("parseFloat")))
                || (owner.equals(DOUBLE_NAME) && (name.equals("parseDouble")))) {
            return true;
        } else if(name.equals("getChars") && (owner.equals(INTEGER_NAME) || owner.equals(LONG_NAME))) {
            return true;
        } else {
            return owner.equals(CHARACTER_NAME) && (name.equals("digit") /*|| name.equals("isWhitespace")*/);
        }
    }

    void setArrayAnalyzer(PrimitiveArrayAnalyzer primitiveArrayFixer) {
        this.arrayAnalyzer = primitiveArrayFixer;
    }

    /**
     * Calls "push" on ControlTaintTagStack, possibly requiring loading the exception data
     */
    void callPushControlTaint(int idx, boolean isTag) {
        super.push(idx);
        super.push(sizeOfControlTaintArray);
        if(lvs.idxOfMasterExceptionLV >= 0) {
            super.visitVarInsn(ALOAD, lvs.idxOfMasterExceptionLV);
            visit(isTag ? CONTROL_PUSH_TAG_EXCEPTION : PUSH_OBJECT_EXCEPTION);
        } else {
            visit(isTag ? CONTROL_PUSH_TAG : CONTROL_PUSH_OBJECT);
        }
        super.visitVarInsn(ASTORE, controlTaintArray);
    }

    private void callPopControlTaint(MethodVisitor _mv, int idx) {
        if(idx < 0) {
            callPopAllControlTaint(_mv);
        } else {
            if(idx > Byte.MAX_VALUE) {
                _mv.visitIntInsn(SIPUSH, idx);
            } else {
                _mv.visitIntInsn(BIPUSH, idx);
            }
            if(lvs.idxOfMasterExceptionLV >= 0) {
                _mv.visitVarInsn(ALOAD, lvs.idxOfMasterExceptionLV);
                CONTROL_POP_EXCEPTION.delegateVisit(_mv);
            } else {
                CONTROL_POP.delegateVisit(_mv);
            }
        }
    }

    private void callPopAllControlTaint(MethodVisitor _mv) {
        if(lvs.idxOfMasterExceptionLV >= 0) {
            _mv.visitVarInsn(ALOAD, lvs.idxOfMasterExceptionLV);
            CONTROL_POP_ALL_EXCEPTION.delegateVisit(_mv);
        } else {
            CONTROL_POP_ALL.delegateVisit(_mv);
        }
    }

    @Override
    public void visitCode() {
        if(this.isExcludedFromControlTrack && this.arrayAnalyzer.isEmptyMethod) {
            this.isExcludedFromControlTrack = false;
        }
        super.visitCode();

        firstLabel = new Label();
        if(Configuration.IMPLICIT_TRACKING || isImplicitLightTracking || Configuration.IMPLICIT_HEADERS_NO_TRACKING) {
            if(lvs.idxOfMasterControlLV < 0) {
                int tmpLV = lvs.createMasterControlTaintLV();
                super.visitTypeInsn(NEW, Type.getInternalName(ControlTaintTagStack.class));
                super.visitInsn(DUP);
                if(name.equals("<clinit>") || isImplicitLightTracking) {
                    super.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(ControlTaintTagStack.class), "<init>", "()V", false);
                }
                super.visitVarInsn(ASTORE, tmpLV);
            } else {
                LocalVariableNode newLVN = new LocalVariableNode("phosphorJumpControlTag", Type.getDescriptor(ControlTaintTagStack.class), null, new LabelNode(lvs.start), new LabelNode(lvs.end), lvs.idxOfMasterControlLV);
                lvs.createdLVs.add(newLVN);
            }
            if(Configuration.IMPLICIT_EXCEPTION_FLOW && this.arrayAnalyzer.nTryCatch > 0) {
                this.idxOfCaughtExceptionTaint = lvs.newControlExceptionTaintLV();
                super.visitInsn(Opcodes.ACONST_NULL);
                super.visitVarInsn(ASTORE, this.idxOfCaughtExceptionTaint);
            }
            if(Configuration.IMPLICIT_EXCEPTION_FLOW && this.arrayAnalyzer.nThrow > 0) {
                //Create the LV for the exception data
                int id = lvs.createExceptionTaintLV();
                super.visitTypeInsn(NEW, Type.getInternalName(ExceptionalTaintData.class));
                super.visitInsn(DUP);
                super.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(ExceptionalTaintData.class), "<init>", "()V", false);
                super.visitVarInsn(ASTORE, id);
            }
            if(arrayAnalyzer.nJumps + arrayAnalyzer.nTryCatch > 0) {
                controlTaintArray = lvs.newControlTaintLV();
                this.sizeOfControlTaintArray = arrayAnalyzer.nJumps + arrayAnalyzer.nTryCatch + 2;
                super.visitInsn(Opcodes.ACONST_NULL);
                super.visitVarInsn(Opcodes.ASTORE, controlTaintArray);
            }
            if(this.isExcludedFromControlTrack) {
                super.visitVarInsn(ALOAD, lvs.idxOfMasterControlLV);
                super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(ControlTaintTagStack.class), "disable", "()V", false);
            }
        }

        Configuration.taintTagFactory.methodEntered(className, name, desc, passThroughMV, lvs, this);
        if(Configuration.IMPLICIT_TRACKING && !arrayAnalyzer.hasFinally && arrayAnalyzer.nTryCatch == 0 && !isSuperUninitialized) {
            super.visitTryCatchBlock(firstLabel, endLabel, popAllLabel, null);
            super.visitLabel(firstLabel);
        }
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        super.visitTryCatchBlock(start, end, handler, type);
        arrayAnalyzer.nTryCatch--;
        if(Configuration.IMPLICIT_TRACKING && !arrayAnalyzer.hasFinally && arrayAnalyzer.nTryCatch == 0 && !isSuperUninitialized) {
            super.visitTryCatchBlock(firstLabel, endLabel, popAllLabel, null);
            super.visitLabel(firstLabel);
        }
    }

    @Override
    public void visitLabel(Label label) {
        if(isIgnoreAllInstrumenting) {
            super.visitLabel(label);
            return;
        }
        if(Configuration.READ_AND_SAVE_BCI && label instanceof OffsetPreservingLabel) {
            Configuration.taintTagFactory.insnIndexVisited(((OffsetPreservingLabel) label).getOriginalPosition());
        }
        super.visitLabel(label);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        Configuration.taintTagFactory.iincOp(var, increment, mv, lvs, this);
        mv.visitIincInsn(var, increment);
        nextLoadIsTracked = false;
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        if(nextLoadIsTracked && opcode == Opcodes.ALOAD) {
            if(analyzer.locals.get(var) != Opcodes.NULL && !TaintUtils.isPrimitiveOrPrimitiveArrayType(getTypeForStackType(analyzer.locals.get(var)))) {
                nextLoadIsTracked = false;
            }
        }
        if(!nextLoadIsTracked && opcode < 200) {
            if((isImplicitLightTracking || Configuration.IMPLICIT_TRACKING) && opcode == Opcodes.ASTORE) {
                super.visitInsn(DUP);
                super.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
                visit(COMBINE_TAGS_ON_OBJECT);
            }
            if(opcode == Opcodes.ASTORE && topCarriesTaint()) {
                throw new IllegalStateException();
            }
            super.visitVarInsn(opcode, var);
            switch(opcode) {
                case Opcodes.ILOAD:
                case Opcodes.FLOAD:
                case Opcodes.ALOAD:
                    analyzer.stackTagStatus.set(analyzer.stack.size() - 1, analyzer.stack.get(analyzer.stack.size() - 1));
                    break;
                case Opcodes.LLOAD:
                case Opcodes.DLOAD:
                    analyzer.stackTagStatus.set(analyzer.stack.size() - 2, analyzer.stack.get(analyzer.stack.size() - 2));
                    break;
            }
            return;
        }
        nextLoadIsTracked = false;
        if(opcode == TaintUtils.NEVER_AUTOBOX) {
            return;
        } else if(opcode == TaintUtils.BRANCH_START) {
            // Note left by the post-dominator analysis for implicit taint tracking
            branchStarting = var;
            return;
        } else if(opcode == TaintUtils.BRANCH_END) {
            // Note left by the post-dominator analysis for implicit taint tracking
            if(controlTaintArray >= 0) {
                passThroughMV.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
                passThroughMV.visitVarInsn(ALOAD, controlTaintArray);
                callPopControlTaint(passThroughMV, var);
            }
            analyzer.clearLabels();
            return;
        }
        if(opcode == TaintUtils.ALWAYS_AUTOBOX && analyzer.locals.size() > var && analyzer.locals.get(var) instanceof String) {
            Type t = Type.getObjectType((String) analyzer.locals.get(var));
            if(t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT && lvs.varToShadowVar.containsKey(var)) {
                //				System.out.println("Restoring " + var + " to be boxed");
                super.visitVarInsn(ALOAD, lvs.varToShadowVar.get(var));
                super.visitVarInsn(ASTORE, var);
            }
            return;
        }
        if(opcode == TaintUtils.ALWAYS_BOX_JUMP) {
            boxAtNextJump.add(var);
            return;
        }
        if(isIgnoreAllInstrumenting) {
            if(opcode != TaintUtils.FORCE_CTRL_STORE) {
                super.visitVarInsn(opcode, var);
            }
            return;
        }

        int shadowVar = -1;

        if((Configuration.IMPLICIT_TRACKING || isImplicitLightTracking) && !Configuration.WITHOUT_PROPOGATION) {
            switch(opcode) {
                case ISTORE:
                case FSTORE:
                    super.visitInsn(SWAP);
                    super.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
                    visit(COMBINE_TAGS_STACK);
                    super.visitInsn(SWAP);
                    break;
                case DSTORE:
                case LSTORE:
                    super.visitInsn(DUP2_X1);
                    super.visitInsn(POP2);
                    super.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
                    visit(COMBINE_TAGS_STACK);
                    super.visitInsn(DUP_X2);
                    super.visitInsn(POP);
                    break;
                case ASTORE:
                    super.visitInsn(DUP);
                    super.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
                    visit(COMBINE_TAGS_ON_OBJECT);
                    break;
                case TaintUtils.FORCE_CTRL_STORE:
                    forceCtrlAdd.add(var);
                    return;
            }
        }

        if(var == 0 && !isStatic) {
            //accessing "this" so no-op, die here so we never have to worry about uninitialized this later on.
            super.visitVarInsn(opcode, var);
            return;
        } else if(var < lastArg && paramTypes[var] != null && TaintUtils.getShadowTaintType(paramTypes[var].getDescriptor()) != null) {
            //accessing an arg; remap it
            Type localType = paramTypes[var];
            if(TaintUtils.DEBUG_LOCAL) {
                System.out.println(Arrays.toString(paramTypes) + ",,," + var);
            }
            if(TaintUtils.getShadowTaintType(localType.getDescriptor()) != null) {
                shadowVar = var - 1;
            }
        } else {
            //not accessing an arg

            Object oldVarType = varTypes.get(var);
            if(lvs.varToShadowVar.containsKey(var)) {
                shadowVar = lvs.varToShadowVar.get(var);
            }
            if(oldVarType != null) {
                //In this case, we already have a shadow for this. Make sure that it's the right kind of shadow though.
                if(TaintUtils.DEBUG_LOCAL) {
                    System.out.println(name + Textifier.OPCODES[opcode] + " " + var + " old type is " + varTypes.get(var) + " shadow is " + shadowVar);
                }
                //First: If we thought this was NULL before, but it's not NULL now (but instead another type), then update that.
                if(opcode == ALOAD && oldVarType == Opcodes.NULL && analyzer.locals.get(var) instanceof String) {
                    varTypes.put(var, analyzer.locals.get(var));
                }
                if((oldVarType == Opcodes.NULL || oldVarType instanceof String) && opcode != ASTORE && opcode != ALOAD) {
                    //Went from a TYPE to a primitive.
                    if(opcode == ISTORE || opcode == FSTORE || opcode == DSTORE || opcode == LSTORE) {
                        varTypes.put(var, getTopOfStackObject());
                    } else {
                        varTypes.put(var, Configuration.TAINT_TAG_STACK_TYPE);
                    }
                    if(shadowVar > -1) {
                        while(shadowVar >= analyzer.locals.size()) {
                            analyzer.locals.add(Opcodes.TOP);
                        }
                        analyzer.locals.set(shadowVar, Configuration.TAINT_TAG_STACK_TYPE);
                        lvs.remapLocal(shadowVar, Type.getType(Configuration.TAINT_TAG_DESC));
                    }
                } else if(oldVarType instanceof Integer && oldVarType != Opcodes.NULL && (opcode == ASTORE || opcode == ALOAD)) {
                    //Went from primitive to TYPE
                    if(opcode == ASTORE) {
                        varTypes.put(var, getTopOfStackObject());
                    } else {
                        varTypes.put(var, "Lunidentified;");
                    }

                    if(shadowVar > -1) {
                        while(shadowVar >= analyzer.locals.size()) {
                            analyzer.locals.add(Opcodes.TOP);
                        }
                        if(opcode == ASTORE) {
                            String shadow = TaintUtils.getShadowTaintType(getTopOfStackType().getDescriptor());
                            if(shadow == null) {
                                shadow = Configuration.TAINT_TAG_ARRAYDESC;
                                analyzer.locals.set(shadowVar, Opcodes.TOP);
                            } else if(shadow.equals(Configuration.TAINT_TAG_DESC)) {
                                analyzer.locals.set(shadowVar, Configuration.TAINT_TAG_STACK_TYPE);
                            } else {
                                analyzer.locals.set(shadowVar, shadow);
                            }
                            lvs.remapLocal(shadowVar, Type.getType(shadow));
                        } else {
                            lvs.remapLocal(shadowVar, Type.getType(Configuration.TAINT_TAG_ARRAYDESC));
                            analyzer.locals.set(shadowVar, Configuration.TAINT_TAG_ARRAYDESC);
                        }
                    }
                }

                if(opcode == ASTORE && !topOfStackIsNull() && !oldVarType.equals(getTopOfStackObject())) {
                    varTypes.put(var, getTopOfStackObject());
                }
            }
            if(shadowVar < 0) {
                //We don't have a shadowvar for this yet. Do we need one?
                if(opcode == ALOAD) {
                    if(analyzer.locals.size() > var && analyzer.locals.get(var) instanceof String) {
                        Type localType = Type.getObjectType((String) analyzer.locals.get(var));
                        if(TaintUtils.isPrimitiveArrayType(localType)) {
                            lvs.varToShadowVar.put(var, lvs.newShadowLV(MultiDTaintedArray.getTypeForType(localType), var));
                            varTypes.put(var, Opcodes.NULL);
                            shadowVar = lvs.varToShadowVar.get(var);
                            if(shadowVar == analyzer.locals.size()) {
                                analyzer.locals.add(Opcodes.NULL);
                            }
                        }
                    } else if(analyzer.locals.size() > var && analyzer.locals.get(var) == Opcodes.NULL) {
                        super.visitInsn(Opcodes.ACONST_NULL);
                        super.visitVarInsn(opcode, var);
                        analyzer.setTopOfStackTagged();
                        return;
                    }
                } else if(opcode == ASTORE) {
                    if(topCarriesTaint()) {
                        // That's easy.
                        if(getTopOfStackObject() == Opcodes.NULL) {
                            lvs.varToShadowVar.put(var, lvs.newShadowLV(Type.getType(Configuration.TAINT_TAG_ARRAYDESC), var));
                        } else {
                            lvs.varToShadowVar.put(var, lvs.newShadowLV(MultiDTaintedArray.getTypeForType(getTopOfStackType()), var));
                        }
                        varTypes.put(var, getTopOfStackObject());
                        shadowVar = lvs.varToShadowVar.get(var);
                    }
                } else {
                    lvs.varToShadowVar.put(var, lvs.newShadowLV(Type.getType(Configuration.TAINT_TAG_DESC), var));
                    varTypes.put(var, Configuration.TAINT_TAG_STACK_TYPE);
                    shadowVar = lvs.varToShadowVar.get(var);
                    if(opcode == ILOAD || opcode == FLOAD || opcode == DLOAD || opcode == LLOAD) {
                        if(shadowVar == analyzer.locals.size()) {
                            analyzer.locals.add(Configuration.TAINT_TAG_STACK_TYPE);
                        }
                    }
                }
            }
        }

        if(shadowVar >= 0) {
            switch(opcode) {
                case Opcodes.ILOAD:
                case Opcodes.FLOAD:
                case Opcodes.LLOAD:
                case Opcodes.DLOAD:
                    super.visitVarInsn((!Configuration.MULTI_TAINTING ? ILOAD : ALOAD), shadowVar);
                    super.visitVarInsn(opcode, var);
                    analyzer.setTopOfStackTagged();
                    return;
                case Opcodes.ALOAD:
                    if(var >= analyzer.locals.size()) {
                        System.err.println(analyzer.locals);
                        System.err.println(className);
                        throw new IllegalStateException("Trying to load an arg (" + var + ") past end of analyzer locals");
                    }

                    if(analyzer.locals.get(var) == Opcodes.NULL) {
                        if(TaintUtils.DEBUG_LOCAL) {
                            System.out.println("Ignoring shadow " + shadowVar + " on ALOAD " + var + " because var is null");
                        }
                        super.visitInsn(ACONST_NULL);
                        super.visitVarInsn(opcode, var);
                        analyzer.setTopOfStackTagged();
                        return;
                    }
                    if(analyzer.locals.get(var) instanceof Integer) {
                        System.out.println(className + "." + name);
                        System.out.println("ALOAD " + var + " but found " + analyzer.locals.get(var));
                        System.out.println(analyzer.locals);
                        throw new IllegalArgumentException();
                    }
                    if(analyzer.locals.get(var) instanceof Label) {
                        // this var is uninitilaized obj. def not an array or
                        // anything we care about.
                        super.visitVarInsn(opcode, var);
                        return;
                    }
                    Type localType;
                    if(analyzer.locals.get(var) instanceof TaggedValue) {
                        localType = Type.getType((String) ((TaggedValue) analyzer.locals.get(var)).v);
                    } else {
                        localType = Type.getType((String) analyzer.locals.get(var));
                    }
                    if(TaintUtils.DEBUG_LOCAL) {
                        System.out.println("Pre ALOAD " + var + "localtype " + localType);
                    }
                    if(localType.getSort() == Type.ARRAY && localType.getDimensions() == 1) {
                        switch(localType.getElementType().getSort()) {
                            case Type.ARRAY:
                            case Type.OBJECT:
                                super.visitVarInsn(opcode, var);
                                return;
                            default:
                                super.visitVarInsn(ALOAD, shadowVar);
                                super.visitVarInsn(opcode, var);
                                analyzer.setTopOfStackTagged();
                                if(TaintUtils.DEBUG_LOCAL) {
                                    System.out.println("POST ALOAD " + var);
                                }
                                if(TaintUtils.DEBUG_LOCAL) {
                                    System.out.println("Locals: " + analyzer.locals);
                                }
                                return;
                        }
                    } else {
                        System.out.println(var + ", shadow " + shadowVar);
                        super.visitVarInsn(opcode, var);
                        System.out.println(analyzer.stackTagStatus);
                        System.out.println(localType);
                        System.out.println(analyzer.locals.get(shadowVar));
                        throw new IllegalStateException("ALOAD " + var + "Shadow " + shadowVar);
                    }
                case Opcodes.ISTORE:
                case Opcodes.LSTORE:
                case Opcodes.FSTORE:
                case Opcodes.DSTORE:
                    super.visitVarInsn(opcode, var);
                    if(Configuration.MULTI_TAINTING) {
                        visit(COPY_TAINT);
                    }
                    super.visitVarInsn((!Configuration.MULTI_TAINTING ? ISTORE : ASTORE), shadowVar);
                    return;
                case Opcodes.ASTORE:
                    Object stackEl = analyzer.stack.get(analyzer.stack.size() - 1);
                    if(stackEl == Opcodes.NULL) {
                        super.visitVarInsn(ASTORE, shadowVar);
                        super.visitVarInsn(opcode, var);
                        if(TaintUtils.DEBUG_LOCAL) {
                            System.out.println("stack top was null, now POST ASTORE " + var + ": stack is " + analyzer.stack + " lvs " + analyzer.locals);
                        }
                        return;
                    }
                    if(!(stackEl instanceof String)) {
                        super.visitVarInsn(opcode, var);
                        IllegalStateException ex = new IllegalStateException("Doing ASTORE but top of stack isn't a type, it's " + stackEl);
                        ex.printStackTrace();
                        return;
                    }
                    Type stackType = Type.getType((String) stackEl);
                    if(stackType.getSort() == Type.ARRAY && stackType.getDimensions() == 1) {
                        switch(stackType.getElementType().getSort()) {
                            case Type.OBJECT:
                                super.visitVarInsn(opcode, var);
                                if(TaintUtils.DEBUG_LOCAL) {
                                    System.out.println("POST ASTORE " + var + ": stack is " + analyzer.stack + " lvs " + analyzer.locals);
                                }
                                return;
                            case Type.ARRAY:
                            default:
                                super.visitVarInsn(opcode, var);
                                super.visitVarInsn(ASTORE, shadowVar);
                                if(TaintUtils.DEBUG_LOCAL) {
                                    System.out.println("POST ASTORE " + var + ": stack is " + analyzer.stack + " lvs " + analyzer.locals);
                                }
                                return;
                        }
                    }
                    super.visitVarInsn(opcode, var);
                    return;
                case Opcodes.RET:
                    break;
            }
        } else {
            if(opcode == ASTORE && TaintUtils.isPrimitiveArrayType(getTopOfStackType())) {
                System.out.println("box astore " + var);
                registerTaintedArray();
            }
            super.visitVarInsn(opcode, var);
            if(TaintUtils.DEBUG_LOCAL) {
                System.out.println("(no shadow) POST " + opcode + " " + var + ": stack is " + analyzer.stack + " lvs " + analyzer.locals);
            }
        }
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        Type descType = Type.getType(desc);
        if(opcode == TaintUtils.FORCE_CTRL_STORE) {
            forceCtrlStoreFields.add(new Field(false, owner, name, desc));
            return;
        } else if(opcode == TaintUtils.FORCE_CTRL_STORE_SFIELD) {
            forceCtrlStoreFields.add(new Field(true, owner, name, desc));
            return;
        }
        if(isIgnoreAllInstrumenting) {
            super.visitFieldInsn(opcode, owner, name, desc);
            return;
        }

        boolean dispatched = false;
        if(descType.getSort() == Type.ARRAY && descType.getElementType().getSort() != Type.OBJECT && descType.getDimensions() > 1) {
            desc = MultiDTaintedArray.getTypeForType(descType).getInternalName();
        }
        if(Instrumenter.isUninstrumentedField(owner, name)) {
            String helperClass = "edu/columbia/cs/psl/phosphor/struct/multid/MultiDTaintedArrayWithObjTag";
            if(!Configuration.MULTI_TAINTING) {
                helperClass = "edu/columbia/cs/psl/phosphor/struct/multid/MultiDTaintedArrayWithIntTag";
            }
            switch(opcode) {
                case GETFIELD:
                case GETSTATIC:
                    //need to turn into a wrapped type
                    super.visitFieldInsn(opcode, owner, name, desc);
                    visit(BOX_IF_NECESSARY);
                    break;
                case PUTFIELD:
                case PUTSTATIC:
                    //Need to drop the taint array, if it's there
                    if(topCarriesTaint()) {
                        super.visitInsn(SWAP);
                        super.visitInsn(POP);
                        super.visitFieldInsn(opcode, owner, name, desc);
                        return;
                    } else {
                        //need to unbox this thing
                        super.visitTypeInsn(CHECKCAST, helperClass);
                        super.visitMethodInsn(INVOKEVIRTUAL, helperClass, "getVal", "()Ljava/lang/Object;", false);
                        super.visitFieldInsn(opcode, owner, name, desc);
                        return;
                    }
            }
        }
        if((!nextLoadIsTracked && (opcode == GETSTATIC || opcode == GETFIELD)) ||
                (opcode == PUTSTATIC && !analyzer.isTopOfStackTagged() && getTopOfStackType().getSort() == Type.ARRAY)) {
            Configuration.taintTagFactory.fieldOp(opcode, owner, name, desc, mv, lvs, this, nextLoadIsTracked);
            if(opcode == PUTSTATIC && owner.equals(className) && descType.getSort() == Type.ARRAY
                    && descType.getDimensions() == 1 && descType.getElementType().getSort() != Type.OBJECT) {
                String wrap = (String) TaintUtils.getShadowTaintTypeForFrame(desc);
                super.visitTypeInsn(NEW, wrap);
                super.visitInsn(DUP_X1);
                super.visitInsn(SWAP);
                super.visitMethodInsn(INVOKESPECIAL, wrap, "<init>", "()V", false);
                super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_FIELD, wrap);
                super.visitFieldInsn(opcode, owner, name, desc);
            } else {
                super.visitFieldInsn(opcode, owner, name, desc);
            }
            return;
        }
        boolean thisIsTracked = nextLoadIsTracked;
//		System.out.println(this.name);
//		System.out.println(nextLoadisTracked);
//		System.out.println(Printer.OPCODES[opcode] + name+owner+desc + "TRACKED");
//		System.out.println(analyzer.stackTagStatus);
        nextLoadIsTracked = false;

        if((Configuration.IMPLICIT_TRACKING || isImplicitLightTracking) && !Configuration.WITHOUT_PROPOGATION) {
            switch(opcode) {
                case PUTFIELD:
                    //taint the object owner of this field
                    if(!isSuperUninitialized) {
                        if(descType.getSize() == 1) {
                            if(topCarriesTaint()) {
                                super.visitInsn(DUP2_X1);
                                super.visitInsn(POP2);
                                super.visitInsn(DUP_X2);
                            } else {
                                super.visitInsn(SWAP);
                                super.visitInsn(DUP_X1);
                            }
                            super.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
                            visit(COMBINE_TAGS_ON_OBJECT);
                        } else {
                            //Obj Taint ValVal
                            int tmp = lvs.getTmpLV(descType);
                            super.visitVarInsn((descType.getSort() == Type.DOUBLE ? DSTORE : LSTORE), tmp);
                            super.visitInsn(SWAP);
                            super.visitInsn(DUP_X1);
                            super.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
                            visit(COMBINE_TAGS_ON_OBJECT);
                            super.visitVarInsn(descType.getSort() == Type.DOUBLE ? DLOAD : LLOAD, tmp);
                            lvs.freeTmpLV(tmp);
                        }

                    }
                case PUTSTATIC:
                    dispatched = true;
                    Configuration.taintTagFactory.fieldOp(opcode, owner, name, desc, mv, lvs, this, thisIsTracked);
                    if(descType.getSize() == 1) {
                        if(descType.getSort() == Type.OBJECT) {
                            super.visitInsn(DUP);
                            super.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
                            visit(COMBINE_TAGS_ON_OBJECT);
                        } else if(descType.getSort() != Type.ARRAY) {
                            super.visitInsn(SWAP);
                            super.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
                            visit(COMBINE_TAGS_STACK);
                            super.visitInsn(SWAP);
                        }
                    } else {
                        super.visitInsn(DUP2_X1);
                        super.visitInsn(POP2);
                        super.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
                        visit(COMBINE_TAGS_STACK);
                        super.visitInsn(DUP_X2);
                        super.visitInsn(POP);
                    }
                    break;
                case ASTORE:
                    break;
            }
        }


        boolean isIgnoredTaint = Instrumenter.isIgnoredClass(owner);

        if(!dispatched) {
            Configuration.taintTagFactory.fieldOp(opcode, owner, name, desc, mv, lvs, this, thisIsTracked);
        }
        switch(opcode) {
            case Opcodes.GETSTATIC:
                if(TaintUtils.isPrimitiveOrPrimitiveArrayType(descType)) {
                    if(isIgnoredTaint) {
                        Configuration.taintTagFactory.generateEmptyTaint(mv);
                        super.visitFieldInsn(opcode, owner, name, desc);
                    } else {
                        super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_FIELD, TaintUtils.getShadowTaintType(descType.getDescriptor()));
                        super.visitFieldInsn(opcode, owner, name, desc);
                    }
                    analyzer.setTopOfStackTagged();
                } else {
                    super.visitFieldInsn(opcode, owner, name, desc);
                }
                break;
            case Opcodes.GETFIELD:
                String shadowType = TaintUtils.getShadowTaintType(desc);
                if(shadowType != null) {
                    if(isIgnoredTaint) {
//					System.out.println("IGNORED " + owner+name+desc);
                        super.visitFieldInsn(opcode, owner, name, desc);
                        if(desc.startsWith("[")) {
                            retrieveTopOfStackTaintArray();
                        } else {
                            Configuration.taintTagFactory.generateEmptyTaint(mv);
                        }
                        super.visitInsn(SWAP);
                    } else {
                        super.visitInsn(DUP);
                        super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_FIELD, shadowType);
                        super.visitInsn(SWAP);
                        super.visitFieldInsn(opcode, owner, name, desc);
                    }
                    analyzer.setTopOfStackTagged();

                } else {
                    super.visitFieldInsn(opcode, owner, name, desc);
                }
                break;
            case Opcodes.PUTSTATIC:
                if(getTopOfStackObject() != Opcodes.NULL && getTopOfStackType().getSort() == Type.OBJECT
                        && descType.getSort() == Type.ARRAY
                        && descType.getDimensions() == 1
                        && descType.getElementType().getSort() != Type.OBJECT) {
                    retrieveTaintedArray(desc);
                }
                shadowType = TaintUtils.getShadowTaintType(desc);
                super.visitFieldInsn(opcode, owner, name, desc);
                if(TaintUtils.isPrimitiveOrPrimitiveArrayType(descType)) {
                    if(isIgnoredTaint) {
                        super.visitInsn(POP);
                    } else {
                        super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_FIELD, shadowType);
                    }
                }
                break;
            case Opcodes.PUTFIELD:
                //get an extra copy of the field owner
                if(getTopOfStackObject() != Opcodes.NULL && getTopOfStackType().getSort() == Type.OBJECT && descType.getSort() == Type.ARRAY && descType.getDimensions() == 1 && descType.getElementType().getSort() != Type.OBJECT) {
                    retrieveTaintedArray(desc);
                }
                shadowType = TaintUtils.getShadowTaintType(desc);

                if(shadowType != null) {
                    if(Type.getType(desc).getSize() == 2) {

                        // R T VV
                        super.visitInsn(DUP2_X2);
                        super.visitInsn(POP2); // VV R T
                        super.visitInsn(SWAP);// VV T R
                        super.visitInsn(DUP_X1); // VV R T R
                        super.visitInsn(SWAP);// VV R R T
                        if(isIgnoredTaint) {
                            super.visitInsn(POP2);
                        } else {
                            super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_FIELD, shadowType);
                        }
                        super.visitInsn(DUP_X2);//VV R
                        super.visitInsn(POP);// R VV R
                        super.visitFieldInsn(opcode, owner, name, desc);// R VV
                    } else {
                        //Are we storing ACONST_NULL to a primitive array field? If so, there won't be a taint!
                        if(Type.getType(desc).getSort() == Type.ARRAY && Type.getType(desc).getElementType().getSort() != Type.OBJECT && analyzer.stack.get(analyzer.stack.size() - 1) == Opcodes.NULL) {
                            super.visitInsn(POP2);
                            super.visitInsn(DUP);
                            super.visitInsn(ACONST_NULL);
                            super.visitFieldInsn(opcode, owner, name, desc);
                            super.visitInsn(ACONST_NULL);
                            if(isIgnoredTaint) {
                                super.visitInsn(POP2);
                            } else {
                                super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_FIELD, shadowType);
                            }
                        } else {
                            super.visitInsn(DUP2_X1);
                            super.visitInsn(POP2);
                            super.visitInsn(DUP_X2);
                            super.visitInsn(SWAP);
                            super.visitFieldInsn(opcode, owner, name, desc);
                            if(isIgnoredTaint) {
                                super.visitInsn(POP2);
                            } else {
                                super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_FIELD, shadowType);
                            }
                        }
                    }
                } else {
                    Type onStack = getTopOfStackType();
                    if(onStack.getSort() == Type.ARRAY && onStack.getElementType().getSort() != Type.OBJECT && onStack.getDimensions() == 1) {
                        registerTaintedArray();
                        super.visitFieldInsn(opcode, owner, name, desc);
                    } else {
                        super.visitFieldInsn(opcode, owner, name, desc);
                    }
                }

                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    public void doForceCtrlStores() {
        if(Configuration.WITHOUT_BRANCH_NOT_TAKEN) {
            forceCtrlStoreFields.clear();
            forceCtrlAdd.clear();
            return;
        }
        MethodVisitor ta = mv;
        for(Field f : forceCtrlStoreFields) {
            addControlTagsToOwnedField(ta, f);
        }
        forceCtrlStoreFields.clear();
        for(int var : forceCtrlAdd) {
            int shadowVar = -1;
            if(analyzer.locals.size() <= var || analyzer.locals.get(var) == Opcodes.TOP) {
                continue;
            }
            if(var < lastArg && TaintUtils.getShadowTaintType(paramTypes[var].getDescriptor()) != null) {
                //accessing an arg; remap it
                Type localType = paramTypes[var];
                if(localType.getSort() != Type.OBJECT && localType.getSort() != Type.ARRAY) {
                    shadowVar = var - 1;
                } else if(localType.getSort() == Type.ARRAY) {
                    continue;
                }
            } else {
                if(lvs.varToShadowVar.containsKey(var)) {
                    shadowVar = lvs.varToShadowVar.get(var);
                    if(analyzer.locals.get(var) instanceof String && ((String) analyzer.locals.get(var)).startsWith("[")) {
                        continue;
                    }
                    if(shadowVar >= analyzer.locals.size() || analyzer.locals.get(shadowVar) instanceof Integer || ((String) analyzer.locals.get(shadowVar)).startsWith("[")) {
                        continue;
                    }
                }
            }
            if(shadowVar >= 0) {
                ta.visitVarInsn(ALOAD, shadowVar);
                ta.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
                COMBINE_TAGS_STACK.delegateVisit(ta);
                ta.visitVarInsn(ASTORE, shadowVar);
            } else {
                if(!(analyzer.locals.get(var) instanceof Integer)) {
                    ta.visitVarInsn(ALOAD, var);
                    ta.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
                    visit(COMBINE_TAGS_ON_OBJECT);
                }
            }
        }
        forceCtrlAdd.clear();
    }

    /**
     * Visits instructions to add tags from the ControlTaintTagStack to the specified field. The specified field
     * should be one that is owned by the class being visited.
     *
     * @param delegate the method visitor to which instruction visits are delegated
     * @param field the field whose associated taint tag should be updated
     */
    private void addControlTagsToOwnedField(MethodVisitor delegate, Field field) {
        int getFieldOpcode = field.isStatic ? GETSTATIC : GETFIELD;
        int putFieldOpcode = field.isStatic ? PUTSTATIC : PUTFIELD;
        if(field.description.equals(BOOLEAN_DESC) || field.description.equals(BYTE_DESC)
                ||field.description.equals(CHARACTER_DESC) || field.description.equals(SHORT_DESC)) {
            if(!field.isStatic) {
                delegate.visitVarInsn(ALOAD, 0); // Load this onto the stack
                delegate.visitInsn(DUP);
            }
            delegate.visitFieldInsn(getFieldOpcode, field.owner, field.name, field.description);
            delegate.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
            delegate.visitMethodInsn(INVOKESTATIC, Type.getInternalName(BoxedPrimitiveStoreWithObjTags.class), "getTaintedBoxedPrimitive",
                    String.format("(%s%s)%s", field.description, Type.getDescriptor(ControlTaintTagStack.class), field.description), false);
            delegate.visitFieldInsn(putFieldOpcode, field.owner, field.name, field.description);
        } else if(TaintUtils.isPrimitiveType(Type.getType(field.description))) {
            if(!field.isStatic) {
                delegate.visitVarInsn(ALOAD, 0); // Load this onto the stack
                delegate.visitInsn(DUP);
            }
            delegate.visitFieldInsn(getFieldOpcode, field.owner, field.name + TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_DESC);
            delegate.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
            COMBINE_TAGS_STACK.delegateVisit(delegate);
            delegate.visitFieldInsn(putFieldOpcode, field.owner, field.name + TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_DESC);
        } else if(Type.getType(field.description).getSort() == Type.OBJECT) {
            if(!field.isStatic) {
                delegate.visitVarInsn(ALOAD, 0); // Load this onto the stack
            }
            delegate.visitFieldInsn(getFieldOpcode, field.owner, field.name, field.description);
            delegate.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
            COMBINE_TAGS_ON_OBJECT.delegateVisit(delegate);
        }
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        if(isIgnoreAllInstrumenting) {
            super.visitIntInsn(opcode, operand);
            return;
        }

        switch(opcode) {
            case TaintUtils.IGNORE_EVERYTHING:
                isIgnoreAllInstrumenting = true;
                break;
            case Opcodes.BIPUSH:
            case Opcodes.SIPUSH:
                if(nextLoadIsTracked) {
                    if(Configuration.IMPLICIT_TRACKING || isImplicitLightTracking) {
                        super.visitVarInsn(ALOAD, lvs.idxOfMasterControlLV);
                        super.visitMethodInsn(INVOKEVIRTUAL, "edu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack", "copyTag" + (Configuration.IMPLICIT_EXCEPTION_FLOW ? "Exceptions" : ""), "()" + Configuration.TAINT_TAG_DESC, false);
                    } else {
                        super.visitInsn(Configuration.NULL_TAINT_LOAD_OPCODE);
                    }
                    nextLoadIsTracked = false;
                    super.visitIntInsn(opcode, operand);
                    analyzer.setTopOfStackTagged();
                } else {
                    super.visitIntInsn(opcode, operand);
                }

                break;
            case Opcodes.NEWARRAY:
                super.visitIntInsn(opcode, operand);
                if(nextLoadIsTracked) {
                    String arType = MultiDTaintedArray.getTaintArrayInternalName(operand);
                    String desc = MultiDTaintedArray.getArrayDescriptor(operand);
                    if(Configuration.ARRAY_LENGTH_TRACKING) {
                        super.visitInsn(DUP_X1); // AR LT AR
                        super.visitTypeInsn(NEW, arType); // AR LT AR T
                        super.visitInsn(DUP_X2);// AR T LT AR T
                        super.visitInsn(DUP_X2);// AR T T LT AR T
                        super.visitInsn(POP);// AR T T LT AR
                        super.visitMethodInsn(INVOKESPECIAL, arType, "<init>", "(" + Configuration.TAINT_TAG_DESC + desc + ")V", false);
                        super.visitInsn(SWAP);
                    } else {
                        super.visitInsn(DUP); // AR AR
                        super.visitTypeInsn(NEW, arType); // AR AR T
                        super.visitInsn(DUP_X1);// AR T AR T
                        super.visitInsn(SWAP);
                        super.visitMethodInsn(INVOKESPECIAL, arType, "<init>", "(" + desc + ")V", false);
                        super.visitInsn(SWAP);
                    }
                    analyzer.setTopOfStackTagged();
                    nextLoadIsTracked = false;
                }
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
        if(isIgnoreAllInstrumenting) {
            super.visitMultiANewArrayInsn(desc, dims);
            return;
        }
        if(nextLoadIsTracked) {
            nextLoadIsTracked = false;
        }
        Type arrayType = Type.getType(desc);
        Type origType = arrayType;
        boolean needToHackDims = false;
        int tmp = 0;
        int tmp2 = 0;
        Type tagType = Type.getType(Configuration.TAINT_TAG_DESC);
        if(arrayType.getElementType().getSort() != Type.OBJECT) {
            if(dims == arrayType.getDimensions()) {
                needToHackDims = true;
                dims--;
                tmp = lvs.getTmpLV(Type.INT_TYPE);
                super.visitVarInsn(Opcodes.ISTORE, tmp);

                if(Configuration.ARRAY_LENGTH_TRACKING) {
                    tmp2 = lvs.getTmpLV(tagType);
                    super.visitVarInsn(tagType.getOpcode(Opcodes.ISTORE), tmp2);
                }
            }
            arrayType = MultiDTaintedArray.getTypeForType(arrayType);
            desc = arrayType.getInternalName();
        }
        int[] dimsLvs;
        int taintsLvs = -1;
        if(Configuration.ARRAY_LENGTH_TRACKING) {
            super.visitIntInsn(BIPUSH, dims);
            if(Configuration.MULTI_TAINTING) {
                super.visitTypeInsn(ANEWARRAY, tagType.getInternalName());
            } else {
                super.visitIntInsn(NEWARRAY, Opcodes.T_INT);
            }
            taintsLvs = lvs.getTmpLV(Type.getType(Configuration.TAINT_TAG_ARRAYDESC));
            super.visitVarInsn(ASTORE, taintsLvs);
            dimsLvs = new int[dims];
            for(int i = 0; i < dims; i++) {
                dimsLvs[i] = lvs.getTmpLV(Type.INT_TYPE);
                super.visitVarInsn(ISTORE, dimsLvs[i]);
                super.visitVarInsn(ALOAD, taintsLvs);
                super.visitInsn(SWAP);
                super.visitIntInsn(BIPUSH, i);
                super.visitInsn(SWAP);
                super.visitInsn(tagType.getOpcode(IASTORE));
            }
            for(int i = 0; i < dims; i++) {
                super.visitVarInsn(ILOAD, dimsLvs[dims - i - 1]);
                lvs.freeTmpLV(dimsLvs[dims - i - 1]);
            }
        }
        if(dims == 1) {
            //It's possible that we dropped down to a 1D object type array
            super.visitTypeInsn(ANEWARRAY, arrayType.getElementType().getInternalName());
        } else {
            super.visitMultiANewArrayInsn(desc, dims);
        }

        if(needToHackDims) {
            super.visitInsn(DUP);
            if(Configuration.ARRAY_LENGTH_TRACKING) {
                super.visitVarInsn(tagType.getOpcode(ILOAD), tmp2);
                lvs.freeTmpLV(tmp2);
            }
            super.visitVarInsn(ILOAD, tmp);
            lvs.freeTmpLV(tmp);
            super.visitIntInsn(BIPUSH, origType.getElementType().getSort());
            super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(MultiDTaintedArray.class), "initLastDim", "([Ljava/lang/Object;" + (Configuration.ARRAY_LENGTH_TRACKING ? Configuration.TAINT_TAG_DESC : "") + "II)V",
                    false);

        }
        if(Configuration.ARRAY_LENGTH_TRACKING) {
            super.visitInsn(DUP);
            super.visitVarInsn(ALOAD, taintsLvs);
            super.visitIntInsn(BIPUSH, dims);
            super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTagsOnArrayInPlace", "([Ljava/lang/Object;[" + Configuration.TAINT_TAG_DESC + "I)V", false);
            lvs.freeTmpLV(taintsLvs);
        }
    }

    @Override
    public void visitLdcInsn(Object cst) {
        if(nextLoadIsTracked) {
            if(Configuration.IMPLICIT_TRACKING || isImplicitLightTracking) {
                super.visitVarInsn(ALOAD, lvs.idxOfMasterControlLV);
                super.visitMethodInsn(INVOKEVIRTUAL, "edu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack", "copyTag" + (Configuration.IMPLICIT_EXCEPTION_FLOW ? "Exceptions" : ""), "()" + Configuration.TAINT_TAG_DESC, false);
            } else {
                super.visitInsn(Configuration.NULL_TAINT_LOAD_OPCODE);
            }
            nextLoadIsTracked = false;
            super.visitLdcInsn(cst);
            analyzer.setTopOfStackTagged();
        } else {
            super.visitLdcInsn(cst);
        }
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        if(isIgnoreAllInstrumenting) {
            super.visitTypeInsn(opcode, type);
            return;
        }
        switch(opcode) {
            case TaintUtils.EXCEPTION_HANDLER_START:
                if(type == null) {
                    //Special sentinel to say that this is the start of the exception handler and that a list of covered types will follow
                    super.visitInsn(DUP);
                    super.visitVarInsn(ALOAD, lvs.idxOfMasterControlLV);
                    super.visitInsn(SWAP);
                    super.visitVarInsn(ALOAD, this.idxOfCaughtExceptionTaint);
                    super.visitMethodInsn(INVOKEVIRTUAL, "edu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack", "exceptionHandlerStart", "(Ljava/lang/Throwable;" + Type.getDescriptor(EnqueuedTaint.class) + ")" + Type.getDescriptor(EnqueuedTaint.class), false);
                    super.visitVarInsn(ASTORE, this.idxOfCaughtExceptionTaint);
                    doForceCtrlStores();
                    return;
                }
                super.visitVarInsn(ALOAD, lvs.idxOfMasterControlLV);
                super.visitLdcInsn(Type.getObjectType(type));
                super.visitMethodInsn(INVOKEVIRTUAL, "edu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack", "exceptionHandlerStart", "(Ljava/lang/Class;)V", false);

                break;
            case TaintUtils.EXCEPTION_HANDLER_END:
                if(type == null) {
                    //end of handler
                    super.visitVarInsn(ALOAD, lvs.idxOfMasterControlLV);
                    super.visitVarInsn(ALOAD, this.idxOfCaughtExceptionTaint);
                    super.visitMethodInsn(INVOKEVIRTUAL, "edu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack", "exceptionHandlerEnd", "(" + Type.getDescriptor(EnqueuedTaint.class) + ")V", false);
                } else {
                    //end of try block
                    doForceCtrlStores();
                    super.visitVarInsn(ALOAD, lvs.idxOfMasterControlLV);
                    super.visitLdcInsn(Type.getObjectType(type));
                    super.visitMethodInsn(INVOKEVIRTUAL, "edu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack", "tryBlockEnd", "(Ljava/lang/Class;)V", false);
                }
                break;
            case TaintUtils.UNTHROWN_EXCEPTION:
                //We want to note that we are returning and might instead have thrown an exception
                super.visitVarInsn(ALOAD, lvs.idxOfMasterControlLV);
                super.visitVarInsn(ALOAD, lvs.idxOfMasterExceptionLV);
                super.visitLdcInsn(Type.getObjectType(type));
                super.visitMethodInsn(INVOKEVIRTUAL, "edu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack", "addUnthrownException", "(" + Type.getDescriptor(ExceptionalTaintData.class) + "Ljava/lang/Class;)V", false);
                break;
            case TaintUtils.UNTHROWN_EXCEPTION_CHECK:
                super.visitVarInsn(ALOAD, lvs.idxOfMasterControlLV);
                super.visitLdcInsn(Type.getObjectType(type));
                super.visitMethodInsn(INVOKEVIRTUAL, "edu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack", "applyPossiblyUnthrownExceptionToTaint", "(Ljava/lang/Class;)V", false);
                break;
            case Opcodes.ANEWARRAY:
                if(Configuration.ARRAY_LENGTH_TRACKING && !Configuration.WITHOUT_PROPOGATION) {
                    Type t = Type.getObjectType(type);
                    if(t.getSort() == Type.ARRAY && t.getElementType().getDescriptor().length() == 1) {
                        //e.g. [I for a 2 D array -> MultiDTaintedIntArray
                        type = MultiDTaintedArray.getTypeForType(t).getInternalName();
                    }
                    //L TL
                    super.visitTypeInsn(opcode, type);
                    //A TL
                    super.visitInsn(DUP_X1);
                    //A TL A
                    super.visitInsn(SWAP);
                    //TL A A
                    super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTagsInPlace", "(Ljava/lang/Object;" + Configuration.TAINT_TAG_DESC + ")V", false);
                } else {
                    Type t = Type.getObjectType(type);
                    if(t.getSort() == Type.ARRAY && t.getElementType().getDescriptor().length() == 1) {
                        //e.g. [I for a 2 D array -> MultiDTaintedIntArray
                        type = MultiDTaintedArray.getTypeForType(t).getInternalName();
                    }
                    super.visitTypeInsn(opcode, type);
                }
                nextLoadIsTracked = false;
                break;
            case Opcodes.NEW:
                super.visitTypeInsn(opcode, type);
                break;
            case Opcodes.CHECKCAST:
                Type t = Type.getObjectType(type);
                if(nextLoadIsTracked) {
                    checkCast(type);
                    nextLoadIsTracked = false;
                    analyzer.setTopOfStackTagged();
                } else if(TaintUtils.isPrimitiveArrayType(t)) {
                    if(getTopOfStackType().getSort() == Type.ARRAY) {
                        super.visitTypeInsn(opcode, type);
                        return;
                    } else {
                        retrieveTaintedArrayWithoutTags(type);
                        super.visitTypeInsn(opcode, type);
                        return;
                    }
                } else {
                    checkCast(type);
                }
                break;
            case Opcodes.INSTANCEOF:
                if(nextLoadIsTracked) {
                    if(Configuration.IMPLICIT_TRACKING || isImplicitLightTracking) {
                        super.visitInsn(DUP);
                        visit(GET_TAINT_OBJECT);
                        super.visitInsn(SWAP);
                        instanceOf(type);
                    } else {
                        instanceOf(type);
                        super.visitInsn(Configuration.NULL_TAINT_LOAD_OPCODE);
                        super.visitInsn(SWAP);
                    }
                    nextLoadIsTracked = false;
                    analyzer.setTopOfStackTagged();
                } else {
                    instanceOf(type);
                }
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    private void instanceOf(String type) {
        Type t = Type.getObjectType(type);

        boolean doIOR = false;
        if(t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT) {
            if(t.getDimensions() > 1) {
                type = MultiDTaintedArray.getTypeForType(t).getDescriptor();
            } else if(!topCarriesTaint()) {
                doIOR = true;
                //Maybe we have it boxed on the stack, maybe we don't - how do we know? Who cares, just check both...
                super.visitInsn(DUP);
                super.visitTypeInsn(INSTANCEOF, Type.getInternalName(MultiDTaintedArray.getClassForComponentType(t.getElementType().getSort())));
                super.visitInsn(SWAP);
            }
        }
        super.visitTypeInsn(INSTANCEOF, type);
        if(doIOR) {
            super.visitInsn(IOR);
        }
    }

    private void checkCast(String type) {
        Type t = Type.getObjectType(type);
        int opcode = CHECKCAST;
        if(t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT) {
            if(t.getDimensions() > 1) {
                //Hahaha you thought you could cast to a primitive multi dimensional array!

                super.visitTypeInsn(opcode, MultiDTaintedArray.getTypeForType(Type.getType(type)).getDescriptor());
                return;
            } else {
                //what is on the top of the stack that we are checkcast'ing?
                Object o = analyzer.stack.get(analyzer.stack.size() - 1);
                if(o instanceof String) {
                    Type zz = TaintAdapter.getTypeForStackType(o);
                    if(zz.getSort() == Type.ARRAY && zz.getElementType().getSort() != Type.OBJECT) {
                        super.visitTypeInsn(opcode, type);
                        return;
                    }
                }
                //cast of Object[] or Object to char[] or int[] etc.
                if(o == Opcodes.NULL) {
                    super.visitInsn(SWAP);
                    super.visitTypeInsn(CHECKCAST, MultiDTaintedArray.getTypeForType(t).getInternalName());
                    super.visitInsn(SWAP);
                } else {
                    Type wrap = MultiDTaintedArray.getTypeForType(t);
                    super.visitTypeInsn(CHECKCAST, wrap.getInternalName());
                    retrieveTaintedArray(type);
                }
                super.visitTypeInsn(opcode, type);
            }
        } else {

            //What if we are casting an array to Object?
            if(TaintUtils.isPrimitiveArrayType(getTopOfStackType())) {
                //Casting array to non-array type

                //Register the taint array for later.
                registerTaintedArray();
            }
            super.visitTypeInsn(opcode, type);
        }
    }

    /**
     * Pre: A Post: TA A
     *
     */
    private void retrieveTaintedArray(String type) {
        //A
        Label isNull = new Label();
        Label isDone = new Label();
        Type toCastTo = MultiDTaintedArray.getTypeForType(Type.getType(type));
        super.visitInsn(DUP);
        if(!isIgnoreAllInstrumenting) {
            super.visitInsn(TaintUtils.IGNORE_EVERYTHING);
        }
        super.visitJumpInsn(IFNULL, isNull);
        if(!isIgnoreAllInstrumenting) {
            super.visitInsn(TaintUtils.IGNORE_EVERYTHING);
        }
        super.visitTypeInsn(CHECKCAST, toCastTo.getInternalName());
        FrameNode fn = getCurrentFrameNode();
        super.visitInsn(DUP);
        //A A
        super.visitFieldInsn(GETFIELD, toCastTo.getInternalName(), "val", type);
        FrameNode fn2 = getCurrentFrameNode();
        super.visitJumpInsn(GOTO, isDone);
        super.visitLabel(isNull);
        acceptFn(fn);
        super.visitTypeInsn(CHECKCAST, toCastTo.getInternalName());
        super.visitInsn(ACONST_NULL);
        super.visitTypeInsn(CHECKCAST, type);
        super.visitLabel(isDone);
        acceptFn(fn2);
    }

    public void retrieveTaintedArrayWithoutTags(String type) {
        //A
        Label isNull = new Label();
        Label isDone = new Label();

        FrameNode fn = getCurrentFrameNode();

        super.visitInsn(DUP);
        if(!isIgnoreAllInstrumenting) {
            super.visitInsn(TaintUtils.IGNORE_EVERYTHING);
        }
        super.visitJumpInsn(IFNULL, isNull);
        if(!isIgnoreAllInstrumenting) {
            super.visitInsn(TaintUtils.IGNORE_EVERYTHING);
        }
        Class boxType = MultiDTaintedArray.getClassForComponentType(Type.getType(type).getElementType().getSort());
        super.visitTypeInsn(CHECKCAST, Type.getInternalName(boxType));
        super.visitFieldInsn(GETFIELD, Type.getInternalName(boxType), "val", type);
        FrameNode fn2 = getCurrentFrameNode();
        super.visitJumpInsn(GOTO, isDone);
        super.visitLabel(isNull);
        acceptFn(fn);
        super.visitTypeInsn(CHECKCAST, type);
        super.visitLabel(isDone);
        acceptFn(fn2);
    }

    private void registerTaintedArray() {
        super.visitInsn(SWAP);
        Label isnull = new Label();
        Label ok = new Label();
        FrameNode fn2 = getCurrentFrameNode();

        super.visitInsn(DUP);
        super.visitJumpInsn(IFNULL, isnull);
        super.visitInsn(DUP_X1);
        super.visitInsn(SWAP);
        Type onTop = getTopOfStackType();
        String wrapper = (String) TaintUtils.getShadowTaintTypeForFrame(onTop.getDescriptor());
        super.visitMethodInsn(INVOKEVIRTUAL, wrapper, "ensureVal", "(" + onTop.getDescriptor() + ")V", false);
        FrameNode fn = getCurrentFrameNode();
        super.visitJumpInsn(GOTO, ok);
        super.visitLabel(isnull);
        acceptFn(fn2);
        super.visitInsn(SWAP);
        super.visitInsn(POP);
        super.visitLabel(ok);
        acceptFn(fn);
        //A
    }

    /**
     * Will insert a NULL after the nth element from the top of the stack
     */
    private void insertNullAt(int n) {
        switch(n) {
            case 0:
                super.visitInsn(ACONST_NULL);
                break;
            case 1:
                super.visitInsn(ACONST_NULL);
                super.visitInsn(SWAP);
                break;
            case 2:
                super.visitInsn(ACONST_NULL);
                super.visitInsn(DUP_X2);
                super.visitInsn(POP);
                break;
            default:
                LocalVariableNode[] d = storeToLocals(n);
                super.visitInsn(ACONST_NULL);
                for(int i = n - 1; i >= 0; i--) {
                    loadLV(i, d);
                }
                freeLVs(d);
        }
    }

    /**
     * Pop at n means pop the nth element down from the top (pop the top is n=0)
     */
    private void popAt(int n) {
        if(TaintUtils.DEBUG_DUPSWAP) {
            System.out.println(name + " POP AT " + n + " from " + analyzer.stack);
        }
        switch(n) {
            case 0:
                Object top = analyzer.stack.get(analyzer.stack.size() - 1);
                if(top == Opcodes.LONG || top == Opcodes.DOUBLE || top == Opcodes.TOP) {
                    super.visitInsn(POP2);
                } else {
                    super.visitInsn(POP);
                }
                break;
            case 1:
                top = analyzer.stack.get(analyzer.stack.size() - 1);
                if(top == Opcodes.LONG || top == Opcodes.DOUBLE || top == Opcodes.TOP) {
                    Object second = analyzer.stack.get(analyzer.stack.size() - 3);
                    if(second == Opcodes.LONG || second == Opcodes.DOUBLE || second == Opcodes.TOP) {
                        //VV VV
                        super.visitInsn(DUP2_X2);
                        super.visitInsn(POP2);
                        super.visitInsn(POP2);
                    } else {
                        //V VV
                        super.visitInsn(DUP2_X1);
                        super.visitInsn(POP2);
                        super.visitInsn(POP);
                    }
                } else {
                    Object second = analyzer.stack.get(analyzer.stack.size() - 2);
                    if(second == Opcodes.LONG || second == Opcodes.DOUBLE || second == Opcodes.TOP) {
                        //VV V
                        super.visitInsn(DUP_X2);
                        super.visitInsn(POP);
                        super.visitInsn(POP2);
                    } else {
                        //V V
                        super.visitInsn(SWAP);
                        super.visitInsn(POP);
                    }
                }
                break;
            default:
                LocalVariableNode[] d = storeToLocals(n);
                super.visitInsn(POP);
                for(int i = n - 1; i >= 0; i--) {
                    loadLV(i, d);
                }
                freeLVs(d);
        }
        if(TaintUtils.DEBUG_CALLS) {
            System.out.println("POST POP AT " + n + ":" + analyzer.stack);
        }
    }

    /**
     * Store at n means pop the nth element down from the top and store it to
     * our arraystore (pop the top is n=0)
     */
    private void storeTaintArrayAt(int n) {
        if(TaintUtils.DEBUG_DUPSWAP) {
            System.out.println(name + " POP AT " + n + " from " + analyzer.stack);
        }
        switch(n) {
            case 0:
                throw new IllegalStateException("Not supposed to ever pop the top like this");
                //			break;
            case 1:
                Object top = analyzer.stack.get(analyzer.stack.size() - 1);
                if(top == Opcodes.LONG || top == Opcodes.DOUBLE || top == Opcodes.TOP) {
                    throw new IllegalStateException("Not supposed to ever pop the top like this");
                } else {
                    Object second = analyzer.stack.get(analyzer.stack.size() - 2);
                    if(second == Opcodes.LONG || second == Opcodes.DOUBLE || second == Opcodes.TOP) {
                        throw new IllegalStateException("Not supposed to ever pop the top like this");
                    } else {
                        //V V
                        registerTaintedArray();
                    }
                }
                break;
            default:
                LocalVariableNode[] d = storeToLocals(n - 1);
                registerTaintedArray();
                for(int i = n - 2; i >= 0; i--) {
                    loadLV(i, d);
                }
                freeLVs(d);
        }
        if(TaintUtils.DEBUG_CALLS) {
            System.out.println("POST POP AT " + n + ":" + analyzer.stack);
        }
    }

    private void unboxTaintArrayAt(int n, String descAtDest) {
        if(TaintUtils.DEBUG_DUPSWAP) {
            System.out.println(name + " unbox AT " + n + " from " + analyzer.stack);
        }
        switch(n) {
            case 0:
                retrieveTaintedArray(descAtDest);
            case 1:
                Object top = analyzer.stack.get(analyzer.stack.size() - 1);
                if(top == Opcodes.LONG || top == Opcodes.DOUBLE || top == Opcodes.TOP) {
                    throw new IllegalStateException("Not supposed to ever pop the top like this");
                } else {
                    Object second = analyzer.stack.get(analyzer.stack.size() - 2);
                    if(second == Opcodes.LONG || second == Opcodes.DOUBLE || second == Opcodes.TOP) {
                        throw new IllegalStateException("Not supposed to ever pop the top like this");
                    } else {
                        //V
                        retrieveTaintedArray(descAtDest);
                    }
                }
                break;
            default:
                LocalVariableNode[] d = storeToLocals(n - 1);
                retrieveTaintedArray(descAtDest);
                for(int i = n - 2; i >= 0; i--) {
                    loadLV(i, d);
                }
                freeLVs(d);
        }
        if(TaintUtils.DEBUG_CALLS) {
            System.out.println("POST POP AT " + n + ":" + analyzer.stack);
        }
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
        String owner = bsm.getOwner();
        boolean hasNewName = !TaintUtils.remapMethodDesc(desc).equals(desc);
        String newDesc = TaintUtils.remapMethodDesc(desc);
        boolean isPreAllocatedReturnType = TaintUtils.isPreAllocReturnType(desc);
        boolean isIgnoredForTaints = Configuration.WITH_SELECTIVE_INST && Instrumenter.isIgnoredMethodFromOurAnalysis(owner, name, desc);
        int opcode = INVOKEVIRTUAL;
        if(bsm.getTag() == Opcodes.H_INVOKESTATIC) {
            opcode = INVOKESTATIC;
        }
        if(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING) {
            hasNewName = true;
            newDesc = TaintUtils.remapMethodDescAndIncludeReturnHolderNoControlStack(desc);
        }
        if(name.equals("<init>") && !newDesc.equals(desc)) {
            //Add the taint sentinel to the desc
            super.visitInsn(ACONST_NULL);
            newDesc = newDesc.substring(0, newDesc.indexOf(")")) + Type.getDescriptor(TaintSentinel.class) + ")" + Type.getReturnType(newDesc).getDescriptor();
        }
        if(isPreAllocatedReturnType) {
            Type t = Type.getReturnType(newDesc);
            newDesc = newDesc.substring(0, newDesc.indexOf(")")) + t.getDescriptor() + ")" + t.getDescriptor();
            super.visitVarInsn(ALOAD, lvs.getPreAllocedReturnTypeVar(t));
        }
        Type origReturnType = Type.getReturnType(desc);
        Type returnType = TaintUtils.getContainerReturnType(Type.getReturnType(desc));
        if(TaintUtils.DEBUG_CALLS) {
            System.out.println("Remapped call from " + owner + "." + name + desc + " to " + owner + "." + name + newDesc);
        }
        if(!name.contains("<") && hasNewName) {
            name += TaintUtils.METHOD_SUFFIX;
        }
        if(TaintUtils.DEBUG_CALLS) {
            System.out.println("Calling w/ stack: " + analyzer.stack);
        }

        //if you call a method and instead of passing a primitive array you pass ACONST_NULL, we need to insert another ACONST_NULL in the stack
        //for the taint for that array
        Type[] args = Type.getArgumentTypes(newDesc);
        Type[] argsInReverse = new Type[args.length];
        int argsSize = 0;
        for(int i = 0; i < args.length; i++) {
            argsInReverse[args.length - i - 1] = args[i];
            argsSize += args[i].getSize();
        }
        int i = 1;
        int n = 1;
        boolean ignoreNext = false;
        for(Type t : argsInReverse) {
            if(analyzer.stack.get(analyzer.stack.size() - i) == Opcodes.TOP) {
                i++;
            }
            Type onStack = getTypeForStackType(analyzer.stack.get(analyzer.stack.size() - i));
            if(!ignoreNext && t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT) {
                //Need to check to see if there's a null on the stack in this position
                if(analyzer.stack.get(analyzer.stack.size() - i) == Opcodes.NULL) {
                    if(TaintUtils.DEBUG_CALLS) {
                        System.err.println("Adding a null in call at " + n);
                    }
                    insertNullAt(n);
                } else if(onStack.getSort() == Type.OBJECT && (!isIgnoredForTaints || t.getDimensions() == 1)) {
                    //Unbox this
                    unboxTaintArrayAt(n, t.getDescriptor());
                }
            } else if(!ignoreNext && onStack.getSort() == Type.ARRAY && onStack.getElementType().getSort() != Type.OBJECT) {
                //There is an extra taint on the stack at this position
                if(TaintUtils.DEBUG_CALLS) {
                    System.err.println("removing taint array in call at " + n);
                }
                storeTaintArrayAt(n);
            }
            if((t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT) || (t.getDescriptor().startsWith("Ledu/columbia/cs/psl/phosphor/struct/Lazy"))) {
                ignoreNext = !ignoreNext;
            }
            n++;
            i++;
        }
        boolean isCalledOnAPrimitiveArrayType = false;
        if(opcode == INVOKEVIRTUAL) {
            if(analyzer.stack.get(analyzer.stack.size() - argsSize - 1) == null) {
                System.out.println("NULL on stack for calllee???" + analyzer.stack + " argsize " + argsSize);
            }
            Type callee = getTypeForStackType(analyzer.stack.get(analyzer.stack.size() - argsSize - 1));
            if(TaintUtils.DEBUG_CALLS) {
                System.out.println("CALLEE IS " + callee);
            }
            if(callee.getSort() == Type.ARRAY && callee.getElementType().getSort() != Type.OBJECT) {
                isCalledOnAPrimitiveArrayType = true;
            }
        }
        if(bsmArgs != null) {

            //Are we remapping something with a primitive return that gets ignored?
            if(bsm.getName().equals("metafactory")) {
                Handle implMethod = (Handle) bsmArgs[1];
                if(!Instrumenter.isIgnoredClass(implMethod.getOwner()) && !Instrumenter.isIgnoredMethod(implMethod.getOwner(), implMethod.getName(), implMethod.getDesc()) && !TaintUtils.remapMethodDescAndIncludeReturnHolder(implMethod.getDesc()).equals(implMethod.getDesc())) {
                    Type uninstSamMethodType = (Type) bsmArgs[0];

                    bsmArgs[0] = Type.getMethodType(TaintUtils.remapMethodDescAndIncludeReturnHolder(((Type) bsmArgs[0]).getDescriptor()));
                    //make sure that we can directly make this call without needing to add additional parameters
                    String implMethodDesc = implMethod.getDesc();

                    Type samMethodType = (Type) bsmArgs[0];

                    String remappedImplDesc = TaintUtils.remapMethodDescAndIncludeReturnHolder(implMethodDesc);

                    Type instantiatedType = (Type) bsmArgs[2];
                    Type[] instantiatedMethodArgs = instantiatedType.getArgumentTypes();
                    Type[] samMethodArgs = samMethodType.getArgumentTypes();


                    String remappedInstantiatedDesc;
                    if(implMethod.getName().equals("<init>")) {
                        remappedInstantiatedDesc = TaintUtils.remapMethodDescAndIncludeReturnHolderInit(instantiatedType.getDescriptor());
                    } else {
                        remappedInstantiatedDesc = TaintUtils.remapMethodDescAndIncludeReturnHolder(instantiatedType.getDescriptor());
                    }
                    boolean needToAddUnWrapper = samMethodType.getArgumentTypes().length != Type.getMethodType(remappedInstantiatedDesc).getArgumentTypes().length;

                    boolean isNEW = (implMethod.getTag() == Opcodes.H_NEWINVOKESPECIAL);
                    boolean isVirtual = (implMethod.getTag() == Opcodes.H_INVOKEVIRTUAL) || implMethod.getTag() == Opcodes.H_INVOKESPECIAL || implMethod.getTag() == Opcodes.H_INVOKEINTERFACE;
                    int nOrigArgs = Type.getArgumentTypes(desc).length;
                    if(isVirtual) {
                        nOrigArgs--;
                    }

                    //OR if there are primitives in the impl but not in the sam!
                    int nPrimsImpl = 0;
                    int nPrimsSAM = 0;
                    for(Type t : uninstSamMethodType.getArgumentTypes()) {
                        if(TaintUtils.isPrimitiveType(t)) {
                            nPrimsSAM++;
                        }
                    }

                    int nArgsSAM = nOrigArgs;
                    for(Type t : Type.getArgumentTypes(implMethodDesc)) {
                        nArgsSAM--;
                        if(nArgsSAM >= 0) {
                            continue;
                        }
                        if(TaintUtils.isPrimitiveType(t)) {
                            nPrimsImpl++;
                        }
                    }
                    if(nPrimsImpl != nPrimsSAM) {
                        needToAddUnWrapper = true;
                    }

                    //OR If there is a wrapped return type on the impl but not in the sam
                    if(TaintUtils.isPrimitiveType(Type.getReturnType(implMethodDesc)) && !TaintUtils.isPrimitiveType(uninstSamMethodType.getReturnType())) {
                        needToAddUnWrapper = true;
                    }

                    if(needToAddUnWrapper) {

                        ArrayList<Type> newWrapperArgs = new ArrayList<>();
                        Type wrapperDesc = Type.getMethodType(implMethod.getDesc());
                        Type[] implMethodArgs = Type.getArgumentTypes(implMethod.getDesc());
                        int extraArgs = Type.getArgumentTypes(implMethodDesc).length - instantiatedType.getArgumentTypes().length;

                        if(isVirtual) {
                            newWrapperArgs.add(Type.getObjectType(implMethod.getOwner()));
                        }

                        for(int j = 0; j < implMethodArgs.length; j++) {
                            if(j < extraArgs) {
                                //Add as-is
                                newWrapperArgs.add(implMethodArgs[j]);
                            } else if(TaintUtils.isPrimitiveOrPrimitiveArrayType(implMethodArgs[j]) &&
                                    samMethodArgs[j - extraArgs].getDescriptor().equals("Ljava/lang/Object;")) {
                                newWrapperArgs.add(samMethodArgs[j - extraArgs]);
                            } else {
                                newWrapperArgs.add(implMethodArgs[j]);
                            }
                        }

                        boolean boxPrimitiveReturn = false;
                        boolean unboxPrimitiveReturn = false;
                        //Are we returning a primitive that needs to be autoboxed?
                        Type newReturnType = TaintUtils.getContainerReturnType(wrapperDesc.getReturnType());
                        Type originalReturnType = wrapperDesc.getReturnType();
                        boolean addReturnWrapper = false;
                        if(TaintUtils.isPrimitiveType(wrapperDesc.getReturnType())) {
                            if(TaintUtils.isPrimitiveType(uninstSamMethodType.getReturnType())) {
                                addReturnWrapper = true;
                                newWrapperArgs.add(newReturnType);
                            } else {
                                boxPrimitiveReturn = true;
                                switch(wrapperDesc.getReturnType().getSort()) {
                                    case Type.CHAR:
                                        newReturnType = Type.getType("Ljava/lang/Character;");
                                        break;
                                    case Type.BOOLEAN:
                                        newReturnType = Type.getType("Ljava/lang/Boolean;");
                                        break;
                                    case Type.DOUBLE:
                                        newReturnType = Type.getType("Ljava/lang/Double;");
                                        break;
                                    case Type.FLOAT:
                                        newReturnType = Type.getType("Ljava/lang/Float;");
                                        break;
                                    case Type.LONG:
                                        newReturnType = Type.getType("Ljava/lang/Long;");
                                        break;
                                    case Type.INT:
                                        newReturnType = Type.getType("Ljava/lang/Integer;");
                                        break;
                                    case Type.SHORT:
                                        newReturnType = Type.getType("Ljava/lang/Short;");
                                        break;
                                    case Type.BYTE:
                                        newReturnType = Type.getType("Ljava/lang/Byte;");
                                }
                            }
                        } else if(isNEW) {
                            newReturnType = Type.getObjectType(implMethod.getOwner());
                        } else if(TaintUtils.isPrimitiveType(uninstSamMethodType.getReturnType()) && !TaintUtils.isPrimitiveType(wrapperDesc.getReturnType())) {
                            //IMPL method returns boxed type, SAM returns primitive
//					        newWrapperArgs.add(TaintUtils.getContainerReturnType(uninstSamMethodType.getReturnType()));
                            newReturnType = uninstSamMethodType.getReturnType();
                            unboxPrimitiveReturn = true;
                        }
                        wrapperDesc = Type.getMethodType(newReturnType, newWrapperArgs.toArray(new Type[0]));

                        String wrapperName = "phosphorWrapInvokeDymnamic" + wrapperMethodsToAdd.size();

                        MethodNode mn = new MethodNode(Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, wrapperName, wrapperDesc.getDescriptor(), null, null);

                        GeneratorAdapter ga = new GeneratorAdapter(mn, Opcodes.ACC_STATIC, wrapperName, wrapperDesc.getDescriptor());
                        ga.visitCode();

                        if(isNEW) {
                            ga.visitTypeInsn(Opcodes.NEW, implMethod.getOwner());
                            ga.visitInsn(DUP);
                        }
                        int offset = 0;
                        if(isVirtual) {
                            ga.visitVarInsn(ALOAD, 0);
                            offset = 1;
                            newWrapperArgs.remove(0);
                        }
                        if(addReturnWrapper) {
                            newWrapperArgs.remove(newWrapperArgs.size() - 1);
                        }
                        for(int j = 0; j < newWrapperArgs.size(); j++) {
                            ga.visitVarInsn(newWrapperArgs.get(j).getOpcode(Opcodes.ILOAD), offset);
                            offset += newWrapperArgs.get(j).getSize();
                            if(!implMethodArgs[j].getDescriptor().equals(newWrapperArgs.get(j).getDescriptor())) {
                                if(implMethodArgs[j].getSort() == Type.ARRAY) {
                                    ga.visitTypeInsn(Opcodes.CHECKCAST, implMethodArgs[j].getInternalName());
                                } else {
                                    //Cast to box type, then unbox
                                    ga.unbox(implMethodArgs[j]);
                                }
                            }
                        }
                        int opToCall;
                        boolean isInterface = false;
                        switch(implMethod.getTag()) {
                            case H_INVOKESTATIC:
                                opToCall = INVOKESTATIC;
                                break;
                            case H_INVOKEVIRTUAL:
                                opToCall = INVOKEVIRTUAL;
                                break;
                            case H_INVOKESPECIAL:
                            case H_NEWINVOKESPECIAL:
                                opToCall = INVOKESPECIAL;
                                break;
                            case H_INVOKEINTERFACE:
                                isInterface = true;
                                opToCall = INVOKEINTERFACE;
                                break;
                            default:
                                throw new UnsupportedOperationException();
                        }
                        if(!boxPrimitiveReturn && !unboxPrimitiveReturn) {
                            ga.visitInsn(TaintUtils.NO_TAINT_STORE_INSN);
                        }
                        ga.visitMethodInsn(opToCall, implMethod.getOwner(), implMethod.getName(), implMethod.getDesc(), isInterface);

                        if(boxPrimitiveReturn) {
                            ga.box(originalReturnType);
                        }
                        if(unboxPrimitiveReturn) {
                            ga.unbox(uninstSamMethodType.getReturnType());
                        }
                        ga.returnValue();
                        ga.visitMaxs(0, 0);
                        ga.visitEnd();
                        mn.maxLocals = 100;

                        wrapperMethodsToAdd.add(mn);

                        //Change the bsmArgs to point to the new wrapper (which may or may not get the suffix)

                        String taintedWrapperDesc = TaintUtils.remapMethodDescAndIncludeReturnHolder(wrapperDesc.getDescriptor());

                        String targetName = wrapperName;
                        if(!taintedWrapperDesc.equals(wrapperDesc.getDescriptor())) {
                            targetName = targetName + TaintUtils.METHOD_SUFFIX;
                        }
                        bsmArgs[1] = new Handle(Opcodes.H_INVOKESTATIC, className, targetName, taintedWrapperDesc, false);

                        //build the new instantiated desc
                        for(int j = 0; j < instantiatedMethodArgs.length; j++) {
                            if(TaintUtils.isPrimitiveArrayType(instantiatedMethodArgs[j])) {
                                instantiatedMethodArgs[j] = MultiDTaintedArray.getTypeForType(instantiatedMethodArgs[j]);
                            }
                        }

                        Type instantiatedReturnType;
                        if(isNEW || unboxPrimitiveReturn || boxPrimitiveReturn) {
                            instantiatedReturnType = newReturnType;
                        } else {
                            instantiatedReturnType = originalReturnType;
                        }
                        if(samMethodType.getReturnType().getSort() == Type.VOID) {
                            instantiatedReturnType = Type.VOID_TYPE;
                        }
                        bsmArgs[2] = Type.getMethodType(TaintUtils.remapMethodDescAndIncludeReturnHolder(Type.getMethodType(instantiatedReturnType, instantiatedMethodArgs).getDescriptor()));

                    } else {
                        bsmArgs[1] = new Handle(implMethod.getTag(), implMethod.getOwner(), implMethod.getName() + (implMethod.getName().equals("<init>") ? "" : TaintUtils.METHOD_SUFFIX), remappedImplDesc, implMethod.isInterface());
                        bsmArgs[2] = Type.getMethodType(TaintUtils.remapMethodDescAndIncludeReturnHolder(instantiatedType.getDescriptor()));
                    }

                }
            } else {
                if(bsmArgs.length > 1 && bsmArgs[1] instanceof Handle) {
                    Type t = Type.getMethodType(((Handle) bsmArgs[1]).getDesc());
                    if(TaintUtils.isPrimitiveType(t.getReturnType())) {
                        Type _t = (Type) bsmArgs[0];
                        if(_t.getReturnType().getSort() == Type.VOID) {
                            //Manually add the return type here;
                            StringBuilder nd = new StringBuilder();
                            nd.append('(');
                            for(Type a : _t.getArgumentTypes()) {
                                nd.append(a.getDescriptor());
                            }
                            nd.append(TaintUtils.getContainerReturnType(t.getReturnType()));
                            nd.append(")V");
                            bsmArgs[0] = Type.getMethodType(nd.toString());
                        }
                        _t = (Type) bsmArgs[2];
                        if(_t.getReturnType().getSort() == Type.VOID) {
                            //Manually add the return type here;
                            StringBuilder nd = new StringBuilder();
                            nd.append('(');
                            for(Type a : _t.getArgumentTypes()) {
                                nd.append(a.getDescriptor());
                            }
                            nd.append(TaintUtils.getContainerReturnType(t.getReturnType()));
                            nd.append(")V");
                            bsmArgs[2] = Type.getMethodType(nd.toString());
                        }
                    }
                }
                for(int k = 0; k < bsmArgs.length; k++) {
                    Object o = bsmArgs[k];
                    if(o instanceof Handle) {
                        String nameH = ((Handle) o).getName();
                        if(!Instrumenter.isIgnoredClass(((Handle) o).getOwner()) && !Instrumenter.isIgnoredMethod(((Handle) o).getOwner(), nameH, ((Handle) o).getDesc()) &&
                                !TaintUtils.remapMethodDescAndIncludeReturnHolder(((Handle) o).getDesc()).equals(((Handle) o).getDesc())) {
                            bsmArgs[k] = new Handle(((Handle) o).getTag(), ((Handle) o).getOwner(), nameH + (nameH.equals("<init>") ? "" : TaintUtils.METHOD_SUFFIX), TaintUtils.remapMethodDescAndIncludeReturnHolder(((Handle) o).getDesc()));
                        }
                    } else if(o instanceof Type) {
                        Type t = (Type) o;
                        bsmArgs[k] = Type.getMethodType(TaintUtils.remapMethodDescAndIncludeReturnHolder(t.getDescriptor()));
                    }
                }
            }
        }
        if(hasNewName && !Instrumenter.isIgnoredClass(bsm.getOwner())) {
            if(!Instrumenter.isIgnoredMethod(bsm.getOwner(), bsm.getName(), bsm.getDesc()) && !TaintUtils.remapMethodDescAndIncludeReturnHolder(bsm.getDesc()).equals(bsm.getDesc())) {
                bsm = new Handle(bsm.getTag(), bsm.getOwner(), bsm.getName() + TaintUtils.METHOD_SUFFIX, TaintUtils.remapMethodDescAndIncludeReturnHolder(bsm.getDesc()), bsm.isInterface());
            }
        }
        super.visitInvokeDynamicInsn(name, newDesc, bsm, bsmArgs);
        //      System.out.println("asdfjas;dfdsf  post invoke " + analyzer.stack);
//      System.out.println("Now: " + analyzer.stack);
        if(isCalledOnAPrimitiveArrayType) {
            if(TaintUtils.DEBUG_CALLS) {
                System.out.println("Post invoke stack: " + analyzer.stack);
            }
            if(Type.getReturnType(desc).getSort() == Type.VOID) {
                super.visitInsn(POP);
            } else if(analyzer.stack.size() >= 2) {// && analyzer.stack.get(analyzer.stack.size() - 2).equals("[I")) {
                //this is so dumb that it's an array type.
                super.visitInsn(SWAP);
                super.visitInsn(POP);
            }

        }
//      System.out.println("after: " + analyzer.stack);

        if(dontUnboxTaints) {
            dontUnboxTaints = false;
            return;
        }
        String taintType = TaintUtils.getShadowTaintType(Type.getReturnType(desc).getDescriptor());
        if(taintType != null) {
            super.visitInsn(DUP);
            super.visitFieldInsn(GETFIELD, returnType.getInternalName(), "taint", taintType);
            super.visitInsn(SWAP);
            super.visitFieldInsn(GETFIELD, returnType.getInternalName(), "val", origReturnType.getDescriptor());
            if(nextLoadIsTracked) {
                throw new UnsupportedOperationException();
            }
        }
        if(TaintUtils.DEBUG_CALLS) {
            System.out.println("Post invoke stack post swap pop maybe: " + analyzer.stack);
        }
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itfc) {
        if(isIgnoreAllInstrumenting || isRawInsns) {
            super.visitMethodInsn(opcode, owner, name, desc, itfc);
            return;
        }
        //Stupid workaround for eclipse benchmark
        if(name.equals("getProperty") && className.equals("org/eclipse/jdt/core/tests/util/Util")) {
            owner = Type.getInternalName(ReflectionMasker.class);
            System.out.println("Fixing getproperty");
            name = "getPropertyHideBootClasspath";
        }

        if(opcode == INVOKESTATIC && isBoxUnboxMethodToWrap(owner, name, desc)) {
            if(name.equals("valueOf") && desc.startsWith("(Ljava/lang/String;")) {
                switch(owner) {
                    case BYTE_NAME:
                        name = "valueOfB";
                        break;
                    case BOOLEAN_NAME:
                        name = "valueOfZ";
                        break;
                    case CHARACTER_NAME:
                        name = "valueOfC";
                        break;
                    case SHORT_NAME:
                        name = "valueOfS";
                        break;
                    default:
                        throw new UnsupportedOperationException(owner);
                }
            }
            owner = "edu/columbia/cs/psl/phosphor/runtime/RuntimeBoxUnboxPropogator";
        }
        boolean isPreAllocatedReturnType = TaintUtils.isPreAllocReturnType(desc);
        if(Instrumenter.isClassWithHashmapTag(owner) && name.equals("valueOf")) {
            Type[] args = Type.getArgumentTypes(desc);
            if(args[0].getSort() != Type.OBJECT) {
                if(!Configuration.MULTI_TAINTING) {
                    owner = Type.getInternalName(BoxedPrimitiveStoreWithIntTags.class);
                    desc = "(I" + desc.substring(1);
                } else {
                    owner = Type.getInternalName(BoxedPrimitiveStoreWithObjTags.class);
                    desc = "(" + Configuration.TAINT_TAG_DESC + desc.substring(1);
                }
                super.visitMethodInsn(Opcodes.INVOKESTATIC, owner, name, desc, false);
                return;
            }
        } else if(Instrumenter.isClassWithHashmapTag(owner) && (name.equals("byteValue") || name.equals("booleanValue") || name.equals("charValue") || name.equals("shortValue"))) {
            Type returnType = Type.getReturnType(desc);
            Type boxedReturn = TaintUtils.getContainerReturnType(returnType);
            desc = "(L" + owner + ";)" + boxedReturn.getDescriptor();
            if(!Configuration.MULTI_TAINTING) {
                owner = Type.getInternalName(BoxedPrimitiveStoreWithIntTags.class);
            } else {
                owner = Type.getInternalName(BoxedPrimitiveStoreWithObjTags.class);
            }
            super.visitMethodInsn(Opcodes.INVOKESTATIC, owner, name, desc, false);
            if(nextLoadIsTracked) {
                super.visitInsn(DUP);
                getTaintFieldOfBoxedType(boxedReturn.getInternalName());
                super.visitInsn(SWAP);
            }
            super.visitFieldInsn(GETFIELD, boxedReturn.getInternalName(), "val", returnType.getDescriptor());
            if(nextLoadIsTracked) {
                analyzer.setTopOfStackTagged();
                nextLoadIsTracked = false;
            }
            return;
        }

        Type ownerType = Type.getObjectType(owner);
        if(opcode == INVOKEVIRTUAL && ownerType.getSort() == Type.ARRAY && ownerType.getElementType().getSort() != Type.OBJECT && ownerType.getDimensions() > 1) {
            //			System.out.println("got to change the owner on primitive array call from " +owner+" to " + MultiDTaintedArray.getTypeForType(ownerType));
            owner = MultiDTaintedArray.getTypeForType(ownerType).getInternalName();
        }
        boolean isCallToPrimitiveArrayClone = opcode == INVOKEVIRTUAL && desc.equals("()Ljava/lang/Object;") && name.equals("clone") && getTopOfStackType().getSort() == Type.ARRAY
                && getTopOfStackType().getElementType().getSort() != Type.OBJECT;
        //When you call primitive array clone, we should first clone the taint array, then register that taint array to the cloned object after calling clone
        Type primitiveArrayType = null;
        if(isCallToPrimitiveArrayClone) {
            registerTaintedArray();
            primitiveArrayType = getTopOfStackType();
            Configuration.taintTagFactory.methodOp(opcode, primitiveArrayType.getInternalName(), "clone", "()Ljava/lang/Object", false, mv, lvs, this);
            super.visitMethodInsn(opcode, primitiveArrayType.getInternalName(), "clone", "()Ljava/lang/Object;", false);
            return;
        }
        if((owner.equals("java/lang/System") || owner.equals("java/lang/VMSystem") || owner.equals("java/lang/VMMemoryManager")) && name.equals("arraycopy")
                && !desc.equals("(Ljava/lang/Object;ILjava/lang/Object;IILjava/lang/DCompMarker;)V")) {
            if(Instrumenter.IS_KAFFE_INST) {
                name = "arraycopyVM";
            } else if(Instrumenter.IS_HARMONY_INST) {
                name = "arraycopyHarmony";
            }
            owner = Type.getInternalName(TaintUtils.class);
            //We have several scenarios here. src/dest may or may not have shadow arrays on the stack
            boolean destIsPrimitive;
            Type destType = getStackTypeAtOffset(4);
//			System.out.println(analyzer.stack);
            destIsPrimitive = !stackElIsNull(4) && destType.getSort() != Type.OBJECT && destType.getElementType().getSort() != Type.OBJECT;
            int srcOffset = 7;
            if(destIsPrimitive) {
                srcOffset++;
            }
            Type srcType = getStackTypeAtOffset(srcOffset);
            boolean srcIsPrimitive = srcType.getSort() != Type.OBJECT && srcType.getElementType().getSort() != Type.OBJECT && !stackElIsNull(srcOffset);
            if(!Configuration.MULTI_TAINTING) {
                if(srcIsPrimitive) {
                    if(destIsPrimitive) {
                        desc = "(Ljava/lang/Object;Ljava/lang/Object;IILjava/lang/Object;Ljava/lang/Object;IIII)V";
                        if(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING) {
                            name = "arraycopyControlTrack";
                        }
                    } else {
                        desc = "(Ljava/lang/Object;Ljava/lang/Object;IILjava/lang/Object;IIII)V";
                    }
                } else {
                    if(destIsPrimitive) {
                        desc = "(Ljava/lang/Object;IILjava/lang/Object;Ljava/lang/Object;IIII)V";
                    } else {
                        desc = "(Ljava/lang/Object;IILjava/lang/Object;IIII)V";

                    }
                }
            } else {
                if(srcIsPrimitive) {
                    if(destIsPrimitive) {
                        desc = "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;ILjava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;ILjava/lang/Object;I)V";
                        if(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING) {
                            name = "arraycopyControlTrack";
                        }
                    } else {
                        desc = "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;ILjava/lang/Object;Ljava/lang/Object;ILjava/lang/Object;I)V";
                    }
                } else {
                    if(destIsPrimitive) {
                        desc = "(Ljava/lang/Object;Ljava/lang/Object;ILjava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;ILjava/lang/Object;I)V";
                    } else {
                        desc = "(Ljava/lang/Object;Ljava/lang/Object;ILjava/lang/Object;Ljava/lang/Object;ILjava/lang/Object;I)V";
                    }
                }
                if(Configuration.IMPLICIT_TRACKING) {
                    desc = desc.substring(0, desc.length() - 2) + Type.getDescriptor(ControlTaintTagStack.class) + ")V";
                    super.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
                }
            }
        }
        if(owner.startsWith("edu/columbia/cs/psl/phosphor") && !name.equals("printConstraints") && !name.equals("hasNoDependencies") && !desc.equals("(I)V") && !owner.endsWith("Tainter") && !owner.endsWith("CharacterUtils")
                && !name.equals("getPHOSPHOR_TAG") && !name.equals("setPHOSPHOR_TAG") && !owner.equals("edu/columbia/cs/psl/phosphor/runtime/RuntimeBoxUnboxPropogator")
                && !owner.equals(Configuration.TAINT_TAG_INTERNAL_NAME)
        ) {
            Configuration.taintTagFactory.methodOp(opcode, owner, name, desc, itfc, mv, lvs, this);
            super.visitMethodInsn(opcode, owner, name, desc, itfc);
            return;
        }
        if(Configuration.IMPLICIT_TRACKING && opcode == Opcodes.INVOKEVIRTUAL && owner.equals("java/lang/Object") && (name.equals("equals") || name.equals("hashCode"))) {
            Type callee = getTopOfStackType();
            if(name.equals("equals")) {
                callee = getStackTypeAtOffset(1);
            }

            if(callee.getSort() == Type.OBJECT) {
                String calledOn = callee.getInternalName();

                try {
                    Class in = Class.forName(calledOn.replace('/', '.'), false, TaintPassingMV.class.getClassLoader());
                    if(!in.isInterface() && !Instrumenter.isIgnoredClass(calledOn)) {
                        owner = calledOn;
                    }
                } catch(Throwable t) {
                    //if not ignored, can still make an invokeinterface
                    if(!Instrumenter.isIgnoredClass(calledOn)) {
                        owner = Type.getInternalName(TaintedWithObjTag.class);
                        opcode = INVOKEINTERFACE;
                        itfc = true;
//						t.printStackTrace();
                    }

                }

            }
        }
        if(opcode == INVOKEVIRTUAL && Configuration.WITH_HEAVY_OBJ_EQUALS_HASHCODE && (name.equals("equals") || name.equals("hashCode")) && owner.equals("java/lang/Object")) {
            opcode = INVOKESTATIC;
            owner = Type.getInternalName(NativeHelper.class);
            if(name.equals("equals")) {
                desc = "(Ljava/lang/Object;Ljava/lang/Object;)Z";
            } else {
                desc = "(Ljava/lang/Object;)I";
            }
        }
        //to reduce how much we need to wrap, we will only rename methods that actually have a different descriptor
        boolean hasNewName = !TaintUtils.remapMethodDesc(desc).equals(desc);
        if(isCallToPrimitiveArrayClone) {
            hasNewName = false;
        }
        boolean isIgnoredForTaints = Configuration.WITH_SELECTIVE_INST &&
                Instrumenter.isIgnoredMethodFromOurAnalysis(owner, name, desc);
        if((Instrumenter.isIgnoredClass(owner) || isIgnoredForTaints || Instrumenter.isIgnoredMethod(owner, name, desc)) && !isInternalTaintingClass(owner)) {
            Type[] args = Type.getArgumentTypes(desc);
            if(TaintUtils.DEBUG_CALLS) {
                System.out.println("Calling non-inst: " + owner + "." + name + desc + " stack " + analyzer.stack);
            }
            int argsSize = 0;
            for(int i = 0; i < args.length; i++) {
                argsSize += args[args.length - i - 1].getSize();
                if(TaintUtils.DEBUG_CALLS) {
                    System.out.println(i + ", " + analyzer.stack.get(analyzer.stack.size() - argsSize) + " " + args[args.length - i - 1]);
                }
                if(args[args.length - i - 1].getSort() == Type.ARRAY && args[args.length - i - 1].getElementType().getSort() != Type.OBJECT && args[args.length - i - 1].getDimensions() > 1) {
                    if(!isIgnoredForTaints) {
                        ensureUnBoxedAt(i, args[args.length - i - 1]);
                    }
//					unboxTaintArrayAt(i+1, args[args.length - i - 1].getDescriptor());
                } else if(isPrimitiveType(args[args.length - i - 1])
                        || (args[args.length - i - 1].equals(Type.getType(Object.class)) && isPrimitiveStackType(analyzer.stack.get(analyzer.stack.size() - argsSize)))) {
                    if(!isPrimitiveType(args[args.length - i - 1]) || analyzer.stack.get(analyzer.stack.size() - argsSize) != Opcodes.NULL) {
                        popAt(i + 1);
                    }
                }
            }
//			System.out.println("After modifying, Calling non-inst: " + owner + "." + name + desc + " stack " + analyzer.stack);

            boolean isCalledOnAPrimitiveArrayType = false;
            if(opcode == INVOKEVIRTUAL) {
                Type callee = getTypeForStackType(analyzer.stack.get(analyzer.stack.size() - argsSize - 1));
                if(TaintUtils.DEBUG_CALLS) {
                    System.out.println("CALLEE IS " + callee);
                }
                if(callee.getSort() == Type.ARRAY && callee.getElementType().getSort() != Type.OBJECT) {
                    isCalledOnAPrimitiveArrayType = true;
                }
            }

            if(isIgnoredForTaints && !owner.startsWith("[") && !Instrumenter.isIgnoredClass(owner)) {
                if(name.equals("<init>")) {
                    super.visitInsn(Opcodes.ACONST_NULL);
                    desc = desc.substring(0, desc.indexOf(')')) + Type.getDescriptor(UninstrumentedTaintSentinel.class) + ")" + desc.substring(desc.indexOf(')') + 1);
                } else {
                    name += TaintUtils.METHOD_SUFFIX_UNINST;
                }
            }
            Configuration.taintTagFactory.methodOp(opcode, owner, name, TaintUtils.remapMethodDescForUninst(desc), itfc, mv, lvs, this);

            super.visitMethodInsn(opcode, owner, name, TaintUtils.remapMethodDescForUninst(desc), itfc);
            if(isCallToPrimitiveArrayClone) {
                //Now we have cloned (but not casted) array, and a cloned (but not casted) taint array
                //TA A
                super.visitTypeInsn(CHECKCAST, primitiveArrayType.getInternalName());
                registerTaintedArray();
            } else if(isCalledOnAPrimitiveArrayType) {
                if(TaintUtils.DEBUG_CALLS) {
                    System.out.println("Post invoke stack: " + analyzer.stack);
                }
                if(Type.getReturnType(desc).getSort() == Type.VOID) {
                    super.visitInsn(POP);
                } else if(analyzer.stack.size() >= 2) {// && analyzer.stack.get(analyzer.stack.size() - 2).equals("[I")) {
                    //this is so dumb that it's an array type.
                    super.visitInsn(SWAP);
                    super.visitInsn(POP); //This is the case that we are calling a method on a primitive array type so need to pop the taint
                }
            }

            Type returnType = Type.getReturnType(desc);
            if(dontUnboxTaints && isIgnoredForTaints) {
                dontUnboxTaints = false;
                if(returnType.getSize() == 2) {
                    super.visitInsn(POP2);
                    super.visitInsn(ACONST_NULL);
                    return;
                } else {
                    super.visitInsn(POP);
                    super.visitInsn(ACONST_NULL);
                    return;
                }
            }
            if(isPrimitiveType(returnType)) {
                if(!nextLoadIsTracked) {
                    return;
                }
                nextLoadIsTracked = false;
                if(returnType.getSort() == Type.ARRAY) {
                    generateEmptyTaintArray(returnType.getDescriptor());
                } else if(returnType.getSize() == 2) {
                    generateUnconstrainedTaint(0);
                    super.visitInsn(DUP_X2);
                    super.visitInsn(POP);
                } else {
                    generateUnconstrainedTaint(0);
                    super.visitInsn(SWAP);
                }
                analyzer.setTopOfStackTagged();
            } else if(returnType.getDescriptor().endsWith("Ljava/lang/Object;")) {
                visit(BOX_IF_NECESSARY);
                super.visitTypeInsn(Opcodes.CHECKCAST, returnType.getInternalName());
            }
            if(TaintUtils.DEBUG_CALLS) {
                System.out.println("Post invoke stack post swap pop maybe: " + analyzer.stack);
            }
            return;
        }
        String newDesc = TaintUtils.remapMethodDesc(desc);
        if((Configuration.IMPLICIT_HEADERS_NO_TRACKING || Configuration.IMPLICIT_TRACKING)) {
            if((isInternalTaintingClass(owner) || owner.startsWith("[")) && !name.equals("getControlFlow") && !name.startsWith("hashCode") && !name.startsWith("equals")) {
                newDesc = newDesc.replace(Type.getDescriptor(ControlTaintTagStack.class), "");
            } else {
                super.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
            }
            if(owner.startsWith("[")) {
                hasNewName = false;
            }
        }
        if(name.equals("<init>") && !newDesc.equals(desc)) {
            //Add the taint sentinel to the desc
            super.visitInsn(ACONST_NULL);
            newDesc = newDesc.substring(0, newDesc.indexOf(")")) + Type.getDescriptor(TaintSentinel.class) + ")" + Type.getReturnType(newDesc).getDescriptor();
        }
        if(isPreAllocatedReturnType) {
            Type t = Type.getReturnType(newDesc);
            newDesc = newDesc.substring(0, newDesc.indexOf(")")) + t.getDescriptor() + ")" + t.getDescriptor();
            super.visitVarInsn(ALOAD, lvs.getPreAllocedReturnTypeVar(t));
        }
        Type origReturnType = Type.getReturnType(desc);
        Type returnType = TaintUtils.getContainerReturnType(Type.getReturnType(desc));
        if(TaintUtils.DEBUG_CALLS) {
            System.out.println("Remapped call from " + owner + "." + name + desc + " to " + owner + "." + name + newDesc);
        }
        if(!name.contains("<") && hasNewName) {
            name += TaintUtils.METHOD_SUFFIX;
        }
        if(TaintUtils.DEBUG_CALLS) {
            System.out.println("Calling w/ stack: " + analyzer.stack);
        }

        //if you call a method and instead of passing a primitive array you pass ACONST_NULL, we need to insert another ACONST_NULL in the stack
        //for the taint for that array
        Type[] args = Type.getArgumentTypes(newDesc);
        Type[] argsInReverse = new Type[args.length];
        int argsSize = 0;
        for(int i = 0; i < args.length; i++) {
            argsInReverse[args.length - i - 1] = args[i];
            argsSize += args[i].getSize();
        }
        int i = 1;
        int n = 1;
        boolean ignoreNext = false;

        for(Type t : argsInReverse) {
            if(analyzer.stack.get(analyzer.stack.size() - i) == Opcodes.TOP) {
                i++;
            }
            Type onStack = getTypeForStackType(analyzer.stack.get(analyzer.stack.size() - i));
            if(!ignoreNext && t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT) {
                if(analyzer.stack.get(analyzer.stack.size() - i) != Opcodes.NULL) {
                    if(onStack.getSort() == Type.OBJECT && (!isIgnoredForTaints || t.getDimensions() == 1)) {
                        //Unbox this
                        unboxTaintArrayAt(n, t.getDescriptor());
                    }
                }
            } else if(!ignoreNext && onStack.getSort() == Type.ARRAY && onStack.getElementType().getSort() != Type.OBJECT) {
                //There is an extra taint on the stack at this position
                if(TaintUtils.DEBUG_CALLS) {
                    System.err.println("removing taint array in call at " + n);
                }
                storeTaintArrayAt(n);
            }
            if((t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT) || (t.getDescriptor().startsWith("Ledu/columbia/cs/psl/phosphor/struct/Lazy"))) {
                ignoreNext = !ignoreNext;
            }
            n++;
            i++;
        }
        boolean isCalledOnAPrimitiveArrayType = false;
        if(opcode == INVOKEVIRTUAL) {
            if(analyzer.stack.get(analyzer.stack.size() - argsSize - 1) == null) {
                System.out.println("NULL on stack for calllee???" + analyzer.stack + " argsize " + argsSize);
            }
            Type callee = getTypeForStackType(analyzer.stack.get(analyzer.stack.size() - argsSize - 1));
            if(TaintUtils.DEBUG_CALLS) {
                System.out.println("CALLEE IS " + callee);
            }
            if(callee.getSort() == Type.ARRAY && callee.getElementType().getSort() != Type.OBJECT) {
                isCalledOnAPrimitiveArrayType = true;
            }
        }
        Configuration.taintTagFactory.methodOp(opcode, owner, name, newDesc, itfc, mv, lvs, this);

        super.visitMethodInsn(opcode, owner, name, newDesc, itfc);

        if(Configuration.IMPLICIT_TRACKING && !arrayAnalyzer.hasFinally && arrayAnalyzer.nTryCatch == 0 && !isSuperUninitialized && this.isSuperUninitialized && opcode == INVOKESPECIAL && name.equals("<init>")) {
            super.visitTryCatchBlock(firstLabel, endLabel, popAllLabel, null);
            super.visitLabel(firstLabel);
            this.isSuperUninitialized = false;
        }
//				System.out.println("asdfjas;dfdsf  post invoke " + analyzer.stack);
//		System.out.println("Now: " + analyzer.stack);
        if(isCallToPrimitiveArrayClone) {
            //Now we have cloned (but not casted) array, and a clopned( but not casted) taint array
            //TA A
            //			System.out.println(owner + name + newDesc + ": " + analyzer.stack);
            super.visitTypeInsn(CHECKCAST, primitiveArrayType.getInternalName());
            //			System.out.println(owner + name + newDesc + ": " + analyzer.stack);
            registerTaintedArray();
        } else if(isCalledOnAPrimitiveArrayType) {
            if(TaintUtils.DEBUG_CALLS) {
                System.out.println("Post invoke stack: " + analyzer.stack);
            }
            if(Type.getReturnType(desc).getSort() == Type.VOID) {
                super.visitInsn(POP);
            } else if(analyzer.stack.size() >= 2) {
                //this is so dumb that it's an array type.
                super.visitInsn(SWAP);
                super.visitInsn(POP);
            }

        }
//		System.out.println("after: " + analyzer.stack);

        if(isTaintlessArrayStore) {
            isTaintlessArrayStore = false;
            return;
        }
        if(dontUnboxTaints) {
            dontUnboxTaints = false;
            return;
        }
        String taintType = TaintUtils.getShadowTaintType(Type.getReturnType(desc).getDescriptor());
        if(taintType != null) {
            if(nextLoadIsTracked) {
                FrameNode fn = getCurrentFrameNode();
                fn.type = Opcodes.F_NEW;
                super.visitInsn(DUP);
                String taintTypeRaw = Configuration.TAINT_TAG_DESC;
                if(Type.getReturnType(desc).getSort() == Type.ARRAY) {
                    nextLoadIsTracked = false;
                    Label ok = new Label();
                    Label isnull = new Label();

                    super.visitJumpInsn(IFNULL, isnull);
                    super.visitInsn(DUP);
                    super.visitFieldInsn(GETFIELD, returnType.getInternalName(), "val", origReturnType.getDescriptor());
                    FrameNode fn2 = getCurrentFrameNode();
                    fn2.type = Opcodes.F_NEW;
                    super.visitJumpInsn(GOTO, ok);
                    super.visitLabel(isnull);
                    fn.accept(this);
                    super.visitInsn(ACONST_NULL);
                    super.visitTypeInsn(CHECKCAST, origReturnType.getInternalName());
                    super.visitLabel(ok);
                    fn2.accept(this);
                    analyzer.setTopOfStackTagged();
                } else {
                    super.visitFieldInsn(GETFIELD, returnType.getInternalName(), "taint", taintTypeRaw);
                    super.visitInsn(SWAP);
                    nextLoadIsTracked = false;
                    super.visitFieldInsn(GETFIELD, returnType.getInternalName(), "val", origReturnType.getDescriptor());
                    analyzer.setTopOfStackTagged();
                }
            } else {
                if(Type.getReturnType(desc).getSort() == Type.ARRAY) {
                    FrameNode fn = getCurrentFrameNode();
                    fn.type = Opcodes.F_NEW;

                    nextLoadIsTracked = false;
                    Label ok = new Label();
                    Label isnull = new Label();

                    super.visitInsn(DUP);
                    super.visitJumpInsn(IFNULL, isnull);
                    super.visitFieldInsn(GETFIELD, returnType.getInternalName(), "val", origReturnType.getDescriptor());
                    FrameNode fn2 = getCurrentFrameNode();
                    fn2.type = Opcodes.F_NEW;
                    super.visitJumpInsn(GOTO, ok);
                    super.visitLabel(isnull);
                    fn.accept(this);
                    super.visitTypeInsn(CHECKCAST, origReturnType.getInternalName());
                    super.visitLabel(ok);
                    fn2.accept(this);
                } else {
                    super.visitFieldInsn(GETFIELD, returnType.getInternalName(), "val", origReturnType.getDescriptor());
                }
            }

        }
        if(TaintUtils.DEBUG_CALLS) {
            System.out.println("Post invoke stack post swap pop maybe: " + analyzer.stack);
        }
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        if(rewriteLVDebug) {
            Label end = new Label();
            super.visitLabel(end);
        }
        if(Configuration.IMPLICIT_TRACKING && !arrayAnalyzer.hasFinally) {
            super.visitLabel(endLabel);
            super.visitLabel(popAllLabel);

            if(this.isExcludedFromControlTrack) {
                super.visitVarInsn(ALOAD, lvs.idxOfMasterControlLV);
                super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(ControlTaintTagStack.class), "enable", "()V", false);
            }

            int maxLV = lvs.idxOfMasterControlLV;
            if(lvs.idxOfMasterExceptionLV > maxLV) {
                maxLV = lvs.idxOfMasterExceptionLV;
            }
            if(controlTaintArray > maxLV) {
                maxLV = controlTaintArray;
            }

            Object[] baseLvs = new Object[maxLV + 1];
            for(int i = 0; i < baseLvs.length; i++) {
                baseLvs[i] = TOP;
            }
            String ctrl = "edu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack";
            baseLvs[lvs.idxOfMasterControlLV] = ctrl;
            if(lvs.idxOfMasterExceptionLV >= 0) {
                baseLvs[lvs.idxOfMasterExceptionLV] = Type.getInternalName(ExceptionalTaintData.class);
            }
            if(controlTaintArray > 0) {
                baseLvs[controlTaintArray] = "[I";
            }
            super.visitFrame(F_NEW, baseLvs.length, baseLvs, 1, new Object[]{"java/lang/Throwable"});
            if(controlTaintArray >= 0) {
                passThroughMV.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
                passThroughMV.visitVarInsn(ALOAD, controlTaintArray);
                callPopAllControlTaint(passThroughMV);
            }
            super.visitInsn(ATHROW);
        }
        super.visitMaxs(maxStack, maxLocals);
    }

    @Override
    public void visitInsn(int opcode) {
        if(opcode == TaintUtils.CUSTOM_SIGNAL_1 || opcode == TaintUtils.CUSTOM_SIGNAL_2 || opcode == TaintUtils.CUSTOM_SIGNAL_3 || opcode == TaintUtils.LOOP_HEADER) {
            Configuration.taintTagFactory.signalOp(opcode, null);
            super.visitInsn(opcode);
            return;
        }
        if(opcode == TaintUtils.DUP_TAINT_AT_0) {
            nextDupCopiesTaint0 = true;
            return;
        }
        if(opcode == TaintUtils.DUP_TAINT_AT_1) {
            nextDupCopiesTaint1 = true;
            return;
        }
        if(opcode == TaintUtils.DUP_TAINT_AT_2) {
            nextDupCopiesTaint2 = true;
            return;
        }
        if(opcode == TaintUtils.DUP_TAINT_AT_3) {
            nextDupCopiesTaint3 = true;
            return;
        }
        if(opcode == TaintUtils.TRACKED_LOAD) {
            nextLoadIsTracked = true;
            super.visitInsn(opcode);
            return;
        }
        if(this.isExcludedFromControlTrack && ((opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) || opcode == Opcodes.ATHROW)) {
            super.visitVarInsn(ALOAD, lvs.idxOfMasterControlLV);
            super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(ControlTaintTagStack.class), "enable", "()V", false);
        }
        if(isLambda && opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) {
            //Do we need to box?
            Type returnType = Type.getReturnType(this.desc);
            if(returnType.getDescriptor().contains("edu/columbia/cs/psl/phosphor/struct")) {
                String t = null;
                if(returnType.getDescriptor().contains("edu/columbia/cs/psl/phosphor/struct/Tainted")) {
                    t = returnType.getDescriptor().replace("Ledu/columbia/cs/psl/phosphor/struct/Tainted", "");
                    t = t.replace("WithIntTag", "");
                    t = t.replace("WithObjTag", "");
                } else if(returnType.getDescriptor().contains("edu/columbia/cs/psl/phosphor/struct/Lazy")) {
                    t = returnType.getDescriptor().replace("Ledu/columbia/cs/psl/phosphor/struct/Lazy", "");
                    t = t.replace("IntTags", "");
                    t = t.replace("ObjTags", "");
                    t = "[" + t;
                }
                t = t.replace(";", "").replace("Int", "I")
                        .replace("Byte", "B")
                        .replace("Short", "S")
                        .replace("Long", "J")
                        .replace("Boolean", "Z")
                        .replace("Float", "F")
                        .replace("Double", "D");
                //Probably need to box...
                int returnHolder = lastArg - 1;
                if(getTopOfStackType().getSize() == 2) {
                    super.visitVarInsn(ALOAD, returnHolder);
                    super.visitInsn(DUP_X2);
                    super.visitInsn(POP);
                } else {
                    super.visitVarInsn(ALOAD, returnHolder);
                    super.visitInsn(SWAP);
                }

                super.visitFieldInsn(PUTFIELD, returnType.getInternalName(), "val", t);
                super.visitVarInsn(ALOAD, returnHolder);
                super.visitInsn(DUP);
                if(t.startsWith("[")) {
                    super.visitInsn(ACONST_NULL);
                    super.visitFieldInsn(PUTFIELD, returnType.getInternalName(), "taints", "[" + Configuration.TAINT_TAG_DESC);
                } else {

                    super.visitInsn(Configuration.NULL_TAINT_LOAD_OPCODE);
                    super.visitFieldInsn(PUTFIELD, returnType.getInternalName(), "taint", Configuration.TAINT_TAG_DESC);
                }
                super.visitInsn(ARETURN);
            } else {
                super.visitInsn(opcode);
            }
            return;
        }
        if(opcode == TaintUtils.FORCE_CTRL_STORE) {
            if(Configuration.WITHOUT_BRANCH_NOT_TAKEN) {
                return;
            }

            //If there is anything on the stack right now, apply the current marker to it
            if(analyzer.stack.isEmpty() || topOfStackIsNull()) {
                return;
            }
            Type onStack = getTopOfStackType();
            if(onStack.getSort() != Type.OBJECT && onStack.getSort() != Type.ARRAY) {
                if(onStack.getSize() == 1) {
                    super.visitInsn(SWAP);
                    super.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
                    visit(COMBINE_TAGS_STACK);
                    super.visitInsn(SWAP);
                } else {
                    super.visitInsn(DUP2_X1);
                    super.visitInsn(POP2);
                    super.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
                    visit(COMBINE_TAGS_STACK);
                    super.visitInsn(DUP_X2);
                    super.visitInsn(POP);
                }
            } else {
                super.visitInsn(DUP);
                super.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
                visit(COMBINE_TAGS_ON_OBJECT);
            }
            return;
        }
        if(opcode == TaintUtils.RAW_INSN) {
            isRawInsns = !isRawInsns;
            return;
        }
        if(opcode == TaintUtils.IGNORE_EVERYTHING) {
//			System.err.println("VisitInsn is ignoreverything! in  " + name);
//			new Exception().printStackTrace();
            isIgnoreAllInstrumenting = !isIgnoreAllInstrumenting;
            isIgnoreEverything = !isIgnoreEverything;
            Configuration.taintTagFactory.signalOp(opcode, null);
            super.visitInsn(opcode);
            return;
        }
        if(opcode == TaintUtils.NO_TAINT_STORE_INSN) {
            isTaintlessArrayStore = true;
            return;
        }
        if(isIgnoreAllInstrumenting || isRawInsns) {
            super.visitInsn(opcode);
            return;
        }

        if(Configuration.IMPLICIT_TRACKING && !Configuration.WITHOUT_PROPOGATION) {
            if(opcode == ATHROW) {
                if(controlTaintArray >= 0) {
                    passThroughMV.visitInsn(DUP);
                    passThroughMV.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
                    COMBINE_TAGS_ON_OBJECT.delegateVisit(passThroughMV);
                    passThroughMV.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
                    passThroughMV.visitVarInsn(ALOAD, controlTaintArray);
                    callPopAllControlTaint(passThroughMV);
                }
            }
        }

        switch(opcode) {
            case Opcodes.NOP:
            case Opcodes.MONITORENTER:
            case Opcodes.MONITOREXIT:
                //You can have a monitor on an array type. If it's a primitive array type, pop the taint off!
            case TaintUtils.FOLLOWED_BY_FRAME:
                super.visitInsn(opcode);
                break;
            case Opcodes.ACONST_NULL:
                super.visitInsn(opcode);
                if(nextLoadIsTracked) {
                    nextLoadIsTracked = false;
                    super.visitInsn(ACONST_NULL);
                    analyzer.setTopOfStackTagged();
                }
                break;
            case Opcodes.ICONST_M1:
            case Opcodes.ICONST_0:
            case Opcodes.ICONST_1:
            case Opcodes.ICONST_2:
            case Opcodes.ICONST_3:
            case Opcodes.ICONST_4:
            case Opcodes.ICONST_5:
            case Opcodes.LCONST_0:
            case Opcodes.LCONST_1:
            case Opcodes.FCONST_0:
            case Opcodes.FCONST_1:
            case Opcodes.FCONST_2:
            case Opcodes.DCONST_0:
            case Opcodes.DCONST_1:
                if(nextLoadIsTracked) {
                    if(Configuration.IMPLICIT_TRACKING || isImplicitLightTracking) {
                        super.visitVarInsn(ALOAD, lvs.idxOfMasterControlLV);
                        super.visitMethodInsn(INVOKEVIRTUAL, "edu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack", "copyTag" + (Configuration.IMPLICIT_EXCEPTION_FLOW ? "Exceptions" : ""), "()" + Configuration.TAINT_TAG_DESC, false);
                    } else {
                        super.visitInsn(Configuration.NULL_TAINT_LOAD_OPCODE);
                    }
                    nextLoadIsTracked = false;
                    super.visitInsn(opcode);
                    analyzer.setTopOfStackTagged();
                } else {
                    super.visitInsn(opcode);
                }
                return;
            case Opcodes.LALOAD:
            case Opcodes.DALOAD:
            case Opcodes.IALOAD:
            case Opcodes.FALOAD:
            case Opcodes.BALOAD:
            case Opcodes.CALOAD:
            case Opcodes.SALOAD:
                boolean doingLoadWithIdxTaint = false;
                if(Configuration.ARRAY_INDEX_TRACKING && topCarriesTaint()) {
                    if(nextLoadIsTracked) {
                        doingLoadWithIdxTaint = true;
                    } else {
                        super.visitInsn(SWAP);
                        super.visitInsn(POP);
                        analyzer.clearTopOfStackTagged();
                    }
                }
                String elType = null;
                String elName = null;
                switch(opcode) {
                    case Opcodes.LALOAD:
                        elName = "Long";
                        elType = "J";
                        break;
                    case Opcodes.DALOAD:
                        elName = "Double";
                        elType = "D";
                        break;
                    case Opcodes.IALOAD:
                        elName = "Int";
                        elType = "I";
                        break;
                    case Opcodes.FALOAD:
                        elName = "Float";
                        elType = "F";
                        break;
                    case Opcodes.BALOAD:
                        elName = "Byte";
                        // System.out.println("BALOAD " + analyzer.stack);
                        if(analyzer.stack.get(analyzer.stack.size() - (doingLoadWithIdxTaint ? 3 : 2)) instanceof Integer) {
                            elType = "B";
                        } else {
                            elType = Type.getType((String) analyzer.stack.get(analyzer.stack.size() - (doingLoadWithIdxTaint ? 3 : 2))).getElementType().getDescriptor();
                        }
                        break;
                    case Opcodes.CALOAD:
                        elName = "Char";
                        elType = "C";
                        break;
                    case Opcodes.SALOAD:
                        elName = "Short";
                        elType = "S";
                        break;
                }
                if(TaintUtils.DEBUG_FRAMES) {
                    System.out.println(name + desc + "PRE XALOAD " + elType + ": " + analyzer.stack + "; " + analyzer.locals);
                }
                if(analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - (doingLoadWithIdxTaint ? 3 : 2)) instanceof TaggedValue
                        || nextLoadIsTracked) {
                    // TA A T I
                    if(elType.equals("Z")) {
                        elName = "Boolean";
                    }
                    Type retType = Type.getObjectType("edu/columbia/cs/psl/phosphor/struct/Tainted" + elName + "With"
                            + (Configuration.MULTI_TAINTING ? "Obj" : "Int") + "Tag");

                    int prealloc = lvs.getPreAllocedReturnTypeVar(retType);
                    super.visitVarInsn(ALOAD, prealloc);
                    String methodName = "get";
                    if(Configuration.IMPLICIT_TRACKING || isImplicitLightTracking) {
                        super.visitVarInsn(ALOAD, lvs.idxOfMasterControlLV);
                    }
                    super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "edu/columbia/cs/psl/phosphor/struct/Lazy" + elName + "Array" + (Configuration.MULTI_TAINTING ? "Obj" : "Int") + "Tags", methodName,
                            "(" + "[" + elType + (doingLoadWithIdxTaint ? Configuration.TAINT_TAG_DESC : "") + "I" + retType.getDescriptor() + (Configuration.IMPLICIT_TRACKING || isImplicitLightTracking ? "Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;" : "") + ")" + retType.getDescriptor(), false);
                    if(nextLoadIsTracked) {
                        super.visitInsn(DUP);
                        super.visitFieldInsn(GETFIELD, retType.getInternalName(), "taint", Configuration.TAINT_TAG_DESC);
                        super.visitInsn(SWAP);
                        super.visitFieldInsn(GETFIELD, retType.getInternalName(), "val", elType);
                        nextLoadIsTracked = false;
                        analyzer.setTopOfStackTagged();
                    } else {
                        super.visitFieldInsn(GETFIELD, retType.getInternalName(), "val", elType);
                    }
                } else {
                    super.visitInsn(opcode);
                }
                break;
            case Opcodes.AALOAD:
                if(Configuration.ARRAY_INDEX_TRACKING && topCarriesTaint()) {
                    super.visitInsn(SWAP);
                    super.visitInsn(POP);
                    analyzer.clearTopOfStackTagged();
                }
                //?TA A I
                Object arrayType = analyzer.stack.get(analyzer.stack.size() - 2);
                Type t = getTypeForStackType(arrayType);
                if(t.getSort() == Type.ARRAY && t.getDimensions() == 1 && t.getElementType().getDescriptor().startsWith("Ledu/columbia/cs/psl/phosphor/struct/Lazy")) {
                    super.visitInsn(opcode);
                    try {
                        retrieveTaintedArray("[" + (MultiDTaintedArray.getPrimitiveTypeForWrapper(Class.forName(t.getElementType().getInternalName().replace("/", ".")))));
                        if(!nextLoadIsTracked) {
                            super.visitInsn(SWAP);
                            super.visitInsn(POP);
                        } else {
                            analyzer.setTopOfStackTagged();
                        }
                        nextLoadIsTracked = false;
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    super.visitInsn(opcode);
                }
                break;
            case Opcodes.AASTORE:
                arrayType = analyzer.stack.get(analyzer.stack.size() - 1);
                t = getTypeForStackType(arrayType);
                boolean idxTainted = Configuration.ARRAY_INDEX_TRACKING && analyzer.stackTagStatus.get(analyzer.stack.size() - (topCarriesTaint() ? 1 : 0) - 2) instanceof TaggedValue;
                if(arrayType == Opcodes.NULL) {
                    Object theArray = analyzer.stack.get(analyzer.stack.size() - 3 - (idxTainted ? 1 : 0));
                    t = getTypeForStackType(theArray);
                    //				System.out.println(theArray);
                    if(theArray != Opcodes.NULL && t.getElementType().getSort() != Type.OBJECT) {
                        super.visitInsn(ACONST_NULL);
                    }
                }
                if(t.getSort() == Type.ARRAY && t.getElementType().getDescriptor().length() == 1) {
                    //this is a multi-d array. make it work, even if it's nasty.
                    if(!analyzer.isTopOfStackTagged()) {
                        super.visitTypeInsn(NEW, MultiDTaintedArray.getTypeForType(t).getInternalName());
                        super.visitInsn(DUP_X1);
                        super.visitInsn(SWAP);
                        super.visitMethodInsn(INVOKESPECIAL, MultiDTaintedArray.getTypeForType(t).getInternalName(), "<init>", "(" + t + ")V", false);
                        nextLoadIsTracked = false;
                    } else {
                        registerTaintedArray();
                    }
                    if(idxTainted) {
                        //Not supported
                        //Array Taint Index Val
                        super.visitInsn(DUP2_X1);
                        //Array Index Val Taint Index Val
                        super.visitInsn(POP2);
                        //Array Index Val Taint
                        super.visitInsn(POP);
                        //Array Index Val
                    }
                    super.visitInsn(opcode);
                } else {
                    if(Configuration.IMPLICIT_TRACKING || isImplicitLightTracking) {
                        super.visitInsn(DUP);
                        super.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
                        visit(COMBINE_TAGS_ON_OBJECT);
                    }
                    if(idxTainted) {
                        //Not supported
                        //Array Taint Index Val
                        super.visitInsn(DUP2_X1);
                        //Array Index Val Taint Index Val
                        super.visitInsn(POP2);
                        //Array Index Val Taint
                        super.visitInsn(POP);
                        //Array Index Val
                    }
                    super.visitInsn(opcode);
                }
                break;
            case Opcodes.IASTORE:
            case Opcodes.LASTORE:
            case Opcodes.FASTORE:
            case Opcodes.DASTORE:
            case Opcodes.BASTORE:
            case Opcodes.CASTORE:
            case Opcodes.SASTORE:
                //	public static void XASTORE(int[] TArray, int[] Array, int idxTaint, int idx, int valTaint, int val) {
                Object beingStored = analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - (opcode == LASTORE || opcode == DASTORE ? 2 : 1));
                int offsetToArray = 4;
                int ob = (opcode == LASTORE || opcode == DASTORE ? 2 : 1) + ((beingStored instanceof TaggedValue) ? 1 : 0) + 1;
                boolean tagIsTracked = Configuration.ARRAY_INDEX_TRACKING && analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - ob) instanceof TaggedValue;
                if(tagIsTracked) {
                    offsetToArray++;
                }
                switch(opcode) {
                    case Opcodes.LASTORE:
                        elType = "J";
                        offsetToArray++;
                        break;
                    case Opcodes.DASTORE:
                        elType = "D";
                        offsetToArray++;
                        break;
                    case Opcodes.IASTORE:
                        elType = "I";
                        break;
                    case Opcodes.FASTORE:
                        elType = "F";
                        break;
                    case Opcodes.BASTORE:
                        elType = "B";
                        if(beingStored instanceof TaggedValue) {
                            elType = Type.getType((String) analyzer.stack.get(analyzer.stack.size() - offsetToArray)).getElementType().getDescriptor();
                        }
                        break;
                    case Opcodes.CASTORE:
                        elType = "C";
                        break;
                    case Opcodes.SASTORE:
                        elType = "S";
                        break;
                    default:
                        elType = null;
                }
                if(!(beingStored instanceof TaggedValue)) {
                    if(tagIsTracked) {
                        String typ = (String) analyzer.stack.get(analyzer.stack.size() - offsetToArray);
                        super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, typ, "set", "([" + elType + Configuration.TAINT_TAG_DESC + "I" + elType + ")V", false);
                    } else {
                        super.visitInsn(opcode);
                    }
                } else if(analyzer.stackTagStatus.get(analyzer.stack.size() - offsetToArray) instanceof TaggedValue) {
                    String typ = (String) analyzer.stack.get(analyzer.stack.size() - offsetToArray - 1);
                    if(Configuration.IMPLICIT_TRACKING || isImplicitLightTracking) {
                        super.visitVarInsn(ALOAD, lvs.idxOfMasterControlLV);
                        if(tagIsTracked) {
                            super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, typ, "set", "([" + elType + Configuration.TAINT_TAG_DESC + "I" + Configuration.TAINT_TAG_DESC + elType + "Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)V", false);
                        } else {
                            super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, typ, "set", "([" + elType + "I" + Configuration.TAINT_TAG_DESC + elType + "Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)V", false);
                        }
                    } else if(tagIsTracked) {
                        super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, typ, "set", "([" + elType + Configuration.TAINT_TAG_DESC + "I" + Configuration.TAINT_TAG_DESC + elType + ")V", false);
                    } else {
                        super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, typ, "set", "([" + elType + "I" + Configuration.TAINT_TAG_DESC + elType + ")V", false);
                    }
                } else {
                    if(tagIsTracked) {
                        throw new IllegalStateException(analyzer.stackTagStatus.toString());
                    }
                    super.visitInsn(opcode);
                }
                isTaintlessArrayStore = false;
                break;
            case Opcodes.POP:
                if(topCarriesTaint()) {
                    super.visitInsn(POP2);
                    return;
                }
                super.visitInsn(opcode);
                return;
            case Opcodes.POP2:
                if(topCarriesTaint()) {
                    super.visitInsn(POP2);
                    if(topCarriesTaint()) {
                        super.visitInsn(POP2);
                    } else {
                        super.visitInsn(POP);
                    }
                    return;
                } else if(analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 2) instanceof TaggedValue) {
                    super.visitInsn(POP);
                    super.visitInsn(POP2);
                } else {
                    super.visitInsn(opcode);
                }
                return;
            case Opcodes.DUP:
                if(nextDupCopiesTaint0 && nextDupCopiesTaint1) {
                    super.visitInsn(Opcodes.DUP2);
                } else if(!nextDupCopiesTaint0 && nextDupCopiesTaint1) {
                    super.visitInsn(DUP_X1);
                    analyzer.stackTagStatus.set(analyzer.stack.size() - 3, analyzer.stack.get(analyzer.stack.size() - 3));
                } else if(nextDupCopiesTaint0 && !nextDupCopiesTaint1) {
                    super.visitInsn(DUP);
                    analyzer.stackTagStatus.set(analyzer.stack.size() - 1, analyzer.stack.get(analyzer.stack.size() - 1));
                } else {
                    //TOP should never be tainted and reach here
                    super.visitInsn(Opcodes.DUP);
                }
                nextDupCopiesTaint0 = false;
                nextDupCopiesTaint1 = false;
                nextDupCopiesTaint2 = false;
                nextDupCopiesTaint3 = false;
                break;
            case Opcodes.DUP2:
                Object topOfStack = analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 1);

                //0 1 -> 0 1 2 3
                if(getStackElementSize(topOfStack) == 1) {
                    if(nextDupCopiesTaint0) {
                        int offset = 2;
                        if(topCarriesTaint()) {
                            offset++;
                        }
                        Object secondOnStack = analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - offset); //3, or 2?
                        if(!(secondOnStack instanceof TaggedValue)) {
                            if(nextDupCopiesTaint1 && !nextDupCopiesTaint2 && !nextDupCopiesTaint3) {
                                //Dup the top 2 items, leaving a taint behind for the top item, the second item is not tainted
                                //A TB B -> A TB B A TB B
                                LocalVariableNode[] lvs = storeToLocals(3);
                                loadLV(2, lvs);
                                loadLV(1, lvs);
                                loadLV(0, lvs);
                                loadLV(2, lvs);
                                loadLV(1, lvs);
                                loadLV(0, lvs);
                                freeLVs(lvs);
                                return;
                            }
                            throw new IllegalStateException("Told to copy taint of second thing on stack but got " + secondOnStack);
                        }
                        if(getStackElementSize(secondOnStack) == 2) {
                            throw new UnsupportedOperationException();//Should be true always or was invalid already
                        }
                        if(nextDupCopiesTaint1) {
                            if(nextDupCopiesTaint2) {
                                if(nextDupCopiesTaint3) {
                                    //0, 1, 2, 3
                                    LocalVariableNode[] lvs = storeToLocals(4);
                                    loadLV(3, lvs);
                                    loadLV(2, lvs);
                                    loadLV(1, lvs);
                                    loadLV(0, lvs);
                                    loadLV(3, lvs);
                                    loadLV(2, lvs);
                                    loadLV(1, lvs);
                                    loadLV(0, lvs);
                                    freeLVs(lvs);
                                } else {
                                    //0, 1, 2, !3
                                    LocalVariableNode[] lvs = storeToLocals(4);
                                    loadLV(3, lvs);
                                    loadLV(2, lvs);
                                    loadLV(1, lvs);
                                    loadLV(0, lvs);
                                    loadLV(3, lvs);
                                    loadLV(2, lvs);
                                    loadLV(0, lvs);
                                    analyzer.clearTopOfStackTagged();
                                    freeLVs(lvs);
                                }
                            } else {
                                //0, 1, !2, 3
                                //0,1,!2,!3
                                throw new UnsupportedOperationException();
                            }
                        } else {
                            if(nextDupCopiesTaint2) {
                                if(nextDupCopiesTaint3) {
                                    //0, !1, 2, 3
                                    //AB CD -> A CD AB CD (Top)
                                    LocalVariableNode[] lvs = storeToLocals(4);
                                    loadLV(3, lvs);
                                    loadLV(2, lvs);
                                    loadLV(0, lvs);
                                    analyzer.clearTopOfStackTagged();
                                    loadLV(3, lvs);
                                    loadLV(2, lvs);
                                    loadLV(1, lvs);
                                    loadLV(0, lvs);
                                    freeLVs(lvs);
                                } else {
                                    //0, !1, 2, !3
                                    //TA ?B -> TA B TA B
                                    if(topOfStack instanceof TaggedValue) {
                                        super.visitInsn(SWAP);
                                        super.visitInsn(POP);
                                    }
                                    //TA B -> TA B TA B
                                    Type topType = getTypeForStackType(topOfStack);
                                    int top = lvs.getTmpLV();
                                    super.visitInsn(TaintUtils.IS_TMP_STORE);
                                    super.visitVarInsn(topType.getOpcode(ISTORE), top);
                                    // TA
                                    super.visitInsn(DUP2);
                                    //TA TA
                                    super.visitVarInsn(topType.getOpcode(ILOAD), top);
                                    // TA TA B
                                    super.visitInsn(DUP_X2);
                                    // AA BB AA BB
                                    lvs.freeTmpLV(top);
                                }
                            } else {
                                //0, !1, !2, 3
                                // 0,!1,!2,!3
                                throw new UnsupportedOperationException();
                            }
                        }
                    } else {
                        //!0
                        if(!nextDupCopiesTaint1) {
                            //!0, !1
                            if(nextDupCopiesTaint2) {
                                if(nextDupCopiesTaint3) {
                                    //!0, !1, 2, 3
                                    //right here..
                                    // T V T V -> V V T V T V
                                    int v1 = lvs.getTmpLV();
                                    super.visitVarInsn(ISTORE, v1);
                                    int t1 = lvs.getTmpLV();
                                    super.visitVarInsn(Configuration.TAINT_STORE_OPCODE, t1);
                                    int v2 = lvs.getTmpLV();
                                    super.visitVarInsn(ISTORE, v2);
                                    int t2 = lvs.getTmpLV();
                                    super.visitVarInsn(Configuration.TAINT_STORE_OPCODE, t2);
                                    super.visitVarInsn(ILOAD, v2);
                                    super.visitVarInsn(ILOAD, v1);
                                    super.visitVarInsn(Configuration.TAINT_LOAD_OPCODE, t2);
                                    super.visitVarInsn(ILOAD, v2);
                                    super.visitVarInsn(Configuration.TAINT_LOAD_OPCODE, t1);
                                    super.visitVarInsn(ILOAD, v1);
                                    lvs.freeTmpLV(v1);
                                    lvs.freeTmpLV(v2);
                                    lvs.freeTmpLV(t1);
                                    lvs.freeTmpLV(t2);
                                    return;
                                } else {
                                    //!0, !1, 2, !3
                                }
                            } else {
                                if(!nextDupCopiesTaint3) {
                                    //!0, !1, !2, !3
                                    if(getStackElementSize(getTopOfStackObject()) == 2) {
                                        if(analyzer.isTopOfStackTagged()) {
                                            throw new UnsupportedOperationException(Printer.OPCODES[opcode] + " " + analyzer.stackTagStatus + " - " + nextDupCopiesTaint0 + " " + nextDupCopiesTaint1 + " " + nextDupCopiesTaint2 + " " + nextDupCopiesTaint3);
                                        } else {
                                            super.visitInsn(opcode);
                                            return;
                                        }
                                    } else {
                                        if(analyzer.isTopOfStackTagged()) {
                                            throw new UnsupportedOperationException(Printer.OPCODES[opcode] + " " + analyzer.stackTagStatus + " - " + nextDupCopiesTaint0 + " " + nextDupCopiesTaint1 + " " + nextDupCopiesTaint2 + " " + nextDupCopiesTaint3);
                                        }
                                        if(analyzer.stackTagStatus.get(analyzer.stack.size() - 2) instanceof TaggedValue) {
                                            throw new UnsupportedOperationException(Printer.OPCODES[opcode] + " " + analyzer.stackTagStatus + " - " + nextDupCopiesTaint0 + " " + nextDupCopiesTaint1 + " " + nextDupCopiesTaint2 + " " + nextDupCopiesTaint3);
                                        }
                                        super.visitInsn(opcode);
                                        return;
                                    }
                                } else {
                                    //!0, !1, !2, 3
                                }
                            }
                        }
                        throw new UnsupportedOperationException(Printer.OPCODES[opcode] + " " + analyzer.stackTagStatus + " - " + nextDupCopiesTaint0 + " " + nextDupCopiesTaint1 + " " + nextDupCopiesTaint2 + " " + nextDupCopiesTaint3);
                    }
                } else {
                    // DUP2, top of stack is double
                    topOfStack = analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 2);
                    if(nextDupCopiesTaint0) {
                        if(nextDupCopiesTaint1) {
                            //TVV -> TVVTVV
                            Type topType = getTypeForStackType(topOfStack);
                            int top = lvs.getTmpLV();
                            super.visitInsn(TaintUtils.IS_TMP_STORE);
                            super.visitVarInsn(topType.getOpcode(ISTORE), top);
                            // T
                            super.visitInsn(DUP);
                            //TT
                            super.visitVarInsn(topType.getOpcode(ILOAD), top);
                            analyzer.setTopOfStackTagged();
                            // TTVV
                            super.visitInsn(DUP2_X1);
                            // TVVTVV
                            lvs.freeTmpLV(top);
                        } else {
                            //TVV -> TVV VV
                            super.visitInsn(DUP2);
                            analyzer.stackTagStatus.set(analyzer.stackTagStatus.size() - 2, analyzer.stack.get(analyzer.stack.size() - 2));
                        }
                    } else {
                        if(nextDupCopiesTaint1) {
                            //TVV -> VVTVV
                            super.visitInsn(DUP2_X1);
                            analyzer.stackTagStatus.set(analyzer.stackTagStatus.size() - 5, analyzer.stack.get(analyzer.stack.size() - 5));
                        } else {
                            //VV -> VVVV
                            if(topOfStack instanceof TaggedValue) {
                                // Should not be possible
                                throw new UnsupportedOperationException();
                            }
                            super.visitInsn(DUP2);
                        }
                    }
                }
                nextDupCopiesTaint0 = false;
                nextDupCopiesTaint1 = false;
                nextDupCopiesTaint2 = false;
                nextDupCopiesTaint3 = false;
                break;
            case Opcodes.DUP_X1:
                if(nextDupCopiesTaint0) {
                    if(!nextDupCopiesTaint1) {
                        throw new UnsupportedOperationException();
                    }
                    topOfStack = analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 1);
                    if(topOfStack instanceof TaggedValue) {
                        //There is a 1 word element at the top of the stack, we want to dup
                        //it and it's taint to go one under so that it's
                        //T V X T V
                        Object underThisOne = analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 3);
                        if(underThisOne instanceof TaggedValue) {
                            // X X T V -> T V X X T V
                            super.visitInsn(DUP2_X2);
                        } else {
                            //X T V -> T V X TV
                            super.visitInsn(DUP2_X1);
                        }
                    } else {
                        Object underThisOne = analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 2);
                        if(underThisOne instanceof TaggedValue) {
                            // X X V -> V X X V
                            super.visitInsn(DUP_X2);
                        } else {
                            //X V -> V X V
                            super.visitInsn(DUP_X1);
                        }
                    }
                } else if(nextDupCopiesTaint1) {
                    Object underThisOne = analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 3);
                    if(underThisOne instanceof TaggedValue) {
                        //T1 V1 T2 V2 -> V2 T1 V1 T2 V2
                        DUPN_XU(1, 3);
                        analyzer.stackTagStatus.set(analyzer.stack.size() - 5, ((TaggedValue) underThisOne).v);
                    } else {
                        super.visitInsn(DUP_X2);
                        Object o = analyzer.stack.get(analyzer.stack.size() - 4);
                        if(o instanceof TaggedValue) {
                            o = ((TaggedValue) o).v;
                        }
                        // Need to make sure we clear the tag status on that dude!
                        analyzer.stackTagStatus.set(analyzer.stack.size() - 4, o);
                    }
                } else if(topCarriesTaint()) {
                    throw new UnsupportedOperationException();
                } else if(analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 2) instanceof TaggedValue) {
                    super.visitInsn(DUP_X2);
                } else {
                    super.visitInsn(DUP_X1);
                }
                nextDupCopiesTaint0 = false;
                nextDupCopiesTaint1 = false;
                nextDupCopiesTaint2 = false;
                nextDupCopiesTaint3 = false;
                break;
            case Opcodes.DUP_X2:
                if(nextDupCopiesTaint0) {
                    if(!nextDupCopiesTaint1) {
                        //0, !1
                        //aka copy the taint under then delete it from top
                        System.out.println(analyzer.stackTagStatus);
                        throw new UnsupportedOperationException();
                    } else {
                        //0, 1
                        if(getStackElementSize(analyzer.stack.get(analyzer.stack.size() - 3)) == 2) {
                            // With long/double under
                            if(analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 4) instanceof TaggedValue) {
                                // Top 2 are tracked
                                DUPN_XU(2, 3);
                            } else {
                                // 2nd is not tracked.
                                DUPN_XU(2, 2);
                            }
                        } else {
                            // With 1word under
                            if(analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 3) instanceof TaggedValue) {
                                // Top 2 are tracked
                                if(analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 5) instanceof TaggedValue) {
                                    DUPN_XU(2, 4);
                                } else {
                                    DUPN_XU(2, 3);
                                }
                            } else {
                                // 2nd is not tracked.
                                if(analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 4) instanceof TaggedValue) {
                                    DUPN_XU(2, 3);
                                } else {
                                    super.visitInsn(DUP2_X2);
                                }
                            }
                        }
                    }
                } else {
                    if(nextDupCopiesTaint1) {
                        Object under = analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 3);
                        if(under == Opcodes.TOP) {
                            // Two byte word
                            throw new UnsupportedOperationException();
                        } else {
                            if(under instanceof TaggedValue) {
                                Object twoUnder = analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 5);
                                if(twoUnder instanceof TaggedValue) {
                                    LocalVariableNode[] lvs = storeToLocals(6);
                                    loadLV(0, lvs);
                                    analyzer.clearTopOfStackTagged();
                                    loadLV(5, lvs);
                                    loadLV(4, lvs);
                                    loadLV(3, lvs);
                                    loadLV(2, lvs);
                                    loadLV(1, lvs);
                                    loadLV(0, lvs);
                                    freeLVs(lvs);
                                } else {
                                    LocalVariableNode[] lvs = storeToLocals(5);
                                    loadLV(0, lvs);
                                    analyzer.clearTopOfStackTagged();
                                    loadLV(4, lvs);
                                    loadLV(3, lvs);
                                    loadLV(2, lvs);
                                    loadLV(1, lvs);
                                    loadLV(0, lvs);
                                    freeLVs(lvs);

                                }
                            } else {
                                Object twoUnder = analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 4);
                                if(twoUnder instanceof TaggedValue) {
                                    DUPN_XU(1, 4);
                                    int off = analyzer.stack.size() - 6;
                                    analyzer.stackTagStatus.set(off, analyzer.stack.get(off));
                                } else {
                                    //TABTC -> CATB
                                    DUPN_XU(1, 3);
                                    int off = analyzer.stack.size() - 5;
                                    analyzer.stackTagStatus.set(off, analyzer.stack.get(off));
                                }
                            }
                        }
                    } else {
                        // Dont want to copy any taint
                        if(topCarriesTaint()) {
                            //Top has a tag, but we don't want to keep it
                            if(getStackElementSize(analyzer.stack.get(analyzer.stack.size() - 2)) == 2) {
                                //With long/double under
                                if(analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 3) instanceof TaggedValue) {
                                    //Top 2 are tracked
                                    DUPN_XU(1, 4);
                                } else {
                                    //2nd is not tracked.
                                    DUPN_XU(1, 3);
                                }
                            } else {
                                //With 1word under
                                if(analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 3) instanceof TaggedValue) {
                                    if(analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 5) instanceof TaggedValue) {
                                        DUPN_XU(1, 5);
                                    } else {
                                        DUPN_XU(1, 4);
                                    }
                                } else {
                                    //2nd is not tracked.
                                    if(analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 4) instanceof TaggedValue) {
                                        DUPN_XU(1, 4);
                                    } else {
                                        DUPN_XU(1, 3);
                                    }
                                }
                            }
                        } else {
                            //What's under us?
                            if(analyzer.stack.get(analyzer.stack.size() - 2) == Opcodes.TOP) {
                                if(analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 3) instanceof TaggedValue) {
                                    LocalVariableNode[] d = storeToLocals(3);
                                    loadLV(0, d);
                                    loadLV(2, d);
                                    loadLV(1, d);
                                    loadLV(0, d);
                                    freeLVs(d);
                                } else {
                                    super.visitInsn(DUP_X2);
                                }
                            } else {
                                // 1 word under us. is it tagged?
                                if(analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 2) instanceof TaggedValue) {
                                    if(analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 4) instanceof TaggedValue) {
                                        throw new UnsupportedOperationException();
                                    } else {
                                        DUPN_XU(1, 3);
                                    }
                                } else if(analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 3) instanceof TaggedValue) {
                                    throw new UnsupportedOperationException();
                                } else {
                                    super.visitInsn(DUP_X2);
                                }
                            }
                        }
                    }
                }
                nextDupCopiesTaint0 = nextDupCopiesTaint1 = nextDupCopiesTaint2 = nextDupCopiesTaint3 = false;
                break;
            case Opcodes.DUP2_X1:
                //ABC -> BCABC (0 1 2 3 4)
                if(nextDupCopiesTaint0) {
                    if(nextDupCopiesTaint1) {
                        if(nextDupCopiesTaint2) {
                            if(nextDupCopiesTaint3) {
                                //0, 1, 2, 3
                                throw new UnsupportedOperationException();
                            } else {
                                //0, 1, 2, !3
                                throw new UnsupportedOperationException();
                            }
                        } else {
                            if(nextDupCopiesTaint3) {
                                //0, 1, !2, 3
                                throw new UnsupportedOperationException();
                            } else {
                                //0,1,!2,!3
                                //ATBTC -> TBTCABC
                                //2 word?
                                topOfStack = analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 1);
                                if(getStackElementSize(topOfStack) == 2) {
                                    topOfStack = analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 2);
                                    if(!(topOfStack instanceof TaggedValue)) {
                                        throw new UnsupportedOperationException("Top must be tagged for this");
                                    }
                                    Object secondOnStack = analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 4);
                                    if(secondOnStack instanceof TaggedValue) {
                                        //TATBB -> TBBTATBB
                                        throw new UnsupportedOperationException();
                                    } else {
                                        //ATBB -> TBBATBB
                                        DUPN_XU(2, 1);
                                    }
                                } else {
                                    throw new UnsupportedOperationException();
                                }
                            }
                        }
                    } else {
                        if(nextDupCopiesTaint2) {
                            if(nextDupCopiesTaint3) {
                                // !0, !1, 2, 3
                                throw new UnsupportedOperationException();
                            } else {
                                // !0, !1, 2, !3
                                throw new UnsupportedOperationException();
                            }
                        } else {
                            if(nextDupCopiesTaint3) {
                                if(getTopOfStackType().getSize() == 2) {
                                    //0 !1 !2 3
                                    LocalVariableNode d[] = storeToLocals(3);
                                    loadLV(1, d);
                                    loadLV(0, d);
                                    loadLV(2, d);
                                    loadLV(1, d);
                                    loadLV(0, d);
                                    freeLVs(d);
                                } else {
                                    // !0, !1, !2, 3
                                    LocalVariableNode d[] = storeToLocals(4);
                                    loadLV(2, d);
                                    loadLV(0, d);
                                    analyzer.clearTopOfStackTagged();
                                    loadLV(3, d);
                                    loadLV(2, d);
                                    loadLV(1, d);
                                    loadLV(0, d);
                                    freeLVs(d);
                                }
                            } else {
                                //!0,!1,!2,!3
                                //We don't want to keep the tags anywhere.
                                //DUP2_X1
                                if(topCarriesTaint()) {
                                    if(!(analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 3) instanceof TaggedValue)) {
                                        super.visitInsn(SWAP);
                                        super.visitInsn(POP);
                                        analyzer.clearTopOfStackTagged();
                                        super.visitInsn(opcode);
                                        return;
                                    }
                                }
                                System.out.println(analyzer.stackTagStatus);
                                throw new UnsupportedOperationException();
                            }
                        }
                    }
                } else {
                    if(nextDupCopiesTaint1) {
                        if(nextDupCopiesTaint2) {
                            //!0, 1, 2, 3
                            //!0, 1, 2, !3
                            throw new UnsupportedOperationException();
                        } else {
                            if(nextDupCopiesTaint3) {
                                //!0, 1, !2, 3
                                throw new UnsupportedOperationException();
                            } else {
                                //!0,1,!2,!3
                                topOfStack = analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 1);
                                if(getStackElementSize(topOfStack) == 2) {
                                    //A TVV -> VV A T VV
                                    super.visitInsn(DUP2_X2);
                                    int idx = analyzer.stack.size() - 6;
                                    analyzer.stackTagStatus.set(idx, analyzer.stack.get(idx));
                                } else {
                                    LocalVariableNode d[] = storeToLocals(4);
                                    loadLV(2, d);
                                    loadLV(1, d);
                                    loadLV(0, d);
                                    loadLV(3, d);
                                    loadLV(2, d);
                                    loadLV(0, d);
                                    analyzer.clearTopOfStackTagged();
                                    freeLVs(d);
                                }
                            }
                        }
                    } else {
                        if(nextDupCopiesTaint2) {
                            if(nextDupCopiesTaint3) {
                                //!0, !1, 2, 3
                                //!0, !1, 2, !3
                                throw new UnsupportedOperationException();
                            }
                        } else {
                            if(nextDupCopiesTaint3) {
                                // !0, !1, !2, 3
                                if(getTopOfStackType().getSize() == 2) {
                                    LocalVariableNode d[] = storeToLocals(3);
                                    loadLV(0, d);
                                    analyzer.clearTopOfStackTagged();
                                    loadLV(2, d);
                                    loadLV(1, d);
                                    loadLV(0, d);
                                    freeLVs(d);
                                } else {
                                    LocalVariableNode d[] = storeToLocals(4);
                                    loadLV(2, d);
                                    loadLV(0, d);
                                    analyzer.clearTopOfStackTagged();
                                    loadLV(3, d);
                                    loadLV(2, d);
                                    loadLV(1, d);
                                    loadLV(0, d);
                                    freeLVs(d);
                                }
                            } else {
                                //!0,!1,!2,!3
                                if(analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 1) instanceof TaggedValue) {
                                    throw new UnsupportedOperationException();
                                } else if(analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 2) instanceof TaggedValue) {
                                    throw new UnsupportedOperationException();
                                } else if(analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 3) instanceof TaggedValue) {
                                    super.visitInsn(DUP2_X2);
                                    return;
                                }
                                super.visitInsn(opcode);
                            }
                        }
                    }
                }
                nextDupCopiesTaint0 = false;
                nextDupCopiesTaint1 = false;
                nextDupCopiesTaint2 = false;
                nextDupCopiesTaint3 = false;
                break;
            case Opcodes.DUP2_X2:
                if(nextDupCopiesTaint0) {
                    if(nextDupCopiesTaint1) {
                        if(nextDupCopiesTaint2) {
                            //0, 1, 2, 3
                            //0, 1, 2, !3
                            throw new UnsupportedOperationException();
                        } else {
                            if(nextDupCopiesTaint3) {
                                //0, 1, !2, 3
                                throw new UnsupportedOperationException();
                            } else {
                                //0,1,!2,!3
                                if(getStackElementSize(getTopOfStackObject()) == 2) {
                                    //?A?BTC -> TC?A?BTC
                                    topOfStack = analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 2);
                                    Object second = analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 4);
                                    if(getStackElementSize(second) == 2) {
                                        //?AATC -> TC?AAC
                                        throw new UnsupportedOperationException();
                                    } else {
                                        if(second instanceof TaggedValue) {
                                            Object third = analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 6);
                                            if(third instanceof TaggedValue) {
                                                DUPN_XU(2, 4);
                                            } else {
                                                //ATBTC -> TCATBTC
                                                DUPN_XU(2, 3);
                                            }
                                        } else {
                                            Object third = analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 5);
                                            if(third instanceof TaggedValue) {
                                                // ATBTC -> TCATBTC
                                                DUPN_XU(2, 3);
                                            } else {
                                                throw new UnsupportedOperationException();
                                            }
                                        }
                                    }
                                } else {
                                    throw new UnsupportedOperationException();
                                }
                            }
                        }
                    } else {
                        //!0, !1, 2, 3
                        //!0, !1, 2, !3
                        //!0, !1, !2, 3
                        //!0,!1,!2,!3
                        throw new UnsupportedOperationException();
                    }
                } else {
                    if(nextDupCopiesTaint1) {
                        if(nextDupCopiesTaint2) {
                            //!0, 1, 2, 3
                            //!0, 1, 2, !3
                            throw new UnsupportedOperationException();
                        } else {
                            if(nextDupCopiesTaint3) {
                                //!0, 1, !2, 3
                                throw new UnsupportedOperationException();
                            } else {
                                //!0,1,!2,!3
                                if(getStackElementSize(getTopOfStackObject()) == 2) {
                                    if(analyzer.stackTagStatus.get(analyzer.stack.size() - 2) instanceof TaggedValue) {
                                        //top 2 words, tagged
                                        if(getStackElementSize(analyzer.stack.get(analyzer.stack.size() - 4)) == 2) {
                                            throw new UnsupportedOperationException();
                                        } else {
                                            //ABTCC -> CCABTCC
                                            Object second = analyzer.stackTagStatus.get(analyzer.stack.size() - 4);
                                            if(second instanceof TaggedValue) {
                                                LocalVariableNode d[] = storeToLocals(6);
                                                loadLV(0, d);
                                                analyzer.clearTopOfStackTagged();
                                                loadLV(5, d);
                                                loadLV(4, d);
                                                loadLV(3, d);
                                                loadLV(2, d);
                                                loadLV(1, d);
                                                loadLV(0, d);
                                                freeLVs(d);
                                            } else {
                                                Object third = analyzer.stackTagStatus.get(analyzer.stack.size() - 5);
                                                if(third instanceof TaggedValue) {
                                                    DUPN_XU(1, 4);
                                                    analyzer.stackTagStatus.set(analyzer.stack.size() - 8, analyzer.stack.get(analyzer.stack.size() - 8));
                                                } else {
                                                    throw new UnsupportedOperationException();
                                                }
                                            }
                                        }
                                    } else {
                                        throw new UnsupportedOperationException();
                                    }
                                } else {
                                    throw new UnsupportedOperationException();
                                }
                            }
                        }
                    } else {
                        if(nextDupCopiesTaint2) {
                            //!0, !1, 2, 3
                            //!0, !1, 2, !3
                            throw new UnsupportedOperationException();
                        } else {
                            if(nextDupCopiesTaint3) {
                                //!0, !1, !2, 3
                                throw new UnsupportedOperationException();
                            } else {
                                //!0,!1,!2,!3
                                if(analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 2) instanceof TaggedValue) {
                                    throw new UnsupportedOperationException();
                                } else if(analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 3) instanceof TaggedValue) {
                                    throw new UnsupportedOperationException();
                                } else if(analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 4) instanceof TaggedValue) {
                                    DUPN_XU(2, 2);
                                } else {
                                    super.visitInsn(DUP2_X2);
                                }
                            }
                        }
                    }
                }
                nextDupCopiesTaint0 = false;
                nextDupCopiesTaint1 = false;
                nextDupCopiesTaint2 = false;
                nextDupCopiesTaint3 = false;
                break;
            case Opcodes.SWAP:
                if(topCarriesTaint()
                        || analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 1) instanceof TaggedValue) {
                    if(nextLoadIsTracked) {
                        if(analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 3) instanceof TaggedValue) {
                            super.visitInsn(DUP2_X2);
                            super.visitInsn(POP2);
                        } else {
                            super.visitInsn(DUP2_X1);
                            super.visitInsn(POP2);
                        }
                    } else {
                        super.visitInsn(DUP2_X1);
                        super.visitInsn(POP2);
                    }
                } else if(analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 2) instanceof TaggedValue) {
                    //Top has no tag, second does
                    super.visitInsn(DUP_X2);
                    super.visitInsn(POP);
                } else {
                    super.visitInsn(SWAP);
                }
                nextLoadIsTracked = false;
                break;
            case Opcodes.FADD:
            case Opcodes.FREM:
            case Opcodes.FSUB:
            case Opcodes.FMUL:
            case Opcodes.FDIV: {
                if(!topCarriesTaint()) {
                    super.visitInsn(opcode);
                    break;
                } else {
                    Configuration.taintTagFactory.stackOp(opcode, mv, lvs, this);
                    analyzer.setTopOfStackTagged();
                }
            }
            break;
            case Opcodes.IADD:
            case Opcodes.ISUB:
            case Opcodes.IMUL:
            case Opcodes.IDIV:
            case Opcodes.IREM:
            case Opcodes.ISHL:
            case Opcodes.ISHR:
            case Opcodes.IUSHR:
            case Opcodes.IOR:
            case Opcodes.IAND:
            case Opcodes.IXOR:
                if(!topCarriesTaint()) {
                    super.visitInsn(opcode);
                    break;
                } else {
                    Configuration.taintTagFactory.stackOp(opcode, mv, lvs, this);
                    analyzer.setTopOfStackTagged();
                }
                break;
            case Opcodes.DADD:
            case Opcodes.DSUB:
            case Opcodes.DMUL:
            case Opcodes.DDIV:
            case Opcodes.DREM:
            case Opcodes.LSHL:
            case Opcodes.LUSHR:
            case Opcodes.LSHR:
            case Opcodes.LSUB:
            case Opcodes.LMUL:
            case Opcodes.LADD:
            case Opcodes.LDIV:
            case Opcodes.LREM:
            case Opcodes.LAND:
            case Opcodes.LOR:
            case Opcodes.LXOR:
            case Opcodes.LCMP:
            case Opcodes.DCMPL:
            case Opcodes.DCMPG:
            case Opcodes.FCMPL:
            case Opcodes.FCMPG:
                if(!topCarriesTaint()) {
                    super.visitInsn(opcode);
                    break;
                } else {
                    Configuration.taintTagFactory.stackOp(opcode, mv, lvs, this);
                    analyzer.setTopOfStackTagged();
                }
                break;
            case Opcodes.INEG:
            case Opcodes.FNEG:
            case Opcodes.LNEG:
            case Opcodes.DNEG:
            case Opcodes.I2L:
            case Opcodes.I2F:
            case Opcodes.I2D:
            case Opcodes.L2I:
            case Opcodes.L2F:
            case Opcodes.L2D:
            case Opcodes.F2I:
            case Opcodes.F2L:
            case Opcodes.F2D:
            case Opcodes.D2I:
            case Opcodes.D2L:
            case Opcodes.D2F:
            case Opcodes.I2B:
            case Opcodes.I2C:
            case Opcodes.I2S:
                Configuration.taintTagFactory.stackOp(opcode, mv, lvs, this);
                nextLoadIsTracked = false;
                break;
            case Opcodes.DRETURN:
            case Opcodes.LRETURN:
                int retIdx = lvs.getPreAllocedReturnTypeVar(newReturnType);

                super.visitVarInsn(ALOAD, retIdx);
                super.visitInsn(DUP_X2);
                super.visitInsn(POP);
                super.visitFieldInsn(PUTFIELD, newReturnType.getInternalName(), "val", originalMethodReturnType.getDescriptor());
                super.visitVarInsn(ALOAD, retIdx);
                super.visitInsn(SWAP);
                super.visitFieldInsn(PUTFIELD, newReturnType.getInternalName(), "taint", Configuration.TAINT_TAG_DESC);
                super.visitVarInsn(ALOAD, retIdx);
                Configuration.taintTagFactory.stackOp(opcode, mv, lvs, this);
                super.visitInsn(ARETURN);
                break;
            case Opcodes.IRETURN:
            case Opcodes.FRETURN:
                retIdx = lvs.getPreAllocedReturnTypeVar(newReturnType);
                super.visitVarInsn(ALOAD, retIdx);
                super.visitInsn(SWAP);
                super.visitFieldInsn(PUTFIELD, newReturnType.getInternalName(), "val", originalMethodReturnType.getDescriptor());
                super.visitVarInsn(ALOAD, retIdx);
                super.visitInsn(SWAP);
                super.visitFieldInsn(PUTFIELD, newReturnType.getInternalName(), "taint", Configuration.TAINT_TAG_DESC);
                super.visitVarInsn(ALOAD, retIdx);
                Configuration.taintTagFactory.stackOp(opcode, mv, lvs, this);
                super.visitInsn(ARETURN);
                break;
            case Opcodes.ARETURN:
                Type onStack = getTopOfStackType();
                if(originalMethodReturnType.getSort() == Type.ARRAY) {
                    if(onStack.getSort() == Type.OBJECT) {
                        super.visitInsn(opcode);
                        return;
                    } else if(originalMethodReturnType.getDimensions() > 1 && (onStack.getSort() != Type.ARRAY || onStack.getElementType().getSort() == Type.OBJECT)) {
                        super.visitInsn(opcode);
                        return;
                    }
                    switch(originalMethodReturnType.getElementType().getSort()) {
                        case Type.INT:
                        case Type.LONG:
                        case Type.BOOLEAN:
                        case Type.BYTE:
                        case Type.CHAR:
                        case Type.DOUBLE:
                        case Type.FLOAT:
                        case Type.SHORT:
                            registerTaintedArray();
                            Configuration.taintTagFactory.stackOp(opcode, mv, lvs, this);
                            super.visitInsn(ARETURN);
                            break;
                        default:
                            Configuration.taintTagFactory.stackOp(opcode, mv, lvs, this);
                            super.visitInsn(opcode);
                    }
                } else if(onStack.getSort() == Type.ARRAY && onStack.getDimensions() == 1 && onStack.getElementType().getSort() != Type.OBJECT) {
                    registerTaintedArray();
                    Configuration.taintTagFactory.stackOp(opcode, mv, lvs, this);
                    super.visitInsn(opcode);
                } else {
                    Configuration.taintTagFactory.stackOp(opcode, mv, lvs, this);
                    super.visitInsn(opcode);
                }
                break;
            case Opcodes.RETURN:
                Configuration.taintTagFactory.stackOp(opcode, mv, lvs, this);
                super.visitInsn(opcode);
                break;
            case Opcodes.ARRAYLENGTH:
                if(nextLoadIsTracked) {
                    Configuration.taintTagFactory.stackOp(opcode, mv, lvs, this);
                    analyzer.setTopOfStackTagged();
                    nextLoadIsTracked = false;
                } else {
                    super.visitInsn(opcode);
                }
                break;
            case Opcodes.ATHROW:
                if(TaintUtils.DEBUG_FRAMES) {
                    System.out.println("ATHROW " + analyzer.stack);
                }
                super.visitInsn(opcode);
                break;
            default:
                super.visitInsn(opcode);
                throw new IllegalArgumentException();
        }
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        if(isIgnoreAllInstrumenting) {
            super.visitJumpInsn(opcode, label);
            return;
        }
        if((Configuration.IMPLICIT_TRACKING || isImplicitLightTracking) && !Configuration.WITHOUT_PROPOGATION) {
            if(!boxAtNextJump.isEmpty()) {
                Label origDest = label;
                Label newDest = new Label();
                Label origFalseLoc = new Label();
                Configuration.taintTagFactory.jumpOp(opcode, branchStarting, newDest, mv, lvs, this);
                branchStarting = DO_NOT_TRACK_BRANCH_STARTING;
                FrameNode fn = getCurrentFrameNode();
                super.visitJumpInsn(GOTO, origFalseLoc);
                super.visitLabel(newDest);
                fn.accept(this);
                for(Integer var : boxAtNextJump) {
                    super.visitVarInsn(ALOAD, lvs.varToShadowVar.get(var));
                    super.visitVarInsn(ASTORE, var);
                }
                super.visitJumpInsn(GOTO, origDest);
                super.visitLabel(origFalseLoc);
                fn.accept(this);
                boxAtNextJump.clear();
            } else {
                Configuration.taintTagFactory.jumpOp(opcode, branchStarting, label, mv, lvs, this);
                branchStarting = DO_NOT_TRACK_BRANCH_STARTING;
            }
        } else {

            if(!boxAtNextJump.isEmpty() && opcode != Opcodes.GOTO) {
                Label origDest = label;
                Label newDest = new Label();
                Label origFalseLoc = new Label();
                Configuration.taintTagFactory.jumpOp(opcode, branchStarting, newDest, mv, lvs, this);
                branchStarting = DO_NOT_TRACK_BRANCH_STARTING;
                FrameNode fn = getCurrentFrameNode();
                fn.type = F_NEW;
                super.visitJumpInsn(GOTO, origFalseLoc);
                super.visitLabel(newDest);
                fn.accept(this);
                for(Integer var : boxAtNextJump) {
                    if(lvs.varToShadowVar.get(var) != null) {
                        super.visitVarInsn(ALOAD, lvs.varToShadowVar.get(var));
                        super.visitVarInsn(ASTORE, var);
                    }
                }
                super.visitJumpInsn(GOTO, origDest);
                super.visitLabel(origFalseLoc);
                fn.accept(this);
                boxAtNextJump.clear();
            } else {
                Configuration.taintTagFactory.jumpOp(opcode, branchStarting, label, mv, lvs, this);
                branchStarting = DO_NOT_TRACK_BRANCH_STARTING;
            }
        }
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label defaultLabel, Label... labels) {
        if(isIgnoreAllInstrumenting) {
            super.visitTableSwitchInsn(min, max, defaultLabel, labels);
            return;
        }
        //Need to remove taint
        if(TaintUtils.DEBUG_FRAMES) {
            System.out.println("Table switch shows: " + analyzer.stack + ", " + analyzer.locals);
        }
        if((Configuration.IMPLICIT_TRACKING || isImplicitLightTracking) && branchStarting != DO_NOT_TRACK_BRANCH_STARTING) {
            super.visitInsn(SWAP);
            super.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
            super.visitInsn(SWAP);
            super.visitVarInsn(ALOAD, controlTaintArray);
            callPushControlTaint(branchStarting, true);
            branchStarting = DO_NOT_TRACK_BRANCH_STARTING;
        } else if(Configuration.IMPLICIT_TRACKING || isImplicitLightTracking) {
            super.visitInsn(SWAP);
            super.visitInsn(POP); // Remove the taint tag
        }
        Configuration.taintTagFactory.tableSwitch(min, max, defaultLabel, labels, mv, lvs, this);
    }

    @Override
    public void visitLookupSwitchInsn(Label defaultLabel, int[] keys, Label[] labels) {
        if(isIgnoreAllInstrumenting) {
            super.visitLookupSwitchInsn(defaultLabel, keys, labels);
            return;
        }
        //Need to remove taint
        if((Configuration.IMPLICIT_TRACKING || isImplicitLightTracking) && branchStarting != DO_NOT_TRACK_BRANCH_STARTING) {
            super.visitInsn(SWAP);
            super.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
            super.visitInsn(SWAP);
            super.visitVarInsn(ALOAD, controlTaintArray);
            callPushControlTaint(branchStarting, true);
            branchStarting = DO_NOT_TRACK_BRANCH_STARTING;
        } else if(Configuration.IMPLICIT_TRACKING || isImplicitLightTracking) {
            super.visitInsn(SWAP);
            super.visitInsn(POP); // Remove the taint tag
        }
        Configuration.taintTagFactory.lookupSwitch(defaultLabel, keys, labels, mv, lvs, this);
    }


    @Override
    public void visitLineNumber(int line, Label start) {
        super.visitLineNumber(line, start);
        Configuration.taintTagFactory.lineNumberVisited(line);
    }

    /**
     * Visits a method instruction for the specified method.
     *
     * @param method the method to be visited
     */
    private void visit(TaintMethod method) {
        super.visitMethodInsn(method.getOpcode(), method.getOwner(), method.getName(), method.getDescriptor(), method.isInterface());
    }
}