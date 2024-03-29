package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.commons.OurLocalVariablesSorter;
import edu.columbia.cs.psl.phosphor.runtime.PhosphorStackFrame;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.StringBuilder;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.*;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;

public class LocalVariableManager extends OurLocalVariablesSorter implements Opcodes {

    private int indexOfMasterControlLV = -1;
    Map<Integer, Integer> varToShadowVar = new HashMap<>();
    public Set<LocalVariableNode> createdLVs = new HashSet<>();
    public Label end;
    public Label newStartLabel = new Label();
    private boolean isIgnoreEverything = false;
    private Map<Integer, Integer> origLVMap = new HashMap<>();
    private Map<Integer, Integer> shadowLVMap = new HashMap<>();
    private Map<Integer, Object> shadowLVMapType = new HashMap<>();
    private Set<Integer> tmpLVIndices = new HashSet<>();
    private List<TmpLV> tmpLVs = new ArrayList<>();
    private boolean endVisited = false;
    private PrimitiveArrayAnalyzer primitiveArrayFixer;
    private int createdLVIdx = 0;
    private Map<Integer, LocalVariableNode> curLocalIdxToLVNode = new HashMap<>();
    private MethodVisitor uninstMV;
    private NeverNullArgAnalyzerAdapter analyzer;
    private boolean isInMethodThatIsTooBig;
    private boolean generateExtraDebug;
    private Type[] argumentTypes;
    private boolean isStatic;
    private boolean disabled = false;
    private Label oldStartLabel;
    int line = 0;
    private Label permanentLocalVariableStartLabel;
    private LocalVariableAdder localVariableAdder;
    private String name;
    private String originalDesc;

    private HashMap<String, Integer> maxStackPerMethodMap;

    public LocalVariableManager(int access, String name, String originalDesc, String desc, MethodVisitor mv, NeverNullArgAnalyzerAdapter analyzer,
            MethodVisitor uninstMV, boolean generateExtraDebug, HashMap<String, Integer> maxStackPerMethodMap) {
        super(Configuration.ASM_VERSION, access, desc, mv);
        this.analyzer = analyzer;
        this.uninstMV = uninstMV;
        this.name = name;
        this.originalDesc = originalDesc;
        this.maxStackPerMethodMap = maxStackPerMethodMap;
        argumentTypes = Type.getArgumentTypes(desc);
        if ((access & Opcodes.ACC_STATIC) == 0) {
        } else {
            isStatic = true;
        }
        int lastArg;
        lastArg = isStatic ? 0 : 1;
        Type[] args = Type.getArgumentTypes(desc);
        for (Type t : args) {
            lastArg += t.getSize();
        }
        end = new Label();
        this.generateExtraDebug = generateExtraDebug;
    }

    public String patchDescToAcceptPhosphorStackFrameAndPushIt(String desc, MethodVisitor mv){
        if(desc.contains(PhosphorStackFrame.DESCRIPTOR)){
            return desc;
        }
        int var = getLocalVariableAdder().getIndexOfPhosphorStackData();
        StringBuilder sb = new StringBuilder();
        int closeArgPos = desc.indexOf(')');
        sb.append(desc.substring(0,closeArgPos));
        sb.append(PhosphorStackFrame.DESCRIPTOR);
        sb.append(desc.substring(closeArgPos));
        mv.visitVarInsn(ALOAD, var);
        return sb.toString();
    }

    public int getStackShadowVar(int offsetFromBottomOfStackAsZero) {
        return this.localVariableAdder.getIndexOfFirstStackTaintTag() + offsetFromBottomOfStackAsZero;
    }

    public int getStackShadowVarFromTop(int offsetFromTopOfStackAsZero) {
        if(analyzer.numberOfVariablesOnStack < offsetFromTopOfStackAsZero + 1){
            throw new IllegalStateException("Asked for taint at offset from top=" + offsetFromTopOfStackAsZero + " but stack was: "  +analyzer.stack);
        }
        return getStackShadowVar(analyzer.numberOfVariablesOnStack - offsetFromTopOfStackAsZero - 1);
    }

    @Override
    public void visitInsn(int opcode) {
        //if (opcode == TaintUtils.IGNORE_EVERYTHING) {
        //    isIgnoreEverything = !isIgnoreEverything;
        //}
        super.visitInsn(opcode);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        if (isIgnoreEverything) {
            isInMethodThatIsTooBig = true;
            mv.visitIincInsn(var, increment);
        } else {
            super.visitIincInsn(var, increment);
        }
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        if (isIgnoreEverything) {
            mv.visitVarInsn(opcode, var);
            return;
        }
        if (opcode == TaintUtils.IGNORE_EVERYTHING) {
            isInMethodThatIsTooBig = true;
        }
        super.visitVarInsn(opcode, var);
    }

