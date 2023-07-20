package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.TaggedValue;
import edu.columbia.cs.psl.phosphor.runtime.MultiDArrayUtils;
import edu.columbia.cs.psl.phosphor.runtime.PhosphorStackFrame;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.ArrayList;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.List;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.StringBuilder;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.*;

public class TaintAdapter extends MethodVisitor implements Opcodes {

    private static final Type taintTagType = Type.getType(Configuration.TAINT_TAG_DESC);
    public List<FieldNode> fields;
    protected LocalVariableManager lvs;
    protected NeverNullArgAnalyzerAdapter analyzer;
    protected String className;
    protected String superName;
    protected String methodName;
    protected String methodDesc;
    protected boolean initializePhosphorStackFrame;

    public TaintAdapter(int access, String className, String name, String desc, String signature, String[] bbbexceptions,
            MethodVisitor mv, NeverNullArgAnalyzerAdapter analyzer, boolean initializePhosphorStackFrame) {
        super(Configuration.ASM_VERSION, mv);
        this.analyzer = analyzer;
        this.className = className;
        this.methodName = name;
        this.methodDesc = desc;
        this.initializePhosphorStackFrame = initializePhosphorStackFrame;
    }

    public TaintAdapter(int access, String className, String name, String desc, String signature, String[] exceptions,
            MethodVisitor mv, NeverNullArgAnalyzerAdapter analyzer, String classSource, String classDebug) {
        super(Configuration.ASM_VERSION, mv);
        this.analyzer = analyzer;
        this.className = className;
        this.methodName = name;
        this.methodDesc = desc;
    }

    public LocalVariableManager getLvs() {
        return lvs;
    }

    public void setFields(List<FieldNode> fields) {
        this.fields = fields;
    }

    public void setSuperName(String parentName) {
        this.superName = parentName;
    }

