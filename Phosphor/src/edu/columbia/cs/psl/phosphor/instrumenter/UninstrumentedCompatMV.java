package edu.columbia.cs.psl.phosphor.instrumenter;

import java.util.Arrays;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.MethodDescriptor;
import edu.columbia.cs.psl.phosphor.SelectiveInstrumentationManager;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.LocalVariableNode;

import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArrayWithObjTag;

public class UninstrumentedCompatMV extends TaintAdapter {
	private NeverNullArgAnalyzerAdapter analyzer;
	private boolean skipFrames;

	public UninstrumentedCompatMV(int access, String className, String name, String desc, String signature, String[] exceptions, MethodVisitor mv, NeverNullArgAnalyzerAdapter analyzer,
			boolean skipFrames) {
		super(access, className, name, desc, signature, exceptions, mv, analyzer);
		this.analyzer = analyzer;
		this.skipFrames = skipFrames;
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		Type t = Type.getType(desc);
		switch (opcode) {
		case Opcodes.GETFIELD:
		case Opcodes.GETSTATIC:
			if (desc.endsWith("Ljava/lang/Object;") || (t.getSort() == Type.ARRAY && t.getDimensions() > 1 && t.getElementType().getSort() != Type.OBJECT)) {
				if (!desc.endsWith("Ljava/lang/Object;"))
					desc = MultiDTaintedArray.getTypeForType(t).getDescriptor();
				super.visitFieldInsn(opcode, owner, name, desc);
				super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArray.class), "maybeUnbox", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
				super.visitTypeInsn(Opcodes.CHECKCAST, t.getInternalName());
			} else
				super.visitFieldInsn(opcode, owner, name, desc);
			break;
		case Opcodes.PUTFIELD:
		case Opcodes.PUTSTATIC:
			if (desc.endsWith("Ljava/lang/Object;") || (t.getSort() == Type.ARRAY && t.getDimensions() > 1 && t.getElementType().getSort() != Type.OBJECT)) {
				super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArray.class), "boxIfNecessary", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
				if (desc.endsWith("Ljava/lang/Object;"))
					super.visitTypeInsn(Opcodes.CHECKCAST, t.getInternalName());
				else {
					desc = MultiDTaintedArray.getTypeForType(t).getDescriptor();
					super.visitTypeInsn(Opcodes.CHECKCAST, MultiDTaintedArray.getTypeForType(t).getInternalName());
				}
			} else if (t.getSort() == Type.ARRAY && t.getDimensions() == 1 && t.getElementType().getSort() != Type.OBJECT) {
				//1d prim array - need to make sure that there is some taint here
				FrameNode fn = getCurrentFrameNode();
				fn.type = Opcodes.F_NEW;
				super.visitInsn(Opcodes.DUP);
				Label ok = new Label();
				super.visitJumpInsn(IFNULL, ok);
				if (opcode == Opcodes.PUTFIELD) {
					//O A
					super.visitInsn(DUP2);
					//O A O A
				} else
					super.visitInsn(Opcodes.DUP);
				super.visitInsn(Opcodes.ARRAYLENGTH);
				if (!Configuration.MULTI_TAINTING)
					super.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_INT);
				else
					super.visitTypeInsn(Opcodes.ANEWARRAY, Configuration.TAINT_TAG_INTERNAL_NAME);
				super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_ARRAYDESC);
				super.visitLabel(ok);
				if (!skipFrames)
					fn.accept(this);
			}
			super.visitFieldInsn(opcode, owner, name, desc);
			break;
		}
	}

	@Override
	public void visitTypeInsn(int opcode, String type) {
		if (opcode == Opcodes.CHECKCAST || opcode == Opcodes.INSTANCEOF) {
			if (analyzer.stack.size() > 0 && "java/lang/Object".equals(analyzer.stack.get(analyzer.stack.size() - 1)) && type.startsWith("[")) {
				super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArray.class), "maybeUnbox", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
			}
		}
		super.visitTypeInsn(opcode, type);
	}

	@Override
	public void visitInsn(int opcode) {
		switch (opcode) {
		case Opcodes.AASTORE:
			Object arType = analyzer.stack.get(analyzer.stack.size() - 3);
			Type elType = getTopOfStackType();
			if(arType.equals("[Ljava/lang/Object;") && (
					(elType.getSort() == Type.ARRAY && elType.getElementType().getSort() != Type.OBJECT)
					|| elType.getDescriptor().equals("Ljava/lang/Object;")))
			{
				super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArray.class), "boxIfNecessary", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
			}
			super.visitInsn(opcode);
			break;
		case Opcodes.AALOAD:
			super.visitInsn(opcode);
			break;
		case Opcodes.MONITORENTER:
		case Opcodes.MONITOREXIT:
			if(getTopOfStackObject().equals("java/lang/Object"))
			{
				//never allow monitor to occur on a multid type
				super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArray.class), "unbox1D", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
			}
			super.visitInsn(opcode);
			break;
		default:
			super.visitInsn(opcode);
			break;
		}
	}

	void ensureBoxedAt(int n, Type t) {
		switch (n) {
		case 0:
			super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArray.class), "boxIfNecessary", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
			super.visitTypeInsn(Opcodes.CHECKCAST, t.getInternalName());
			break;
		case 1:
			Object top = analyzer.stack.get(analyzer.stack.size() - 1);
			if (top == Opcodes.LONG || top == Opcodes.DOUBLE || top == Opcodes.TOP) {
				super.visitInsn(DUP2_X1);
				super.visitInsn(POP2);
				super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArray.class), "boxIfNecessary", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
				super.visitTypeInsn(Opcodes.CHECKCAST, t.getInternalName());
				super.visitInsn(DUP_X2);
				super.visitInsn(POP);
			} else {
				super.visitInsn(SWAP);
				super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArray.class), "boxIfNecessary", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
				super.visitTypeInsn(Opcodes.CHECKCAST, t.getInternalName());
				super.visitInsn(SWAP);
			}
			break;
		default:
			LocalVariableNode[] d = storeToLocals(n);

			super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArray.class), "boxIfNecessary", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
			super.visitTypeInsn(Opcodes.CHECKCAST, t.getInternalName());
			for (int i = n - 1; i >= 0; i--) {
				loadLV(i, d);
			}
			freeLVs(d);

		}
	}
	
	@Override
	public void visitJumpInsn(int opcode, Label label) {
		if(Configuration.WITH_UNBOX_ACMPEQ && (opcode == Opcodes.IF_ACMPEQ || opcode == Opcodes.IF_ACMPNE))
		{
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(TaintUtils.class), "ensureUnboxed", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
			mv.visitInsn(SWAP);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(TaintUtils.class), "ensureUnboxed", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
			mv.visitInsn(SWAP);
		}
		super.visitJumpInsn(opcode, label);
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
		if(Instrumenter.isIgnoredClass(owner))
		{
			super.visitMethodInsn(opcode, owner, name, desc, itf);
			return;
		}
		if (Configuration.WITH_SELECTIVE_INST && !name.startsWith("<") && !owner.startsWith("edu/columbia/") && !owner.startsWith("[") && !name.equals("compareTo") && !name.equals("hashCode")
				&& !name.equals("equals") && !SelectiveInstrumentationManager.methodsToInstrument.contains(new MethodDescriptor(name, owner, desc))) {
			name = name + TaintUtils.METHOD_SUFFIX_UNINST;
		} else {
			//Determine if there is no wrapper
			if (desc.equals(TaintUtils.remapMethodDesc(desc))) {
				//Calling an instrumented method possibly!
				Type[] args = Type.getArgumentTypes(desc);
				int argsSize = 0;
				for (int i = 0; i < args.length; i++) {
					argsSize += args[args.length - i - 1].getSize();
					//				if (TaintUtils.DEBUG_CALLS)
					//					System.out.println(i + ", " + analyzer.stack.get(analyzer.stack.size() - argsSize) + " " + args[args.length - i - 1]);
					//					System.out.println(analyzer.stack);
					if (args[args.length - i - 1].getDescriptor().endsWith("java/lang/Object;")) {
						ensureBoxedAt(i, args[args.length - i - 1]);
					}
				}
			}
		}
		super.visitMethodInsn(opcode, owner, name, desc, itf);
	}

}
