/***
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.columbia.cs.psl.phosphor.instrumenter.analyzer;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.PhosphorInstructionInfo;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.AnalyzerAdapter;

import static edu.columbia.cs.psl.phosphor.TaintUtils.RAW_INSN;

/**
 * A {@link MethodVisitor} that keeps track of stack map frame changes between
 * {@link #visitFrame(int, int, Object[], int, Object[]) visitFrame} calls. This
 * adapter must be used with the
 * {@link org.objectweb.asm.ClassReader#EXPAND_FRAMES} option. Each
 * visit<i>X</i> instruction delegates to the next visitor in the chain, if any,
 * and then simulates the effect of this instruction on the stack map frame,
 * represented by {@link #locals} and {@link #stack}. The next visitor in the
 * chain can get the state of the stack map frame <i>before</i> each instruction
 * by reading the value of these fields in its visit<i>X</i> methods (this
 * requires a reference to the AnalyzerAdapter that is before it in the chain).
 * If this adapter is used with a class that does not contain stack map table
 * attributes (i.e., pre Java 6 classes) then this adapter may not be able to
 * compute the stack map frame for each instruction. In this case no exception
 * is thrown but the {@link #locals} and {@link #stack} fields will be null for
 * these instructions.
 *
 * @author Eric Bruneton
 */
public class NeverNullArgAnalyzerAdapter extends MethodVisitor {

    /**
     * <code>List</code> of the local variable slots for current execution
     * frame. Primitive types are represented by {@link Opcodes#TOP},
     * {@link Opcodes#INTEGER}, {@link Opcodes#FLOAT}, {@link Opcodes#LONG},
     * {@link Opcodes#DOUBLE},{@link Opcodes#NULL} or
     * {@link Opcodes#UNINITIALIZED_THIS} (long and double are represented by
     * two elements, the second one being TOP). Reference types are represented
     * by String objects (representing internal names), and uninitialized types
     * by Label objects (this label designates the NEW instruction that created
     * this uninitialized value). This field is <tt>null</tt> for unreachable
     * instructions.
     */
    public List<Object> locals;

    /**
     * <code>List</code> of the operand stack slots for current execution frame.
     * Primitive types are represented by {@link Opcodes#TOP},
     * {@link Opcodes#INTEGER}, {@link Opcodes#FLOAT}, {@link Opcodes#LONG},
     * {@link Opcodes#DOUBLE},{@link Opcodes#NULL} or
     * {@link Opcodes#UNINITIALIZED_THIS} (long and double are represented by
     * two elements, the second one being TOP). Reference types are represented
     * by String objects (representing internal names), and uninitialized types
     * by Label objects (this label designates the NEW instruction that created
     * this uninitialized value). This field is <tt>null</tt> for unreachable
     * instructions.
     */
    public List<Object> stack;
    public List<Object> stackTagStatus;

    public List<Object> stackConstantVals;

    public boolean isFollowedByFrame;
    /**
     * Information about uninitialized types in the current execution frame.
     * This map associates internal names to Label objects. Each label
     * designates a NEW instruction that created the currently uninitialized
     * types, and the associated internal name represents the NEW operand, i.e.
     * the final, initialized type value.
     */
    public Map<Object, Object> uninitializedTypes;
    public String name;
    public List<Object> frameLocals;
    boolean noInsnsSinceListFrame = false;
    /**
     * The labels that designate the next instruction to be visited. May be
     * <tt>null</tt>.
     */
    private List<Label> labels;
    /**
     * The maximum stack size of this method.
     */
    private int maxStack;
    /**
     * The maximum number of local variables of this method.
     */
    private int maxLocals;
    /**
     * The owner's class name.
     */
    private String owner;
    private List<Object> args;
    private List<Object> argsFormattedForFrame;

    /**
     * Creates a new {@link NeverNullArgAnalyzerAdapter}. <i>Subclasses must not use this
     * constructor</i>. Instead, they must use the
     * {@link AnalyzerAdapter (int, String, int, String, String, MethodVisitor)}
     * version.
     *
     * @param owner  the owner's class name.
     * @param access the method's access flags (see {@link Opcodes}).
     * @param name   the method's name.
     * @param desc   the method's descriptor (see {@link Type Type}).
     * @param mv     the method visitor to which this adapter delegates calls. May
     *               be <tt>null</tt>.
     */
    public NeverNullArgAnalyzerAdapter(final String owner, final int access,
                                       final String name, final String desc, final MethodVisitor mv) {
        this(Configuration.ASM_VERSION, owner, access, name, desc, mv);
        this.name = name;
    }

