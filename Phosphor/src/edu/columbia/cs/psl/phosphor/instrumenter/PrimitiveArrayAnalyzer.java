package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.PhosphorInstructionInfo;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.BasicArrayInterpreter;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.BindingControlFlowAnalyzer;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.ReferenceArrayTarget;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.graph.BaseControlFlowGraphCreator;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.graph.BasicBlock;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.graph.FlowGraph;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.graph.FlowGraph.NaturalLoop;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.graph.SimpleBasicBlock;
import edu.columbia.cs.psl.phosphor.struct.Field;
import edu.columbia.cs.psl.phosphor.struct.SinglyLinkedList;
import edu.columbia.cs.psl.phosphor.struct.TaintedReferenceWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.*;

import java.util.Iterator;
import java.util.Objects;

public class PrimitiveArrayAnalyzer extends MethodVisitor {

    static final boolean DEBUG = false;
    public MethodNode mn;
    boolean isEmptyMethod = true;
    Set<Type> wrapperTypesToPreAlloc = new HashSet<>();
    int nJumps;
    int nTryCatch;
    int nThrow;
    private NeverNullArgAnalyzerAdapter analyzer;
    private Map<Label, List<Label>> newLabels = new HashMap<>();
    private boolean isImplicitLightTracking;
    private Label newFirstLabel = new Label();
    private Label oldFirstLabel;
    private boolean fixLDCClass;

    public PrimitiveArrayAnalyzer(final String className, int access, final String name, final String desc, String signature, String[] exceptions, final MethodVisitor cmv, final boolean isImplicitLightTracking, final boolean fixLDCClass) {
        super(Configuration.ASM_VERSION);
        this.mn = new PrimitiveArrayAnalyzerMN(access, name, desc, signature, exceptions, className, cmv);
        this.mv = mn;
        this.isImplicitLightTracking = isImplicitLightTracking;
        this.fixLDCClass = fixLDCClass;
    }

    public PrimitiveArrayAnalyzer(Type singleWrapperTypeToAdd) {
        super(Configuration.ASM_VERSION);
        this.mv = new PrimitiveArrayAnalyzerMN(0, null, null, null, null, null, null);
        if(singleWrapperTypeToAdd.getSort() == Type.OBJECT && singleWrapperTypeToAdd.getInternalName().startsWith("edu/columbia/cs/psl/phosphor/struct/Tainted")) {
            this.wrapperTypesToPreAlloc.add(singleWrapperTypeToAdd);
        }
    }

    /**
     * Returns the immediate post-dominator of the specified block if one the specified block's post-dominators can be
     * reached from the block along successor edges. If such a post-dominator cannot be found, returns null and
     * ensures that the specified visited set contains all of the blocks reachable from the specified block using
     * successor edges.
     */
    private AnnotatedInstruction findImmediatePostDominator(AnnotatedInstruction block, HashSet<AnnotatedInstruction> visited) {
        SinglyLinkedList<AnnotatedInstruction> queue = new SinglyLinkedList<>();
        queue.enqueue(block);
        visited.add(block);
        while(!queue.isEmpty()) {
            AnnotatedInstruction cur = queue.dequeue();
            if(!cur.equals(block) && block.postDominators.contains(cur)) {
                return cur; // Found a post-dominator, first one found using BFS has shortest path
            } else {
                for(AnnotatedInstruction successor : cur.successors) {
                    if(visited.add(successor)) {
                        queue.enqueue(successor);
                    }
                }
            }
        }
        return null;
    }

    /* Returns whether the specified instruction triggers a method exit. */
    private boolean isExitInstruction(AbstractInsnNode instruction) {
        int opcode = instruction.getOpcode();
        return TaintUtils.isReturnOpcode(opcode) || opcode == Opcodes.ATHROW;
    }

    private boolean mightEndBlock(AbstractInsnNode insn) {
        return insn.getType() == AbstractInsnNode.LABEL || isExitInstruction(insn) || insn.getOpcode() == Opcodes.GOTO;
    }

    @Override
    public void visitLdcInsn(Object value) {
        super.visitLdcInsn(value);
        if(value instanceof Type && fixLDCClass) {
            wrapperTypesToPreAlloc.add(Type.getType(TaintedReferenceWithObjTag.class));
        }
    }

