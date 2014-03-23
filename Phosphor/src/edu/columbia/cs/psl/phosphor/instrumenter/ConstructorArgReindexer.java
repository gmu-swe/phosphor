package edu.columbia.cs.psl.phosphor.instrumenter;

import java.util.ArrayList;
import java.util.Arrays;

import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Label;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Type;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.commons.InstructionAdapter;
import edu.columbia.cs.psl.phosphor.runtime.TaintSentinel;

public class ConstructorArgReindexer extends InstructionAdapter {
	int originalLastArgIdx;
	int[] oldArgMappings;
	int newArgOffset;
	boolean isStatic;
	int origNumArgs;
	String name;
	String desc;
	boolean hasTaintSentinalAddedToDesc =false;
	boolean hasPreAllocedReturnAddr = false;
	Type newReturnType = null;
	ArrayList<Type> oldArgTypesList;
	Type[]oldArgTypes;
	public ConstructorArgReindexer(MethodVisitor mv, int access, String name, String desc, String originalDesc) {
		super(Opcodes.ASM5,mv);
		this.name = name;
		this.desc =desc;
		oldArgTypes = Type.getArgumentTypes(originalDesc);
		Type[] newArgTypes = Type.getArgumentTypes(desc);
		origNumArgs = oldArgTypes.length;
		isStatic = (Opcodes.ACC_STATIC & access) != 0;
		for (Type t : oldArgTypes)
			originalLastArgIdx += t.getSize();
		if (!isStatic)
			originalLastArgIdx++;
		if(!isStatic)
			origNumArgs++;
		newArgOffset = 0;

		oldArgTypesList = new ArrayList<Type>();
		if(!isStatic)
			oldArgTypesList.add(Type.getType("Lthis;"));
		for(Type t : Type.getArgumentTypes(originalDesc))
		{
			oldArgTypesList.add(t);
		}
		
		boolean hasBeenRemapped = false;
		oldArgMappings = new int[originalLastArgIdx + 1];
		int oldVarCount = (isStatic ? 0 : 1);
		for (int i = 0; i < oldArgTypes.length; i++) {
			oldArgMappings[oldVarCount] = oldVarCount + newArgOffset;
			oldVarCount += oldArgTypes[i].getSize();
		}
		hasBeenRemapped = true;
		if(name.equals("<init>") && hasBeenRemapped)
		{
			hasTaintSentinalAddedToDesc = true;
			newArgOffset++;
		}
		hasPreAllocedReturnAddr = TaintUtils.isPreAllocReturnType(originalDesc);
		if(hasPreAllocedReturnAddr)
		{
			newReturnType = Type.getReturnType(desc);
			newArgOffset++;
		}
		if (TaintUtils.DEBUG_FRAMES || TaintUtils.DEBUG_LOCAL)
			System.out.println(name + " origLastArg " + originalLastArgIdx + ", oldvarcount = " + oldVarCount + ", newargoffset " + newArgOffset);
	}

	public int getNewArgOffset() {
		return newArgOffset;
	}

	int nLVTaintsCounted = 0;

	boolean addedbumlvinfo = false;
	boolean hasAnyLVs = false;
	@Override
	public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
		hasAnyLVs = true;
		if (!isStatic && index == 0)
			super.visitLocalVariable(name, desc, signature, start, end, index);
		else if (index < originalLastArgIdx)
		{
			super.visitLocalVariable(name, desc, signature, start, end, oldArgMappings[index]);
		}
		else
			super.visitLocalVariable(name, desc, signature, start, end, index + newArgOffset);
		
		if(index == originalLastArgIdx -1)
		{
			addedbumlvinfo=true;
			super.visitLocalVariable("TAINT_STUFF_TO_IGNORE_HAHA", "Ljava/lang/Object;", null, start, end, oldArgMappings[index]+1);
		}
	}
	Label firstLabel;
	Label lastLabel;
	@Override
	public void visitLabel(Label label) {
		super.visitLabel(label);
		lastLabel = label;
	}
	@Override
	public void visitCode() {
		firstLabel = new Label();
		super.visitCode();
		super.visitLabel(firstLabel);
	}
	@Override
	public void visitEnd() {
		if(!addedbumlvinfo)
		{
			if(lastLabel == null)
			{
				lastLabel = new Label();
				super.visitLabel(lastLabel);
			}
			if(hasAnyLVs)
				super.visitLocalVariable("TAINT_STUFF_TO_IGNORE_HAHA", "Ljava/lang/Object;", null, firstLabel, lastLabel, 1);
		}
		super.visitEnd();
	}
	@Override
	public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
		Object[] remappedLocals = new Object[local.length + newArgOffset]; //was +1, not sure why??
		if (TaintUtils.DEBUG_FRAMES)
		{
			System.out.println(name+desc + " orig nArgs = " + origNumArgs);
			System.out.println("Pre-reindex Frame: " + Arrays.toString(local) + ";" + nLocal + " ; " + type);
		}

		int newIdx = 0;
		int origNLocal = nLocal;
		if (type == Opcodes.F_FULL || type == Opcodes.F_NEW) {
			for (int i = 0; i < origNLocal; i++) {
				if(i == origNumArgs && hasTaintSentinalAddedToDesc)
				{
						remappedLocals[newIdx] = Type.getInternalName(TaintSentinel.class);
						newIdx++;
						nLocal++;
				}
				if(i == origNumArgs && hasPreAllocedReturnAddr)
				{
						remappedLocals[newIdx] = newReturnType.getInternalName();
						newIdx++;
						nLocal++;
				}
				remappedLocals[newIdx] = local[i];
				newIdx++;
			}

		} else {
			remappedLocals = local;
		}
		ArrayList<Object> newStack = new ArrayList<Object>();
		int origNStack = nStack;
		for (int i = 0; i < origNStack; i++) {
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
		if(opcode == Opcodes.INVOKEINTERFACE)
			Instrumenter.interfaces.add(owner);
		super.visitMethodInsn(opcode, owner, name, desc,itfc);
	}
	public void visitVarInsn(int opcode, int var) {
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
		super.visitVarInsn(opcode, var);
	}
}
