package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.control.ControlFlowAnalyzer;
import edu.columbia.cs.psl.phosphor.control.graph.BaseControlFlowGraphCreator;
import edu.columbia.cs.psl.phosphor.control.graph.BasicBlock;
import edu.columbia.cs.psl.phosphor.control.graph.FlowGraph;
import edu.columbia.cs.psl.phosphor.control.graph.FlowGraph.NaturalLoop;
import edu.columbia.cs.psl.phosphor.control.graph.SimpleBasicBlock;
import edu.columbia.cs.psl.phosphor.control.standard.NoControlFlowAnalyzer;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.BasicArrayInterpreter;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.PhosphorOpcodeIgnoringAnalyzer;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.ReferenceArrayTarget;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.analysis.Analyzer;
import edu.columbia.cs.psl.phosphor.struct.TaintedReferenceWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import static edu.columbia.cs.psl.phosphor.LoopHeaderInfo.LOOP_HEADER;

public class PrimitiveArrayAnalyzer extends MethodVisitor {

    private final ControlFlowAnalyzer flowAnalyzer;
    public MethodNode mn;
    boolean isEmptyMethod = true;
    Set<Type> wrapperTypesToPreAlloc = new HashSet<>();
    private int numberOfTryCatch;
    private NeverNullArgAnalyzerAdapter analyzer;
    private Map<Label, List<Label>> newLabels = new HashMap<>();
    private boolean isImplicitLightTracking;
    private Label newFirstLabel = new Label();
    private Label oldFirstLabel;
    private boolean fixLDCClass;

    public PrimitiveArrayAnalyzer(final String className, int access, final String name, final String desc,
                                  String signature, String[] exceptions, final MethodVisitor cmv,
                                  final boolean isImplicitLightTracking, final boolean fixLDCClass,
                                  ControlFlowAnalyzer flowAnalyzer) {
        super(Configuration.ASM_VERSION);
        this.mn = new PrimitiveArrayAnalyzerMN(access, name, desc, signature, exceptions, className, cmv);
        this.mv = mn;
        this.isImplicitLightTracking = isImplicitLightTracking;
        this.fixLDCClass = fixLDCClass;
        this.flowAnalyzer = flowAnalyzer;
    }

    public PrimitiveArrayAnalyzer(Type singleWrapperTypeToAdd) {
        super(Configuration.ASM_VERSION);
        this.mv = new PrimitiveArrayAnalyzerMN(0, null, null, null, null, null, null);
        if(singleWrapperTypeToAdd.getSort() == Type.OBJECT && singleWrapperTypeToAdd.getInternalName().startsWith("edu/columbia/cs/psl/phosphor/struct/Tainted")) {
            this.wrapperTypesToPreAlloc.add(singleWrapperTypeToAdd);
        }
        flowAnalyzer = new NoControlFlowAnalyzer();
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
        numberOfTryCatch++;
    }

    public int getNumberOfTryCatch() {
        return numberOfTryCatch;
    }

    private static void patchFrames(InsnList instructions) {
        for(AbstractInsnNode insn : instructions.toArray()) {
            patchFrames(insn, instructions);
        }
    }

    private static void patchFrames(AbstractInsnNode insn, InsnList instructions) {
        while(insn != null && (insn.getType() == AbstractInsnNode.FRAME || insn.getType() == AbstractInsnNode.LINE
                || insn.getType() == AbstractInsnNode.LABEL || insn.getOpcode() > 200)) {
            insn = insn.getNext();
        }
        if(insn == null) {
            return;
        }
        if(insn.getOpcode() == Opcodes.NEW) {
            // Need to patch all frames to have the correct label in them :'(
            AbstractInsnNode i = insn;
            while(i != null && i.getType() != AbstractInsnNode.LABEL) {
                i = i.getPrevious();
            }
            LinkedList<LabelNode> oldLabels = new LinkedList<>();
            oldLabels.add(((LabelNode) i));
            while (i != null && i.getPrevious() != null && i.getPrevious().getType() == AbstractInsnNode.LABEL) {
                oldLabels.add(((LabelNode) i.getPrevious()));
                i = i.getPrevious();
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
                mn.instructions.insertBefore(header, new LdcInsnNode(LOOP_HEADER));
            }
        }
    }

    final class PrimitiveArrayAnalyzerMN extends MethodNode {
        private final String className;
        private final MethodVisitor cmv;
        int curLabel = 0;

        PrimitiveArrayAnalyzerMN(int access, String name, String desc, String signature, String[] exceptions, String className, MethodVisitor cmv) {
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
        public void visitCode() {
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

        @Override
        public void visitEnd() {
            final HashMap<AbstractInsnNode, String> referenceArraysToCheckCast = new HashMap<>();
            BasicArrayInterpreter interpreter = new BasicArrayInterpreter((this.access & Opcodes.ACC_STATIC) != 0,
                    isImplicitLightTracking, referenceArraysToCheckCast);
            Analyzer<BasicValue> a = new PhosphorOpcodeIgnoringAnalyzer<>(interpreter);
            try {
                Frame<BasicValue>[] frames = a.analyze(className, this);
                for(int i = 0; i < instructions.size(); i++) {
                    if(frames[i] == null) {
                        //TODO dead code elimination.
                        //This should be done more generically
                        //But, this worked for JDT's stupid bytecode, so...
                        AbstractInsnNode insn = instructions.get(i);
                        if(insn.getOpcode() == Opcodes.ATHROW || insn.getOpcode() == Opcodes.GOTO
                                || (insn.getOpcode() >= Opcodes.IRETURN && insn.getOpcode() <= Opcodes.RETURN)) {
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
            flowAnalyzer.annotate(className, this);
            patchFrames(instructions);
            this.maxStack += 100;
            for(Map.Entry<AbstractInsnNode, String> each : referenceArraysToCheckCast.entrySet()) {
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
    }
}
