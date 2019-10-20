package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.*;
import edu.columbia.cs.psl.phosphor.runtime.CharacterUtils;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.*;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Value;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class TaintLoadCoercer extends MethodVisitor implements Opcodes {

    private boolean ignoreExistingFrames;
    private InstOrUninstChoosingMV instOrUninstChoosingMV;
    private boolean aggressivelyReduceMethodSize;
    private boolean isImplicitLightTracking;

    TaintLoadCoercer(final String className, int access, final String name, final String desc, String signature, String[] exceptions, final MethodVisitor cmv, boolean ignoreExistingFrames, final InstOrUninstChoosingMV instOrUninstChoosingMV, boolean aggressivelyReduceMethodSize, final boolean isImplicitLightTracking) {
        super(Configuration.ASM_VERSION);
        this.mv = new UninstTaintLoadCoercerMN(className, access, name, desc, signature, exceptions, cmv);
        this.ignoreExistingFrames = ignoreExistingFrames;
        this.instOrUninstChoosingMV = instOrUninstChoosingMV;
        this.aggressivelyReduceMethodSize = aggressivelyReduceMethodSize;
        this.isImplicitLightTracking = isImplicitLightTracking;
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        if(owner.equals(Type.getInternalName(Character.class)) && (name.startsWith("codePointAt")
                || name.startsWith("toChars") || name.startsWith("codePointBefore") || name.startsWith("reverseBytes")
                || name.startsWith("toLowerCase") || name.startsWith("toTitleCase") || name.startsWith("toUpperCase"))) {
            super.visitMethodInsn(opcode, Type.getInternalName(CharacterUtils.class), name, descriptor, isInterface);
        } else {
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }
    }

    class UninstTaintLoadCoercerMN extends MethodNode {
        String className;
        MethodVisitor cmv;

        UninstTaintLoadCoercerMN(String className, int access, String name, String desc, String signature, String[] exceptions, MethodVisitor cmv) {
            super(Configuration.ASM_VERSION, access, name, desc, signature, exceptions);
            this.className = className;
            this.cmv = cmv;
        }

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
            Map<AbstractInsnNode, Value> liveValues = new HashMap<>();
            List<SinkableArrayValue> relevantValues = new LinkedList<>();
            Type[] args = Type.getArgumentTypes(desc);
            final int nargs = args.length + ((Opcodes.ACC_STATIC & access) == 0 ? 1 : 0);
            Analyzer<BasicValue> a = new Analyzer<BasicValue>(new InstMethodSinkInterpreter(relevantValues, liveValues)) {
                @Override
                protected Frame<BasicValue> newFrame(int nLocals, int nStack) {
                    return new PFrame(nLocals, nStack, nargs);
                }

                @Override
                protected Frame<BasicValue> newFrame(Frame<? extends BasicValue> src) {
                    return new PFrame(src);
                }
            };
            try {
                boolean canIgnoreTaintsTillEnd = true;
                Frame[] frames = a.analyze(className, this);
                AbstractInsnNode insn = this.instructions.getFirst();
                for(Frame<?> frame : frames) {
                    PFrame f = (PFrame) frame;
                    if(f == null) {
                        insn = insn.getNext();
                        continue;
                    }
                    switch(insn.getType()) {
                        case AbstractInsnNode.LINE:
                            break;
                        case AbstractInsnNode.INSN:
                            switch(insn.getOpcode()) {
                                case Opcodes.AASTORE:
                                case Opcodes.BASTORE:
                                case Opcodes.CASTORE:
                                case Opcodes.IASTORE:
                                case Opcodes.FASTORE:
                                case Opcodes.LASTORE:
                                case Opcodes.DASTORE:
                                case Opcodes.SASTORE:
                                    BasicValue value3 = f.getStack(f.getStackSize() - 1);
                                    BasicValue value1 = f.getStack(f.getStackSize() - 3);
                                    BasicValue value2 = f.getStack(f.getStackSize() - 2);
                                    if(Configuration.ARRAY_INDEX_TRACKING) {
                                        if(!(value2 instanceof SinkableArrayValue && ((SinkableArrayValue) value2).isConstant)) {
                                            if(value1 instanceof SinkableArrayValue && insn.getOpcode() != Opcodes.AASTORE) {
                                                relevantValues.addAll(((SinkableArrayValue) value1).tag(insn));
                                            }
                                            relevantValues.addAll(((SinkableArrayValue) value2).tag(insn));
                                        }
                                    }
                                    if(value1 instanceof SinkableArrayValue && value3 instanceof SinkableArrayValue) {
                                        if(((SinkableArrayValue) value1).isNewArray && ((SinkableArrayValue) value3).isConstant) {
                                            //Storing constant to new array - no need to tag, ever.
                                            break;
                                        }
                                    }
                                    if(value3 instanceof SinkableArrayValue && TaintUtils.isPrimitiveOrPrimitiveArrayType(value3.getType())) {
                                        relevantValues.addAll(((SinkableArrayValue) value3).tag(insn));
                                    }
                                    if(value1 instanceof SinkableArrayValue && TaintUtils.isPrimitiveOrPrimitiveArrayType(value1.getType())) {
                                        relevantValues.addAll(((SinkableArrayValue) value1).tag(insn));
                                    }
                                    break;
                                case Opcodes.ARETURN:
                                    BasicValue v = f.getStack(f.getStackSize() - 1);
                                    if(v instanceof SinkableArrayValue && (TaintUtils.isPrimitiveArrayType(v.getType()) || (v.getType() == null && TaintUtils.isPrimitiveArrayType(Type.getReturnType(desc))))) {
                                        relevantValues.addAll(((SinkableArrayValue) v).tag(insn));
                                    }
                                    break;
                                case Opcodes.IALOAD:
                                case Opcodes.SALOAD:
                                case Opcodes.CALOAD:
                                case Opcodes.BALOAD:
                                case Opcodes.LALOAD:
                                case Opcodes.DALOAD:
                                case Opcodes.FALOAD:
                                case Opcodes.AALOAD:
                                    if(Configuration.ARRAY_INDEX_TRACKING) {
                                        value1 = f.getStack(f.getStackSize() - 1);
                                        if(value1 instanceof SinkableArrayValue && !(((SinkableArrayValue) value1).isConstant
                                                || (((SinkableArrayValue) value1).copyOf != null)
                                                && ((SinkableArrayValue) value1).copyOf.isConstant)) {
                                            relevantValues.addAll(((SinkableArrayValue) value1).tag(insn));
                                        }
                                    }
                                    break;
                                case IRETURN:
                                case FRETURN:
                                case LRETURN:
                                case DRETURN:
                                    v = f.getStack(f.getStackSize() - 1);
                                    relevantValues.addAll(((SinkableArrayValue) v).tag(insn));
                                    break;
                            }
                        case AbstractInsnNode.VAR_INSN:
                            if(insn.getOpcode() == TaintUtils.ALWAYS_BOX_JUMP || insn.getOpcode() == TaintUtils.ALWAYS_AUTOBOX) {
                                VarInsnNode vinsn = ((VarInsnNode) insn);
                                BasicValue v = f.getLocal(vinsn.var);
                                if(v instanceof SinkableArrayValue && TaintUtils.isPrimitiveArrayType(v.getType())) {
                                    relevantValues.addAll(((SinkableArrayValue) v).tag(insn));
                                }
                            }
                            break;
                        case AbstractInsnNode.FIELD_INSN:
                            switch(insn.getOpcode()) {
                                case Opcodes.GETFIELD:
                                case Opcodes.GETSTATIC:
                                    canIgnoreTaintsTillEnd = false;
                                    break;
                                case Opcodes.PUTFIELD:
                                    canIgnoreTaintsTillEnd = false;
                                case Opcodes.PUTSTATIC:
                                    BasicValue value = f.getStack(f.getStackSize() - 1);
                                    Type vt = Type.getType(((FieldInsnNode) insn).desc);
                                    if(value instanceof SinkableArrayValue && !((SinkableArrayValue) value).flowsToInstMethodCall
                                            && ((TaintUtils.isPrimitiveOrPrimitiveArrayType(vt)
                                            || TaintUtils.isPrimitiveOrPrimitiveArrayType(value.getType()))
                                            || (TaintUtils.isPrimitiveOrPrimitiveArrayType(value.getType())
                                            && !TaintUtils.isPrimitiveOrPrimitiveArrayType(vt)))) {
                                        relevantValues.addAll(((SinkableArrayValue) value).tag(insn));
                                    }
                                    break;
                            }
                            break;
                        case AbstractInsnNode.TABLESWITCH_INSN:
                        case AbstractInsnNode.LOOKUPSWITCH_INSN:
                            if(Configuration.WITH_TAGS_FOR_JUMPS || isImplicitLightTracking) {
                                BasicValue value = f.getStack(f.getStackSize() - 1);
                                if(value instanceof SinkableArrayValue
                                        && !((SinkableArrayValue) value).flowsToInstMethodCall) {
                                    relevantValues.addAll(((SinkableArrayValue) value).tag(insn));
                                }
                            }
                            break;
                        case AbstractInsnNode.IINC_INSN:
                            if(Configuration.IMPLICIT_TRACKING || isImplicitLightTracking || Configuration.WITH_TAGS_FOR_JUMPS) {
                                IincInsnNode iinc = (IincInsnNode) insn;
                                BasicValue value = f.getLocal(iinc.var);
                                if(value instanceof SinkableArrayValue && !((SinkableArrayValue) value).flowsToInstMethodCall) {
                                    relevantValues.addAll(((SinkableArrayValue) value).tag(insn));
                                }
                            }
                            break;
                        case AbstractInsnNode.JUMP_INSN:
                            if(Configuration.WITH_TAGS_FOR_JUMPS || isImplicitLightTracking) {
                                switch(insn.getOpcode()) {
                                    case Opcodes.IFGE:
                                    case Opcodes.IFGT:
                                    case Opcodes.IFLE:
                                    case Opcodes.IFLT:
                                    case Opcodes.IFNE:
                                    case Opcodes.IFEQ:
                                        BasicValue value = f.getStack(f.getStackSize() - 1);
                                        if(value instanceof SinkableArrayValue && !((SinkableArrayValue) value).flowsToInstMethodCall) {
                                            relevantValues.addAll(((SinkableArrayValue) value).tag(insn));
                                        }
                                        break;
                                    case Opcodes.IF_ICMPEQ:
                                    case Opcodes.IF_ICMPLE:
                                    case Opcodes.IF_ICMPLT:
                                    case Opcodes.IF_ICMPGT:
                                    case Opcodes.IF_ICMPGE:
                                    case Opcodes.IF_ICMPNE:
                                        value = f.getStack(f.getStackSize() - 1);
                                        if(value instanceof SinkableArrayValue && !((SinkableArrayValue) value).flowsToInstMethodCall) {
                                            relevantValues.addAll(((SinkableArrayValue) value).tag(insn));
                                        }
                                        value = f.getStack(f.getStackSize() - 2);
                                        if(value instanceof SinkableArrayValue && !((SinkableArrayValue) value).flowsToInstMethodCall) {
                                            relevantValues.addAll(((SinkableArrayValue) value).tag(insn));
                                        }
                                        break;
                                }
                            }
                            break;
                        case AbstractInsnNode.INVOKE_DYNAMIC_INSN:
                            canIgnoreTaintsTillEnd = false;
                            InvokeDynamicInsnNode idin = (InvokeDynamicInsnNode) insn;
                            Type[] margs = Type.getArgumentTypes(idin.desc);
                            for(int j = 0; j < margs.length; j++) {
                                Object o = f.getStack(f.getStackSize() - (margs.length - j));
                                if(o instanceof SinkableArrayValue) {
                                    SinkableArrayValue v = (SinkableArrayValue) o;
                                    Type t = margs[j];
                                    //if we make an inst call and are passing a prim array type, then we need the tags
                                    if(TaintUtils.isPrimitiveOrPrimitiveArrayType(t) && !v.flowsToInstMethodCall) {
                                        relevantValues.addAll(v.tag(insn));
                                    }
                                    //regardless of the kind of call, if we upcast then we need it
                                    else if(t.getDescriptor().equals("Ljava/lang/Object;") && TaintUtils.isPrimitiveArrayType(v.getType()) && !v.flowsToInstMethodCall) {
                                        //TODO: when doing this for *uninst* arrays, it's ok to just leave as is and not patch
                                        relevantValues.addAll(v.tag(insn));
                                    }
                                }
                            }
                            break;
                        case AbstractInsnNode.METHOD_INSN:
                            canIgnoreTaintsTillEnd = false;
                            MethodInsnNode min = (MethodInsnNode) insn;
                            margs = Type.getArgumentTypes(((MethodInsnNode) insn).desc);
                            if(insn.getOpcode() != Opcodes.INVOKESTATIC) {
                                Object o = f.getStack(f.getStackSize() - margs.length - 1);
                                if(o instanceof SinkableArrayValue && ((SinkableArrayValue) o).getType() != null) {
                                    relevantValues.addAll(((SinkableArrayValue) o).tag(insn));
                                }
                            }
                            for(int j = 0; j < margs.length; j++) {
                                Object o = f.getStack(f.getStackSize() - (margs.length - j));
                                if(o instanceof SinkableArrayValue) {
                                    SinkableArrayValue v = (SinkableArrayValue) o;
                                    Type t = margs[j];
                                    //if we make an inst call and are passing a prim array type, then we need the tags
                                    if(TaintUtils.isPrimitiveOrPrimitiveArrayType(t) && !v.flowsToInstMethodCall) {
                                        relevantValues.addAll(v.tag(insn));
                                    }
                                    //regardless of the kind of call, if we upcast then we need it
                                    else if((t.getDescriptor().equals("Ljava/lang/Object;") || t.getDescriptor().equals("Ljava/io/Serializable;")) && (TaintUtils.isPrimitiveArrayType(v.getType())) && !v.flowsToInstMethodCall) {
                                        //TODO: when doing this for *uninst* arrays, it's ok to just leave as is and not patch
                                        relevantValues.addAll(v.tag(insn));
                                    } else if(t.getDescriptor().equals("Ljava/lang/Object;") && min.owner.equals("java/lang/System") && min.name.equals("arraycopy") && !v.flowsToInstMethodCall) {
                                        relevantValues.addAll(v.tag(insn));
                                    }
                                }
                            }
                            break;
                        case AbstractInsnNode.TYPE_INSN:
                            switch(insn.getOpcode()) {
                                case Opcodes.CHECKCAST:
                                    BasicValue value = f.getStack(f.getStackSize() - 1);
                                    if(value instanceof SinkableArrayValue && ((TypeInsnNode) insn).desc.startsWith("java/")
                                            && TaintUtils.isPrimitiveArrayType(value.getType())
                                            && !((SinkableArrayValue) value).flowsToInstMethodCall) {
                                        relevantValues.addAll(((SinkableArrayValue) value).tag(insn));
                                    }
                                    break;
                                case Opcodes.ANEWARRAY:
                                    if(Configuration.ARRAY_LENGTH_TRACKING) {
                                        relevantValues.addAll(((SinkableArrayValue) f.getStack(f.getStackSize() - 1)).tag(insn));
                                    }
                                    break;
                            }
                            break;
                        case AbstractInsnNode.MULTIANEWARRAY_INSN:
                            if(Configuration.ARRAY_LENGTH_TRACKING) {
                                MultiANewArrayInsnNode ins = (MultiANewArrayInsnNode) insn;
                                for(int j = 1; j <= ins.dims; j++) {
                                    relevantValues.addAll(((SinkableArrayValue) f.getStack(f.getStackSize() - j)).tag(insn));
                                }
                            }
                            break;
                    }
                    insn = insn.getNext();
                }
                if(this.instructions.size() > 20000 && !aggressivelyReduceMethodSize) {
                    if(canIgnoreTaintsTillEnd) {
                        insn = this.instructions.getFirst();
                        this.instructions.insert(new InsnNode(TaintUtils.IGNORE_EVERYTHING));
                        while(insn != null) {
                            if(insn.getOpcode() == PUTSTATIC) {
                                Type origType = Type.getType(((FieldInsnNode) insn).desc);
                                if(origType.getSort() == Type.ARRAY && (origType.getElementType().getSort() != Type.OBJECT
                                        || origType.getElementType().getInternalName().equals("java/lang/Object"))) {
                                    if(origType.getElementType().getSort() != Type.OBJECT) {
                                        Type wrappedType = MultiDTaintedArray.getTypeForType(origType);
                                        if(origType.getDimensions() == 1) {
                                            this.instructions.insertBefore(insn, new InsnNode(DUP));
                                            this.instructions.insertBefore(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArray.class), "boxIfNecessary", "(Ljava/lang/Object;)Ljava/lang/Object;", false));
                                            this.instructions.insertBefore(insn, new TypeInsnNode(CHECKCAST, wrappedType.getInternalName()));
                                            this.instructions.insertBefore(insn, new FieldInsnNode(PUTSTATIC, ((FieldInsnNode) insn).owner, ((FieldInsnNode) insn).name + TaintUtils.TAINT_FIELD, wrappedType.getDescriptor()));
                                        } else {
                                            ((FieldInsnNode) insn).desc = wrappedType.getDescriptor();
                                            this.instructions.insertBefore(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArray.class), "boxIfNecessary", "(Ljava/lang/Object;)Ljava/lang/Object;", false));
                                            this.instructions.insertBefore(insn, new TypeInsnNode(CHECKCAST, wrappedType.getInternalName()));
                                        }
                                    } else {
                                        this.instructions.insertBefore(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArray.class), "boxIfNecessary", "(Ljava/lang/Object;)Ljava/lang/Object;", false));
                                        this.instructions.insertBefore(insn, new TypeInsnNode(CHECKCAST, origType.getInternalName()));
                                    }
                                }
                            } else if(insn.getOpcode() == ARETURN) {
                                Type origType = Type.getReturnType(this.desc);
                                if(origType.getSort() == Type.ARRAY && (origType.getElementType().getSort() != Type.OBJECT || origType.getElementType().getInternalName().equals("java/lang/Object"))) {
                                    Type wrappedType = MultiDTaintedArray.getTypeForType(origType);
                                    this.instructions.insertBefore(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArray.class), "boxIfNecessary", "(Ljava/lang/Object;)Ljava/lang/Object;", false));
                                    this.instructions.insertBefore(insn, new TypeInsnNode(CHECKCAST, wrappedType.getInternalName()));
                                }
                            }
                            insn = insn.getNext();
                        }
                        super.visitEnd();
                        this.accept(cmv);
                        return;
                    }
                } else if(aggressivelyReduceMethodSize) {
                    //Not able to use these cheap tricks. Let's bail.
                    this.instructions.insert(new InsnNode(TaintUtils.IGNORE_EVERYTHING));
                    this.instructions.insert(new VarInsnNode(TaintUtils.IGNORE_EVERYTHING, 0));
                    instOrUninstChoosingMV.disableTainting();
                    super.visitEnd();
                    this.accept(cmv);
                    return;
                }
                Set<SinkableArrayValue> swapPop = new HashSet<>();
                insn = this.instructions.getFirst();
                for(Frame frame : frames) {
                    if(insn.getType() == AbstractInsnNode.FRAME && frame != null) {
                        FrameNode fn = (FrameNode) insn;
                        int analyzerFrameIdx = 0;
                        for(int j = 0; j < fn.local.size(); j++) {
                            Object valInExistingFrame = fn.local.get(j);
                            BasicValue calculatedVal = (BasicValue) frame.getLocal(analyzerFrameIdx);
                            if(calculatedVal instanceof SinkableArrayValue && ((SinkableArrayValue) calculatedVal).flowsToInstMethodCall &&
                                    (TaintAdapter.isPrimitiveStackType(valInExistingFrame) || valInExistingFrame == Opcodes.NULL
                                            || valInExistingFrame.equals("java/lang/Object")) && valInExistingFrame != Opcodes.TOP) {
                                ignoreExistingFrames = false;
                                if(valInExistingFrame.equals("java/lang/Object")) {
                                    fn.local.set(j, new TaggedValue(TaintUtils.getStackTypeForType(calculatedVal.getType())));
                                    // Look for any GOTO that might have had an AUTOBOX added to it because we thought that this was.
                                    // See SynchronizedBlockObjTagITCase for an example of where this is needed.
                                    // This is a terrible fix, a much better one would be to clean up that whole autobox
                                    // mess and move it into here, using the analyzer frames instead of the inFrames/outFrames
                                    removeAUTOBOXCallsForVar(j);
                                } else {
                                    fn.local.set(j, new TaggedValue(valInExistingFrame));
                                }
                            }
                            analyzerFrameIdx++;
                            if(valInExistingFrame == Opcodes.DOUBLE || valInExistingFrame == Opcodes.LONG) {
                                analyzerFrameIdx++;
                            }
                        }
                        for(int j = 0; j < fn.stack.size(); j++) {
                            Object l = fn.stack.get(j);
                            BasicValue v = (BasicValue) frame.getStack(j);
                            if(v instanceof SinkableArrayValue && ((SinkableArrayValue) v).flowsToInstMethodCall && (TaintAdapter.isPrimitiveStackType(l) || l == Opcodes.NULL || l.equals("java/lang/Object"))) {
                                fn.stack.set(j, new TaggedValue((ignoreExistingFrames ? TaintUtils.getStackTypeForType(v.getType()) : l)));
                            }
                        }
                    }
                    insn = insn.getNext();
                }
                Set<AbstractInsnNode> added = new HashSet<>();
                //Before we start mucking around with the instruction list, find the analyzed frame that exists at each relevant value source
                HashMap<SinkableArrayValue, Frame> framesAtValues = new HashMap<>();
                for(SinkableArrayValue v : relevantValues) {
                    if(v.getSrc() != null && framesAtValues.get(v) == null) {
                        if(this.instructions.contains(v.getSrc())) {
                            int k = this.instructions.indexOf(v.getSrc());
                            if(k < frames.length && frames[k] != null) {
                                framesAtValues.put(v, frames[k]);
                            }
                        }
                    }

                }
                for(SinkableArrayValue v : relevantValues) {
                    if(!added.add(v.getSrc())) {
                        continue;
                    }
                    if(this.instructions.contains(v.getSrc())) {
                        if(v.masterDup != null || !v.otherDups.isEmpty()) {
                            if(framesAtValues.containsKey(v)) {
                                Frame f = framesAtValues.get(v);
                                Object r = f.getStack(f.getStackSize() - 1);
                                if(!(r instanceof SinkableArrayValue)) {
                                    continue;
                                }
                            }
                            SinkableArrayValue masterDup = v;
                            if(v.masterDup != null) {
                                masterDup = v.masterDup;
                            }
                            int i = 0;

                            if(relevantValues.contains(masterDup)) {
                                //bottom one is relevant
                                this.instructions.insertBefore(v.getSrc(), new InsnNode(TaintUtils.DUP_TAINT_AT_0 + i));
                            }
                            i++;
                            if(v.getSrc().getOpcode() == Opcodes.DUP2_X1) {
                                i += 2;
                            }
                            for(SinkableArrayValue d : masterDup.otherDups) {
                                if(relevantValues.contains(d)) {
                                    this.instructions.insertBefore(v.getSrc(), new InsnNode(TaintUtils.DUP_TAINT_AT_0 + i));
                                }
                                i++;
                            }
                        } else {
                            if(v.getSrc().getOpcode() == Opcodes.DUP_X1) {
                                throw new IllegalStateException();
                            }
                            this.instructions.insertBefore(v.getSrc(), new InsnNode(TaintUtils.TRACKED_LOAD));
                        }
                    }
                }
                for(SinkableArrayValue v : swapPop) {
                    this.instructions.insert(v.getSrc(), new InsnNode(TaintUtils.IGNORE_EVERYTHING));
                    this.instructions.insert(v.getSrc(), new InsnNode(Opcodes.POP));
                    this.instructions.insert(v.getSrc(), new InsnNode(TaintUtils.IGNORE_EVERYTHING));
                }

            } catch(Throwable e) {
                e.printStackTrace();
                PrintWriter pw;
                try {
                    pw = new PrintWriter("lastMethod.txt");
                    TraceClassVisitor tcv = new TraceClassVisitor(null, new PhosphorTextifier(), pw);
                    this.accept(tcv);
                    tcv.visitEnd();
                    pw.flush();
                } catch(FileNotFoundException e1) {
                    e1.printStackTrace();
                }
                throw new IllegalStateException(e);
            }
            super.visitEnd();
            this.accept(cmv);
        }

        private void removeAUTOBOXCallsForVar(int j) {
            AbstractInsnNode insn = this.instructions.getFirst();
            AbstractInsnNode next;
            while(insn != null) {
                next = insn.getNext();
                if((insn.getOpcode() == TaintUtils.ALWAYS_AUTOBOX || insn.getOpcode() == TaintUtils.ALWAYS_BOX_JUMP
                        || insn.getOpcode() == TaintUtils.ALWAYS_UNBOX_JUMP) && insn.getType() == AbstractInsnNode.VAR_INSN
                        && ((VarInsnNode) insn).var == j) {
                    this.instructions.remove(insn);
                    // Need to maintain the instruction list size/indexing, so add a NOP
                    this.instructions.insertBefore(next, new InsnNode(Opcodes.NOP));
                }
                insn = next;
            }
        }
    }

    public static void main(String[] args) throws Throwable {
        Configuration.IMPLICIT_TRACKING = true;
        Configuration.IMPLICIT_EXCEPTION_FLOW = true;
//		Configuration.IMPLICIT_LIGHT_TRACKING = true;
//		Configuration.ARRAY_LENGTH_TRACKING = true;
//		Configuration.ARRAY_INDEX_TRACKING = true;
//		Configuration.ANNOTATE_LOOPS = true;
        ClassReader cr = new ClassReader(new FileInputStream("z.class"));
        final String className = cr.getClassName();
        PrintWriter pw = new PrintWriter("z.txt");
        TraceClassVisitor tcv = new TraceClassVisitor(null, new PhosphorTextifier(), pw);
        ClassWriter cw1 = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {
            @Override
            protected String getCommonSuperClass(String arg0, String arg1) {
                try {
                    return super.getCommonSuperClass(arg0, arg1);
                } catch(Throwable t) {
                    return "java/lang/Object";
                }
            }
        };
        cr.accept(new ClassVisitor(Configuration.ASM_VERSION, cw1) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                mv = new JSRInlinerAdapter(mv, access, name, desc, signature, exceptions);
                return mv;
            }
        }, ClassReader.EXPAND_FRAMES);
        cr = new ClassReader(cw1.toByteArray());
        ClassVisitor cv = new ClassVisitor(Configuration.ASM_VERSION, tcv) {
            String className;
            Set<FieldNode> fields = new HashSet<>();

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                super.visit(version, access, name, signature, superName, interfaces);
                this.className = name;
            }

            @Override
            public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
                fields.add(new FieldNode(access, name, desc, signature, value));
                return super.visitField(access, name, desc, signature, value);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                mv = new MethodVisitor(Configuration.ASM_VERSION, mv) {
                    @Override
                    public void visitInsn(int opcode) {
                        super.visitInsn(opcode);
                    }
                };
//				mv = new SpecialOpcodeRemovingMV(mv,false,className,false);
//				NeverNullArgAnalyzerAdapter analyzer = new NeverNullArgAnalyzerAdapter(className, access, className, desc, mv);
                mv = new TaintLoadCoercer(className, access, name, desc, signature, exceptions, mv, true, null, false, false);
//				LocalVariableManager lvs = new LocalVariableManager(access, desc, mv, analyzer, mv, false);
//				mv = lvs;
                PrimitiveArrayAnalyzer paa = new PrimitiveArrayAnalyzer(className, access, name, desc, signature, exceptions, mv, false);
                NeverNullArgAnalyzerAdapter an = new NeverNullArgAnalyzerAdapter(className, access, name, desc, paa);
                paa.setAnalyzer(an);
//				((PrimitiveArrayAnalyzer) mv).setAnalyzer(an);
                mv = an;
                return mv;
            }
        };
        cr.accept(cv, ClassReader.EXPAND_FRAMES);
        pw.flush();

    }
}
