package edu.columbia.cs.psl.phosphor.instrumenter;

import java.util.ArrayList;
import java.util.Arrays;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;

public class MethodArgReindexer extends MethodVisitor {
	int originalLastArgIdx;
	int[] oldArgMappings;
	int newArgOffset;
	boolean isStatic;
	int origNumArgs;
	String name;
	String desc;
	boolean hasTaintSentinalAddedToDesc = false;
	ArrayList<Type> oldArgTypesList;
	Type[] oldArgTypes;
	MethodNode lvStore;

	Type[] firstFrameLocals;
	int idxOfReturnPrealloc;

	boolean hasPreAllocedReturnAddr;
	Type newReturnType;

	String className;
	ArrayList<Type> oldTypesDoublesAreOne;
	public MethodArgReindexer(MethodVisitor mv, int access,String className, String name, String desc, String originalDesc, MethodNode lvStore) {
		super(Opcodes.ASM5,mv);
		this.lvStore = lvStore;
		lvStore.localVariables = new ArrayList<LocalVariableNode>();
		this.name = name;
		this.desc = desc;
		this.className = className;
		oldArgTypes = Type.getArgumentTypes(originalDesc);
		Type[] newArgTypes = Type.getArgumentTypes(desc);
		origNumArgs = oldArgTypes.length;
		isStatic = (Opcodes.ACC_STATIC & access) != 0;
		for (Type t : oldArgTypes)
			originalLastArgIdx += t.getSize();
		if (!isStatic)
			originalLastArgIdx++;
		if (!isStatic)
			origNumArgs++;
		newArgOffset = 0;
//		System.out.println(name+originalDesc + " -> origLastArg is " + originalLastArgIdx + "orig nargs " + origNumArgs);
		oldArgTypesList = new ArrayList<Type>();
		oldTypesDoublesAreOne = new ArrayList<Type>();
		if (!isStatic)
		{
			oldArgTypesList.add(Type.getType("Lthis;"));
			oldTypesDoublesAreOne.add(Type.getType("Lthis;"));
		}
		firstFrameLocals = new Type[origNumArgs];
		int ffl = 0;
		if(!isStatic)
		{
			firstFrameLocals[0] = Type.getObjectType("java/lang/Object");
			ffl++;
		}
		for (Type t : Type.getArgumentTypes(originalDesc)) {
			oldArgTypesList.add(t);
			oldTypesDoublesAreOne.add(t);
			firstFrameLocals[ffl] = t;
			ffl++;
			if (t.getSize() == 2)
				oldArgTypesList.add(Type.getType("LTOP;"));
		}

//		System.out.println("OLd:::" + oldTypesDoublesAreOne);
		boolean hasBeenRemapped = false;
		oldArgMappings = new int[originalLastArgIdx + 1];
		int oldVarCount = (isStatic ? 0 : 1);
		for (int i = 0; i < oldArgTypes.length; i++) {
			if (oldArgTypes[i].getSort() == Type.ARRAY) {
				if (oldArgTypes[i].getElementType().getSort() != Type.OBJECT) {
					if (oldArgTypes[i].getDimensions() == 1)
						newArgOffset++;
					hasBeenRemapped = true;
				}
			} else if (oldArgTypes[i].getSort() != Type.OBJECT) {
				hasBeenRemapped = true;
				newArgOffset += 1;
			}
			oldArgMappings[oldVarCount] = oldVarCount + newArgOffset;
			if (TaintUtils.DEBUG_LOCAL)
				System.out.println(">>>>" + oldVarCount + "->" + oldArgMappings[oldVarCount]);
			oldVarCount += oldArgTypes[i].getSize();
		}
		if(TaintUtils.PREALLOC_RETURN_ARRAY)
			hasBeenRemapped = true;
		if(Configuration.IMPLICIT_TRACKING && !name.equals("<clinit>"))
			newArgOffset++;
		if (name.equals("<init>") && hasBeenRemapped) {
			hasTaintSentinalAddedToDesc = true;
			newArgOffset++;
		}
		hasPreAllocedReturnAddr = TaintUtils.isPreAllocReturnType(originalDesc);
		if(name.equals("<clinit>"))
			hasPreAllocedReturnAddr = false;
		if (hasPreAllocedReturnAddr) {
			if (TaintUtils.PREALLOC_RETURN_ARRAY)
				newReturnType = Type.getType(Configuration.TAINTED_RETURN_HOLDER_DESC);
			else
				newReturnType = Type.getReturnType(desc);
//			System.out.println("Original lastarg " + originalLastArgIdx + " in " + originalDesc);
			idxOfReturnPrealloc = origNumArgs + newArgOffset;
			newArgOffset++;

		}
//		System.out.println(name+desc+"NEWARGOFFSET: "  + newArgOffset);
//		System.out.println(name+desc + " prealloc at " + idxOfReturnPrealloc);
		if (TaintUtils.DEBUG_FRAMES || TaintUtils.DEBUG_LOCAL)
			System.out.println(name + " origLastArg " + originalLastArgIdx + ", oldvarcount = " + oldVarCount + ", newargoffset " + newArgOffset);
	}

