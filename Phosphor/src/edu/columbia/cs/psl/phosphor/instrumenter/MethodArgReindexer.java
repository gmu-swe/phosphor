package edu.columbia.cs.psl.phosphor.instrumenter;

import java.util.ArrayList;
import java.util.Arrays;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.TaggedValue;
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

	ArrayList<Type> oldTypesDoublesAreOne;
	public MethodArgReindexer(MethodVisitor mv, int access, String name, String desc, String originalDesc, MethodNode lvStore) {
		super(Opcodes.ASM5,mv);
		this.lvStore = lvStore;
		lvStore.localVariables = new ArrayList<LocalVariableNode>();
		this.name = name;
		this.desc = desc;
		oldArgTypes = Type.getArgumentTypes(originalDesc);
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
		if(Configuration.IMPLICIT_TRACKING && !name.equals("<clinit>"))
		{
			hasBeenRemapped = true;
			newArgOffset++;
		}
		if (name.equals("<init>") && hasBeenRemapped) {
			hasTaintSentinalAddedToDesc = true;
			newArgOffset++;
		}
		hasPreAllocedReturnAddr = TaintUtils.isPreAllocReturnType(originalDesc);
		if (hasPreAllocedReturnAddr) {
			newReturnType = Type.getReturnType(desc);
			newArgOffset++;
			idxOfReturnPrealloc = originalLastArgIdx + newArgOffset - 1;
		}
//		System.out.println(name+desc+"NEWARGOFFSET: "  + newArgOffset);
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
		Object[] remappedLocals = new Object[local.length + newArgOffset + 1]; //was +1, not sure why??
		if (TaintUtils.DEBUG_FRAMES) {
			System.out.println(name + desc + " orig nArgs = " + origNumArgs);
			System.out.println("Pre-reindex Frame: " + Arrays.toString(local) + ";" + nLocal + " ; " + Arrays.toString(stack) + nStack);
		}
		
		int newIdx = 0;
		int origNLocal = nLocal;
//		System.out.println("MAR stac " + Arrays.toString(stack));
//		System.out.println(name+desc+"Orig locals: " + Arrays.toString(local));
		if (type == Opcodes.F_FULL || type == Opcodes.F_NEW) {
			if (origNumArgs == 0 && Configuration.IMPLICIT_TRACKING && !name.equals("<clinit>")) {
				remappedLocals[newIdx] = Type.getInternalName(ControlTaintTagStack.class);
				newIdx++;
				nLocal++;
			}
			if (origNumArgs == 0 && hasPreAllocedReturnAddr) {
				//					System.out.println("Adding local storage for " + newReturnType.getInternalName() + " At " + newIdx);
				remappedLocals[newIdx] = newReturnType.getInternalName();
				newIdx++;
				nLocal++;
			}
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
//				System.out.println(newIdx + " vs " + idxOfReturnPrealloc);
				if (origNumArgs!=0 && i ==origNumArgs-1 && hasPreAllocedReturnAddr) {

//					System.out.println("Adding local storage for " + newReturnType.getInternalName() + " At " + newIdx);
					remappedLocals[newIdx] = newReturnType.getInternalName();
					newIdx++;
					nLocal++;
				}
//				System.out.println(">"+Arrays.toString(remappedLocals));
			}

		} else {
			remappedLocals = local;
		}

//		System.out.println("New locals : " + name + desc + ":\t\t" + Arrays.toString(remappedLocals));
//		System.out.println("Nlocal: " + nLocal);
		ArrayList<Object> newStack = new ArrayList<Object>();
		int origNStack = nStack;
		for (int i = 0; i < origNStack; i++) {
			if(stack[i] instanceof TaggedValue)
			{
				Object o = ((TaggedValue) stack[i]).v;
				if(o instanceof String || o == Opcodes.NULL)
				{
					if(o == Opcodes.NULL)
						newStack.add(Opcodes.NULL);
					else
						newStack.add(TaintUtils.getShadowTaintTypeForFrame((String)(o)));
					nStack++;
				}
				else
				{
					newStack.add(Configuration.TAINT_TAG_STACK_TYPE);
					nStack++;
				}
				newStack.add(stack[i]);
			}
			else if (stack[i] != Opcodes.TOP && stack[i] instanceof String && ((String) stack[i]).charAt(1) == '[' && Type.getObjectType((String) stack[i]).getElementType().getSort() != Type.OBJECT) {
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