    public void freeTmpLV(int idx) {
        for (TmpLV v : tmpLVs) {
            if (v.idx == idx && v.inUse) {
                Label lbl = new Label();
                super.visitLabel(lbl);
                curLocalIdxToLVNode.get(v.idx).end = new LabelNode(lbl);
                v.inUse = false;
                v.owner = null;
                if (analyzer.locals != null && idx < analyzer.locals.size()) {
                    analyzer.locals.set(idx, Opcodes.TOP);
                }
                return;
            }
        }
        throw new IllegalArgumentException("asked to free tmp lv " + idx + " but couldn't find it?");
    }

    @Deprecated
    public int newLocal(Type type) {
        throw new UnsupportedOperationException();
    }

    int newShadowLV(Type type, int shadows) {
        int idx = super.newLocal(type);
        Label lbl = new Label();
        super.visitLabel(lbl);
        String shadowName = null;
        if (primitiveArrayFixer != null) {
            for (Object o : primitiveArrayFixer.mn.localVariables) {
                LocalVariableNode lv = (LocalVariableNode) o;
                int id = remap(lv.index,
                        Type.getType(lv.desc));
                if (id == shadows) {
                    shadowName = lv.name + "$$PHOSPHORTAG";
                }
            }
        }
        if (shadowName == null) {
            shadowName = "phosphorShadowLVFor" + shadows + "XX" + createdLVIdx;
        }
        LocalVariableNode newLVN = new LocalVariableNode(shadowName, type.getDescriptor(), null, new LabelNode(lbl),
                new LabelNode(end), idx);
        createdLVs.add(newLVN);
        curLocalIdxToLVNode.put(idx, newLVN);
        createdLVIdx++;
        shadowLVMap.put(idx, origLVMap.get(shadows));
        varToShadowVar.put(shadows, idx);
        return idx;
    }

    public int getIndexOfMasterControlLV() {
        return indexOfMasterControlLV;
    }

    public void createMasterControlStackLV() {
        indexOfMasterControlLV = createPermanentLocalVariable(Configuration.controlFlowManager.getControlStackClass(),
                "phosphorJumpControlTag");
    }

    public int createPermanentLocalVariable(Class<?> clazz, String name) {
        if (permanentLocalVariableStartLabel == null) {
            permanentLocalVariableStartLabel = new Label();
            visitLabel(permanentLocalVariableStartLabel);
        }
        Type type = Type.getType(clazz);
        int index = super.newLocal(type);
        LocalVariableNode newLVN = new LocalVariableNode(name, type.getDescriptor(), null,
                new LabelNode(permanentLocalVariableStartLabel), new LabelNode(end), index);
        createdLVs.add(newLVN);
        analyzer.locals.add(index, type.getInternalName());
        return index;
    }

    /**
     * Gets a tmp lv capable of storing the top stack el
     */
    public int getTmpLV() {
        Object obj = analyzer.stack.get(analyzer.stack.size() - 1);
        if (obj instanceof String) {
            return getTmpLV(Type.getObjectType((String) obj));
        }
        if (obj == Opcodes.INTEGER) {
            return getTmpLV(Type.INT_TYPE);
        }
        if (obj == Opcodes.FLOAT) {
            return getTmpLV(Type.FLOAT_TYPE);
        }
        if (obj == Opcodes.DOUBLE) {
            return getTmpLV(Type.DOUBLE_TYPE);
        }
        if (obj == Opcodes.LONG) {
            return getTmpLV(Type.LONG_TYPE);
        }
        if (obj == Opcodes.TOP) {
            obj = analyzer.stack.get(analyzer.stack.size() - 2);
            if (obj == Opcodes.DOUBLE) {
                return getTmpLV(Type.DOUBLE_TYPE);
            }
            if (obj == Opcodes.LONG) {
                return getTmpLV(Type.LONG_TYPE);
            }
        }
        return getTmpLV(Type.getType("Ljava/lang/Object;"));
    }

