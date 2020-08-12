package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.ReferenceArrayTarget;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashSet;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.List;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.FrameNode;


public class DefaultTaintCheckingMethodVisitor extends MethodVisitor implements Opcodes {

    Set<String> checkedThisFrame = new HashSet<>();
    private boolean implementsSerializable;
    private NeverNullArgAnalyzerAdapter analyzer;
    ReferenceArrayTarget arrayTarget;
    private boolean isUninitConstructor;
    private List<FieldNode> fields;
    private String className;


    @Override
    public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
        checkedThisFrame = new HashSet<>();
        super.visitFrame(type, nLocal, local, nStack, stack);
    }

    public DefaultTaintCheckingMethodVisitor(MethodVisitor mv, int acc, String className, String name, String desc, boolean implementsSerializable, NeverNullArgAnalyzerAdapter analyzer, List<FieldNode> fields) {
        super(Configuration.ASM_VERSION, mv);
        this.analyzer = analyzer;
        this.implementsSerializable = implementsSerializable;
        this.className = className;
        this.isUninitConstructor = name.equals("<init>");
        this.fields = fields;
    }

    @Override
    public void visitLdcInsn(Object value) {
        if(value instanceof ReferenceArrayTarget) {
            this.arrayTarget = (ReferenceArrayTarget) value;
        } else {
            super.visitLdcInsn(value);
        }
    }

    // @Override
    // public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
    //     super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    //     if(opcode == INVOKESPECIAL && name.equals("<init>") && isUninitConstructor){
    //         //TODO this should be calculated correctly...
    //         fixInstanceTaintFields();
    //         isUninitConstructor = false;
    //     }
    // }
    //
    // private void fixInstanceTaintFields() {
    //     for(FieldNode fn : fields){
    //         if((fn.access & Opcodes.ACC_STATIC) == 0) {
    //             FrameNode frame = TaintAdapter.getCurrentFrameNode(analyzer);
    //             frame.type = Opcodes.F_NEW;
    //             Label lbl = new Label();
    //             super.visitVarInsn(ALOAD, 0);
    //             super.visitFieldInsn(GETFIELD, className, fn.name + TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_DESC);
    //             super.visitJumpInsn(IFNONNULL, lbl);
    //             super.visitVarInsn(ALOAD, 0);
    //             TaintMethodRecord.NEW_EMPTY_TAINT.delegateVisit(mv);
    //             super.visitFieldInsn(PUTFIELD, className, fn.name + TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_DESC);
    //             super.visitLabel(lbl);
    //             frame.accept(mv);
    //         }
    //     }
    // }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        Type accessedType = Type.getType(desc);
        Type originalType = TaintUtils.getUnwrappedType(accessedType);
        if(arrayTarget != null) {
            originalType = Type.getType(arrayTarget.getOriginalArrayType());
            arrayTarget = null;
        }
        if(originalType.getSort() != Type.ARRAY || originalType.getDimensions() > 1 || (originalType.getDescriptor().equals("[Ljava/lang/Object;") && !owner.startsWith("java/lang/reflect") && !owner.equals("java/lang/AssertionStatusDirectives"))) {
            //TODO do reference arrays need this hacking?
            super.visitFieldInsn(opcode, owner, name, desc);
            return;
        }
        if(opcode == Opcodes.GETFIELD && !Instrumenter.isIgnoredClass(owner) && name.endsWith(TaintUtils.TAINT_WRAPPER_FIELD) &&
                !checkedThisFrame.contains(owner + "." + name)
                && (owner.equals("java/lang/String") || implementsSerializable || owner.equals("java/io/BufferedInputStream")
                || owner.equals("java/lang/AssertionStatusDirectives")
                || owner.equals("java/net/NetworkInterface")
                || owner.startsWith("java/lang/reflect") || owner.equals("com/sun/security/auth/module/UnixSystem")
                || owner.startsWith("sun/font/Font"))) {
            //For GETFIELD operations on a 1D array wrapper, make sure that the wrapper is initialized and pointing to the array.
            String originalName = name.replace(TaintUtils.TAINT_WRAPPER_FIELD, "");
            //Obj
            //Make sure that it's been initialized
            Label isOK = new Label();
            FrameNode fn1 = TaintAdapter.getCurrentFrameNode(analyzer);
            //Ob Ob
            super.visitInsn(DUP);
            super.visitFieldInsn(opcode, owner, originalName, originalType.getDescriptor());
            super.visitJumpInsn(IFNULL, isOK); //if value is null, do nothing


            //Array is not null, unsure about wrapper
            //Ob
            super.visitInsn(DUP);
            //Ob Ob
            super.visitInsn(DUP);
            //Ob Ob Ob
            super.visitFieldInsn(opcode, owner, originalName, originalType.getDescriptor());
            //Ob Ob Ar
            super.visitInsn(SWAP);
            //Ob Ar Ob
            super.visitFieldInsn(opcode, owner, name, desc);
            //Ob Ar Wrapper
            super.visitMethodInsn(INVOKESTATIC, accessedType.getInternalName(), "unwrap", "(" + accessedType.getDescriptor() + ")" + (originalType.getElementType().getSort() == Type.OBJECT ? "[Ljava/lang/Object;" : originalType.getDescriptor()), false);
            //Ob Ar Ar
            super.visitJumpInsn(IF_ACMPEQ, isOK);
            //Wrapper is not correct for the array.
            //Ob
            super.visitInsn(DUP);
            super.visitInsn(DUP);
            //Ob Ob Ob
            super.visitFieldInsn(opcode, owner, originalName, originalType.getDescriptor());
            //Ob Ob Ar
            super.visitTypeInsn(NEW, accessedType.getInternalName());
            super.visitInsn(DUP_X1);
            //Ob Ob W Ar W
            super.visitInsn(SWAP);
            //Ob Ob W W Ar
            super.visitMethodInsn(INVOKESPECIAL, accessedType.getInternalName(), "<init>", "(" + (originalType.getElementType().getSort() == Type.OBJECT ? "[Ljava/lang/Object;" : originalType.getDescriptor()) + ")V", false);
            //Ob Ob W
            super.visitFieldInsn(PUTFIELD, owner, name, desc); // O
            super.visitLabel(isOK);
            TaintAdapter.acceptFn(fn1, this);
            super.visitFieldInsn(opcode, owner, name, desc);
        } else if(opcode == Opcodes.GETFIELD && !Instrumenter.isIgnoredClass(owner) && accessedType.getSort() == Type.ARRAY && !name.equals("taint") &&
                accessedType.getElementType().getSort() != Type.OBJECT && accessedType.getDimensions() == 2 && !checkedThisFrame.contains(owner + "." + name)) {
            //For 2D wrapped arrays
            //TODO
            super.visitInsn(SWAP);
            super.visitInsn(POP);
            //Make sure that it's been initialized
            Label isOK = new Label();
            Label doInit = new Label();

            FrameNode fn1 = TaintAdapter.getCurrentFrameNode(analyzer);

            super.visitInsn(DUP);
            super.visitFieldInsn(opcode, owner, name, desc);
            super.visitJumpInsn(IFNULL, isOK); //if value is null, do nothing

            super.visitInsn(DUP);
            super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_WRAPPER_FIELD, "[[I");
            super.visitJumpInsn(IFNULL, doInit); //if taint is null, def init
            //if taint is not null, check the length
            super.visitInsn(DUP); // O O
            super.visitInsn(DUP); // O O O
            super.visitFieldInsn(opcode, owner, name, desc);
            super.visitInsn(ARRAYLENGTH);
            super.visitInsn(SWAP);
            super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_WRAPPER_FIELD, "[[I");
            super.visitInsn(ARRAYLENGTH);
            super.visitJumpInsn(IF_ICMPLE, isOK); //if taint is shorter than value, reinit it
            super.visitLabel(doInit);
            TaintAdapter.acceptFn(fn1, this);
            super.visitInsn(DUP); // O O
            super.visitInsn(DUP); // O O O
            super.visitFieldInsn(opcode, owner, name, desc); //O O A
            super.visitInsn(DUP);
            super.visitInsn(ARRAYLENGTH);//O O A L
            super.visitMultiANewArrayInsn("[[I", 1); //O O A TA
            super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintUtils.class), "create2DTaintArray", "(Ljava/lang/Object;[[I)[[I", false);
            super.visitFieldInsn(PUTFIELD, owner, name + TaintUtils.TAINT_WRAPPER_FIELD, "[[I"); // O
            super.visitLabel(isOK);
            TaintAdapter.acceptFn(fn1, this);
            //O
            super.visitInsn(DUP);
            super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_WRAPPER_FIELD, "[[I");
            super.visitInsn(SWAP);
            super.visitFieldInsn(opcode, owner, name, desc);
            throw new UnsupportedOperationException();
        } else if(opcode == Opcodes.GETFIELD && !Instrumenter.isIgnoredClass(owner) && accessedType.getSort() == Type.ARRAY && !name.endsWith(TaintUtils.TAINT_WRAPPER_FIELD) && !name.equals("taint") &&
                accessedType.getElementType().getSort() != Type.OBJECT && accessedType.getDimensions() == 3 && !checkedThisFrame.contains(owner + "." + name)) {
            super.visitInsn(SWAP);
            super.visitInsn(POP);

            //Make sure that it's been initialized
            Label isOK = new Label();
            Label doInit = new Label();

            FrameNode fn1 = TaintAdapter.getCurrentFrameNode(analyzer);
            super.visitInsn(DUP);
            super.visitFieldInsn(opcode, owner, name, desc);
            super.visitJumpInsn(IFNULL, isOK); //if value is null, do nothing

            super.visitInsn(DUP);
            super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_WRAPPER_FIELD, "[[[I");
            super.visitJumpInsn(IFNULL, doInit); //if taint is null, def init
            //if taint is not null, check the length
            super.visitInsn(DUP); // O O
            super.visitInsn(DUP); // O O O
            super.visitFieldInsn(opcode, owner, name, desc);
            super.visitInsn(ARRAYLENGTH);
            super.visitInsn(SWAP);
            super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_WRAPPER_FIELD, "[[[I");
            super.visitInsn(ARRAYLENGTH);
            super.visitJumpInsn(IF_ICMPLE, isOK); //if taint is shorter than value, reinit it
            super.visitLabel(doInit);
            TaintAdapter.acceptFn(fn1, this);
            super.visitInsn(DUP); // O O
            super.visitInsn(DUP); // O O O
            super.visitFieldInsn(opcode, owner, name, desc); //O O A
            super.visitInsn(DUP);
            super.visitInsn(ARRAYLENGTH);//O O A L
            super.visitMultiANewArrayInsn("[[[I", 1); //O O A TA
            super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintUtils.class), "create3DTaintArray", "(Ljava/lang/Object;[[[I)[[[I", false);
            super.visitFieldInsn(PUTFIELD, owner, name + TaintUtils.TAINT_WRAPPER_FIELD, "[[[I"); // O
            super.visitLabel(isOK);
            TaintAdapter.acceptFn(fn1, this);
            //O
            super.visitInsn(DUP);
            super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_WRAPPER_FIELD, "[[[I");
            super.visitInsn(SWAP);
            super.visitFieldInsn(opcode, owner, name, desc);
            throw new UnsupportedOperationException();
        } else {
            super.visitFieldInsn(opcode, owner, name, desc);
        }
    }
}