    /**
     * Creates a new {@link NeverNullArgAnalyzerAdapter}.
     *
     * @param api    the ASM API version implemented by this visitor. Must be one
     *               of {@link Opcodes#ASM4}.
     * @param owner  the owner's class name.
     * @param access the method's access flags (see {@link Opcodes}).
     * @param name   the method's name.
     * @param desc   the method's descriptor (see {@link Type Type}).
     * @param mv     the method visitor to which this adapter delegates calls. May
     *               be <tt>null</tt>.
     */
    protected NeverNullArgAnalyzerAdapter(final int api, final String owner,
                                          final int access, final String name, final String desc,
                                          final MethodVisitor mv) {
        super(api, mv);
        this.owner = owner;
        locals = new ArrayList<>();
        frameLocals = new ArrayList<>();
        stack = new ArrayList<>();
        stackTagStatus = new ArrayList<>();
        stackConstantVals = new ArrayList<>();
        uninitializedTypes = new HashMap<>();
        args = new ArrayList<>();
        argsFormattedForFrame = new ArrayList<>();
        if((access & Opcodes.ACC_STATIC) == 0) {
            if("<init>".equals(name)) {
                locals.add(Opcodes.UNINITIALIZED_THIS);
            } else {
                locals.add(owner);
            }
            args.add(owner);
            argsFormattedForFrame.add(owner);
        }
        Type[] types = Type.getArgumentTypes(desc);
        for(Type type : types) {
            switch(type.getSort()) {
                case Type.BOOLEAN:
                case Type.CHAR:
                case Type.BYTE:
                case Type.SHORT:
                case Type.INT:
                    locals.add(Opcodes.INTEGER);
                    args.add(Opcodes.INTEGER);
                    argsFormattedForFrame.add(Opcodes.INTEGER);
                    break;
                case Type.FLOAT:
                    locals.add(Opcodes.FLOAT);
                    args.add(Opcodes.FLOAT);
                    argsFormattedForFrame.add(Opcodes.FLOAT);
                    break;
                case Type.LONG:
                    locals.add(Opcodes.LONG);
                    locals.add(Opcodes.TOP);
                    args.add(Opcodes.LONG);
                    args.add(Opcodes.TOP);
                    argsFormattedForFrame.add(Opcodes.LONG);
                    break;
                case Type.DOUBLE:
                    locals.add(Opcodes.DOUBLE);
                    locals.add(Opcodes.TOP);
                    args.add(Opcodes.LONG);
                    args.add(Opcodes.TOP);
                    argsFormattedForFrame.add(Opcodes.DOUBLE);
                    break;
                case Type.ARRAY:
                    locals.add(type.getDescriptor());
                    args.add(type.getDescriptor());
                    argsFormattedForFrame.add(type.getDescriptor());
                    break;
                // case Type.OBJECT:
                default:
                    locals.add(type.getInternalName());
                    args.add(type.getInternalName());
                    argsFormattedForFrame.add(type.getInternalName());
            }
        }
    }

    @Override
    public void visitFrame(final int type, int nLocal,
                           Object[] local, final int nStack, final Object[] stack) {
        if(type != Opcodes.F_NEW && type != RAW_INSN) { // uncompressed frame
            throw new IllegalStateException(
                    "ClassReader.accept() should be called with EXPAND_FRAMES flag");
        }
        if(noInsnsSinceListFrame && this.locals != null) {
            return;
        }
        if(argsFormattedForFrame != null && argsFormattedForFrame.size() > nLocal) {
            Object[] oldLocals = local;
            local = new Object[argsFormattedForFrame.size()];
            for(int i = 0; i < nLocal; i++) {
                local[i] = oldLocals[i];
            }
            for(int i = nLocal; i < argsFormattedForFrame.size(); i++) {
                local[i] = Opcodes.TOP;
            }
            nLocal = argsFormattedForFrame.size();
        }
        isFollowedByFrame = false;
        noInsnsSinceListFrame = true;
        if(mv != null) {
            mv.visitFrame(type, nLocal, local, nStack, stack);
        }
        if(this.locals != null) {
            this.frameLocals.clear();
            this.locals.clear();
            this.stack.clear();
            this.stackConstantVals.clear();
            this.stackTagStatus.clear();
        } else {
            this.frameLocals = new ArrayList<>();
            this.locals = new ArrayList<>();
            this.stack = new ArrayList<>();
            this.stackTagStatus = new ArrayList<>();
            this.stackConstantVals = new ArrayList<>(nStack);
        }
        visitFrameTypes(nLocal, local, this.frameLocals);
        visitFrameTypes(nLocal, local, this.locals);
        visitFrameTypes(nStack, stack, this.stack);
        for(int i = 0; i < this.stack.size(); i++) {
            this.stackTagStatus.add(this.stack.get(i));
            if(this.stack.get(i) instanceof TaggedValue) {
                this.stack.set(i, ((TaggedValue) this.stack.get(i)).v);
            }
        }
        while(this.stack.size() > this.stackConstantVals.size()) {
            this.stackConstantVals.add(null);
        }
        maxStack = Math.max(maxStack, this.stack.size());
    }

