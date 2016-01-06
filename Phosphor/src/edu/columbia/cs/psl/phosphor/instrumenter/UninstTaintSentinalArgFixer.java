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
//	int idxOfReturnPrealloc;

	boolean hasPreAllocedReturnAddr;
	Type newReturnType;

	ArrayList<Type> oldTypesDoublesAreOne;

	@Override
	public void visitMaxs(int maxStack, int maxLocals) {
		super.visitMaxs(maxStack, maxLocals+newArgOffset);
	}
	public UninstTaintSentinalArgFixer(MethodVisitor mv, int access, String name, String desc, String originalDesc) {
		super(Opcodes.ASM5, mv);
		this.name = name;
		this.desc = desc;
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
		firstFrameLocals = new Type[origNumArgs];

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
			}
			oldArgMappings[oldVarCount] = oldVarCount + newArgOffset;
			if (TaintUtils.DEBUG_LOCAL)
				System.out.println(">>>> OVC" + oldVarCount + "->" + oldArgMappings[oldVarCount]);
			oldVarCount += oldArgTypes[i].getSize();
		}
//				System.out.println(name+originalDesc + " "+isStatic+" -> origLastArg is " + originalLastArgIdx + "orig nargs " + origNumArgs);
		oldArgTypesList = new ArrayList<Type>();
		oldTypesDoublesAreOne = new ArrayList<Type>();
		if (!isStatic) {
			oldArgTypesList.add(Type.getType("Lthis;"));
			oldTypesDoublesAreOne.add(Type.getType("Lthis;"));
		}
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
		if (name.equals("<init>")) {
			hasTaintSentinalAddedToDesc = true;
			newArgOffset++;
		}
		if(!name.equals("<clinit>") && TaintUtils.PREALLOC_RETURN_ARRAY)
		{
			lvForReturnVar = originalLastArgIdx + newArgOffset;
			newArgOffset++;
		}
//		System.out.println("PRealloc at " + lvForReturnVar);
	}
	public int lvForReturnVar;
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
			super.visitLocalVariable(name, desc, signature, start, end, oldArgMappings[index] );
			if (index == originalLastArgIdx - 1 && this.name.equals("<init>") && hasTaintSentinalAddedToDesc) {
				super.visitLocalVariable("TAINT_STUFF_TO_IGNORE_HAHA", "Ljava/lang/Object;", null, start, end, originalLastArgIdx+ (Configuration.IMPLICIT_TRACKING ? 2 : 1));
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
		if (type == Opcodes.F_FULL || type == Opcodes.F_NEW) {
			int numLocalsToIterateOverForArgs = origNumArgs;
			int idxToUseForArgs = 0;
			boolean lastWasTop2Words = false;
			for (int i = 0; i < origNLocal; i++) {

				if (i == origNumArgs && hasTaintSentinalAddedToDesc) {
					remappedLocals[newIdx] = Opcodes.NULL;
					newIdx++;
					nLocal++;
				}
				if (i == origNumArgs && TaintUtils.PREALLOC_RETURN_ARRAY && !name.equals("<clinit>")) {
					remappedLocals[newIdx] = Configuration.TAINTED_RETURN_HOLDER_DESC;
					newIdx++;
					nLocal++;
				}
				if (i < firstFrameLocals.length && TaintUtils.isPrimitiveArrayType(firstFrameLocals[i])) {
					remappedLocals[newIdx] = Configuration.TAINT_TAG_ARRAY_STACK_TYPE;
					newIdx++;
					nLocal++;
				}
				remappedLocals[newIdx] = local[i];
				
				newIdx++;

			}

		} else {
			remappedLocals = local;
		}
//		System.out.println("Remaped locals:  "+ Arrays.toString(remappedLocals));
		super.visitFrame(type, nLocal, remappedLocals, nStack, stack);

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
		if (opcode == TaintUtils.BRANCH_END || opcode == TaintUtils.BRANCH_START) {
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
		super.visitVarInsn(opcode, var);
	}
}