    public int getTmpLV(Type t) {
        if (t.getDescriptor().equals("java/lang/Object;")) {
            throw new IllegalArgumentException();
        }
        for (TmpLV lv : tmpLVs) {
            if (!lv.inUse && lv.type.getSize() == t.getSize()) {
                if (!lv.type.equals(t)) {
                    // End the old LV node, make a new one
                    Label lbl = new Label();
                    super.visitLabel(lbl);
                    LocalVariableNode newLVN = new LocalVariableNode("phosphorTempStack" + createdLVIdx,
                            t.getDescriptor(), null, new LabelNode(lbl), new LabelNode(end), lv.idx);
                    createdLVs.add(newLVN);
                    curLocalIdxToLVNode.put(lv.idx, newLVN);
                    createdLVIdx++;
                    remapLocal(lv.idx, t);
                    if (analyzer.locals != null && lv.idx < analyzer.locals.size()) {
                        analyzer.locals.set(lv.idx, TaintUtils.getStackTypeForType(t));
                    }
                    lv.type = t;
                }
                lv.inUse = true;
                return lv.idx;
            }
        }

        int idx = super.newLocal(t);
        Label lbl = new Label();
        super.visitLabel(lbl);

        LocalVariableNode newLVN = new LocalVariableNode("phosphorTempStack" + createdLVIdx, t.getDescriptor(), null,
                new LabelNode(lbl), new LabelNode(end), idx);
        createdLVs.add(newLVN);
        curLocalIdxToLVNode.put(idx, newLVN);
        createdLVIdx++;

        TmpLV newLV = new TmpLV();
        newLV.idx = idx;
        newLV.type = t;
        newLV.inUse = true;
        tmpLVs.add(newLV);
        tmpLVIndices.add(newLV.idx);
        return newLV.idx;
    }

