package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.PhosphorInstructionInfo;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.TaggedValue;
import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

public class MethodArgReindexer extends MethodVisitor {

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
    int line;
    private boolean hasPreAllocatedReturnAddress;
    private Type newReturnType;

    private Map<String, Integer> parameters = new HashMap<>();
    private int indexOfControlTagsInLocals;
    HashSet<Label> tryCatchHandlers = new HashSet<>();

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
    boolean isHandler;
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
        for(Type oldArgType : oldArgTypes) {
            oldArgMappings[oldVarCount] = oldVarCount + newArgOffset;
            if(oldArgType.getSize() == 2) {
                oldArgMappings[oldVarCount + 1] = oldVarCount + newArgOffset + 1;
                oldVarCount++;
            }
            oldVarCount++;
            if(!isLambda) {
                if(TaintUtils.isShadowedType(oldArgType)) {
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
        newArgOffset += nWrappers;
    }

    @Override
    public void visitLdcInsn(Object cst) {
        if(cst instanceof PhosphorInstructionInfo) {
            mv.visitLdcInsn(cst);
        } else {
            super.visitLdcInsn(cst);
        }
    }

    @Override
    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
        Type t = Type.getType(desc);
        if (TaintUtils.isWrappedType(t)) {
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
                super.visitLocalVariable("Phopshor$$ImplicitTaintTrackingFromParent", Type.getDescriptor(ControlTaintTagStack.class), null, start, end, oldArgMappings[index] + 1);
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
        Object[] remappedLocals = new Object[Math.max(local.length, origNumArgs) + newArgOffset + 1]; //was +1, not sure why??
        int nLocalsInputFrame = nLocal;
        boolean isExceptionHandler = isHandler;
        isHandler = false;
        if(type == Opcodes.F_FULL || type == Opcodes.F_NEW) {
            // System.out.println("In frame: " + Arrays.toString(local));
            int thisLocalIndexInNewFrame = 0; //does not account for long/double
            //Special cases of no args
            if(origNumArgs == 0 && isStatic) {
                if((Configuration.IMPLICIT_HEADERS_NO_TRACKING || Configuration.IMPLICIT_TRACKING) && !name.equals("<clinit>")) {
                    remappedLocals[thisLocalIndexInNewFrame] = Type.getInternalName(ControlTaintTagStack.class);
                    thisLocalIndexInNewFrame++;
                    nLocal++;
                }
                if(hasPreAllocatedReturnAddress) {
                    remappedLocals[thisLocalIndexInNewFrame] = newReturnType.getInternalName();
                    thisLocalIndexInNewFrame++;
                    nLocal++;
                }
                for(int i = 0; i < nWrappers; i++) {
                    remappedLocals[thisLocalIndexInNewFrame] = Opcodes.TOP;
                    thisLocalIndexInNewFrame++;
                    nLocal++;
                }
            }

            //Iterate over every LV slot. Some LV slots may be high end of 2-word vars.
            int idxInOldLocals = 0;
            int idxInOldArgs = 0;
            boolean isAllTOPs = true;
            for(Object o : local) {
                if(o != Integer.valueOf(0)) {
                    isAllTOPs = false;
                    break;
                }
            }
            if(!isAllTOPs) {
                for(idxInOldArgs = 0; idxInOldArgs < Math.min(nLocal, oldArgTypesList.size()); idxInOldArgs++) {
                    Type t = oldArgTypesList.get(idxInOldArgs);
                    if(TaintUtils.isWrappedType(t)) {
                        remappedLocals[thisLocalIndexInNewFrame] = TaintUtils.getWrapperType(t).getInternalName();
                    } else {
                        remappedLocals[thisLocalIndexInNewFrame] = local[idxInOldLocals];
                        if(local[idxInOldLocals] == Opcodes.TOP && t.getSize() == 2) {
                            thisLocalIndexInNewFrame++;
                            remappedLocals[thisLocalIndexInNewFrame] = Opcodes.TOP;
                            idxInOldLocals++;
                        }
                    }
                    thisLocalIndexInNewFrame++;
                    if(local[idxInOldLocals] == Opcodes.TOP) {
                        remappedLocals[thisLocalIndexInNewFrame] = Opcodes.TOP;
                    } else {
                        remappedLocals[thisLocalIndexInNewFrame] = Configuration.TAINT_TAG_INTERNAL_NAME;
                    }
                    nLocal++;
                    thisLocalIndexInNewFrame++;
                    idxInOldLocals++;
                }
            }
            if(origNumArgs != 0) {
                if(Configuration.IMPLICIT_HEADERS_NO_TRACKING || Configuration.IMPLICIT_TRACKING) {
                    while(thisLocalIndexInNewFrame < indexOfControlTagsInLocals) {
                        //There are no locals in this frame, BUT there were args on the method - make sure metadata goes to the right spot
                        remappedLocals[thisLocalIndexInNewFrame] = Opcodes.TOP;
                        thisLocalIndexInNewFrame++;
                        nLocal++;
                    }
                    remappedLocals[thisLocalIndexInNewFrame] = Type.getInternalName(ControlTaintTagStack.class);
                    thisLocalIndexInNewFrame++;
                    nLocal++;
                }
                if(hasPreAllocatedReturnAddress) {
                    remappedLocals[thisLocalIndexInNewFrame] = newReturnType.getInternalName();
                    thisLocalIndexInNewFrame++;
                    nLocal++;
                }
                for(int i = 0; i < nWrappers; i++) {
                    remappedLocals[thisLocalIndexInNewFrame] = Opcodes.TOP;
                    thisLocalIndexInNewFrame++;
                    nLocal++;
                }
            }
            for(int i = idxInOldLocals; i < nLocalsInputFrame; i++) {
                remappedLocals[thisLocalIndexInNewFrame] = local[i];
                Type t = getTypeForStackTypeTOPAsNull(local[i]);
                if(TaintUtils.isWrappedType(t)) {
                    remappedLocals[thisLocalIndexInNewFrame] = TaintUtils.getWrapperType(Type.getObjectType((String) local[i])).getInternalName();
                }
                thisLocalIndexInNewFrame++;
            }

        } else {
            remappedLocals = local;
        }

        if(nLocal > remappedLocals.length) {
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
        // System.out.println("Out frame: " + Arrays.toString(remappedLocals));
        super.visitFrame(type, nLocal, remappedLocals, nStack, stack2);

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
        if(TaintUtils.DEBUG_LOCAL) {
            System.out.println("\t\t" + origVar + "->" + var);
        }
        super.visitIincInsn(var, increment);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itfc) {
        super.visitMethodInsn(opcode, owner, name, desc, itfc);
    }

    public void visitVarInsn(int opcode, int var) {
        if(opcode == TaintUtils.BRANCH_END || opcode == TaintUtils.BRANCH_START || opcode == TaintUtils.REVISABLE_BRANCH_START) {
            super.visitVarInsn(opcode, var);
            return;
        }
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
        if(TaintUtils.DEBUG_LOCAL) {
            System.out.println("MAR\t\t" + origVar + "->" + var + " " + originalLastArgIdx);
        }
        super.visitVarInsn(opcode, var);
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