	public int getNewArgOffset() {
		return newArgOffset;
	}

	int nLVTaintsCounted = 0;

	boolean returnLVVisited = false;

	@Override
	public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
		if (index < originalLastArgIdx) {
			boolean found = false;
			for (Object _lv : lvStore.localVariables)
			{
				LocalVariableNode lv = (LocalVariableNode) _lv;
				if (lv != null && lv.name != null && lv.name.equals(name) && lv.index == index)
					found = true;
			}
			if (!found)
				lvStore.localVariables.add(new LocalVariableNode(name, desc, signature, null, null, index));
		}
		if (!isStatic && index == 0)
			super.visitLocalVariable(name, desc, signature, start, end, index);
		else if (index < originalLastArgIdx) {
			String shadow = TaintUtils.getShadowTaintType(desc);
			if (shadow != null)
				super.visitLocalVariable(name + "_TAINT", shadow, null, start, end, oldArgMappings[index] - 1);
			super.visitLocalVariable(name, desc, signature, start, end, oldArgMappings[index]);
			if(index == originalLastArgIdx - 1 && Configuration.IMPLICIT_TRACKING)
			{
				super.visitLocalVariable("PhopshorImplicitTaintTrackingFromParent", Type.getDescriptor(ControlTaintTagStack.class), null, start, end, oldArgMappings[index]+1);
			}
			if (index == originalLastArgIdx - 1 && this.name.equals("<init>") && hasTaintSentinalAddedToDesc) {
				super.visitLocalVariable("TAINT_STUFF_TO_IGNORE_HAHA", "Ljava/lang/Object;", null, start, end, oldArgMappings[index] + (Configuration.IMPLICIT_TRACKING ? 2 : 1));
			}
			if ((index == originalLastArgIdx - Type.getType(desc).getSize()) && hasPreAllocedReturnAddr) {
				super.visitLocalVariable("PHOSPHORPREALLOCRETURNHAHA", newReturnType.getDescriptor(), null, start, end, oldArgMappings[index] + (Configuration.IMPLICIT_TRACKING ? 2 : 1));
			}
		} else {
			super.visitLocalVariable(name, desc, signature, start, end, index + newArgOffset);
		}
	}

	@Override
	public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
		Object[] remappedLocals = new Object[Math.max(local.length + newArgOffset + 2, origNumArgs + newArgOffset + 2)]; //was +1, not sure why??
		if (TaintUtils.DEBUG_FRAMES) {
			System.out.println(name + desc + " orig nArgs = " + origNumArgs);
			System.out.println("Pre-reindex Frame: " + Arrays.toString(local) + ";" + nLocal + " ; " + type);
		}

		int newIdx = 0;
		int origNLocal = nLocal;