    void ensureUnBoxedAt(int n, Type t) {
        switch (n) {
            case 0:
                super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDArrayUtils.class), "unboxRaw",
                        "(Ljava/lang/Object;)Ljava/lang/Object;", false);
                super.visitTypeInsn(Opcodes.CHECKCAST, t.getInternalName());
                break;
            case 1:
                Object top = analyzer.stack.get(analyzer.stack.size() - 1);
                if (top == Opcodes.LONG || top == Opcodes.DOUBLE || top == Opcodes.TOP) {
                    super.visitInsn(DUP2_X1);
                    super.visitInsn(POP2);
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDArrayUtils.class),
                            "unboxRaw", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
                    super.visitTypeInsn(Opcodes.CHECKCAST, t.getInternalName());
                    super.visitInsn(DUP_X2);
                    super.visitInsn(POP);
                } else {
                    super.visitInsn(SWAP);
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDArrayUtils.class),
                            "unboxRaw", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
                    super.visitTypeInsn(Opcodes.CHECKCAST, t.getInternalName());
                    super.visitInsn(SWAP);
                }
                break;
            default:
                LocalVariableNode[] d = storeToLocals(n);

                super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDArrayUtils.class), "unboxRaw",
                        "(Ljava/lang/Object;)Ljava/lang/Object;", false);
                super.visitTypeInsn(Opcodes.CHECKCAST, t.getInternalName());
                for (int i = n - 1; i >= 0; i--) {
                    loadLV(i, d);
                }
                freeLVs(d);

        }
    }

    public boolean topHas0Taint() {
        if (getTopOfStackObject() == Opcodes.TOP) {
            return Integer.valueOf(0).equals(analyzer.stackConstantVals.get(analyzer.stackConstantVals.size() - 3));
        } else {
            return Integer.valueOf(0).equals(analyzer.stackConstantVals.get(analyzer.stackConstantVals.size() - 2));
        }
    }

    public boolean secondHas0Taint() {
        int offset = 2;
        if (getTopOfStackObject() == Opcodes.TOP) {
            offset++;
        }
        if (getStackElementSize(offset) == Opcodes.TOP) {
            offset++;
        }
        offset++;
        return Integer.valueOf(0).equals(analyzer.stackConstantVals.get(analyzer.stackConstantVals.size() - offset));
    }

    protected void getTaintFieldOfBoxedType(String owner) {
        super.visitFieldInsn(GETFIELD, owner, "taint", Configuration.TAINT_TAG_DESC);
    }

    public void setLocalVariableSorter(LocalVariableManager lvs) {
        this.lvs = lvs;
    }

    public boolean topOfStackIsNull() {
        return stackElIsNull(0);
    }

    public Type getTopOfStackType() {
        if (analyzer.stack == null) {
            throw new NullPointerException();
        }
        if (analyzer.stack.get(analyzer.stack.size() - 1) == Opcodes.TOP) {
            return getStackTypeAtOffset(1);
        }
        return getStackTypeAtOffset(0);
    }

    public Object getTopOfStackObject() {
        return analyzer.stack.get(analyzer.stack.size() - 1);
    }

    /**
     * Returns the type of the stack element n down from the top: n=0 -> top of
     * stack
     */
    public Type getStackTypeAtOffset(int n) {
        return getTypeForStackType(analyzer.stack.get(analyzer.stack.size() - 1 - n));
    }

    public boolean stackElIsNull(int n) {
        return analyzer.stack.get(analyzer.stack.size() - 1 - n) == Opcodes.NULL;
    }

    public NeverNullArgAnalyzerAdapter getAnalyzer() {
        return analyzer;
    }

    /**
     * Retrieve the taint of the object at the top of the stack (popping that
     * object) Assumes that the top of the stack is an object (may or may not be
     * null)
     */
    protected void safelyFetchObjectTaint() {
        // Check for null first.
        // TODO handle numerics, other ignore classes for which we know the taint
        if (className.equals("java/util/HashMap")) {
            super.visitInsn(POP);
            Configuration.taintTagFactory.generateEmptyTaint(mv);
        } else {
            super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintUtils.class), "getTaint",
                    "(Ljava/lang/Object;)I", false);
        }
    }

    protected void generateUnconstrainedTaint(int reason) {
        Configuration.taintTagFactory.generateEmptyTaint(mv);
    }

    /**
     * Precondition: top stack element is an array Postconditoins: top stack
     * element is the same array, second stack element is an int array of the
     * same length.
     */
    protected void generateEmptyTaintArray(String arrayDesc) {
        Type arrayType = Type.getType(arrayDesc);
        Label isNull = new Label();
        Label done = new Label();
        if (arrayType.getDimensions() == 2) {

            FrameNode fn = getCurrentFrameNode();
            super.visitInsn(DUP);
            super.visitInsn(TaintUtils.IGNORE_EVERYTHING);
            super.visitJumpInsn(IFNULL, isNull);
            super.visitInsn(TaintUtils.IGNORE_EVERYTHING);
            super.visitInsn(DUP);
            super.visitInsn(DUP);
            super.visitInsn(ARRAYLENGTH);
            super.visitMultiANewArrayInsn("[[I", 1);
            super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintUtils.class), "create2DTaintArray",
                    "(Ljava/lang/Object;[[I)[[I", false);
            if (!(Configuration.taintTagFactory instanceof DataAndControlFlowTagFactory)) {
                super.visitInsn(DUP);
                super.visitInsn(ICONST_2);
                super.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(TaintTagFactory.class),
                        "generateEmptyTaintArray", "([Ljava/lang/Object;I)V", false);
            }
            super.visitInsn(SWAP);
            FrameNode fn2 = getCurrentFrameNode();

            super.visitJumpInsn(GOTO, done);
            super.visitLabel(isNull);
            acceptFn(fn);
            super.visitInsn(ACONST_NULL);
            super.visitInsn(SWAP);
            super.visitLabel(done);
            acceptFn(fn2);

        } else if (arrayType.getDimensions() == 3) {
            FrameNode fn = getCurrentFrameNode();
            super.visitInsn(DUP);
            super.visitInsn(TaintUtils.IGNORE_EVERYTHING);
            super.visitJumpInsn(IFNULL, isNull);
            super.visitInsn(TaintUtils.IGNORE_EVERYTHING);
            super.visitInsn(DUP);
            super.visitInsn(DUP);
            super.visitInsn(ARRAYLENGTH);
            super.visitMultiANewArrayInsn("[[[I", 1);
            super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintUtils.class), "create3DTaintArray",
                    "(Ljava/lang/Object;[[[I)[[[I", false);
            if (!(Configuration.taintTagFactory instanceof DataAndControlFlowTagFactory)) {
                super.visitInsn(DUP);
                super.visitInsn(ICONST_3);
                super.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(TaintTagFactory.class),
                        "generateEmptyTaintArray", "([Ljava/lang/Object;I)V", false);
            }
            super.visitInsn(SWAP);
            FrameNode fn2 = getCurrentFrameNode();

            super.visitJumpInsn(GOTO, done);

            super.visitLabel(isNull);
            acceptFn(fn);
            super.visitInsn(ACONST_NULL);
            super.visitInsn(SWAP);
            super.visitLabel(done);
            acceptFn(fn2);
        } else if (arrayType.getDimensions() == 1) {
            FrameNode fn = getCurrentFrameNode();
            super.visitInsn(DUP);
            super.visitInsn(TaintUtils.IGNORE_EVERYTHING);
            super.visitJumpInsn(IFNULL, isNull);
            super.visitInsn(TaintUtils.IGNORE_EVERYTHING);
            Type wrapType = MultiDArrayUtils.getTypeForType(arrayType);

            super.visitInsn(DUP);
            super.visitTypeInsn(NEW, wrapType.getInternalName());
            super.visitInsn(DUP_X1);
            super.visitInsn(SWAP);
            super.visitMethodInsn(INVOKESPECIAL, wrapType.getInternalName(), "<init>",
                    "(" + arrayType.getDescriptor() + ")V", false);
            super.visitInsn(SWAP);
            FrameNode fn2 = getCurrentFrameNode();

            super.visitJumpInsn(GOTO, done);

            super.visitLabel(isNull);
            acceptFn(fn);
            super.visitInsn(ACONST_NULL);
            super.visitInsn(SWAP);
            super.visitLabel(done);
            acceptFn(fn2);
        } else {
            throw new IllegalStateException(
                    "Can't handle casts to multi-d array type of dimension " + arrayType.getDimensions());
        }
    }

    protected void freeLVs(LocalVariableNode[] lvArray) {
        for (LocalVariableNode n : lvArray) {
            lvs.freeTmpLV(n.index);
        }
    }

    protected void loadLV(int n, LocalVariableNode[] lvArray) {
        super.visitVarInsn(Type.getType(lvArray[n].desc).getOpcode(ILOAD), lvArray[n].index);
        if (lvArray[n].signature != null && lvArray[n].signature.equals("true")) {
            analyzer.setTopOfStackTagged();
        }
    }

    /**
     * Generates instructions to modify stack taint tags based on stack operations
     * DUP2_X1 will dup the top 2 elements under the 1 beneath them.
     */
    protected void DUPN_XU(int n, int u) {
        int stackShadowVar = lvs.getStackShadowVar(analyzer.numberOfVariablesOnStack - 1);
        switch (n) {
            case 1:
                switch (u) {
                    case 1:
                        super.visitVarInsn(ALOAD, stackShadowVar - 1);
                        super.visitVarInsn(ALOAD, stackShadowVar);
                        super.visitInsn(DUP);
                        super.visitVarInsn(ASTORE, stackShadowVar + 1);
                        super.visitVarInsn(ASTORE, stackShadowVar - 1);
                        super.visitVarInsn(ASTORE, stackShadowVar);
                        return;
                    case 2:
                        super.visitVarInsn(ALOAD, stackShadowVar - 2);
                        super.visitVarInsn(ALOAD, stackShadowVar - 1);
                        super.visitVarInsn(ALOAD, stackShadowVar);
                        super.visitInsn(DUP);
                        super.visitVarInsn(ASTORE, stackShadowVar + 1);
                        super.visitVarInsn(ASTORE, stackShadowVar - 2);
                        super.visitVarInsn(ASTORE, stackShadowVar);
                        super.visitVarInsn(ASTORE, stackShadowVar - 1);
                        return;
                    default:
                        throw new UnsupportedOperationException();
                }
            case 2:
                switch (u) {
                    case 1:
                        // A B C -> B C A B C
                        super.visitVarInsn(ALOAD, stackShadowVar - 2);
                        super.visitVarInsn(ALOAD, stackShadowVar - 1);
                        super.visitVarInsn(ALOAD, stackShadowVar);
                        super.visitInsn(DUP);
                        super.visitVarInsn(ASTORE, stackShadowVar + 2);
                        super.visitVarInsn(ASTORE, stackShadowVar - 1);
                        super.visitInsn(DUP);
                        super.visitVarInsn(ASTORE, stackShadowVar + 1);
                        super.visitVarInsn(ASTORE, stackShadowVar - 2);
                        super.visitVarInsn(ASTORE, stackShadowVar);
                        return;
                    case 2:
                        // A B C D -> C D A B C D
                        super.visitVarInsn(ALOAD, stackShadowVar - 3);
                        super.visitVarInsn(ALOAD, stackShadowVar - 2);
                        super.visitVarInsn(ALOAD, stackShadowVar - 1);
                        super.visitVarInsn(ALOAD, stackShadowVar);
                        super.visitInsn(DUP);
                        super.visitVarInsn(ASTORE, stackShadowVar + 2);
                        super.visitVarInsn(ASTORE, stackShadowVar - 2);
                        super.visitInsn(DUP);
                        super.visitVarInsn(ASTORE, stackShadowVar + 1);
                        super.visitVarInsn(ASTORE, stackShadowVar - 3);
                        super.visitVarInsn(ASTORE, stackShadowVar);
                        super.visitVarInsn(ASTORE, stackShadowVar - 1);
                        return;
                    default:
                        throw new UnsupportedOperationException();
                }
        }

    }

    public void acceptFn(FrameNode fn) {
        acceptFn(fn, mv);
    }

    public FrameNode getCurrentFrameNode() {
        return getCurrentFrameNode(analyzer);
    }

    /**
     * Stores the top n stack elements as local variables. Returns an array of
     * all of the lv indices. return[0] is the top element.
     */
    protected LocalVariableNode[] storeToLocals(int n) {
        LocalVariableNode[] ret = new LocalVariableNode[n];
        for (int i = 0; i < n; i++) {
            Type elType;
            if (analyzer.stack.get(analyzer.stack.size() - 1) == Opcodes.TOP) {
                elType = getTypeForStackType(analyzer.stack.get(analyzer.stack.size() - 2));
            } else {
                elType = getTypeForStackType(analyzer.stack.get(analyzer.stack.size() - 1));
            }
            ret[i] = new LocalVariableNode(null, elType.getDescriptor(), null, null, null, lvs.getTmpLV());
            super.visitVarInsn(elType.getOpcode(ISTORE), ret[i].index);
        }
        return ret;
    }

    public void push(final int value) {
        if (value >= -1 && value <= 5) {
            super.visitInsn(Opcodes.ICONST_0 + value);
        } else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            super.visitIntInsn(Opcodes.BIPUSH, value);
        } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
            super.visitIntInsn(Opcodes.SIPUSH, value);
        } else {
            super.visitLdcInsn(value);
        }
    }

    public static final Type getTagType(String internalName) {
        if (canRawTaintAccess(internalName)) {
            return taintTagType;
        }
        return Type.INT_TYPE;
    }

    public static final boolean canRawTaintAccess(String internalName) {
        return (!(internalName.equals("java/lang/Float") ||
                internalName.equals("java/lang/Character") ||
                internalName.equals("java/lang/Double") || internalName.equals("java/lang/Integer") ||
                internalName.equals("java/lang/Long") || internalName.equals("java/lang/StackTraceElement")));
    }

    public static Object[] removeLongsDoubleTopVal(List<Object> in) {
        List<Object> ret = new ArrayList<>();
        boolean lastWas2Word = false;
        for (Object n : in) {
            if ((n == Opcodes.TOP || (n instanceof TaggedValue && ((TaggedValue) n).v == Opcodes.TOP))
                    && lastWas2Word) {
                // nop
            } else {
                ret.add(n);
            }
            lastWas2Word = n == Opcodes.DOUBLE || n == Opcodes.LONG || (n instanceof TaggedValue
                    && (((TaggedValue) n).v == Opcodes.DOUBLE || ((TaggedValue) n).v == Opcodes.LONG));
        }
        return ret.toArray();
    }

    static int[][][] foo(int[][][] in) {
        int[][][] ret = new int[in.length][][];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = new int[in[i].length][];
            for (int j = 0; j < ret[i].length; j++) {
                ret[i][j] = new int[in[i][j].length];
            }
        }
        return ret;

    }

    static int[][] foo(int[][] in) {
        int[][] ret = new int[in.length][];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = new int[in[i].length];
        }
        return ret;
    }

    public static void createNewTaintArray(String arrayDesc, NeverNullArgAnalyzerAdapter analyzer, MethodVisitor mv,
            LocalVariableManager lvs) {
        Type arrayType = Type.getType(arrayDesc);
        Type wrapper = TaintUtils.getWrapperType(arrayType);
        Label isNull = new Label();
        Label done = new Label();
        Object[] locals1 = removeLongsDoubleTopVal(analyzer.locals);
        int localSize1 = locals1.length;

        Object[] stack1 = removeLongsDoubleTopVal(analyzer.stack);
        int stackSize1 = stack1.length;

        mv.visitInsn(Opcodes.DUP);
        mv.visitJumpInsn(Opcodes.IFNULL, isNull);

        if (arrayType.getDimensions() == 2) {
            mv.visitInsn(DUP);
            mv.visitInsn(DUP);
            mv.visitInsn(ARRAYLENGTH);
            Type toUse = MultiDArrayUtils.getTypeForType(arrayType);
            if (toUse.getSort() == Type.ARRAY) {
                toUse = toUse.getElementType();
            }
            mv.visitMultiANewArrayInsn(toUse.getDescriptor(), 1);
            mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintUtils.class), "create2DTaintArray",
                    "(Ljava/lang/Object;[[Ljava/lang/Object;)[[Ljava/lang/Object;", false);
            mv.visitInsn(SWAP);
        } else if (arrayType.getDimensions() == 3) {
            mv.visitInsn(DUP);
            mv.visitInsn(DUP);
            mv.visitInsn(ARRAYLENGTH);
            Type toUse = MultiDArrayUtils.getTypeForType(arrayType);
            if (toUse.getSort() == Type.ARRAY) {
                toUse = toUse.getElementType();
            }
            mv.visitMultiANewArrayInsn("[" + toUse.getDescriptor(), 1);
            mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintUtils.class), "create3DTaintArray",
                    "(Ljava/lang/Object;[[[Ljava/lang/Object;)[[[Ljava/lang/Object;", false);
            mv.visitInsn(SWAP);
        } else if (arrayType.getDimensions() > 1) {
            int tmp = lvs.getTmpLV(arrayType);
            mv.visitInsn(DUP);
            mv.visitVarInsn(ASTORE, tmp);

            for (int i = 0; i < arrayType.getDimensions(); i++) {
                mv.visitVarInsn(ALOAD, tmp);

                for (int j = 0; j < i; j++) {
                    mv.visitInsn(ICONST_0);
                    mv.visitInsn(AALOAD);
                }
                mv.visitInsn(Opcodes.ARRAYLENGTH);
            }
            mv.visitMultiANewArrayInsn(arrayDesc.substring(0, arrayDesc.length() - 1) + Configuration.TAINT_TAG_DESC,
                    arrayType.getDimensions()); // TODO XXX this won't be properly initialized.

        } else {
            mv.visitTypeInsn(NEW, wrapper.getInternalName());
            mv.visitInsn(DUP_X1);
            mv.visitInsn(SWAP);
            if (arrayType.getElementType().getSort() == Type.OBJECT) {
                mv.visitMethodInsn(INVOKESPECIAL, wrapper.getInternalName(), "<init>", "([Ljava/lang/Object;)V", false);
            } else {
                mv.visitMethodInsn(INVOKESPECIAL, wrapper.getInternalName(), "<init>", "(" + arrayDesc + ")V", false);
            }
        }

        Object[] locals = removeLongsDoubleTopVal(analyzer.locals);
        int localSize = locals.length;

        Object[] stack = removeLongsDoubleTopVal(analyzer.stack);
        int stackSize = stack.length;

        mv.visitJumpInsn(Opcodes.GOTO, done);
        mv.visitFrame(TaintUtils.RAW_INSN, localSize1, locals1, stackSize1, stack1);

        mv.visitLabel(isNull);

        mv.visitTypeInsn(CHECKCAST, wrapper.getInternalName());
        mv.visitLabel(done);
        mv.visitFrame(TaintUtils.RAW_INSN, localSize, locals, stackSize, stack);
    }

    public static Type getTypeForStackType(Object obj) {
        if (obj instanceof TaggedValue) {
            obj = ((TaggedValue) obj).v;
        }
        if (obj == Opcodes.INTEGER) {
            return Type.INT_TYPE;
        }
        if (obj == Opcodes.FLOAT) {
            return Type.FLOAT_TYPE;
        }
        if (obj == Opcodes.DOUBLE) {
            return Type.DOUBLE_TYPE;
        }
        if (obj == Opcodes.LONG) {
            return Type.LONG_TYPE;
        }
        if (obj == Opcodes.NULL) {
            return Type.getType("Ljava/lang/Object;");
        }
        if (obj instanceof String) {
            return Type.getObjectType((String) obj);
        }
        if (obj instanceof Label || obj == Opcodes.UNINITIALIZED_THIS) {
            return Type.getType("Luninitialized;");
        }
        throw new IllegalArgumentException("Unexpected stack type, got:" + obj);
    }

    public static boolean isPrimitiveType(Type t) {
        if (t == null) {
            return false;
        }
        switch (t.getSort()) {
            case Type.ARRAY:
                return t.getElementType().getSort() != Type.OBJECT && t.getDimensions() == 1;
            case Type.OBJECT:
            case Type.VOID:
                return false;
            default:
                return true;
        }
    }

    public static int getStackElementSize(Object obj) {
        if (obj instanceof TaggedValue) {
            obj = ((TaggedValue) obj).v;
        }
        if (obj == Opcodes.DOUBLE || obj == Opcodes.LONG || obj == Opcodes.TOP) {
            return 2;
        }
        return 1;
    }

    private static Object[] asArray(final java.util.List<Object> l) {
        Object[] objs = new Object[l.size()];
        for (int i = 0; i < objs.length; ++i) {
            Object o = l.get(i);
            if (o instanceof LabelNode) {
                o = ((LabelNode) o).getLabel();
            }
            objs[i] = o;
        }
        return objs;
    }

    public static void acceptFn(FrameNode fn, MethodVisitor mv) {
        switch (fn.type) {
            case Opcodes.F_NEW:
            case Opcodes.F_FULL:
            case TaintUtils.RAW_INSN:
                mv.visitFrame(fn.type, fn.local.size(), asArray(fn.local), fn.stack.size(),
                        asArray(fn.stack));
                break;
            case Opcodes.F_APPEND:
                mv.visitFrame(fn.type, fn.local.size(), asArray(fn.local), 0, null);
                break;
            case Opcodes.F_CHOP:
                mv.visitFrame(fn.type, fn.local.size(), null, 0, null);
                break;
            case Opcodes.F_SAME:
                mv.visitFrame(fn.type, 0, null, 0, null);
                break;
            case Opcodes.F_SAME1:
                mv.visitFrame(fn.type, 0, null, 1, asArray(fn.stack));
                break;
        }
    }

    public static FrameNode getCurrentFrameNode(NeverNullArgAnalyzerAdapter a) {
        if (a.locals == null || a.stack == null) {
            throw new IllegalArgumentException();
        }
        Object[] locals = removeLongsDoubleTopVal(a.locals);
        Object[] stack = removeLongsDoubleTopVal(a.stack);
        FrameNode ret = new FrameNode(Opcodes.F_NEW, locals.length, locals, stack.length, stack);
        ret.type = TaintUtils.RAW_INSN;
        return ret;
    }
    /**
     * Stores the taint tag on the top of the stack in the corresponding slot
     * for the value immediately beneath it.
     * Assumes that there are no other taint tags on the stack, and that the stack
     * represents the uninstrumented state - if there are instrumentation artifacts
     * on the stack
     * this will silently be wrong.
     */
    public void storeStackTopShadowVar() {
        mv.visitVarInsn(ASTORE, lvs.getStackShadowVarFromTop(1));
    }

    public void loadStackTopShadowVar() {
        mv.visitVarInsn(ALOAD, lvs.getStackShadowVarFromTop(0));
    }

    /**
     * Loads the phosphor stack frame to the top of the stack
     */
    public void pushPhosphorStackFrame(){
        mv.visitVarInsn(ALOAD, getIndexOfPhosphorStackData());
    }

    /**
     * Retrieves the last taint tag pushed to the current phosphor stack frame
     */
    public void retrieveReturnTaint(){
        pushPhosphorStackFrame();
        GET_RETURN_TAINT.delegateVisit(mv);
    }

    /**
     * Given: Top element on stack is a value
     * Sets the taint tag for the phosphor stackframe to be the taint tag for that value
     */
    public void pushReturnTaint(){
        int lv = lvs.getStackShadowVarFromTop(0);
        pushPhosphorStackFrame();
        super.visitVarInsn(ALOAD, lv);
        SET_RETURN_TAINT.delegateVisit(mv);
    }

    public int getIndexOfPhosphorStackData(){
        return this.lvs.getLocalVariableAdder().getIndexOfPhosphorStackData();
    }

    protected static boolean shouldEnsureArgsAreUnwrapped(String owner, String name, String desc){
        return owner.equals("java/lang/Class") && desc.contains("Ljava/lang/Object;") && !name.equals("cast");
        //return desc.contains("Ljava/lang/Object;");
    }

    /**
    We wrap all arrays on the stack so that we can track the taint tags for the array with the array itself.
    No matter what we do with 1-d arrays, this is always necessary for multi-d arrays, so we will always need
     to do some special handling to unwrap them when we call any method, because we are not instrumenting method
     descriptors.
     */
    public void unwrapArraysForCallTo(String owner, String name, String desc){
        Type[] args = Type.getArgumentTypes(desc);
        boolean forceUnwrapObjects = shouldEnsureArgsAreUnwrapped(owner, name, desc);
        boolean hasArgsToUnwrap = false;
        for(Type t : args){
           if(TaintUtils.isWrappedType(t)){
               hasArgsToUnwrap = true;
               break;
           }
        }
        if (!hasArgsToUnwrap && !forceUnwrapObjects) {
            return;
        }
        int[] tmp = new int[args.length];
        Type[] ts = new Type[args.length];
        for (int i = 0; i < args.length; i++) {
            int idxOfThisArg = args.length - i - 1; //recording args from last first
            Type expected = args[idxOfThisArg];
            Type t = getTopOfStackType();
            ts[i] = t;
            tmp[i] = lvs.getTmpLV(t);
            if (forceUnwrapObjects && expected.getDescriptor().equals("Ljava/lang/Object;") && !topOfStackIsNull()) {
                super.visitInsn(DUP);
                pushPhosphorStackFrame();
                super.visitInsn(SWAP);
                push(idxOfThisArg);
                SET_ARG_WRAPPER.delegateVisit(mv);
                ENSURE_UNBOXED.delegateVisit(mv);
            } else if (TaintUtils.isWrappedType(expected) && !topOfStackIsNull() && t.getSort() != Type.ARRAY) {
                super.visitInsn(DUP);
                pushPhosphorStackFrame();
                super.visitInsn(SWAP);
                push(idxOfThisArg);
                SET_ARG_WRAPPER.delegateVisit(mv);
                //For 1-d arrays: still pass  the actual array
                if (expected.getDimensions() == 1) {
                    Type unwrapped = TaintUtils.getUnwrappedType(t);
                    super.visitMethodInsn(INVOKESTATIC, t.getInternalName(), "unwrap", "(" + t.getDescriptor() + ")" + unwrapped.getDescriptor(), false);
                    if (unwrapped.getDescriptor().equals("[Ljava/lang/Object;")) {
                        super.visitTypeInsn(CHECKCAST, expected.getInternalName());
                    }
                } else {
                    // Greater than 1d: can't make it line up...
                    super.visitInsn(POP);
                    super.visitInsn(ACONST_NULL);
                }
            }
            super.visitVarInsn(t.getOpcode(ISTORE), tmp[i]);
        }
        for (int i = args.length - 1; i >= 0; i--) {
            super.visitVarInsn(ts[i].getOpcode(ILOAD), tmp[i]);
            lvs.freeTmpLV(tmp[i]);
        }
    }
    protected static String getMethodKeyForStackFrame(String name, String desc,
                                                      boolean polymorphicSignatureMethod) {
        return name + getStackFrameDesc(desc, polymorphicSignatureMethod);
    }

    private static String getStackFrameDesc(String desc, boolean polymorphicSignatureMethod) {
        if (polymorphicSignatureMethod) {
            StringBuilder sig = new StringBuilder("(");
            Type[] args = Type.getArgumentTypes(desc);
            for (Type t : args) {
                if (t.getSort() == Type.ARRAY) {
                    sig.append("Ljava/lang/Object;");
                } else {
                    sig.append(t.getDescriptor());
                }
            }
            return sig.append(')').toString();
        } else {
            return desc.substring(0, 1 + desc.indexOf(')'));
        }
    }

    protected static int getHashForStackFrame(String name, String desc, boolean polymorphicSignatureMethod) {
        return PhosphorStackFrame.computeFrameHash(name, getStackFrameDesc(desc, polymorphicSignatureMethod));
    }

    protected  static String fixDescForStack(String className, String methodDesc) {
        String descForStackFrame = methodDesc;
        if (className.startsWith("java/lang/invoke/VarHandle")) {
            // There are methods that the JDK will internally resolve to and prefix the arguments
            // with a handle to the underlying object.
            // Remove the parameter for the handle to the underlying object as the caller does not
            // explicitly pass the handle.
            if (methodDesc.startsWith("(Ljava/lang/invoke/VarHandle")) {
                descForStackFrame = "(" + methodDesc.substring(methodDesc.indexOf(";") + 1);
            }
        }
        return descForStackFrame;
    }

    protected void prepareMetadataLocalVariables(){
        if (this.initializePhosphorStackFrame) {
            String descForStackFrame = fixDescForStack(className, methodDesc);
            if (Configuration.DEBUG_STACK_FRAME_WRAPPERS) {
                super.visitLdcInsn(getMethodKeyForStackFrame(this.methodName, descForStackFrame, false));
                STACK_FRAME_FOR_METHOD_DEBUG.delegateVisit(mv);
            } else {
                push(getHashForStackFrame(this.methodName, descForStackFrame, false));
                STACK_FRAME_FOR_METHOD_FAST.delegateVisit(mv);
            }
            super.visitInsn(DUP);
            GET_AND_CLEAR_CLEANUP_FLAG.delegateVisit(mv);
            super.visitInsn(POP);
            super.visitVarInsn(ASTORE, getIndexOfPhosphorStackData());
        } else if (Configuration.DEBUG_STACK_FRAME_WRAPPERS) {
            String descForStackFrame = fixDescForStack(className, methodDesc)
                    .replace(PhosphorStackFrame.DESCRIPTOR, "");
            super.visitVarInsn(ALOAD, getIndexOfPhosphorStackData());
            super.visitLdcInsn(getMethodKeyForStackFrame(this.methodName, descForStackFrame, false));
            CHECK_STACK_FRAME_TARGET.delegateVisit(mv);
        }
        for (int i = this.lvs.getLocalVariableAdder().getIndexOfFirstStackTaintTag(); i < this.lvs.getLocalVariableAdder().getIndexOfLastStackTaintTag(); i++) {
            TaintMethodRecord.NEW_EMPTY_TAINT.delegateVisit(mv);
            super.visitVarInsn(Opcodes.ASTORE, i);
        }
    }
}
