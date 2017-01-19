package edu.columbia.cs.psl.phosphor.instrumenter;

import java.util.ArrayList;
import java.util.Arrays;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;

public class UninstTaintSentinalArgFixer extends MethodVisitor {
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

	Type[] firstFrameLocals;
	int idxOfReturnPrealloc;

	boolean hasPreAllocedReturnAddr;
	Type newReturnType;

	ArrayList<Type> oldTypesDoublesAreOne;

	public UninstTaintSentinalArgFixer(MethodVisitor mv, int access, String name, String desc, String originalDesc) {
		super(Opcodes.ASM5, mv);
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
		if (!isStatic) {
			oldArgTypesList.add(Type.getType("Lthis;"));
			oldTypesDoublesAreOne.add(Type.getType("Lthis;"));
		}
		for (Type t : Type.getArgumentTypes(originalDesc)) {
			oldArgTypesList.add(t);
			oldTypesDoublesAreOne.add(t);
			if (t.getSize() == 2)
				oldArgTypesList.add(Type.getType("LTOP;"));
		}
		if (name.equals("<init>")) {
			hasTaintSentinalAddedToDesc = true;
			newArgOffset++;
		}
	}

	public int getNewArgOffset() {
		return newArgOffset;
	}

	int nLVTaintsCounted = 0;

	boolean returnLVVisited = false;

	@Override
	public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
		if (!isStatic && index == 0)
			super.visitLocalVariable(name, desc, signature, start, end, index);
		else if (index < originalLastArgIdx) {
			super.visitLocalVariable(name, desc, signature, start, end, index);
			if (index == originalLastArgIdx - 1 && this.name.equals("<init>") && hasTaintSentinalAddedToDesc) {
				super.visitLocalVariable("TAINT_STUFF_TO_IGNORE_HAHA", "Ljava/lang/Object;", null, start, end, originalLastArgIdx+ (Configuration.IMPLICIT_TRACKING ? 2 : 1));
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
			System.out.println("Pre-reindex Frame: " + Arrays.toString(local) + ";" + nLocal + " ; " + type);
		}

		int newIdx = 0;
		int origNLocal = nLocal;
		if (type == Opcodes.F_FULL || type == Opcodes.F_NEW) {
			int numLocalsToIterateOverForArgs = origNumArgs;
			int idxToUseForArgs = 0;
			boolean lastWasTop2Words = false;
			for (int i = 0; i < origNLocal; i++) {

				if (i == origNumArgs && hasTaintSentinalAddedToDesc) {
					remappedLocals[newIdx] = Opcodes.TOP;
					newIdx++;
					nLocal++;
				}
				remappedLocals[newIdx] = local[i];
				
				newIdx++;

			}

		} else {
			remappedLocals = local;
		}
		super.visitFrame(type, nLocal, remappedLocals, nStack, stack);

	}

	@Override
	public void visitIincInsn(int var, int increment) {
		int origVar = var;
		if (!isStatic && var == 0)
			var = 0;
		else if (var < originalLastArgIdx) {
			//nothing
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
		if (opcode == TaintUtils.BRANCH_END || opcode == TaintUtils.BRANCH_START) {
			super.visitVarInsn(opcode, var);
			return;
		}
		int origVar = var;
		if (!isStatic && var == 0)
			var = 0;
		else if (var < originalLastArgIdx) {
			//nothing
		} else {
			//not accessing an arg. just add offset.
			var += newArgOffset;
		}

		super.visitVarInsn(opcode, var);
	}
}