//		System.out.println("MAR stac " + Arrays.toString(stack));
//		System.out.println("Orig locals: " + Arrays.toString(local) + nLocal);
		if (type == Opcodes.F_FULL || type == Opcodes.F_NEW) {
			if(nLocal == 0 && !isStatic)
			{
				nLocal = 1;
				local = new Object[]{className};
				origNLocal = 1;
			}
			if (origNumArgs == 0 && Configuration.IMPLICIT_TRACKING && !name.equals("<clinit>")) {
				remappedLocals[newIdx] = Type.getInternalName(ControlTaintTagStack.class);
				newIdx++;
				nLocal++;
			}
//			System.out.println("zzOrig locals: " + Arrays.toString(local) + nLocal);

			int numLocalsToIterateOverForArgs = origNumArgs;
			int idxToUseForArgs =0;
			boolean lastWasTop2Words = false;
			for (int i = 0; i < origNLocal; i++) {
//				System.out.println("Start " + i);
//				System.out.println(newIdx +" :"+local[i]);
//				System.out.println(i + " vs " + origNLocal +", " + origNumArgsToUse);
//				System.out.println(i +": "+ local[i]);

				if (i < numLocalsToIterateOverForArgs) {
//					System.out.println(i + " " + local[i]);
					if(lastWasTop2Words)
					{
						lastWasTop2Words = false;
						numLocalsToIterateOverForArgs++;
//						origNumArgsToUse++;
						idxToUseForArgs--;
					}
					else if (local[i] == Opcodes.NULL) {
						Type t = oldTypesDoublesAreOne.get(i);
						if (t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT && t.getDimensions() == 1) {

							remappedLocals[newIdx] = TaintUtils.getShadowTaintTypeForFrame(t.getDescriptor());
							if (TaintUtils.DEBUG_FRAMES)
								System.out.println("Adding taint storage for local type " + local[i]);
							newIdx++;
							nLocal++;
							remappedLocals[newIdx] = t.getInternalName();
							newIdx++;
							idxToUseForArgs++;
							if(i == origNumArgs-1 && origNumArgs !=0 && Configuration.IMPLICIT_TRACKING)
							{
								remappedLocals[newIdx] = Type.getInternalName(ControlTaintTagStack.class);
								newIdx++;
								nLocal++;
							}
							continue;
						}
					} else if(local[i] == Opcodes.TOP)
					{
						Type t = oldTypesDoublesAreOne.get(idxToUseForArgs);
//						System.out.println(i+ " " + t +  " vs " + local[i]);
						if(t.getSize() == 2)
						{
							lastWasTop2Words = true;
						}
//						System.out.println("Old t " + t);
						if((t.getSort() != Type.OBJECT && t.getSort() != Type.ARRAY) || (t.getSort() == Type.ARRAY && t.getDimensions() == 1 && t.getElementType().getSort() != Type.OBJECT))
						{
//							System.out.println("Add a TOP");
							remappedLocals[newIdx] = Opcodes.TOP;
							newIdx++;
							nLocal++;
						}
					}
					else if (TaintAdapter.isPrimitiveType(firstFrameLocals[i])) {
						if (!(local[i] != Opcodes.TOP && local[i] instanceof String && ((String) local[i]).charAt(1) == '[')) {
							if (local[i] != Opcodes.TOP) {
								try {
									Type argType = TaintAdapter.getTypeForStackType(local[i]);
									if(Configuration.SINGLE_TAG_PER_ARRAY && !Configuration.MULTI_TAINTING)
										remappedLocals[newIdx] = Opcodes.INTEGER;
									else
										remappedLocals[newIdx] = TaintUtils.getShadowTaintTypeForFrame(argType.getDescriptor());
									//									if (TaintUtils.DEBUG_FRAMES)

									newIdx++;
									nLocal++;
//									System.out.println("Adding taint storage for local type " + local[i] +", ffl is " + firstFrameLocals[i]);
								} catch (IllegalArgumentException ex) {
									System.err.println("Locals were: " + Arrays.toString(local) + ", curious about " + i);
									throw ex;
								}
							}
						}
					}
					idxToUseForArgs++;
				}
				if (i == origNumArgs && hasTaintSentinalAddedToDesc) {
//					remappedLocals[newIdx] = Type.getInternalName(TaintSentinel.class);
					remappedLocals[newIdx] = Opcodes.TOP;

					newIdx++;
					nLocal++;
				}
//				System.out.println(remappedLocals[newIdx]);
				
//				System.out.println(newIdx + " vs " + idxOfReturnPrealloc);
				if(i == origNumArgs && hasPreAllocedReturnAddr){
//				if (origNumArgs!=0 && i ==origNumArgs-1 && hasPreAllocedReturnAddr) {
//				if (i ==origNumArgs && hasPreAllocedReturnAddr) {
//					System.out.println("Adding local storage for " + newReturnType.getInternalName() + " At " + newIdx + " in " + name + desc);
//					remappedLocals[newIdx] = newReturnType.getInternalName();
//					System.out.println("Added return storage at " + newIdx);
					newIdx++;
//					nLocal++;
				}
				
				remappedLocals[newIdx] = local[i];
				if (local[i] != Opcodes.TOP && local[i] instanceof String &&((String) local[i]).length() > 1 && ((String) local[i]).charAt(1) == '[' && Type.getObjectType((String) local[i]).getElementType().getSort() != Type.OBJECT) {
					remappedLocals[newIdx] = MultiDTaintedArray.getTypeForType(Type.getObjectType((String) local[i])).getInternalName();
				}
				newIdx++;

				if(i == origNumArgs-1 && origNumArgs !=0 && Configuration.IMPLICIT_TRACKING)
				{
					remappedLocals[newIdx] = Type.getInternalName(ControlTaintTagStack.class);
					newIdx++;
					nLocal++;
				}


			}

		} else {
			remappedLocals = local;
		}
		if(hasPreAllocedReturnAddr)
		{
			remappedLocals[idxOfReturnPrealloc] = newReturnType.getInternalName();
			nLocal++;
		}
