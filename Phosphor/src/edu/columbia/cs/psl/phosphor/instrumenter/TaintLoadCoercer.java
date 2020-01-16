package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.control.ControlFlowAnalyzer;
import edu.columbia.cs.psl.phosphor.control.ControlFlowManager;
import edu.columbia.cs.psl.phosphor.control.standard.StandardControlFlowManager;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.InstMethodSinkInterpreter;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.PFrame;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.SinkableArrayValue;
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

    public static void main(String[] args) throws Throwable {
        Configuration.IMPLICIT_TRACKING = true;
        Configuration.IMPLICIT_EXCEPTION_FLOW = true;
        // Configuration.IMPLICIT_LIGHT_TRACKING = true;
        // Configuration.ARRAY_LENGTH_TRACKING = true;
        // Configuration.ARRAY_INDEX_TRACKING = true;
        // Configuration.ANNOTATE_LOOPS = true;
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
                mv = new TaintLoadCoercer(className, access, name, desc, signature, exceptions, mv, true, null, false, false);
                ControlFlowManager controlManager = new StandardControlFlowManager();
                ControlFlowAnalyzer flowAnalyzer = controlManager.createPropagationPolicy(access, className, name, desc).getFlowAnalyzer();
                PrimitiveArrayAnalyzer paa = new PrimitiveArrayAnalyzer(className, access, name, desc, signature, exceptions, mv, false, false, flowAnalyzer);
                NeverNullArgAnalyzerAdapter an = new NeverNullArgAnalyzerAdapter(className, access, name, desc, paa);
                paa.setAnalyzer(an);
                mv = an;
                return mv;
            }
        };
        cr.accept(cv, ClassReader.EXPAND_FRAMES);
        pw.flush();

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
                AbstractInsnNode insn = this.instructions.getFirst();
                while(insn != null) {
                    switch(insn.getType()) {
                        case AbstractInsnNode.FIELD_INSN:
                            switch(insn.getOpcode()) {
                                case Opcodes.GETFIELD:
                                case Opcodes.GETSTATIC:
                                case Opcodes.PUTFIELD:
                                    canIgnoreTaintsTillEnd = false;
                                case Opcodes.PUTSTATIC:
                                    break;
                            }
                            break;
                        case AbstractInsnNode.INVOKE_DYNAMIC_INSN:
                        case AbstractInsnNode.METHOD_INSN:
                            canIgnoreTaintsTillEnd = false;
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
                                            this.instructions.insertBefore(insn, new FieldInsnNode(PUTSTATIC, ((FieldInsnNode) insn).owner, ((FieldInsnNode) insn).name + TaintUtils.TAINT_WRAPPER_FIELD, wrappedType.getDescriptor()));
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
                                    if(origType.getElementType().getSort() != Type.OBJECT) {
                                        Type wrappedType = MultiDTaintedArray.getTypeForType(origType);
                                        this.instructions.insertBefore(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArray.class), "boxIfNecessary", "(Ljava/lang/Object;)Ljava/lang/Object;", false));
                                        this.instructions.insertBefore(insn, new TypeInsnNode(CHECKCAST, wrappedType.getInternalName()));
                                    } else {
                                        this.instructions.insertBefore(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArray.class), "boxIfNecessary", "(Ljava/lang/Object;)Ljava/lang/Object;", false));
                                        this.instructions.insertBefore(insn, new TypeInsnNode(CHECKCAST, origType.getInternalName()));
                                    }
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
                //TODO I think this can be deleted
                // Set<SinkableArrayValue> swapPop = new HashSet<>();
                // insn = this.instructions.getFirst();
                // for(Frame frame : frames) {
                //     if(insn.getType() == AbstractInsnNode.FRAME && frame != null) {
                //         FrameNode fn = (FrameNode) insn;
                //         int analyzerFrameIdx = 0;
                //         for(int j = 0; j < fn.local.size(); j++) {
                //             Object valInExistingFrame = fn.local.get(j);
                //             BasicValue calculatedVal = (BasicValue) frame.getLocal(analyzerFrameIdx);
                //             if(calculatedVal instanceof SinkableArrayValue && ((SinkableArrayValue) calculatedVal).flowsToInstMethodCall &&
                //                     valInExistingFrame != Opcodes.TOP &&
                //                     (TaintUtils.isShadowedType(TaintAdapter.getTypeForStackType(valInExistingFrame)) || valInExistingFrame == Opcodes.NULL
                //                             || valInExistingFrame.equals("java/lang/Object"))) {
                //                 ignoreExistingFrames = false;
                //                 if(valInExistingFrame.equals("java/lang/Object")) {
                //                     fn.local.set(j, new TaggedValue(TaintUtils.getStackTypeForType(calculatedVal.getType())));
                //                     // Look for any GOTO that might have had an AUTOBOX added to it because we thought that this was.
                //                     // See SynchronizedBlockObjTagITCase for an example of where this is needed.
                //                     // This is a terrible fix, a much better one would be to clean up that whole autobox
                //                     // mess and move it into here, using the analyzer frames instead of the inFrames/outFrames
                //                     removeAUTOBOXCallsForVar(j);
                //                 } else {
                //                     fn.local.set(j, new TaggedValue(valInExistingFrame));
                //                 }
                //             }
                //             analyzerFrameIdx++;
                //             if(valInExistingFrame == Opcodes.DOUBLE || valInExistingFrame == Opcodes.LONG) {
                //                 analyzerFrameIdx++;
                //             }
                //         }
                //         for(int j = 0; j < fn.stack.size(); j++) {
                //             Object l = fn.stack.get(j);
                //             BasicValue v = (BasicValue) frame.getStack(j);
                //             if(v instanceof SinkableArrayValue && ((SinkableArrayValue) v).flowsToInstMethodCall && (TaintUtils.isShadowedType(TaintAdapter.getTypeForStackType(l)) || l == Opcodes.NULL || l.equals("java/lang/Object"))) {
                //                 fn.stack.set(j, new TaggedValue((ignoreExistingFrames ? TaintUtils.getStackTypeForType(v.getType()) : l)));
                //             }
                //         }
                //     }
                //     insn = insn.getNext();
                // }
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
    }
}