    public boolean isTopOfStackTagged() {
        if(stackTagStatus.get(stackTagStatus.size() - 1) == Opcodes.TOP) {
            return stackTagStatus.get(stackTagStatus.size() - 2) instanceof TaggedValue;
        } else {
            return stackTagStatus.get(stackTagStatus.size() - 1) instanceof TaggedValue;
        }
    }

    public void setTopOfStackTagged() {
        if(stackTagStatus.get(stackTagStatus.size() - 1) == Opcodes.TOP) {
            stackTagStatus.set(stackTagStatus.size() - 2, new TaggedValue(stackTagStatus.get(stackTagStatus.size() - 2)));
        } else {
            stackTagStatus.set(stackTagStatus.size() - 1, new TaggedValue(stackTagStatus.get(stackTagStatus.size() - 1)));
        }
    }

    public void clearTopOfStackTagged() {
        if(stackTagStatus.get(stackTagStatus.size() - 1) == Opcodes.TOP) {
            stackTagStatus.set(stackTagStatus.size() - 2, stack.get(stack.size() - 2));
        } else {
            stackTagStatus.set(stackTagStatus.size() - 1, stack.get(stack.size() - 1));
        }
    }

    @Override
    public void visitInsn(final int opcode) {

        if(mv != null) {
            mv.visitInsn(opcode);
        }
        noInsnsSinceListFrame = false;
        isFollowedByFrame = opcode == TaintUtils.FOLLOWED_BY_FRAME;
        if(opcode > 200) {
            return;
        }
        execute(opcode, 0, null);
        if((opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN)
                || opcode == Opcodes.ATHROW) {
            this.locals = null;
            this.stack = null;
            this.stackConstantVals = null;
        }
    }

    @Override
    public void visitIntInsn(final int opcode, final int operand) {
        if(mv != null) {
            mv.visitIntInsn(opcode, operand);
        }
        execute(opcode, operand, null);
    }

    @Override
    public void visitVarInsn(final int opcode, final int var) {
        if(mv != null) {
            mv.visitVarInsn(opcode, var);
        }
        if(opcode != TaintUtils.IGNORE_EVERYTHING) {
            execute(opcode, var, null);
        }
    }

    @Override
    public void visitTypeInsn(final int opcode, final String type) {
        if(opcode == Opcodes.NEW) {
            if(labels == null) {
                Label l = new Label();
                labels = new ArrayList<>(3);
                labels.add(l);
                if(mv != null) {
                    mv.visitLabel(l);
                }
            }
            for(int i = 0; i < labels.size(); ++i) {
                uninitializedTypes.put(labels.get(i), type);
            }
        }
        if(mv != null) {
            mv.visitTypeInsn(opcode, type);
        }
        if(opcode > 200) {
            return;
        }
        execute(opcode, 0, type);
    }

    @Override
    public void visitFieldInsn(final int opcode, final String owner,
                               final String name, final String desc) {
        if(mv != null) {
            mv.visitFieldInsn(opcode, owner, name, desc);
        }
        execute(opcode, 0, desc);
    }

    @Deprecated
    @Override
    public void visitMethodInsn(final int opcode, final String owner,
                                final String name, final String desc) {
        throw new IllegalArgumentException();
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        if(mv != null) {
            mv.visitMethodInsn(opcode, owner, name, desc, itf);
        }
        noInsnsSinceListFrame = false;
        isFollowedByFrame = false;
        if(this.locals == null) {
            labels = null;
            return;
        }
        pop(desc);

        if(opcode != Opcodes.INVOKESTATIC) {
            Object t = pop();
            if(opcode == Opcodes.INVOKESPECIAL && name.charAt(0) == '<') {
                Object u;
                if(t == Opcodes.UNINITIALIZED_THIS) {
                    u = this.owner;
                } else {
                    u = uninitializedTypes.get(t);
                }
                for(int i = 0; i < locals.size(); ++i) {
                    if(locals.get(i) == t) {
                        locals.set(i, u);
                    }
                }
                for(int i = 0; i < stack.size(); ++i) {
                    if(stack.get(i) == t) {
                        stack.set(i, u);
                        stackTagStatus.set(i, u);
                        stackConstantVals.set(i, null);
                    }
                }
            }
        }
        pushDesc(desc);
        labels = null;
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm,
                                       Object... bsmArgs) {
        noInsnsSinceListFrame = false;
        if(mv != null) {
            mv.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
        }
        isFollowedByFrame = false;
        if(this.locals == null) {
            labels = null;
            return;
        }
        pop(desc);
        pushDesc(desc);
        labels = null;
    }

    @Override
    public void visitJumpInsn(final int opcode, final Label label) {
        noInsnsSinceListFrame = false;
        if(mv != null) {
            mv.visitJumpInsn(opcode, label);
        }
        isFollowedByFrame = false;
        execute(opcode, 0, null);
        if(opcode == Opcodes.GOTO) {
            this.locals = null;
            this.stack = null;
            this.stackConstantVals = null;
        }
    }