//		System.out.println("New locals : " + name + desc + ":\t\t" + Arrays.toString(remappedLocals) + " size: " + nLocal);
		ArrayList<Object> newStack = new ArrayList<Object>();
		int origNStack = nStack;
		for (int i = 0; i < origNStack; i++) {
			if (stack[i] == Opcodes.INTEGER || stack[i] == Opcodes.FLOAT || stack[i] == Opcodes.LONG || stack[i] == Opcodes.DOUBLE) {
				if (TaintUtils.DEBUG_FRAMES)
					System.out.println("Adding taint storage for type " + stack[i]);
				newStack.add(Configuration.TAINT_TAG_STACK_TYPE);
				nStack++;
			} else if (stack[i] instanceof String) {
				Type t = TaintAdapter.getTypeForStackType(stack[i]);
				if (t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT && t.getDimensions() == 1) {
					if (TaintUtils.DEBUG_FRAMES)
						System.out.println("Adding taint storage for type " + stack[i]);
					newStack.add(TaintUtils.getShadowTaintTypeForFrame(t.getDescriptor()));
					nStack++;
				}
			}
			if (stack[i] != Opcodes.TOP && stack[i] instanceof String && ((String) stack[i]).charAt(1) == '[' && Type.getObjectType((String) stack[i]).getElementType().getSort() != Type.OBJECT) {
				newStack.add(MultiDTaintedArray.getTypeForType(Type.getObjectType((String) stack[i])).getInternalName());
			} else
				newStack.add(stack[i]);
		}
		Object[] stack2 = new Object[newStack.size()];
		stack2 = newStack.toArray();
		if (TaintUtils.DEBUG_FRAMES)
			System.out.println("Post-adjust Frame: " + Arrays.toString(remappedLocals) + ";" + Arrays.toString(stack2));
		super.visitFrame(type, nLocal, remappedLocals, nStack, stack2);
		if (TaintUtils.DEBUG_FRAMES)
			System.out.println("Post-visit Frame: " + Arrays.toString(remappedLocals) + ";" + Arrays.toString(stack2));
	}

	@Override
	public void visitIincInsn(int var, int increment) {
		int origVar = var;
		if (!isStatic && var == 0)
			var = 0;
		else if (var < originalLastArgIdx) {
			//accessing an arg; remap it
			var = oldArgMappings[var];// + (isStatic?0:1);
		} else {
			//not accessing an arg. just add offset.
			var += newArgOffset;
		}
		if (TaintUtils.DEBUG_LOCAL)
			System.out.println("\t\t" + origVar + "->" + var);
		super.visitIincInsn(var, increment);
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itfc) {
		super.visitMethodInsn(opcode, owner, name, desc, itfc);
	}

	public void visitVarInsn(int opcode, int var) {
		if(opcode == TaintUtils.BRANCH_END || opcode == TaintUtils.BRANCH_START)
		{
			super.visitVarInsn(opcode, var);
			return;
		}
		int origVar = var;
		if (!isStatic && var == 0)
			var = 0;
		else if (var < originalLastArgIdx) {
			//accessing an arg; remap it
			var = oldArgMappings[var];// + (isStatic?0:1);
		} else {
			//not accessing an arg. just add offset.
			var += newArgOffset;
		}
		if (TaintUtils.DEBUG_LOCAL)
			System.out.println("MAR\t\t" + origVar + "->" + var);
		super.visitVarInsn(opcode, var);
	}
}