    @Override
    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
        if (start == oldStartLabel) {
            start = this.newStartLabel;
        }
        if (!Configuration.SKIP_LOCAL_VARIABLE_TABLE) {
            if (varToShadowVar.containsKey(index)) {
                int shadowVar = varToShadowVar.get(index);
                if (curLocalIdxToLVNode.containsKey(shadowVar)) {
                    LocalVariableNode n = curLocalIdxToLVNode.get(shadowVar);
                    uninstMV.visitLocalVariable(n.name, n.desc, n.signature, start, end, n.index);
                }
            }
        }
        super.visitLocalVariable(name, desc, signature, start, end, index);
        if (!createdLVs.isEmpty()) {
            if (!endVisited) {
                super.visitLabel(this.end);
                endVisited = true;
            }
            createdLVs.clear();
        }
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        if (!endVisited) {
            super.visitLabel(end);
            endVisited = true;
        }
        if (generateExtraDebug && !Configuration.SKIP_LOCAL_VARIABLE_TABLE) {
            int n = 0;
            if (!isStatic) {
                super.visitLocalVariable("argidx" + n, "Ljava/lang/Object;", null, this.newStartLabel, this.end, n);
                n++;
            }
            for (Type t : argumentTypes) {
                super.visitLocalVariable("argidx" + n, t.getDescriptor(), null, this.newStartLabel, this.end, n);
                n += t.getSize();
            }
        }
        super.visitMaxs(maxStack, maxLocals);
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        for (TmpLV l : tmpLVs) {
            if (l.inUse) {
                throw l.owner;
            }
        }
    }

    @Override
    public void visitLabel(Label label) {
        if (oldStartLabel == null) {
            oldStartLabel = label;
        }
        super.visitLabel(label);
    }

    protected int remap(int var, Type type) {
        int ret = super.remap(var, type);
        origLVMap.put(ret, var);
        Object objType = "[I";
        switch (type.getSort()) {
            case Type.BOOLEAN:
            case Type.SHORT:
            case Type.INT:
                objType = Opcodes.INTEGER;
                break;
            case Type.LONG:
                objType = Opcodes.LONG;
                break;
            case Type.DOUBLE:
                objType = Opcodes.DOUBLE;
                break;
            case Type.FLOAT:
                objType = Opcodes.FLOAT;
                break;
        }
        shadowLVMapType.put(ret, objType);
        // System.out.println(var + " -> " + ret);
        return ret;
    }

    void setPrimitiveArrayAnalyzer(PrimitiveArrayAnalyzer primitiveArrayFixer) {
        this.primitiveArrayFixer = primitiveArrayFixer;

    }

    public void visitCode() {
        localVariableAdder.setMaxStack(maxStackPerMethodMap.get(name+originalDesc));
        this.setNumLocalVariablesAddedAfterArgs(localVariableAdder.getNumLocalVariablesAddedAfterArgs());
        super.visitCode();
        super.visitLabel(newStartLabel);
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        this.line = line;
        super.visitLineNumber(line, start);
    }

    @Override
    public void visitFrame(final int type, final int nLocal, final Object[] local, final int nStack,
            final Object[] stack) {
        if (type == TaintUtils.RAW_INSN) {
            mv.visitFrame(Opcodes.F_NEW, nLocal, local, nStack, stack);
            return;
        }
        if (type != Opcodes.F_NEW) { // uncompressed frame
            throw new IllegalStateException(
                    "ClassReader.accept() should be called with EXPAND_FRAMES flag");
        }
        if ((!changed && !isFirstFrame)) {
            // optimization for the case where mapping = identity
            mv.visitFrame(type, nLocal, local, nStack, stack);
            return;
        }
        isFirstFrame = false;
        // creates a copy of newLocals
        Object[] oldLocals = new Object[newLocals.length];
        System.arraycopy(newLocals, 0, oldLocals, 0, oldLocals.length);
        updateNewLocals(newLocals);

        for (int i = 0; i < newLocals.length; i++) {
            // Ignore tmp lv's in the stack frames.
            if (tmpLVIndices.contains(i)) {
                newLocals[i] = Opcodes.TOP;
            }
        }
        ArrayList<Object> locals = new ArrayList<>();
        for (int i = 0; i < nLocal; i++) {
            Object o = local[i];
            locals.add(o);
            if (o == Opcodes.DOUBLE || o == Opcodes.LONG) {
                locals.add(Opcodes.TOP);
            }
        }
        // copies types from 'local' to 'newLocals'
        // 'newLocals' currently empty
        int index = 0; // old local variable index
        int number = 0; // old local variable number
        for (; number < nLocal; ++number) {
            Object t = local[number];
            int size = t == Opcodes.LONG || t == Opcodes.DOUBLE ? 2 : 1;
            if (t != Opcodes.TOP) {
                Type typ = OBJECT_TYPE;
                if (t == Opcodes.INTEGER) {
                    typ = Type.INT_TYPE;
                } else if (t == Opcodes.FLOAT) {
                    typ = Type.FLOAT_TYPE;
                } else if (t == Opcodes.LONG) {
                    typ = Type.LONG_TYPE;
                } else if (t == Opcodes.DOUBLE) {
                    typ = Type.DOUBLE_TYPE;
                } else if (t instanceof String) {
                    typ = Type.getObjectType((String) t);
                }
                int remapped = remap(index, typ);
                setFrameLocal(remapped, t);
                if (!disabled && index >= this.firstLocal) {
                    Object shadowType = Configuration.TAINT_TAG_STACK_TYPE;
                    int shadowVar;
                    if (!varToShadowVar.containsKey(remapped)) {
                        shadowVar = newShadowLV(typ, remapped);
                    } else {
                        shadowVar = varToShadowVar.get(remapped);
                    }
                    setFrameLocal(shadowVar, shadowType);
                }
            }
            index += size;
        }

        for (int i : varToShadowVar.keySet()) {
            if (i < newLocals.length && newLocals[i] == null && varToShadowVar.get(i) < newLocals.length) {
                newLocals[varToShadowVar.get(i)] = Opcodes.TOP;
            } else if (i < newLocals.length && newLocals[i] != null
                    && !TaintUtils.isShadowedType(TaintAdapter.getTypeForStackType(newLocals[i]))
                    && varToShadowVar.get(i) < newLocals.length) {
                newLocals[varToShadowVar.get(i)] = Opcodes.TOP;
            }
        }
        // removes TOP after long and double types as well as trailing TOPs

        index = 0;
        number = 0;
        for (int i = 0; index < newLocals.length; ++i) {
            Object t = newLocals[index++];
            if (t != null && t != Opcodes.TOP) {
                newLocals[i] = t;
                number = i + 1;
                if (t == Opcodes.LONG || t == Opcodes.DOUBLE) {
                    index += 1;
                }
            } else {
                newLocals[i] = Opcodes.TOP;
            }

        }

        // visits remapped frame
        // System.out.println("LVM OUt " + Arrays.toString(newLocals));
        mv.visitFrame(type, number, newLocals, nStack, stack);
        // restores original value of 'newLocals'
        newLocals = oldLocals;
    }

    public void disable() {
        disabled = true;
    }

    public Map<Integer, Integer> getVarToShadowVar() {
        return Collections.unmodifiableMap(varToShadowVar);
    }

    public void setLocalVariableAdder(LocalVariableAdder localVariableAdder) {
        this.localVariableAdder = localVariableAdder;
    }

    public LocalVariableAdder getLocalVariableAdder() {
        return localVariableAdder;
    }

    private static class TmpLV {
        int idx;
        Type type;
        boolean inUse;
        IllegalStateException owner;

        @Override
        public String toString() {
            return "TmpLV [idx=" + idx + ", type=" + type + ", inUse=" + inUse + "]";
        }
    }

    public void setNumLocalVariablesAddedAfterArgs(int numLocalVariablesAddedAfterArgs) {
        if(this.firstLocal != this.nextLocal){
            throw new IllegalStateException("Must set this before creating new LVs");
        }
        this.firstLocal += numLocalVariablesAddedAfterArgs;
        this.nextLocal = this.firstLocal;
    }
}