    @Override
    public void visitLabel(final Label label) {
        if(mv != null) {
            mv.visitLabel(label);
        }
        if(labels == null) {
            labels = new ArrayList<>(3);
        }
        labels.add(label);
    }

    @Override
    public void visitLdcInsn(final Object cst) {
        if(mv != null) {
            mv.visitLdcInsn(cst);
        }
        if(cst instanceof PhosphorInstructionInfo) {
            return;
        }
        noInsnsSinceListFrame = false;
        isFollowedByFrame = false;
        if(this.locals == null) {
            labels = null;
            return;
        }
        if(cst instanceof Integer) {
            push(Opcodes.INTEGER, cst, false);
        } else if(cst instanceof Long) {
            push(Opcodes.LONG, cst, false);
            push(Opcodes.TOP);
        } else if(cst instanceof Float) {
            push(Opcodes.FLOAT, cst, false);
        } else if(cst instanceof Double) {
            push(Opcodes.DOUBLE, cst, false);
            push(Opcodes.TOP);
        } else if(cst instanceof String) {
            push("java/lang/String", cst, false);
        } else if(cst instanceof Type) {
            int sort = ((Type) cst).getSort();
            if(sort == Type.OBJECT || sort == Type.ARRAY) {
                push("java/lang/Class", cst, false);
            } else if(sort == Type.METHOD) {
                push("java/lang/invoke/MethodType", cst, false);
            } else {
                throw new IllegalArgumentException();
            }
        } else if(cst instanceof Handle) {
            push("java/lang/invoke/MethodHandle", cst, false);
        } else {
            throw new IllegalArgumentException();
        }
        labels = null;
    }

    @Override
    public void visitIincInsn(final int var, final int increment) {
        if(mv != null) {
            mv.visitIincInsn(var, increment);
        }
        execute(Opcodes.IINC, var, null);
    }

    @Override
    public void visitTableSwitchInsn(final int min, final int max,
                                     final Label dflt, final Label... labels) {
        if(mv != null) {
            mv.visitTableSwitchInsn(min, max, dflt, labels);
        }
        execute(Opcodes.TABLESWITCH, 0, null);
        this.locals = null;
        this.stack = null;
        this.stackConstantVals = null;
    }

    @Override
    public void visitLookupSwitchInsn(final Label dflt, final int[] keys,
                                      final Label[] labels) {
        if(mv != null) {
            mv.visitLookupSwitchInsn(dflt, keys, labels);
        }
        execute(Opcodes.LOOKUPSWITCH, 0, null);
        this.locals = null;
        this.stack = null;
        this.stackConstantVals = null;
    }

    @Override
    public void visitMultiANewArrayInsn(final String desc, final int dims) {
        if(mv != null) {
            mv.visitMultiANewArrayInsn(desc, dims);
        }
        execute(Opcodes.MULTIANEWARRAY, dims, desc);
    }

    @Override
    public void visitMaxs(final int maxStack, final int maxLocals) {
        if(mv != null) {
            this.maxStack = Math.max(this.maxStack, maxStack);
            this.maxLocals = Math.max(this.maxLocals, maxLocals);
            mv.visitMaxs(this.maxStack, this.maxLocals);
        }
    }

    private Object get(final int local) {
        maxLocals = Math.max(maxLocals, local);
        return local < locals.size() ? locals.get(local) : Opcodes.TOP;
    }

    // ------------------------------------------------------------------------

    private void set(final int local, final Object type) {
        if(type.equals("java/lang/Object;")) {
            throw new IllegalArgumentException("Got " + type);
        }
        maxLocals = Math.max(maxLocals, local);

        while(local >= locals.size()) {
            locals.add(Opcodes.TOP);
        }
        if(type == Opcodes.NULL && local < args.size()) {
            locals.set(local, args.get(local)); //Prohibit the analyzer from making an arg be null, even if it is
        } else {
            locals.set(local, type);
        }

    }

    private void setTopNolongerConstant() {
        if(!stackConstantVals.isEmpty()) {
            stackConstantVals.set(stackConstantVals.size() - 1, null);
        }
    }

    private void push(Object type, final Object val, final Object tag) {
        if(type.equals("java/lang/Object;")) {
            throw new IllegalArgumentException("Got " + type);
        }
        if(type instanceof TaggedValue) {
            type = ((TaggedValue) type).v;
        }
        stack.add(type);
        stackConstantVals.add(val);
        stackTagStatus.add((tag instanceof Boolean ? type : tag));
        maxStack = Math.max(maxStack, stack.size());
    }

    private void push(final Object type) {
        push(type, null, type);
    }

