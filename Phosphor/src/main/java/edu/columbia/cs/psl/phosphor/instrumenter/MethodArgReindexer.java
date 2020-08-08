package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.LocalVariablePhosphorInstructionInfo;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.TaggedValue;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintTrackingClassVisitor.CONTROL_STACK_DESC;
import static edu.columbia.cs.psl.phosphor.instrumenter.TaintTrackingClassVisitor.CONTROL_STACK_INTERNAL_NAME;

public class MethodArgReindexer extends MethodVisitor {

    int line;
    HashSet<Label> tryCatchHandlers = new HashSet<>();
    boolean isHandler;
    private int originalLastArgIdx;
    private int[] oldArgMappings;
    private int newArgOffset;
    private boolean isStatic;
    private int origNumArgs;
    private String name;
    private String desc;
    private List<Type> oldArgTypesList;
    private MethodNode lvStore;
    private int nNewArgs = 0;
    private boolean hasPreAllocatedReturnAddress;
    private Type newReturnType;
    private Map<String, Integer> parameters = new HashMap<>();
    private int indexOfControlTagsInLocals;
    private int nWrappers = 0;

    MethodArgReindexer(MethodVisitor mv, int access, String name, String desc, String originalDesc, MethodNode lvStore, boolean isLambda) {
        super(Configuration.ASM_VERSION, mv);
        this.lvStore = lvStore;
        lvStore.localVariables = new java.util.ArrayList<>();
        this.name = name;
        this.desc = desc;
        Type[] oldArgTypes = Type.getArgumentTypes(originalDesc);
        origNumArgs = oldArgTypes.length;
        isStatic = (Opcodes.ACC_STATIC & access) != 0;
        for(Type t : oldArgTypes) {
            originalLastArgIdx += t.getSize();
        }
        if(!isStatic) {
            originalLastArgIdx++;
        }
        if(!isStatic) {
            origNumArgs++;
        }
        newArgOffset = 0;
        oldArgTypesList = new ArrayList<>();
        List<Type> oldTypesDoublesAreOne = new ArrayList<>();
        if(!isStatic) {
            oldArgTypesList.add(Type.getType("Lthis;"));
            oldTypesDoublesAreOne.add(Type.getType("Lthis;"));
        }
        Type[] firstFrameLocals = new Type[origNumArgs];
        int ffl = 0;
        if(!isStatic) {
            firstFrameLocals[0] = Type.getObjectType("java/lang/Object");
            ffl++;
        }
        for(Type t : Type.getArgumentTypes(originalDesc)) {
            oldArgTypesList.add(t);
            oldTypesDoublesAreOne.add(t);
            firstFrameLocals[ffl] = t;
            ffl++;
        }

        if(!isStatic) {
            newArgOffset++;
            nNewArgs++;
        }

        boolean hasBeenRemapped = false;
        oldArgMappings = new int[originalLastArgIdx + 1];
        int oldVarCount = (isStatic ? 0 : 1);
        boolean hasSyntheticReferenceTaint = name.startsWith("phosphorWrapInvokeDynamic") && desc.startsWith("("+Configuration.TAINT_TAG_DESC);
        for(Type oldArgType : oldArgTypes) {
            oldArgMappings[oldVarCount] = oldVarCount + newArgOffset;
            if(oldArgType.getSize() == 2) {
                oldArgMappings[oldVarCount + 1] = oldVarCount + newArgOffset + 1;
                oldVarCount++;
            }
            oldVarCount++;
            if(!isLambda) {
                if(TaintUtils.isShadowedType(oldArgType) && !(hasSyntheticReferenceTaint && oldVarCount == 1)) {
                    newArgOffset++;
                    nNewArgs++;
                    hasBeenRemapped = true;
                }
                if(TaintUtils.isWrappedTypeWithErasedType(oldArgType)) {
                    hasBeenRemapped = true;
                    nWrappers++;
                    nNewArgs++;
                }
            }
        }

        if((Configuration.IMPLICIT_HEADERS_NO_TRACKING || Configuration.IMPLICIT_TRACKING) && !name.equals("<clinit>")) {
            hasBeenRemapped = true;
            indexOfControlTagsInLocals = oldArgTypes.length + newArgOffset + (isStatic ? 0 : 1);
            newArgOffset++;
        }

        hasPreAllocatedReturnAddress = TaintUtils.isPreAllocReturnType(originalDesc);
        if(hasPreAllocatedReturnAddress) {
            newReturnType = Type.getReturnType(desc);
            newArgOffset++;
            nNewArgs++;
        }
        if(TaintUtils.isErasedReturnType(Type.getReturnType(originalDesc))) {
            hasBeenRemapped = true;
            nWrappers++;
            nNewArgs++;
        }
        newArgOffset += nWrappers;
    }

    public int getNewArgOffset() {
        return newArgOffset;
    }