    @Override
    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
        super.visitMultiANewArrayInsn(descriptor, numDimensions);
        if(fixLDCClass) {
            wrapperTypesToPreAlloc.add(Type.getType(TaintedReferenceWithObjTag.class));
        }
    }

    @Override
    public void visitInsn(int opcode) {
        switch(opcode) {
            case Opcodes.FADD:
            case Opcodes.FREM:
            case Opcodes.FSUB:
            case Opcodes.FMUL:
            case Opcodes.FDIV:
            case Opcodes.I2F:
            case Opcodes.L2F:
            case Opcodes.D2F:
                if(Configuration.PREALLOC_STACK_OPS) {
                    wrapperTypesToPreAlloc.add(TaintUtils.getContainerReturnType("F"));
                }
                break;
            case Opcodes.DADD:
            case Opcodes.DSUB:
            case Opcodes.DMUL:
            case Opcodes.DDIV:
            case Opcodes.DREM:
            case Opcodes.DALOAD:
                wrapperTypesToPreAlloc.add(TaintUtils.getContainerReturnType("D"));
                break;
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
            case Opcodes.LALOAD:
                wrapperTypesToPreAlloc.add(TaintUtils.getContainerReturnType("J"));
                break;
            case Opcodes.LCMP:
            case Opcodes.DCMPL:
            case Opcodes.DCMPG:
            case Opcodes.FCMPG:
            case Opcodes.FCMPL:
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
            case Opcodes.IALOAD:
                wrapperTypesToPreAlloc.add(TaintUtils.getContainerReturnType("I"));
                break;
            case Opcodes.BALOAD:
                wrapperTypesToPreAlloc.add(TaintUtils.getContainerReturnType("B"));
                wrapperTypesToPreAlloc.add(TaintUtils.getContainerReturnType("Z"));
                break;
            case Opcodes.CALOAD:
                wrapperTypesToPreAlloc.add(TaintUtils.getContainerReturnType("C"));
                break;
            case Opcodes.FALOAD:
                wrapperTypesToPreAlloc.add(TaintUtils.getContainerReturnType("F"));
                break;
            case Opcodes.SALOAD:
                wrapperTypesToPreAlloc.add(TaintUtils.getContainerReturnType("S"));
                break;
            case Opcodes.AALOAD:
                String arrayType = "[Ljava/lang/Object;";
                Object onStack = analyzer.stack.get(analyzer.stack.size() - 2);
                if(onStack instanceof String) {
                    arrayType = (String) onStack;
                }
                super.visitLdcInsn(new ReferenceArrayTarget(arrayType));
                wrapperTypesToPreAlloc.add(Type.getType(TaintedReferenceWithObjTag.class));
                break;
            case Opcodes.I2C:
                if(Configuration.PREALLOC_STACK_OPS) {
                    wrapperTypesToPreAlloc.add(TaintUtils.getContainerReturnType("C"));
                }
                break;
            case Opcodes.I2B:
                if(Configuration.PREALLOC_STACK_OPS) {
                    wrapperTypesToPreAlloc.add(TaintUtils.getContainerReturnType("B"));
                }
                break;
            case Opcodes.I2D:
            case Opcodes.F2D:
            case Opcodes.L2D:
                if(Configuration.PREALLOC_STACK_OPS) {
                    wrapperTypesToPreAlloc.add(TaintUtils.getContainerReturnType("D"));
                }
                break;
            case Opcodes.I2L:
            case Opcodes.F2L:
            case Opcodes.D2L:
                if(Configuration.PREALLOC_STACK_OPS) {
                    wrapperTypesToPreAlloc.add(TaintUtils.getContainerReturnType("J"));
                }
                break;
            case Opcodes.I2S:
                if(Configuration.PREALLOC_STACK_OPS) {
                    wrapperTypesToPreAlloc.add(TaintUtils.getContainerReturnType("S"));
                }
                break;
            case Opcodes.F2I:
            case Opcodes.L2I:
            case Opcodes.D2I:
                if(Configuration.PREALLOC_STACK_OPS) {
                    wrapperTypesToPreAlloc.add(TaintUtils.getContainerReturnType("I"));
                }
                break;
            case Opcodes.ATHROW:
                nThrow++;
                break;
        }
        super.visitInsn(opcode);
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String desc, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
        super.visitInvokeDynamicInsn(name, desc, bootstrapMethodHandle, bootstrapMethodArguments);
        Type returnType = Type.getReturnType(desc);
        Type newReturnType = TaintUtils.getContainerReturnType(returnType);
        isEmptyMethod = false;
        if(newReturnType != returnType) {
            wrapperTypesToPreAlloc.add(newReturnType);
        }
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isInterface) {
        super.visitMethodInsn(opcode, owner, name, desc, isInterface);
        Type returnType = Type.getReturnType(desc);
        Type newReturnType = TaintUtils.getContainerReturnType(returnType);
        isEmptyMethod = false;
        if(newReturnType != returnType) {
            wrapperTypesToPreAlloc.add(newReturnType);
        }
    }

    public void setAnalyzer(NeverNullArgAnalyzerAdapter preAnalyzer) {
        analyzer = preAnalyzer;
    }

    private Label addUniqueLabelFor(Label existing) {
        Label ret = new Label();
        if(!newLabels.containsKey(existing)) {
            newLabels.put(existing, new LinkedList<>());
        }
        newLabels.get(existing).add(ret);
        return ret;
    }

    @Override
    public void visitLabel(Label label) {
        super.visitLabel(label);
        List<Label> labels = this.newLabels.remove(label);
        if(labels != null) {
            for(Label l : labels) {
                super.visitLabel(l);
            }
        }
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        super.visitTryCatchBlock(addUniqueLabelFor(start), end, handler, type);
        nTryCatch++;
    }

    private static void patchFrames(Collection<AnnotatedInstruction> annotatedInstructions, InsnList instructions) {
        for(AnnotatedInstruction annotatedInstruction : annotatedInstructions) {
            patchFrames(annotatedInstruction, instructions);
        }
    }

    private static void patchFrames(AnnotatedInstruction annotatedInstruction, InsnList instructions) {
        AbstractInsnNode insn = annotatedInstruction.insn;
        while(insn.getType() == AbstractInsnNode.FRAME || insn.getType() == AbstractInsnNode.LINE
                || insn.getType() == AbstractInsnNode.LABEL || insn.getOpcode() > 200) {
            insn = insn.getNext();
        }
        if(insn.getOpcode() == Opcodes.NEW) {
            // Need to patch all frames to have the correct label in them :'(
            AbstractInsnNode i = insn;
            while(i != null && i.getType() != AbstractInsnNode.LABEL) {
                i = i.getPrevious();
            }
            LinkedList<LabelNode> oldLabels = new LinkedList<>();
            oldLabels.add(((LabelNode) i));
            if(i != null && i.getPrevious() != null && i.getPrevious().getType() == AbstractInsnNode.LABEL) {
                oldLabels.add(((LabelNode) i.getPrevious()));
            }
            LabelNode newLabel = new LabelNode(new Label());
            instructions.insertBefore(insn, newLabel);
            i = instructions.getFirst();
            while(i != null) {
                if(i instanceof FrameNode) {
                    FrameNode fr = (FrameNode) i;
                    for(int j = 0; j < fr.stack.size(); j++) {
                        if(oldLabels.contains(fr.stack.get(j))) {
                            fr.stack.set(j, newLabel.getLabel());
                        }
                    }
                }
                i = i.getNext();
            }
        }
    }

    private static void annotateLoops(MethodNode mn) {
        FlowGraph<BasicBlock> cfg = new BaseControlFlowGraphCreator().createControlFlowGraph(mn);
        for(NaturalLoop<BasicBlock> loop : cfg.getNaturalLoops()) {
            AbstractInsnNode header = loop.getHeader().getLastInsn();
            if(loop.getHeader() instanceof SimpleBasicBlock) {
                mn.instructions.insertBefore(header, new InsnNode(TaintUtils.LOOP_HEADER));
            }
        }
    }

    final class PrimitiveArrayAnalyzerMN extends MethodNode {
        private final String className;
        private final boolean shouldTrackExceptions;
        private final MethodVisitor cmv;
        int curLabel = 0;
        Map<Integer, AnnotatedInstruction> implicitAnalysisBlocks = new HashMap<>();

        PrimitiveArrayAnalyzerMN(int access, String name, String desc, String signature, String[] exceptions, String className, MethodVisitor cmv) {
            super(Configuration.ASM_VERSION, access, name, desc, signature, exceptions);
            this.className = className;
            this.cmv = cmv;
            shouldTrackExceptions = Configuration.IMPLICIT_EXCEPTION_FLOW;
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
        public void visitCode() {
            if(DEBUG) {
                System.out.println("Visiting: " + className + "." + name + desc);
            }
            super.visitCode();
            visitLabel(newFirstLabel);
        }

        @Override
        public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
            if(start == oldFirstLabel) {
                start = newFirstLabel;
            }
            super.visitLocalVariable(name, descriptor, signature, start, end, index);
        }

        @Override
        public void visitLabel(Label label) {
            if(oldFirstLabel == null && label != newFirstLabel) {
                oldFirstLabel = label;
            }
            super.visitLabel(label);
            curLabel++;
        }

        public void visitJumpInsn(int opcode, Label label) {
            super.visitJumpInsn(opcode, label);
            int nToPop = 0;
            switch(opcode) {
                case Opcodes.IFEQ:
                case Opcodes.IFNE:
                case Opcodes.IFLT:
                case Opcodes.IFGE:
                case Opcodes.IFGT:
                case Opcodes.IFLE:
                case Opcodes.IFNULL:
                case Opcodes.IFNONNULL:
                    //pop 1
                    nToPop = 1;
                    break;
                case Opcodes.IF_ICMPEQ:
                case Opcodes.IF_ICMPNE:
                case Opcodes.IF_ICMPLT:
                case Opcodes.IF_ICMPGE:
                case Opcodes.IF_ICMPGT:
                case Opcodes.IF_ICMPLE:
                case Opcodes.IF_ACMPEQ:
                case Opcodes.IF_ACMPNE:
                    //pop 2
                    nToPop = 2;
                    break;
                case Opcodes.GOTO:
                    //pop none
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        }

        @Override
        public void visitEnd() {

            final HashMap<AbstractInsnNode, String> referenceArraysToCheckcast = new HashMap<>();
            @SuppressWarnings("unchecked")
            Analyzer a = new Analyzer(new BasicArrayInterpreter((this.access & Opcodes.ACC_STATIC) != 0, isImplicitLightTracking, referenceArraysToCheckcast)) {
                protected int[] insnToLabel;
                HashMap<Integer, LinkedList<Integer>> edges = new HashMap<>();
                LinkedList<Integer> varsStoredThisInsn = new LinkedList<>();
                HashSet<String> visited = new HashSet<>();
                int insnIdxOrderVisited = 0;

                int getLabel(int insn) {
                    int label = -1;
                    for(int j = 0; j <= insn; j++) {
                        label = insnToLabel[j];
                    }
                    return label;
                }

                int getInsnAfterFrameFor(int insn) {
                    int r = 0;
                    for(int i = 0; i < insn; i++) {
                        if(instructions.get(i).getType() == AbstractInsnNode.FRAME) {
                            r = i + 1;
                        }
                    }
                    return r;
                }

                int getLastInsnByLabel(int label) {
                    int r = 0;
                    for(int j = 0; j < insnToLabel.length; j++) {
                        if(insnToLabel[j] == label) {
                            if(instructions.get(j).getType() == AbstractInsnNode.FRAME) {
                                continue;
                            }
                            r = j;
                        }
                    }
                    return r;
                }

                int getFirstInsnByLabel(int label) {
                    for(int j = 0; j < insnToLabel.length; j++) {
                        if(insnToLabel[j] == label) {
                            if(instructions.get(j).getType() == AbstractInsnNode.FRAME || instructions.get(j).getType() == AbstractInsnNode.LABEL
                                    || instructions.get(j).getType() == AbstractInsnNode.LINE) {
                                continue;
                            }
                            return j;
                        }
                    }
                    return -1;
                }

                protected Frame newFrame(int nLocals, int nStack) {
                    return new Frame(nLocals, nStack) {
                        @Override
                        public void execute(AbstractInsnNode insn, Interpreter interpreter) throws AnalyzerException {
                            if(insn.getOpcode() > 200 || (insn instanceof LdcInsnNode && ((LdcInsnNode) insn).cst instanceof PhosphorInstructionInfo)) {
                                return;
                            }
                            super.execute(insn, interpreter);
                        }
                    };
                }

                @Override
                public Frame[] analyze(String owner, MethodNode m) throws AnalyzerException {
                    Iterator<AbstractInsnNode> insns = m.instructions.iterator();
                    insnToLabel = new int[m.instructions.size()];
                    int label = -1;
                    boolean isFirst = true;
                    while(insns.hasNext()) {
                        AbstractInsnNode nextInsn = insns.next();
                        int idx = m.instructions.indexOf(nextInsn);
                        if(nextInsn instanceof LabelNode) {
                            label++;
                        }
                        insnToLabel[idx] = (isFirst ? 1 : label);
                        isFirst = false;
                    }
                    Frame[] ret = super.analyze(owner, m);
                    if(shouldTrackExceptions) {
                        insns = m.instructions.iterator();
                        while(insns.hasNext()) {
                            AbstractInsnNode insn = insns.next();
                            int idx = m.instructions.indexOf(insn);
                            if(insn.getOpcode() == Opcodes.ATHROW) {
                                //Are we in a try/catch block that can catch this?
                                for(TryCatchBlockNode each : m.tryCatchBlocks) {
                                    try {
                                        Class<?> caught = Class.forName((each.type == null ? "java.lang.Throwable" : each.type.replace('/', '.')), false, PrimitiveArrayAnalyzer.class.getClassLoader());
                                        if(caught == Throwable.class) {
                                            //if catching Throwable, we'll catch this regardless of whether we can load the exception type or not
                                            newControlFlowEdge(idx, m.instructions.indexOf(each.handler));
                                            break;
                                        }
                                        Class<?> thrown;
                                        try {
                                            String onStack = ret[idx].getStack(ret[idx].getStackSize() - 1).toString();
                                            if(!onStack.startsWith("L")) {
                                                continue;
                                            }
                                            thrown = Class.forName(Type.getType(ret[idx].getStack(0).toString()).getClassName(), false, PrimitiveArrayAnalyzer.class.getClassLoader());
                                        } catch(Throwable t) {
                                            continue;
                                        }
                                        if(caught.isAssignableFrom(thrown)) {
                                            //Found a handler for this thrown
                                            newControlFlowEdge(idx, m.instructions.indexOf(each.handler));
                                            break;
                                        }
                                    } catch(Throwable t) {
                                        //Maybe can't load exception type, that's ok
                                    }
                                }
                            }
                        }
                    }

                    return ret;
                }

                @Override
                protected boolean newControlFlowExceptionEdge(int insnIndex, int successorIndex) {
                    return true;
                }

                @Override
                protected void newControlFlowEdge(int insn, int successor) {
                    newControlFlowEdge(insn, successor, false);
                }

                void newControlFlowEdge(int insn, int successor, boolean isExceptionalEdge) {
                    if(visited.contains(insn + "-" + successor)) {
                        return;
                    }
                    visited.add(insn + "-" + successor);
                    if(!edges.containsKey(successor)) {
                        edges.put(successor, new LinkedList<>());
                    }
                    if(!edges.get(successor).contains(insn)) {
                        edges.get(successor).add(insn);
                    }
                    AnnotatedInstruction fromBlock;
                    if(!implicitAnalysisBlocks.containsKey(insn)) {
                        //insn not added yet
                        fromBlock = new AnnotatedInstruction();
                        fromBlock.idx = insn;
                        fromBlock.insn = instructions.get(insn);
                        implicitAnalysisBlocks.put(insn, fromBlock);
                    } else {
                        fromBlock = implicitAnalysisBlocks.get(insn);
                    }

                    AbstractInsnNode insnN = instructions.get(insn);
                    fromBlock.isJump = (insnN.getType() == AbstractInsnNode.JUMP_INSN && insnN.getOpcode() != Opcodes.GOTO)
                            || insnN.getType() == AbstractInsnNode.LOOKUPSWITCH_INSN || insnN.getType() == AbstractInsnNode.TABLESWITCH_INSN;
                    if(fromBlock.isJump && insnN.getType() == AbstractInsnNode.JUMP_INSN) {
                        switch(insnN.getOpcode()) {
                            case Opcodes.IF_ICMPEQ:
                            case Opcodes.IF_ICMPNE:
                            case Opcodes.IF_ICMPGE:
                            case Opcodes.IF_ICMPGT:
                            case Opcodes.IF_ICMPLT:
                            case Opcodes.IF_ICMPLE:
                            case Opcodes.IF_ACMPEQ:
                            case Opcodes.IF_ACMPNE:
                                fromBlock.isTwoOperandJumpInstruction = true;
                                break;
                        }
                    }
                    AnnotatedInstruction successorBlock;
                    if(implicitAnalysisBlocks.containsKey(successor)) {
                        successorBlock = implicitAnalysisBlocks.get(successor);
                    } else {
                        successorBlock = new AnnotatedInstruction();
                        successorBlock.idx = successor;
                        successorBlock.insn = instructions.get(successor);
                        implicitAnalysisBlocks.put(successor, successorBlock);
                        if(successorBlock.insn.getType() == AbstractInsnNode.IINC_INSN) {
                            successorBlock.varsWritten.add(new LVAccess(((IincInsnNode) successorBlock.insn).var, "I"));
                        } else if(successorBlock.insn.getType() == AbstractInsnNode.VAR_INSN) {
                            switch(successorBlock.insn.getOpcode()) {
                                case ISTORE:
                                    successorBlock.varsWritten.add(new LVAccess(((VarInsnNode) successorBlock.insn).var, "I"));
                                    break;
                                case ASTORE:
                                    successorBlock.varsWritten.add(new LVAccess(((VarInsnNode) successorBlock.insn).var, "Ljava/lang/Object;"));
                                    break;
                                case DSTORE:
                                    successorBlock.varsWritten.add(new LVAccess(((VarInsnNode) successorBlock.insn).var, "D"));
                                    break;
                                case LSTORE:
                                    successorBlock.varsWritten.add(new LVAccess(((VarInsnNode) successorBlock.insn).var, "J"));
                                    break;
                            }
                        } else if(successorBlock.insn.getType() == AbstractInsnNode.FIELD_INSN) {
                            FieldInsnNode fin = (FieldInsnNode) successorBlock.insn;
                            if(fin.getOpcode() == Opcodes.PUTFIELD) {
                                Frame fr = this.getFrames()[successor];
                                if(fr != null && fr.getStack(fr.getStackSize() - 2) == BasicArrayInterpreter.THIS_VALUE) {
                                    successorBlock.fieldsWritten.add(new Field(false, fin.owner, fin.name, fin.desc));
                                }
                            } else if(fin.getOpcode() == Opcodes.PUTSTATIC) {
                                successorBlock.fieldsWritten.add(new Field(true, fin.owner, fin.name, fin.desc));
                            }
                        } else if(successorBlock.insn.getType() == AbstractInsnNode.METHOD_INSN) {
                            MethodInsnNode min = (MethodInsnNode) successorBlock.insn;
                            if(min.getOpcode() == INVOKEVIRTUAL || min.getOpcode() == INVOKESPECIAL) {
                                Type[] desc = Type.getArgumentTypes(min.desc);
                                if((desc.length == 1 && (Type.getReturnType(min.desc).getSort() == Type.VOID || min.desc.equals("Ljava/lang/StringBuilder;")))
                                        || (desc.length == 2 && Type.getReturnType(min.desc).getSort() == Type.VOID && min.name.startsWith("set"))) {
                                    Frame fr = this.getFrames()[successor];
                                    if(fr != null && fr.getStack(fr.getStackSize() - 2) instanceof BasicArrayInterpreter.BasicThisFieldValue) {
                                        BasicArrayInterpreter.BasicThisFieldValue vv = (BasicArrayInterpreter.BasicThisFieldValue) fr.getStack(fr.getStackSize() - 2);
                                        successorBlock.fieldsWritten.add(vv.getField());
                                    }
                                }
                            }
                        } else if(successorBlock.insn.getOpcode() == Opcodes.ATHROW) {
                            BasicValue ex = (BasicValue) this.getFrames()[successor].getStack(0);
                            if(shouldTrackExceptions && ex != null && ex.getType() != null && (ex.getType().getDescriptor().contains("Exception") || ex.getType().getDescriptor().contains("Error"))) {
                                successorBlock.exceptionsThrown.add(ex.getType().getInternalName() + "#" + successor);
                            }
                        }
                    }
                    fromBlock.successors.add(successorBlock);
                    successorBlock.predecessors.add(fromBlock);
                    if(fromBlock.isJump) {
                        if(fromBlock.covered) {
                            successorBlock.onTrueSideOfJumpFrom.add(fromBlock);
                        } else {
                            successorBlock.onFalseSideOfJumpFrom.add(fromBlock);
                            fromBlock.covered = true;
                        }
                    }
                    super.newControlFlowEdge(insn, successor);
                }
            };
            try {
                Frame[] frames = a.analyze(className, this);
                for(int i = 0; i < instructions.size(); i++) {
                    if(frames[i] == null) {
                        //TODO dead code elimination.
                        //This should be done more generically
                        //But, this worked for JDT's stupid bytecode, so...
                        AbstractInsnNode insn = instructions.get(i);
                        //while (insn != null && insn.getType() != AbstractInsnNode.LABEL) {
                        if(insn.getOpcode() == Opcodes.ATHROW || insn.getOpcode() == Opcodes.GOTO || (insn.getOpcode() >= Opcodes.IRETURN && insn.getOpcode() <= Opcodes.RETURN)) {
                            instructions.insertBefore(insn, new InsnNode(Opcodes.ATHROW));
                            instructions.remove(insn);
                            break;
                        } else if(insn instanceof FrameNode) {
                            FrameNode fn = (FrameNode) insn;
                            fn.local = java.util.Collections.emptyList();
                            fn.stack = java.util.Collections.singletonList("java/lang/Throwable");
                        } else if(!(insn instanceof LineNumberNode) && !(insn instanceof LabelNode)) {
                            instructions.insertBefore(insn, new InsnNode(Opcodes.NOP));
                            instructions.remove(insn);
                        }
                    }
                }
            } catch(Throwable e) {
                System.err.println("While analyzing " + className);
                e.printStackTrace();
            }
            if(Configuration.ANNOTATE_LOOPS) {
                annotateLoops(this);
            }
            if(Configuration.BINDING_CONTROL_FLOWS_ONLY) {
                try {
                    nJumps = BindingControlFlowAnalyzer.analyzeAndModify(className, this);
                } catch(AnalyzerException e) {
                    nJumps = 0;
                }
                patchFrames(implicitAnalysisBlocks.values(), instructions);
            } else if(Configuration.IMPLICIT_TRACKING || isImplicitLightTracking) {
                annotateCodeForControlFlow();
            }

            this.maxStack += 100;

            for(Map.Entry<AbstractInsnNode, String> each : referenceArraysToCheckcast.entrySet()) {
                AbstractInsnNode existing = each.getKey().getPrevious();
                if(existing instanceof LdcInsnNode) {
                    Object cst = ((LdcInsnNode) existing).cst;
                    if(cst instanceof ReferenceArrayTarget) {
                        if(!((ReferenceArrayTarget) cst).getOriginalArrayType().equals("[Ljava/lang/Object;")) {
                            continue;
                        }
                    }
                }
                instructions.insertBefore(each.getKey(), new LdcInsnNode(new ReferenceArrayTarget(each.getValue())));
            }

            AbstractInsnNode insn = instructions.getFirst();
            while(insn != null) {
                if(insn.getType() == AbstractInsnNode.FRAME) {
                    //Insert a note before the instruction before this guy
                    AbstractInsnNode insertBefore = insn;
                    while(insertBefore != null && (insertBefore.getType() == AbstractInsnNode.FRAME || insertBefore.getType() == AbstractInsnNode.LINE
                            || insertBefore.getType() == AbstractInsnNode.LABEL)) {
                        insertBefore = insertBefore.getPrevious();
                    }
                    if(insertBefore != null) {
                        this.instructions.insertBefore(insertBefore, new InsnNode(TaintUtils.FOLLOWED_BY_FRAME));
                    }
                }
                insn = insn.getNext();
            }
            this.accept(cmv);
        }

        /*
        This method should:
            BRANCH_START, BRANCH_END signals to the instruction stream

            For branch not taken:
                Add FORCE_CONTROL_STORE for:
                       Item at top of stack (add an InsnNode)
                       Local variables
                       Fields of "this"
                       Static fields
         */
        private void annotateCodeForControlFlow() {
            boolean hasJumps = false;
            HashSet<AnnotatedInstruction> tryCatchHandlers = new HashSet<>();
            if(shouldTrackExceptions && nTryCatch > 0) {
                int exceptionHandlerCount = 1;
                hasJumps = true;
                for(Object o : tryCatchBlocks) {
                    TryCatchBlockNode t = (TryCatchBlockNode) o;
                    AnnotatedInstruction startBlock = null;
                    AnnotatedInstruction handlerBlock = null;
                    AnnotatedInstruction endBlock = null;
                    Integer startKey = null;
                    Integer endKey = null;


                    for(Map.Entry<Integer, AnnotatedInstruction> e : implicitAnalysisBlocks.entrySet()) {
                        AnnotatedInstruction b = e.getValue();
                        Integer i = e.getKey();
                        if(b.insn == t.handler) {
                            handlerBlock = b;
                        }
                        if(b.insn == t.start) {
                            startBlock = b;
                            startKey = i;
                        }
                        if(b.insn == t.end) {
                            endBlock = b;
                            endKey = i;
                        }
                        if(startBlock != null && handlerBlock != null && endBlock != null) {
                            break;
                        }
                    }
                    if(startBlock == handlerBlock || endBlock == null) {
                        continue;
                    }

                    //Identify all of the instructions in this try block
                    if(startBlock != null) {
                        for(int i = startKey; i <= endKey; i++) {
                            if(implicitAnalysisBlocks.get(i) != null) {
                                implicitAnalysisBlocks.get(i).coveredByTryBlockFor.add(t.type);
                            }
                        }
                    }
                    handlerBlock.exceptionsHandled.add(t.type);
                    tryCatchHandlers.add(handlerBlock);
                    startBlock.isTryBlockStart = true;
                    startBlock.exceptionsHandled.add(t.type);
                    handlerBlock.onFalseSideOfJumpFrom.add(startBlock);
                    handlerBlock.handlerForRegionStartingAt.add(startBlock);
                    startBlock.successors.add(handlerBlock);
                    startBlock.ex_count = exceptionHandlerCount;
                    startBlock.tryBlockEnd = endBlock;
                    startBlock.handledAt = handlerBlock;


                    exceptionHandlerCount++;
                    for(AnnotatedInstruction suc : startBlock.successors) {
                        if(!suc.onFalseSideOfJumpFrom.contains(startBlock)) {
                            suc.onTrueSideOfJumpFrom.add(startBlock);
                        }
                    }
                }
            }

            for(AnnotatedInstruction b : implicitAnalysisBlocks.values()) {
                if(b.isJump) {
                    hasJumps = true;
                    break;
                }
            }
            if(implicitAnalysisBlocks.size() > 1 && hasJumps) {
                Stack<AnnotatedInstruction> stack = new Stack<>();
                //Fix successors to only point to jumps or labels
                boolean changed = true;
                while(changed) {
                    changed = false;
                    for(AnnotatedInstruction b : implicitAnalysisBlocks.values()) {
                        for(AnnotatedInstruction s : b.successors) {
                            if(s.isInteresting()) {
                                changed |= b.basicBlockStartingSuccessors.add(s);
                            } else {
                                changed |= b.basicBlockStartingSuccessors.addAll(s.basicBlockStartingSuccessors);
                            }
                        }
                    }
                }
                // Post dominator analysis
                HashSet<AnnotatedInstruction> interestingBlocks = new HashSet<>();
                for(AnnotatedInstruction b : implicitAnalysisBlocks.values()) {
                    if(b.isInteresting()) {
                        interestingBlocks.add(b);
                    }
                }
                for(AnnotatedInstruction b : implicitAnalysisBlocks.values()) {
                    if(b.basicBlockStartingSuccessors.isEmpty()) {
                        b.postDominators.add(b);
                    } else {
                        b.postDominators.addAll(interestingBlocks);
                    }
                }
                changed = true;
                while(changed) {
                    changed = false;
                    for(AnnotatedInstruction b : implicitAnalysisBlocks.values()) {
                        if(!b.basicBlockStartingSuccessors.isEmpty() && b.isInteresting()) {
                            Iterator<AnnotatedInstruction> iter = b.basicBlockStartingSuccessors.iterator();
                            AnnotatedInstruction successor = iter.next();
                            HashSet<AnnotatedInstruction> intersectionOfPredecessors = new HashSet<>(successor.postDominators);
                            while(iter.hasNext()) {
                                successor = iter.next();
                                intersectionOfPredecessors.retainAll(successor.postDominators);
                            }
                            intersectionOfPredecessors.add(b);
                            if(!b.postDominators.equals(intersectionOfPredecessors)) {
                                changed = true;
                                b.postDominators = intersectionOfPredecessors;
                            }
                        }
                    }
                }


                // Add in markings for where jumps are resolved
                for(AnnotatedInstruction j : implicitAnalysisBlocks.values()) {
                    if(j.isJump || j.isTryBlockStart) {
                        j.postDominators.remove(j);
                        HashSet<AnnotatedInstruction> visited = new HashSet<>();
                        AnnotatedInstruction min = findImmediatePostDominator(j, visited);
                        if(min != null) {
                            min.resolvedBlocks.add(j);
                            min.resolvedHereBlocks.add(j);
                        } else {
                            // There are no post-dominators of this branch. That means that one leg of the
                            // branch goes to a return. So, we'll say that this gets resolved at each return that
                            // is a successor
                            for(AnnotatedInstruction b : visited) {
                                if(isExitInstruction(b.insn)) {
                                    b.resolvedHereBlocks.add(j);
                                }
                            }
                        }
                    }
                }
                // Propagate forward true-side/false-side to determine which vars are written
                stack.add(implicitAnalysisBlocks.get(0));
                while(!stack.isEmpty()) {
                    AnnotatedInstruction b = stack.pop();
                    if(b.visited) {
                        continue;
                    }
                    b.visited = true;
                    b.onFalseSideOfJumpFrom.removeAll(b.resolvedBlocks);
                    b.onTrueSideOfJumpFrom.removeAll(b.resolvedBlocks);
                    // Propagate markings to successors
                    for(AnnotatedInstruction s : b.successors) {
                        s.onFalseSideOfJumpFrom.addAll(b.onFalseSideOfJumpFrom);
                        s.onTrueSideOfJumpFrom.addAll(b.onTrueSideOfJumpFrom);
                        s.resolvedBlocks.addAll(b.resolvedBlocks);
                        s.onFalseSideOfJumpFrom.remove(s);
                        s.onTrueSideOfJumpFrom.remove(s);
                        stack.add(s);
                    }
                }
                for(AnnotatedInstruction j : implicitAnalysisBlocks.values()) {
                    j.visited = false;

                }
                for(AnnotatedInstruction j : implicitAnalysisBlocks.values()) {
                    if(j.isJump || j.isTryBlockStart) {
                        stack = new Stack<>();
                        stack.addAll(j.successors);
                        Set<AnnotatedInstruction> visited = new HashSet<>();
                        while(!stack.isEmpty()) {
                            AnnotatedInstruction b = stack.pop();
                            if(!visited.add(b)) {
                                continue;
                            }
                            if(b.onTrueSideOfJumpFrom.contains(j)) {
                                j.varsWrittenTrueSide.addAll(b.varsWritten);
                                j.fieldsWrittenTrueSide.addAll(b.fieldsWritten);
                                j.exceptionsThrownTrueSide.addAll(b.exceptionsThrown);
                                stack.addAll(b.successors);
                            } else if(b.onFalseSideOfJumpFrom.contains(j)) {
                                j.varsWrittenFalseSide.addAll(b.varsWritten);
                                j.fieldsWrittenFalseSide.addAll(b.fieldsWritten);
                                j.exceptionsThrownFalseSide.addAll(b.exceptionsThrown);
                                stack.addAll(b.successors);
                            }
                        }
                    }
                }
                HashMap<AnnotatedInstruction, Integer> jumpIDs = new HashMap<>();
                int jumpID = 0;
                for(AnnotatedInstruction r : implicitAnalysisBlocks.values()) {
                    if(r.isTryBlockStart) {
                        //Need to actually insert this code at every exit from the SCC that is this try-catch block.
                        //Find the end of the handler
                        //this is any block that succeeds the handler and either: has no successors or has a successor in common with the start block
                        LinkedList<AnnotatedInstruction> handlerEndBlock = new LinkedList<>();
                        LinkedList<AnnotatedInstruction> toCheck = new LinkedList<>(r.handledAt.successors);
                        HashSet<AnnotatedInstruction> visited = new HashSet<>();
                        while(!toCheck.isEmpty()) {
                            AnnotatedInstruction e = toCheck.poll();
                            if(!visited.add(e)) {
                                continue;
                            }
                            if(e.successors.isEmpty()) {
                                handlerEndBlock.add(e);
                            } else if(r.postDominators.contains(e)) {
                                handlerEndBlock.add(e);
                            } else {
                                toCheck.addAll(e.successors);
                            }
                        }

                        AbstractInsnNode lastInstructionInTryBlock = r.tryBlockEnd.insn;
                        while(lastInstructionInTryBlock.getType() == AbstractInsnNode.FRAME || lastInstructionInTryBlock.getType() == AbstractInsnNode.LINE || lastInstructionInTryBlock.getType() == AbstractInsnNode.LABEL) {
                            lastInstructionInTryBlock = lastInstructionInTryBlock.getNext();
                        }
                        //Set up the force control store's at the bottom of the try block
                        HashSet<LVAccess> lvsOnlyInHandler = new HashSet<>(r.varsWrittenFalseSide);
                        lvsOnlyInHandler.removeAll(r.varsWrittenTrueSide);
                        HashSet<Field> fieldsOnlyInHandler = new HashSet<>(r.fieldsWrittenFalseSide);
                        fieldsOnlyInHandler.removeAll(r.fieldsWrittenTrueSide);
                        for(LVAccess i : lvsOnlyInHandler) {
                            this.instructions.insertBefore(lastInstructionInTryBlock, i.getNewForceCtrlStoreNode());
                        }
                        for(Field f : fieldsOnlyInHandler) {
                            this.instructions.insertBefore(lastInstructionInTryBlock, new FieldInsnNode((f.isStatic ? TaintUtils.FORCE_CTRL_STORE_SFIELD : TaintUtils.FORCE_CTRL_STORE), f.owner, f.name, f.description));
                        }
                        AbstractInsnNode handledAtInsn = r.handledAt.insn;
                        HashSet<String> handledHereAlready = new HashSet<>();
                        HashSet<Integer> forceStoreAlready = new HashSet<>();
                        while(handledAtInsn.getType() == AbstractInsnNode.FRAME || handledAtInsn.getType() == AbstractInsnNode.LINE || handledAtInsn.getType() == AbstractInsnNode.LABEL || handledAtInsn.getOpcode() > 200) {
                            if(handledAtInsn.getOpcode() == TaintUtils.EXCEPTION_HANDLER_START) {
                                TypeInsnNode tin = (TypeInsnNode) handledAtInsn;
                                if(tin.desc != null) {
                                    handledHereAlready.add(tin.desc);
                                }
                            } else if(handledAtInsn.getOpcode() == TaintUtils.FORCE_CTRL_STORE && handledAtInsn.getType() == AbstractInsnNode.VAR_INSN) {
                                VarInsnNode vn = (VarInsnNode) handledAtInsn;
                                forceStoreAlready.add(vn.var);
                            }
                            handledAtInsn = handledAtInsn.getNext();
                        }

                        //Then do all of the force-ctr-stores
                        //In the exception handler, force a store of what was written
                        HashSet<LVAccess> diff = new HashSet<>();

                        diff.addAll(r.varsWrittenTrueSide);
                        diff.removeAll(r.varsWrittenFalseSide);

                        HashSet<Field> diffFields = new HashSet<>();
                        diffFields.addAll(r.fieldsWrittenTrueSide);
                        diffFields.removeAll(r.fieldsWrittenFalseSide);

                        for(LVAccess i : diff) {
                            if(!forceStoreAlready.contains(i.idx)) {
                                instructions.insertBefore(handledAtInsn, i.getNewForceCtrlStoreNode());
                            }
                        }
                        for(Field f : diffFields) {
                            instructions.insertBefore(handledAtInsn, new FieldInsnNode((f.isStatic ? TaintUtils.FORCE_CTRL_STORE_SFIELD : TaintUtils.FORCE_CTRL_STORE), f.owner, f.name, f.description));
                        }

                        //At the START of the handler, note that it's the start...
                        if(handledHereAlready.isEmpty()) {
                            instructions.insertBefore(handledAtInsn, new TypeInsnNode(TaintUtils.EXCEPTION_HANDLER_START, null));
                        }
                        for(String ex : r.exceptionsHandled) {
                            if(ex == null) {
                                ex = "java/lang/Throwable";
                            }
                            if(!handledHereAlready.contains(ex)) {
                                instructions.insertBefore(handledAtInsn, new TypeInsnNode(TaintUtils.EXCEPTION_HANDLER_START, ex));
                            }
                            this.instructions.insertBefore(lastInstructionInTryBlock, new TypeInsnNode(TaintUtils.EXCEPTION_HANDLER_END, ex));
                        }

                        //At the END of the handler, remove this exception from the queue
                        for(AnnotatedInstruction b : handlerEndBlock) {
                            AbstractInsnNode insn = b.insn;
                            //Peek backwards to see if we are behind a GOTO
                            while(insn != null && insn.getPrevious() != null && mightEndBlock(insn.getPrevious())) {
                                insn = insn.getPrevious();
                            }
                            if(insn.getType() == AbstractInsnNode.LABEL || insn.getType() == AbstractInsnNode.LINE || insn.getType() == AbstractInsnNode.FRAME) {
                                insn = b.insn;
                            }
                            instructions.insertBefore(insn, new TypeInsnNode(TaintUtils.EXCEPTION_HANDLER_END, null));
                        }
                    } else if(r.isJump) {
                        jumpID++;

                        HashSet<LVAccess> common = new HashSet<>();
                        common.addAll(r.varsWrittenFalseSide);
                        common.retainAll(r.varsWrittenTrueSide);
                        HashSet<LVAccess> diff = new HashSet<>();
                        diff.addAll(r.varsWrittenTrueSide);
                        diff.addAll(r.varsWrittenFalseSide);
                        diff.removeAll(common);

                        HashSet<Field> commonFields = new HashSet<>();
                        commonFields.addAll(r.fieldsWrittenTrueSide);
                        commonFields.retainAll(r.fieldsWrittenFalseSide);
                        HashSet<Field> diffFields = new HashSet<>();
                        diffFields.addAll(r.fieldsWrittenFalseSide);
                        diffFields.addAll(r.fieldsWrittenTrueSide);
                        diffFields.removeAll(common);

                        HashSet<String> commonExceptionsThrown = new HashSet<>();
                        commonExceptionsThrown.addAll(r.exceptionsThrownFalseSide);
                        commonExceptionsThrown.retainAll(r.exceptionsThrownTrueSide);
                        HashSet<String> diffExceptions = new HashSet<>();
                        diffExceptions.addAll(r.exceptionsThrownTrueSide);
                        diffExceptions.addAll(r.exceptionsThrownFalseSide);
                        diffExceptions.removeAll(commonExceptionsThrown);

                        instructions.insertBefore(r.insn, new VarInsnNode(TaintUtils.BRANCH_START, jumpID));
                        jumpIDs.put(r, jumpID);
                        if(r.isTwoOperandJumpInstruction) {
                            jumpID++;
                        }

                        for(LVAccess i : diff) {
                            instructions.insertBefore(r.insn, i.getNewForceCtrlStoreNode());
                        }
                        for(Field f : diffFields) {
                            instructions.insertBefore(r.insn, new FieldInsnNode((f.isStatic ? TaintUtils.FORCE_CTRL_STORE_SFIELD : TaintUtils.FORCE_CTRL_STORE), f.owner, f.name, f.description));
                        }

                    } else if(shouldTrackExceptions && r.insn.getOpcode() >= Opcodes.IRETURN && r.insn.getOpcode() <= Opcodes.RETURN) {
                        //Return statement: check to see how we might have gotten here, and then find which exceptions we might have thrown if we came otherwise
                        HashSet<String> missedExceptions = new HashSet<>();
                        for(AnnotatedInstruction b : r.onFalseSideOfJumpFrom) {
                            HashSet<String> tmp = new HashSet<>(b.exceptionsThrownTrueSide);
                            tmp.removeAll(b.exceptionsThrownFalseSide);
                            missedExceptions.addAll(tmp);
                        }
                        for(AnnotatedInstruction b : r.onTrueSideOfJumpFrom) {
                            HashSet<String> tmp = new HashSet<>(b.exceptionsThrownFalseSide);
                            tmp.removeAll(b.exceptionsThrownTrueSide);
                            missedExceptions.addAll(tmp);
                        }
                        HashSet<String> filtered = new HashSet<>();
                        for(String s : missedExceptions) {
                            if(s == null) {
                                s = "java/lang/Throwable";
                            }
                            if(s.contains("#")) {
                                s = s.substring(0, s.indexOf('#'));
                            }
                            if(filtered.add(s)) {
                                instructions.insertBefore(r.insn, new TypeInsnNode(TaintUtils.UNTHROWN_EXCEPTION, s));
                            }
                        }
                    } else if(shouldTrackExceptions && (r.insn.getType() == AbstractInsnNode.METHOD_INSN
                            || r.insn.getType() == AbstractInsnNode.INVOKE_DYNAMIC_INSN)) {
                        // Are we in a try handler? If so, after this instruction, we should note that our execution may be contingent on some exception
                        for(String s : r.coveredByTryBlockFor) {
                            if(s == null) {
                                s = "java/lang/Throwable";
                            }
                            instructions.insert(r.insn, new TypeInsnNode(TaintUtils.UNTHROWN_EXCEPTION_CHECK, s));
                        }
                    }
                }
                for(AnnotatedInstruction b : implicitAnalysisBlocks.values()) {
                    AbstractInsnNode insn = b.insn;
                    while(insn.getType() == AbstractInsnNode.FRAME || insn.getType() == AbstractInsnNode.LINE || insn.getType() == AbstractInsnNode.LABEL) {
                        insn = insn.getNext();
                    }
                    if(b.resolvedHereBlocks.size() == jumpIDs.size()) {
                        //Everything is resolved
                        instructions.insertBefore(insn, new VarInsnNode(TaintUtils.BRANCH_END, -1));
                    } else {
                        for(AnnotatedInstruction r : b.resolvedHereBlocks) {
                            if(!r.isTryBlockStart) {
                                if(!b.successors.isEmpty()) {
                                    // For any return or athrow, we'll just bulk pop-all
                                    instructions.insertBefore(insn, new VarInsnNode(TaintUtils.BRANCH_END, jumpIDs.get(r)));
                                    if(r.isTwoOperandJumpInstruction) {
                                        instructions.insertBefore(insn, new VarInsnNode(TaintUtils.BRANCH_END, jumpIDs.get(r) + 1));
                                    }
                                }
                            }
                        }
                    }
                    if(!b.resolvedHereBlocks.isEmpty()) {
                        patchFrames(b, instructions);
                    }
                    // In light tracking mode no need to POP off of control at RETURN/THROW, because we don't reuse the obj
                    if(b.successors.isEmpty() && !isImplicitLightTracking) {
                        instructions.insertBefore(b.insn, new InsnNode(TaintUtils.FORCE_CTRL_STORE));
                        instructions.insertBefore(b.insn, new VarInsnNode(TaintUtils.BRANCH_END, -1));
                    }
                }
                nJumps = jumpID;
            }
        }
    }

    static class AnnotatedInstruction {
        AnnotatedInstruction handledAt;
        Set<AnnotatedInstruction> postDominators = new HashSet<>();
        int idx;
        Set<AnnotatedInstruction> basicBlockStartingSuccessors = new HashSet<>();
        Set<AnnotatedInstruction> successors = new HashSet<>();
        Set<AnnotatedInstruction> predecessors = new HashSet<>();
        AbstractInsnNode insn;
        boolean covered;
        boolean visited;
        boolean isTryBlockStart;
        boolean isJump;
        boolean isTwoOperandJumpInstruction;
        int ex_count;
        Set<String> exceptionsHandled = new HashSet<>();
        Set<String> coveredByTryBlockFor = new HashSet<>();
        Set<AnnotatedInstruction> handlerForRegionStartingAt = new HashSet<>();
        AnnotatedInstruction tryBlockEnd;
        Set<AnnotatedInstruction> resolvedHereBlocks = new HashSet<>();
        Set<AnnotatedInstruction> resolvedBlocks = new HashSet<>();
        Set<AnnotatedInstruction> onFalseSideOfJumpFrom = new HashSet<>();
        Set<AnnotatedInstruction> onTrueSideOfJumpFrom = new HashSet<>();
        Set<LVAccess> varsWritten = new HashSet<>();
        Set<Field> fieldsWritten = new HashSet<>();
        Set<String> exceptionsThrown = new HashSet<>();
        Set<String> exceptionsThrownTrueSide = new HashSet<>();
        Set<String> exceptionsThrownFalseSide = new HashSet<>();
        Set<LVAccess> varsWrittenTrueSide = new HashSet<>();
        Set<LVAccess> varsWrittenFalseSide = new HashSet<>();
        Set<Field> fieldsWrittenTrueSide = new HashSet<>();
        Set<Field> fieldsWrittenFalseSide = new HashSet<>();

        boolean isInteresting() {
            return isJump || isTryBlockStart || insn instanceof LabelNode;
        }

        Set<AnnotatedInstruction> getSuccessorsOutsideOfRegion(int s, int e, Set<AnnotatedInstruction> visited) {
            if(visited.contains(this)) {
                return Collections.emptySet();
            }
            visited.add(this);
            if(idx < s || idx > e) {
                return Collections.singleton(this);
            }
            Set<AnnotatedInstruction> ret = new HashSet<>();
            for(AnnotatedInstruction suc : successors) {
                ret.addAll(suc.getSuccessorsOutsideOfRegion(s, e, visited));
            }
            return ret;

        }

        AbstractInsnNode getNextNormalBlockAfterGOTO() {
            System.out.println(this + "," + successors + " " + this.insn);
            if(this.insn instanceof LineNumberNode) {
                System.out.println("(" + ((LineNumberNode) insn).line + ")");
            }
            if(this.insn.getOpcode() == Opcodes.GOTO) {
                AnnotatedInstruction suc = successors.iterator().next();
                AbstractInsnNode successorInsn = suc.insn;
                while(successorInsn.getType() == AbstractInsnNode.FRAME || successorInsn.getType() == AbstractInsnNode.LINE || successorInsn.getType() == AbstractInsnNode.LABEL) {
                    successorInsn = successorInsn.getNext();
                }
                return successorInsn;
            }
            if(successors.size() > 1) {
                throw new IllegalStateException();
            }
            return successors.iterator().next().getNextNormalBlockAfterGOTO();
        }

        @Override
        public String toString() {
            return "" + idx;
        }
    }

    static class LVAccess {
        int idx;
        String desc;

        LVAccess(int idx, String desc) {
            this.idx = idx;
            this.desc = desc;
        }

        @Override
        public String toString() {
            return "LVAccess{" +
                    "idx=" + idx +
                    ", desc='" + desc + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if(this == o) {
                return true;
            }
            if(o == null || getClass() != o.getClass()) {
                return false;
            }
            LVAccess lvAccess = (LVAccess) o;
            return idx == lvAccess.idx && Objects.equals(desc, lvAccess.desc);
        }

        @Override
        public int hashCode() {
            return Objects.hash(idx, desc);
        }

        VarInsnNode getNewForceCtrlStoreNode() {
            if(this.desc.equals("J") || this.desc.equals("D")) {
                return new VarInsnNode(TaintUtils.FORCE_CTRL_STORE_WIDE, idx);
            }
            return new VarInsnNode(TaintUtils.FORCE_CTRL_STORE, idx);
        }
    }
}