    private void pushDesc(final String desc) {
        int index = desc.charAt(0) == '(' ? desc.indexOf(')') + 1 : 0;
        switch(desc.charAt(index)) {
            case 'V':
                return;
            case 'Z':
            case 'C':
            case 'B':
            case 'S':
            case 'I':
                push(Opcodes.INTEGER);
                return;
            case 'F':
                push(Opcodes.FLOAT);
                return;
            case 'J':
                push(Opcodes.LONG);
                push(Opcodes.TOP);
                return;
            case 'D':
                push(Opcodes.DOUBLE);
                push(Opcodes.TOP);
                return;
            case '[':
                if(index == 0) {
                    push(desc);
                } else {
                    push(desc.substring(index));
                }
                break;
            // case 'L':
            default:
                if(index == 0) {
                    push(desc.substring(1, desc.length() - 1));
                } else {
                    push(desc.substring(index + 1, desc.length() - 1));
                }
        }
    }

    private Object pop() {
        stackConstantVals.remove(stackConstantVals.size() - 1);
        stackTagStatus.remove(stackTagStatus.size() - 1);
        return stack.remove(stack.size() - 1);
    }

    private void pop(final int n) {
        int size = stack.size();
        int end = size - n;
        for(int i = size - 1; i >= end; --i) {
            stack.remove(i);
            stackConstantVals.remove(i);
            stackTagStatus.remove(i);
        }
    }

    private void pop(final String desc) {
        char c = desc.charAt(0);
        if(c == '(') {
            int n = 0;
            Type[] types = Type.getArgumentTypes(desc);
            for(int i = 0; i < types.length; ++i) {
                n += types[i].getSize();
            }
            pop(n);
        } else if(c == 'J' || c == 'D') {
            pop(2);
        } else {
            pop(1);
        }
    }