    @Override
    public void visitParameter(String name, int access) {
        super.visitParameter(name, access);
        parameters.put(name, access);
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        if(!parameters.isEmpty()) {
            // Add fake params
            for(int i = 0; i < nNewArgs; i++) {
                super.visitParameter("Phosphor$$Param$$" + i, 0);
            }
        }
    }

    @Override
    public void visitLdcInsn(Object cst) {
        if(cst instanceof LocalVariablePhosphorInstructionInfo) {
            LocalVariablePhosphorInstructionInfo info = (LocalVariablePhosphorInstructionInfo) cst;
            super.visitLdcInsn(info.setLocalVariableIndex(remapLocal(info.getLocalVariableIndex())));
        } else {
            super.visitLdcInsn(cst);
        }
    }

    @Override
    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
        Type t = Type.getType(desc);
        if(TaintUtils.isWrappedType(t)) {
            desc = TaintUtils.getWrapperType(t).getDescriptor();
        }
        if(index < originalLastArgIdx) {
            boolean found = false;
            for(Object _lv : lvStore.localVariables) {
                LocalVariableNode lv = (LocalVariableNode) _lv;
                if(lv != null && lv.name != null && lv.name.equals(name) && lv.index == index) {
                    found = true;
                    break;
                }
            }
            if(!found) {
                lvStore.localVariables.add(new LocalVariableNode(name, desc, signature, null, null, index));
            }
        }
        if(!isStatic && index == 0) {
            super.visitLocalVariable(name, desc, signature, start, end, index);
        } else if(index < originalLastArgIdx) {
            String shadow = TaintUtils.getShadowTaintType(desc);
            if(shadow != null) {
                super.visitLocalVariable(name + TaintUtils.METHOD_SUFFIX, shadow, null, start, end, oldArgMappings[index] + 1);
            }
            super.visitLocalVariable(name, desc, signature, start, end, oldArgMappings[index]);
            if(index == originalLastArgIdx - 1 && (Configuration.IMPLICIT_HEADERS_NO_TRACKING || Configuration.IMPLICIT_TRACKING)) {
                super.visitLocalVariable("Phopshor$$ImplicitTaintTrackingFromParent", CONTROL_STACK_DESC, null, start, end, oldArgMappings[index] + 1);
            }
            if((index == originalLastArgIdx - Type.getType(desc).getSize()) && hasPreAllocatedReturnAddress) {
                super.visitLocalVariable("Phosphor$$ReturnPreAllocated", newReturnType.getDescriptor(), null, start, end, oldArgMappings[index] + ((Configuration.IMPLICIT_HEADERS_NO_TRACKING || Configuration.IMPLICIT_TRACKING) ? 2 : 1));
            }
            if(index == originalLastArgIdx - 1) {
                for(int i = 0; i < nWrappers; i++) {
                    super.visitLocalVariable("PhosphorMethodDescriptorHelper" + i, "Ljava/lang/Object;", null, start, end, oldArgMappings[index] + ((Configuration.IMPLICIT_HEADERS_NO_TRACKING || Configuration.IMPLICIT_TRACKING) ? 2 : 1));
                }
            }
        } else {
            super.visitLocalVariable(name, desc, signature, start, end, index + newArgOffset);
        }
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        super.visitLineNumber(line, start);
        this.line = line;
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        super.visitTryCatchBlock(start, end, handler, type);
        tryCatchHandlers.add(handler);
    }

    @Override
    public void visitLabel(Label label) {
        super.visitLabel(label);
        isHandler |= tryCatchHandlers.contains(label);
    }

    @Override
    public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
        boolean isExceptionHandler = isHandler;
        isHandler = false;
        if(type == Opcodes.F_FULL || type == Opcodes.F_NEW) {
            Object[] remappedLocals = new Object[Math.max(local.length, origNumArgs) + newArgOffset + 1]; //was +1, not sure why??
            nLocal = remapLocals(nLocal, local, remappedLocals);
            local = remappedLocals;
        }
        if(nLocal > local.length) {
            throw new IllegalStateException();
        }
        ArrayList<Object> newStack = new ArrayList<>();
        int origNStack = nStack;
        for(int i = 0; i < origNStack; i++) {
            if(TaintUtils.isWrappedType(TaintAdapter.getTypeForStackType(stack[i]))) {
                newStack.add(TaintUtils.getWrapperType(TaintAdapter.getTypeForStackType(stack[i])).getInternalName());
            } else {
                newStack.add(stack[i]);
            }
            if(!isExceptionHandler) {
                newStack.add(Configuration.TAINT_TAG_INTERNAL_NAME);
                nStack++;
            }
        }
        Object[] stack2;
        stack2 = newStack.toArray();
        super.visitFrame(type, nLocal, local, nStack, stack2);
    }

    private int remapLocals(int nLocal, Object[] local, Object[] remappedLocals) {
        int oldLocalIndex;
        int newLocalIndex = 0;
        for(oldLocalIndex = 0; oldLocalIndex < Math.min(oldArgTypesList.size(), nLocal); oldLocalIndex++) {
            Type oldArgType = oldArgTypesList.get(oldLocalIndex);
            if(TaintUtils.isWrappedType(oldArgType)) {
                remappedLocals[newLocalIndex++] = TaintUtils.getWrapperType(oldArgType).getInternalName();
            } else {
                remappedLocals[newLocalIndex++] = local[oldLocalIndex];
            }
            if(local[oldLocalIndex] == Opcodes.TOP) {
                remappedLocals[newLocalIndex++] = Opcodes.TOP;
            } else {
                remappedLocals[newLocalIndex++] = Configuration.TAINT_TAG_INTERNAL_NAME;
            }
        }
        if((Configuration.IMPLICIT_HEADERS_NO_TRACKING || Configuration.IMPLICIT_TRACKING) && !name.equals("<clinit>")) {
            while(newLocalIndex < indexOfControlTagsInLocals) {
                remappedLocals[newLocalIndex++] = Opcodes.TOP;
            }
            remappedLocals[newLocalIndex++] = CONTROL_STACK_INTERNAL_NAME;
        }
        if(hasPreAllocatedReturnAddress) {
            remappedLocals[newLocalIndex++] = newReturnType.getInternalName();
        }
        for(int i = 0; i < nWrappers; i++) {
            remappedLocals[newLocalIndex++] = Opcodes.TOP;
        }
        for(; oldLocalIndex < nLocal; oldLocalIndex++) {
            Type t = getTypeForStackTypeTOPAsNull(local[oldLocalIndex]);
            if(TaintUtils.isWrappedType(t)) {
                remappedLocals[newLocalIndex++] = TaintUtils.getWrapperType(Type.getObjectType((String) local[oldLocalIndex])).getInternalName();
            } else {
                remappedLocals[newLocalIndex++] = local[oldLocalIndex];

            }
        }
        return newLocalIndex;
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
        if(!isStatic) {
            parameter++;
        }
        int remappedVar = parameter;
        if(parameter < originalLastArgIdx) {
            remappedVar = oldArgMappings[parameter];
        } else {
            remappedVar += newArgOffset;
        }
        if(!isStatic) {
            remappedVar--;
        }
        return super.visitParameterAnnotation(remappedVar, descriptor, visible);
    }

    @Override
    public void visitAnnotableParameterCount(int parameterCount, boolean visible) {
        if(!isStatic) {
            parameterCount++;
        }
        int remappedVar = parameterCount;
        if(parameterCount < originalLastArgIdx) {
            remappedVar = oldArgMappings[parameterCount];
        } else {
            remappedVar += newArgOffset;
        }
        if(!isStatic) {
            remappedVar--;
        }
        super.visitAnnotableParameterCount(remappedVar, visible);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        int origVar = var;
        if(isStatic || var != 0) {
            if(var < originalLastArgIdx) {
                //accessing an arg; remap it
                var = oldArgMappings[var];// + (isStatic?0:1);
            } else {
                //not accessing an arg. just add offset.
                var += newArgOffset;
            }
        }
        super.visitIincInsn(var, increment);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itfc) {
        super.visitMethodInsn(opcode, owner, name, desc, itfc);
    }

    public void visitVarInsn(int opcode, int var) {
        super.visitVarInsn(opcode, remapLocal(var));
    }

    private int remapLocal(int originalVar) {
        if(isStatic || originalVar != 0) {
            if(originalVar < originalLastArgIdx) {
                //accessing an arg; remap it
                return oldArgMappings[originalVar];
            } else {
                return originalVar + newArgOffset;
            }
        } else {
            return originalVar;
        }
    }

    private static Type getTypeForStackTypeTOPAsNull(Object obj) {
        if(obj instanceof TaggedValue) {
            obj = ((TaggedValue) obj).v;
        }
        if(obj == Opcodes.INTEGER) {
            return Type.INT_TYPE;
        }
        if(obj == Opcodes.FLOAT) {
            return Type.FLOAT_TYPE;
        }
        if(obj == Opcodes.DOUBLE) {
            return Type.DOUBLE_TYPE;
        }
        if(obj == Opcodes.LONG) {
            return Type.LONG_TYPE;
        }
        if(obj == Opcodes.NULL) {
            return Type.getType("Ljava/lang/Object;");
        }
        if(obj == Opcodes.TOP) {
            return Type.getType("Ljava/lang/Object;");
        }
        if(obj instanceof String) {
            if(!(((String) obj).charAt(0) == '[') && ((String) obj).length() > 1) {
                return Type.getType("L" + obj + ";");
            } else {
                return Type.getObjectType((String) obj);
            }
        }
        if(obj instanceof Label || obj == Opcodes.UNINITIALIZED_THIS) {
            return Type.getType("Luninitialized;");
        }
        throw new IllegalArgumentException("got " + obj + " zzz" + obj.getClass());
    }
}