    private void execute(final int opcode, final int iarg, final String sarg) {
        noInsnsSinceListFrame = false;
        isFollowedByFrame = false;

        if(this.locals == null) {
            labels = null;
            return;
        }

        Object t1, t2, t3, t4;
        switch(opcode) {
            case Opcodes.INEG:
            case Opcodes.LNEG:
            case Opcodes.FNEG:
            case Opcodes.DNEG:
            case Opcodes.I2B:
            case Opcodes.I2C:
            case Opcodes.I2S:
                setTopNolongerConstant();
            case Opcodes.NOP:
            case Opcodes.GOTO:
            case Opcodes.RETURN:
                break;
            case Opcodes.ACONST_NULL:
                push(Opcodes.NULL);
                break;
            case Opcodes.ICONST_M1:
                push(Opcodes.INTEGER, -1, false);
                break;
            case Opcodes.ICONST_0:
                push(Opcodes.INTEGER, 0, false);
                break;
            case Opcodes.ICONST_1:
                push(Opcodes.INTEGER, 1, false);
                break;
            case Opcodes.ICONST_2:
                push(Opcodes.INTEGER, 2, false);
                break;
            case Opcodes.ICONST_3:
                push(Opcodes.INTEGER, 3, false);
                break;
            case Opcodes.ICONST_4:
                push(Opcodes.INTEGER, 4, false);
                break;
            case Opcodes.ICONST_5:
                push(Opcodes.INTEGER, 5, false);
                break;
            case Opcodes.BIPUSH:
            case Opcodes.SIPUSH:
                push(Opcodes.INTEGER, iarg, false);
                break;
            case Opcodes.LCONST_0:
                push(Opcodes.LONG, 0L, false);
                push(Opcodes.TOP);
                break;
            case Opcodes.LCONST_1:
                push(Opcodes.LONG, 1L, false);
                push(Opcodes.TOP);
                break;
            case Opcodes.FCONST_0:
                push(Opcodes.FLOAT, 0F, false);
                break;
            case Opcodes.FCONST_1:
                push(Opcodes.FLOAT, 1F, false);
                break;
            case Opcodes.FCONST_2:
                push(Opcodes.FLOAT, 2F, false);
                break;
            case Opcodes.DCONST_0:
                push(Opcodes.DOUBLE, 0D, false);
                push(Opcodes.TOP);
                break;
            case Opcodes.DCONST_1:
                push(Opcodes.DOUBLE, 1D, false);
                push(Opcodes.TOP);
                break;
            case Opcodes.ILOAD:
            case Opcodes.FLOAD:
            case Opcodes.ALOAD:
                push(get(iarg), null, false);
                break;
            case Opcodes.LLOAD:
            case Opcodes.DLOAD:
                push(get(iarg), null, false);
                push(Opcodes.TOP);
                break;
            case Opcodes.IALOAD:
            case Opcodes.BALOAD:
            case Opcodes.CALOAD:
            case Opcodes.SALOAD:
                pop(2);
                push(Opcodes.INTEGER);
                break;
            case Opcodes.LALOAD:
                pop(2);
                push(Opcodes.LONG);
                push(Opcodes.TOP);
                break;
            case Opcodes.D2L:
                boolean isTagged = stackTagStatus.get(stackTagStatus.size() - 2) instanceof TaggedValue;
                pop(2);
                push(Opcodes.LONG, null, isTagged ? new TaggedValue(Opcodes.LONG) : false);
                push(Opcodes.TOP);
                break;
            case Opcodes.FALOAD:
                pop(2);
                push(Opcodes.FLOAT);
                break;
            case Opcodes.DALOAD:
                pop(2);
                push(Opcodes.DOUBLE);
                push(Opcodes.TOP);
                break;
            case Opcodes.L2D:
                isTagged = stackTagStatus.get(stackTagStatus.size() - 2) instanceof TaggedValue;
                pop(2);
                push(Opcodes.DOUBLE, null, isTagged ? new TaggedValue(Opcodes.DOUBLE) : false);
                push(Opcodes.TOP);
                break;
            case Opcodes.AALOAD:
                pop(1);
                t1 = pop();
                if(t1 instanceof String) {
                    pushDesc(((String) t1).substring(1));
                } else {
                    push("java/lang/Object");
                }
                break;
            case Opcodes.ISTORE:
            case Opcodes.FSTORE:
            case Opcodes.ASTORE:
                t1 = pop();
                set(iarg, t1);
                if(iarg > 0) {
                    t2 = get(iarg - 1);
                    if(t2 == Opcodes.LONG || t2 == Opcodes.DOUBLE) {
                        set(iarg - 1, Opcodes.TOP);
                    }
                }
                break;
            case Opcodes.LSTORE:
            case Opcodes.DSTORE:
                pop(1);
                t1 = pop();
                set(iarg, t1);
                set(iarg + 1, Opcodes.TOP);
                if(iarg > 0) {
                    t2 = get(iarg - 1);
                    if(t2 == Opcodes.LONG || t2 == Opcodes.DOUBLE) {
                        set(iarg - 1, Opcodes.TOP);
                    }
                }
                break;
            case Opcodes.IASTORE:
            case Opcodes.BASTORE:
            case Opcodes.CASTORE:
            case Opcodes.SASTORE:
            case Opcodes.FASTORE:
            case Opcodes.AASTORE:
                pop(3);
                break;
            case Opcodes.LASTORE:
            case Opcodes.DASTORE:
                pop(4);
                break;
            case Opcodes.POP:
            case Opcodes.IFEQ:
            case Opcodes.IFNE:
            case Opcodes.IFLT:
            case Opcodes.IFGE:
            case Opcodes.IFGT:
            case Opcodes.IFLE:
            case Opcodes.IRETURN:
            case Opcodes.FRETURN:
            case Opcodes.ARETURN:
            case Opcodes.TABLESWITCH:
            case Opcodes.LOOKUPSWITCH:
            case Opcodes.ATHROW:
            case Opcodes.MONITORENTER:
            case Opcodes.MONITOREXIT:
            case Opcodes.IFNULL:
            case Opcodes.IFNONNULL:
                pop(1);
                break;
            case Opcodes.POP2:
            case Opcodes.IF_ICMPEQ:
            case Opcodes.IF_ICMPNE:
            case Opcodes.IF_ICMPLT:
            case Opcodes.IF_ICMPGE:
            case Opcodes.IF_ICMPGT:
            case Opcodes.IF_ICMPLE:
            case Opcodes.IF_ACMPEQ:
            case Opcodes.IF_ACMPNE:
            case Opcodes.LRETURN:
            case Opcodes.DRETURN:
                pop(2);
                break;
            case Opcodes.DUP:
                Object z = stackConstantVals.get(stackConstantVals.size() - 1);
                Object z2 = stackTagStatus.get(stackTagStatus.size() - 1);
                t1 = pop();
                push(t1, z, z2);
                push(t1, z, z2);
                break;
            case Opcodes.DUP_X1:
                z = stackConstantVals.get(stackConstantVals.size() - 1);
                Object z3 = stackTagStatus.get(stackTagStatus.size() - 1);
                t1 = pop();
                z2 = stackConstantVals.get(stackConstantVals.size() - 1);
                Object z4 = stackTagStatus.get(stackTagStatus.size() - 1);
                t2 = pop();
                push(t1, z, z3);
                push(t2, z2, z4);
                push(t1, z, z3);
                break;
            case Opcodes.DUP_X2:
                z = stackConstantVals.get(stackConstantVals.size() - 1);
                z4 = stackTagStatus.get(stackTagStatus.size() - 1);
                t1 = pop();
                z2 = stackConstantVals.get(stackConstantVals.size() - 1);
                Object z5 = stackTagStatus.get(stackTagStatus.size() - 1);
                t2 = pop();
                z3 = stackConstantVals.get(stackConstantVals.size() - 1);
                Object z6 = stackTagStatus.get(stackTagStatus.size() - 1);
                t3 = pop();
                push(t1, z, z4);
                push(t3, z3, z6);
                push(t2, z2, z5);
                push(t1, z, z4);
                break;
            case Opcodes.DUP2:
                z = stackConstantVals.get(stackConstantVals.size() - 1);
                z3 = stackTagStatus.get(stackTagStatus.size() - 1);
                t1 = pop();
                z2 = stackConstantVals.get(stackConstantVals.size() - 1);
                z4 = stackTagStatus.get(stackTagStatus.size() - 1);
                t2 = pop();
                push(t2, z2, z4);
                push(t1, z, z3);
                push(t2, z2, z4);
                push(t1, z, z3);
                break;
            case Opcodes.DUP2_X1:
                z = stackConstantVals.get(stackConstantVals.size() - 1);
                z4 = stackTagStatus.get(stackTagStatus.size() - 1);
                t1 = pop();
                z2 = stackConstantVals.get(stackConstantVals.size() - 1);
                z5 = stackTagStatus.get(stackTagStatus.size() - 1);
                t2 = pop();
                z3 = stackConstantVals.get(stackConstantVals.size() - 1);
                z6 = stackTagStatus.get(stackTagStatus.size() - 1);
                t3 = pop();
                push(t2, z2, z5);
                push(t1, z, z4);
                push(t3, z3, z6);
                push(t2, z2, z5);
                push(t1, z, z4);
                break;
            case Opcodes.DUP2_X2:
                z = stackConstantVals.get(stackConstantVals.size() - 1);
                z5 = stackTagStatus.get(stackTagStatus.size() - 1);
                t1 = pop();
                z2 = stackConstantVals.get(stackConstantVals.size() - 1);
                z6 = stackTagStatus.get(stackTagStatus.size() - 1);
                t2 = pop();
                z3 = stackConstantVals.get(stackConstantVals.size() - 1);
                Object z7 = stackTagStatus.get(stackTagStatus.size() - 1);
                t3 = pop();
                z4 = stackConstantVals.get(stackConstantVals.size() - 1);
                Object z8 = stackTagStatus.get(stackTagStatus.size() - 1);
                t4 = pop();
                push(t2, z2, z6);
                push(t1, z, z5);
                push(t4, z4, z8);
                push(t3, z3, z7);
                push(t2, z2, z6);
                push(t1, z, z5);
                break;
            case Opcodes.SWAP:
                z = stackConstantVals.get(stackConstantVals.size() - 1);
                z3 = stackTagStatus.get(stackTagStatus.size() - 1);
                t1 = pop();
                z2 = stackConstantVals.get(stackConstantVals.size() - 1);
                z4 = stackTagStatus.get(stackTagStatus.size() - 1);
                t2 = pop();
                push(t1, z, z3);
                push(t2, z2, z4);
                break;
            case Opcodes.IADD:
            case Opcodes.ISUB:
            case Opcodes.IMUL:
            case Opcodes.IDIV:
            case Opcodes.IREM:
            case Opcodes.IAND:
            case Opcodes.IOR:
            case Opcodes.IXOR:
            case Opcodes.ISHL:
            case Opcodes.ISHR:
            case Opcodes.IUSHR:
            case Opcodes.FCMPL:
            case Opcodes.FCMPG:
                isTagged = stackTagStatus.get(stackTagStatus.size() - 1) instanceof TaggedValue;
                pop(2);
                push(Opcodes.INTEGER, null, isTagged ? new TaggedValue(Opcodes.INTEGER) : false);
                break;
            case Opcodes.L2I:
            case Opcodes.D2I:
                isTagged = stackTagStatus.get(stackTagStatus.size() - 2) instanceof TaggedValue;
                pop(2);
                push(Opcodes.INTEGER, null, isTagged ? new TaggedValue(Opcodes.INTEGER) : false);
                break;
            case Opcodes.LADD:
            case Opcodes.LSUB:
            case Opcodes.LMUL:
            case Opcodes.LDIV:
            case Opcodes.LREM:
            case Opcodes.LAND:
            case Opcodes.LOR:
            case Opcodes.LXOR:
                isTagged = stackTagStatus.get(stackTagStatus.size() - 2) instanceof TaggedValue;
                pop(4);
                push(Opcodes.LONG, null, isTagged ? new TaggedValue(Opcodes.LONG) : false);
                push(Opcodes.TOP);
                break;
            case Opcodes.FADD:
            case Opcodes.FSUB:
            case Opcodes.FMUL:
            case Opcodes.FDIV:
            case Opcodes.FREM:
                isTagged = stackTagStatus.get(stackTagStatus.size() - 1) instanceof TaggedValue;
                pop(2);
                push(Opcodes.FLOAT, null, isTagged ? new TaggedValue(Opcodes.FLOAT) : false);
                break;
            case Opcodes.L2F:
            case Opcodes.D2F:
                isTagged = stackTagStatus.get(stackTagStatus.size() - 2) instanceof TaggedValue;
                pop(2);
                push(Opcodes.FLOAT, null, isTagged ? new TaggedValue(Opcodes.FLOAT) : false);
                break;
            case Opcodes.DADD:
            case Opcodes.DSUB:
            case Opcodes.DMUL:
            case Opcodes.DDIV:
            case Opcodes.DREM:
                isTagged = stackTagStatus.get(stackTagStatus.size() - 2) instanceof TaggedValue;
                pop(4);
                push(Opcodes.DOUBLE, null, isTagged ? new TaggedValue(Opcodes.DOUBLE) : false);
                push(Opcodes.TOP);
                break;
            case Opcodes.LSHL:
            case Opcodes.LSHR:
            case Opcodes.LUSHR:
                isTagged = stackTagStatus.get(stackTagStatus.size() - 2) instanceof TaggedValue;
                pop(3);
                push(Opcodes.LONG, null, isTagged ? new TaggedValue(Opcodes.LONG) : false);
                push(Opcodes.TOP);
                break;
            case Opcodes.IINC:
                set(iarg, Opcodes.INTEGER);
                setTopNolongerConstant();
                break;
            case Opcodes.I2L:
            case Opcodes.F2L:
                isTagged = stackTagStatus.get(stackTagStatus.size() - 1) instanceof TaggedValue;
                pop(1);
                push(Opcodes.LONG, null, isTagged ? new TaggedValue(Opcodes.LONG) : false);
                push(Opcodes.TOP);
                break;
            case Opcodes.I2F:
                isTagged = stackTagStatus.get(stackTagStatus.size() - 1) instanceof TaggedValue;
                pop(1);
                push(Opcodes.FLOAT, null, isTagged ? new TaggedValue(Opcodes.FLOAT) : false);
                break;
            case Opcodes.I2D:
            case Opcodes.F2D:
                isTagged = stackTagStatus.get(stackTagStatus.size() - 1) instanceof TaggedValue;
                pop(1);
                push(Opcodes.DOUBLE, null, isTagged ? new TaggedValue(Opcodes.DOUBLE) : false);
                push(Opcodes.TOP);
                break;
            case Opcodes.F2I:
            case Opcodes.ARRAYLENGTH:
            case Opcodes.INSTANCEOF:
                isTagged = stackTagStatus.get(stackTagStatus.size() - 1) instanceof TaggedValue;
                pop(1);
                push(Opcodes.INTEGER, null, isTagged ? new TaggedValue(Opcodes.INTEGER) : false);
                break;
            case Opcodes.LCMP:
            case Opcodes.DCMPL:
            case Opcodes.DCMPG:
                isTagged = stackTagStatus.get(stackTagStatus.size() - 2) instanceof TaggedValue;
                pop(4);
                push(Opcodes.INTEGER, null, isTagged ? new TaggedValue(Opcodes.INTEGER) : false);
                break;
            case Opcodes.JSR:
            case Opcodes.RET:
                throw new RuntimeException("JSR/RET are not supported");
            case Opcodes.GETSTATIC:
                pushDesc(sarg);
                break;
            case Opcodes.PUTSTATIC:
                pop(sarg);
                break;
            case Opcodes.GETFIELD:
                pop(1);
                pushDesc(sarg);
                break;
            case Opcodes.PUTFIELD:
                pop(sarg);
                pop();
                break;
            case Opcodes.NEW:
                push(labels.get(0));
                break;
            case Opcodes.NEWARRAY:
                pop();
                switch(iarg) {
                    case Opcodes.T_BOOLEAN:
                        pushDesc("[Z");
                        break;
                    case Opcodes.T_CHAR:
                        pushDesc("[C");
                        break;
                    case Opcodes.T_BYTE:
                        pushDesc("[B");
                        break;
                    case Opcodes.T_SHORT:
                        pushDesc("[S");
                        break;
                    case Opcodes.T_INT:
                        pushDesc("[I");
                        break;
                    case Opcodes.T_FLOAT:
                        pushDesc("[F");
                        break;
                    case Opcodes.T_DOUBLE:
                        pushDesc("[D");
                        break;
                    // case Opcodes.T_LONG:
                    default:
                        pushDesc("[J");
                        break;
                }
                break;
            case Opcodes.ANEWARRAY:
                pop();
                pushDesc("[" + Type.getObjectType(sarg));
                break;
            case Opcodes.CHECKCAST:
                pop();
                pushDesc(Type.getObjectType(sarg).getDescriptor());
                break;
            // case Opcodes.MULTIANEWARRAY:
            default:
                pop(iarg);
                pushDesc(sarg);
                break;
        }
        labels = null;
    }

    public void clearLabels() {
        labels = null;
    }

    private static void visitFrameTypes(final int n, final Object[] types,
                                        final List<Object> result) {
        for(int i = 0; i < n; ++i) {
            Object type = types[i];
            if(type.equals("java/lang/Object;")) {
                throw new IllegalArgumentException("Got " + type + " IN" + Arrays.toString(types));
            }
            result.add(type);
            if(type instanceof TaggedValue) {
                type = ((TaggedValue) type).v;
            }
            if(type == Opcodes.LONG || type == Opcodes.DOUBLE) {
                result.add(Opcodes.TOP);
            }
        }
    }

    public void printLocals() {
        System.out.println("LOCALS:");
        for(int i = 0; i < locals.size(); i++) {
            System.out.println("\t" + i + ": " + locals.get(i));
        }
    }
}
