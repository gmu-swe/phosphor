package edu.columbia.cs.psl.phosphor.instrumenter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.util.Printer;

import edu.columbia.cs.psl.phosphor.org.objectweb.asm.commons.OurLocalVariablesSorter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;
import edu.columbia.cs.psl.phosphor.struct.EnqueuedTaint;

public class LocalVariableManager extends OurLocalVariablesSorter implements Opcodes {
	private static final boolean DEBUG = false;

	private HashSet<LocalVariableNode> createdLVs = new HashSet<LocalVariableNode>();
	private HashMap<Integer, LocalVariableNode> curLocalIdxToLVNode =
		new HashMap<Integer, LocalVariableNode>();
	private HashMap<Integer, Integer> origLVMap = new HashMap<Integer, Integer>();
	private HashMap<Integer, Integer> shadowLVMap = new HashMap<Integer, Integer>();
	private HashMap<Integer, Object> shadowLVMapType = new HashMap<Integer, Object>();
	private HashMap<Type, Integer> preAllocedReturnTypes = new HashMap<Type, Integer>();
	private HashSet<Integer> tmpLVIdices = new HashSet<Integer>();

	public HashMap<Integer, Integer> varToShadowVar = new HashMap<Integer, Integer>();
	public HashMap<Integer,Integer> varsToRemove = new HashMap<Integer, Integer>();

	private ArrayList<Type> oldArgTypes = new ArrayList<Type>();
	private ArrayList<TmpLV> tmpLVs = new ArrayList<LocalVariableManager.TmpLV>();

	private NeverNullArgAnalyzerAdapter analyzer;
	private Label ctrlTagStartLbl;
	private Label end;
	private PrimitiveArrayAnalyzer primitiveArrayFixer;
	private MethodVisitor uninstMV;
	private Type returnType;

	private boolean disabled = false;
	boolean isIgnoreEverything = false;
	boolean endVisited = false;
	int jumpIdx;
	int idxOfMasterControlLV = -1;
	int createdLVIdx = 0;

	// this argument has not initialized!
	int lastArg;

	@Override
	public void visitInsn(int opcode) {
		if(opcode == TaintUtils.IGNORE_EVERYTHING)
			isIgnoreEverything = !isIgnoreEverything;
		super.visitInsn(opcode);
	}

	@Override
	public void visitVarInsn(int opcode, int var) {
		if(opcode == TaintUtils.BRANCH_END
				|| opcode == TaintUtils.BRANCH_START
				|| isIgnoreEverything) {
			if(var == -1)
				mv.visitVarInsn(opcode, idxOfMasterControlLV);
			else
				mv.visitVarInsn(opcode, var);
			return;
				}
		super.visitVarInsn(opcode, var);
	}

	public LocalVariableManager(int access, String desc,
			MethodVisitor mv, NeverNullArgAnalyzerAdapter analyzer, MethodVisitor uninstMV) {
		super(ASM5, access, desc, mv);
		this.analyzer = analyzer;
		this.uninstMV = uninstMV;
		returnType = Type.getReturnType(desc);
		Type[] args = Type.getArgumentTypes(desc);
		if ((access & Opcodes.ACC_STATIC) == 0) {
			lastArg++;
			oldArgTypes.add(Type.getType("Lthis;"));
		}

		for (int i = 0; i < args.length; i++) {
			lastArg += args[i].getSize();
			oldArgTypes.add(args[i]);
			if(args[i].getSize() > 1) {
				oldArgTypes.add(Type.getType("Ltop;"));
			}
			if(args[i].getDescriptor().equals(
						Type.getDescriptor(ControlTaintTagStack.class))) {
				idxOfMasterControlLV = lastArg - 1;
						}
		}
		lastArg--;
		end = new Label();
		preAllocedReturnTypes.put(returnType, lastArg);
	}

	public void freeTmpLV(int idx) {
		for (TmpLV v : tmpLVs) {
			if (v.idx == idx && v.inUse) {
				Label lbl = new Label();
				super.visitLabel(lbl);
				curLocalIdxToLVNode.get(v.idx).end = new LabelNode(lbl);
				v.inUse = false;
				v.owner = null;
				if(idx < analyzer.locals.size())
					analyzer.locals.set(idx, Opcodes.TOP);
				return;
			}
		}
		throw new IllegalArgumentException("asked to free tmp lv "
				+ idx
				+ " but couldn't find it?");
	}

	@Deprecated
	public int newLocal(Type type) {
		int idx = super.newLocal(type);
		Label lbl = new Label();
		super.visitLabel(lbl);

		LocalVariableNode newLVN =
			new LocalVariableNode("phosphorShadowLV" + createdLVIdx,
					type.getDescriptor(), null,
					new LabelNode(lbl), new LabelNode(end), idx);
		createdLVs.add(newLVN);
		curLocalIdxToLVNode.put(idx, newLVN);
		createdLVIdx++;

		return idx;
	}

	public int newShadowLV(Type type, int shadows) {
		int idx = super.newLocal(type);
		Label lbl = new Label();
		super.visitLabel(lbl);

		LocalVariableNode newLVN =
			new LocalVariableNode("phosphorShadowLVFor" + shadows + "XX" + createdLVIdx,
					type.getDescriptor(), null,
					new LabelNode(lbl), new LabelNode(end), idx);

		createdLVs.add(newLVN);
		curLocalIdxToLVNode.put(idx, newLVN);
		createdLVIdx++;
		shadowLVMap.put(idx, origLVMap.get(shadows));
		varToShadowVar.put(shadows, idx);

		return idx;
	}


	public int getIdxOfMasterControlLV() {
		return idxOfMasterControlLV;
	}

	public int createMasterControlTaintLV() {
		int idx = super.newLocal(Type.getType(ControlTaintTagStack.class));

		if (ctrlTagStartLbl == null) {
			ctrlTagStartLbl = new Label();
			super.visitLabel(ctrlTagStartLbl);
		}

		LocalVariableNode newLVN =
			new LocalVariableNode("phosphorJumpControlTag" + jumpIdx,
					Type.getDescriptor(ControlTaintTagStack.class), null,
					new LabelNode(ctrlTagStartLbl), new LabelNode(end), idx);
		createdLVs.add(newLVN);
		analyzer.locals.add(idx, Type.getInternalName(ControlTaintTagStack.class));
		this.idxOfMasterControlLV = idx;
		jumpIdx++;

		return idx;
	}

	public int newControlTaintLV() {
		int idx = super.newLocal(
				Type.getType("Ledu/columbia/cs/psl/phosphor/struct/EnqueuedTaint;"));

		if (ctrlTagStartLbl == null) {
			ctrlTagStartLbl = new Label();
			super.visitLabel(ctrlTagStartLbl);
		}

		LocalVariableNode newLVN =
			new LocalVariableNode("phosphorJumpControlTag" + jumpIdx,
					"Ledu/columbia/cs/psl/phosphor/struct/EnqueuedTaint;", null,
					new LabelNode(ctrlTagStartLbl), new LabelNode(end), idx);
		createdLVs.add(newLVN);
		analyzer.locals.add(idx, "edu/columbia/cs/psl/phosphor/struct/EnqueuedTaint");
		jumpIdx++;

		return idx;
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
			objType= Opcodes.LONG;
			break;
		case Type.DOUBLE:
			objType= Opcodes.DOUBLE;
			break;
		case Type.FLOAT:
			objType= Opcodes.FLOAT;
			break;
		}
		shadowLVMapType.put(ret, objType);
		return ret;
	}

	private int newPreAllocedReturnType(Type type) {
		int idx = super.newLocal(type);
		Label lbl = new Label();
		super.visitLabel(lbl);
		LocalVariableNode newLVN =
			new LocalVariableNode("phosphorReturnPreAlloc" + createdLVIdx,
					type.getDescriptor(), null,
					new LabelNode(lbl), new LabelNode(end), idx);
		createdLVs.add(newLVN);
		curLocalIdxToLVNode.put(idx, newLVN);
		createdLVIdx++;
		analyzer.locals.add(idx, type.getInternalName());

		return idx;
	}

	@Override
	public void remapLocal(int local, Type type) {
		Label lbl = new Label();
		super.visitLabel(lbl);
		curLocalIdxToLVNode.get(local).end = new LabelNode(lbl);
		super.remapLocal(local, type);

		LocalVariableNode newLVN =
			new LocalVariableNode("phosphorShadowLV" + createdLVIdx,
					type.getDescriptor(), null,
					new LabelNode(lbl), new LabelNode(end), local);
		createdLVs.add(newLVN);
		curLocalIdxToLVNode.put(local, newLVN);
		createdLVIdx++;
	}

	/**
	 * Gets a tmp lv capable of storing the top stack el
	 * 
	 * @return
	 */
	public int getTmpLV() {
		Object obj = analyzer.stack.get(analyzer.stack.size() - 1);
		if (obj instanceof String)
			return getTmpLV(Type.getObjectType((String) obj));
		if (obj == Opcodes.INTEGER)
			return getTmpLV(Type.INT_TYPE);
		if (obj == Opcodes.FLOAT)
			return getTmpLV(Type.FLOAT_TYPE);
		if (obj == Opcodes.DOUBLE)
			return getTmpLV(Type.DOUBLE_TYPE);
		if (obj == Opcodes.LONG)
			return getTmpLV(Type.LONG_TYPE);
		if (obj == Opcodes.TOP) {
			obj = analyzer.stack.get(analyzer.stack.size() - 2);
			if (obj == Opcodes.DOUBLE)
				return getTmpLV(Type.DOUBLE_TYPE);
			if (obj == Opcodes.LONG)
				return getTmpLV(Type.LONG_TYPE);
		}
		return getTmpLV(Type.getType("Ljava/lang/Object;"));
	}

	public int getTmpLV(Type t) {
		if (t.getDescriptor().equals("java/lang/Object;"))
			throw new IllegalArgumentException();

		for (TmpLV lv : tmpLVs) {
			if (!lv.inUse && lv.type.getSize() == t.getSize()) {
				if (!lv.type.equals(t)) {
					remapLocal(lv.idx, t);
					if (analyzer.locals != null
							&& lv.idx < analyzer.locals.size()) {
						analyzer.locals.set(lv.idx, TaintUtils.getStackTypeForType(t));
					}
					lv.type = t;
				}
				lv.inUse = true;
				if (DEBUG) {
					lv.owner = new IllegalStateException("Unclosed tmp lv created at:");
					lv.owner.fillInStackTrace();
				}
				return lv.idx;
			}
		}
		TmpLV newLV = new TmpLV();
		newLV.idx = newLocal(t);
		newLV.type = t;
		newLV.inUse = true;
		tmpLVs.add(newLV);
		tmpLVIdices.add(newLV.idx);
		if (DEBUG) {
			newLV.owner = new IllegalStateException("Unclosed tmp lv created at:");
			newLV.owner.fillInStackTrace();
		}
		return newLV.idx;
	}

	@Override
	public void visitLocalVariable(String name, String desc, String signature,
			Label start, Label end, int index) {
		super.visitLocalVariable(name, desc, signature, start, end, index);
		if (!createdLVs.isEmpty()) {
			if (!endVisited) {
				super.visitLabel(this.end);
				endVisited = true;
			}

			for (LocalVariableNode n : createdLVs) {
				uninstMV.visitLocalVariable(n.name, n.desc, n.signature,
						n.start.getLabel(), n.end.getLabel(), n.index);
			}
			createdLVs.clear();
		}
	}

	@Override
	public void visitMaxs(int maxStack, int maxLocals) {
		if(!endVisited) {
			super.visitLabel(end);
			endVisited = true;
		}
		super.visitMaxs(maxStack, maxLocals);
	}

	@Override
	public void visitEnd() {
		super.visitEnd();
		for (TmpLV l : tmpLVs) {
			if (l.inUse)
				throw l.owner;
		}
	}

	private class TmpLV {
		int idx;
		Type type;
		boolean inUse;
		IllegalStateException owner;

		@Override
		public String toString() {
			return "TmpLV [idx=" + idx + ", type=" + type + ", inUse=" + inUse + "]";
		}
	}

	public void visitCode() {
		super.visitCode();
		if (primitiveArrayFixer != null)
			for (Type t : primitiveArrayFixer.wrapperTypesToPreAlloc) {
				if (t.equals(returnType)) {
					preAllocedReturnTypes.put(t, lastArg);
				} else {
					int lv = newPreAllocedReturnType(t);
					preAllocedReturnTypes.put(t, lv);
					super.visitTypeInsn(NEW, t.getInternalName());
					super.visitInsn(DUP);
					super.visitMethodInsn(INVOKESPECIAL, t.getInternalName(), "<init>", "()V", false);
					mv.visitVarInsn(ASTORE, lv);
				}
			}
	}

	public void setPrimitiveArrayAnalyzer(PrimitiveArrayAnalyzer primitiveArrayFixer) {
		this.primitiveArrayFixer = primitiveArrayFixer;
	}

	public int getPreAllocedReturnTypeVar(Type newReturnType) {
		if(!preAllocedReturnTypes.containsKey(newReturnType))
			throw new IllegalArgumentException("Got "
					+ newReturnType
					+ " but have "
					+ preAllocedReturnTypes);
		return preAllocedReturnTypes.get(newReturnType);
	}

	@Override
	public void visitLineNumber(int line, Label start) {
		super.visitLineNumber(line, start);
	}
	
	@Override
	public void visitFrame(final int type, final int nLocal,
			final Object[] local, final int nStack, final Object[] stack) {
		if(type == TaintUtils.RAW_INSN) {
			mv.visitFrame(Opcodes.F_NEW, nLocal, local, nStack, stack);
			return;
		}

		if (type != Opcodes.F_NEW) { // uncompressed frame
			throw new IllegalStateException(
					"ClassReader.accept() should be called with EXPAND_FRAMES flag");
		}

		if (!changed && !isFirstFrame) { // optimization for the case where mapping = identity
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
			if(tmpLVIdices.contains(i))
				newLocals[i] = Opcodes.TOP;
		}

		ArrayList<Object> locals = new ArrayList<Object>();
		for(Object o : local) {
			locals.add(o);
			if(o == Opcodes.DOUBLE || o == Opcodes.LONG)
				locals.add(Opcodes.TOP);
		}

		// copies types from 'local' to 'newLocals'
		// 'newLocals' currently empty

		if(!disabled)
			for(Entry<Type, Integer> t : preAllocedReturnTypes.entrySet()) {
				if(t.getKey().getSort() != Type.OBJECT)
					continue;
				int idx = t.getValue();
				if(idx >= 0) {
					setFrameLocal(idx, t.getKey().getInternalName());
				}
			}
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
				setFrameLocal(remap(index, typ), t);
				Object shadowType = null;

				if (t instanceof Integer
						&& t != Opcodes.NULL
						&& t != Opcodes.UNINITIALIZED_THIS) {
					shadowType = Configuration.TAINT_TAG_STACK_TYPE;
				} else if (t instanceof String) {
					Type _t = Type.getObjectType((String) t);
					if(_t.getSort() == Type.ARRAY
							&& _t.getDimensions() == 1
							&& _t.getElementType().getSort() != Type.OBJECT)
						shadowType = Configuration.TAINT_TAG_ARRAY_STACK_TYPE;
				}

				if (!disabled && shadowType != null) {

					int newVar = remap(index, typ);
					int shadowVar = 0;
					if (newVar > lastArg) {
						if (!varToShadowVar.containsKey(newVar))
							shadowVar = newShadowLV(typ, newVar);
						else
							shadowVar = varToShadowVar.get(newVar);
						// System.out.println("Adding storage for " + newVar + " at  " + shadowVar);
						setFrameLocal(shadowVar, shadowType);
					} else {
						// Check to make sure that we already allocated 
						// a shadow LV for this in the methodargreindexer
						// System.out.println("Reusing local storage for " + newVar + " at " + (newVar -1));
						// System.out.println(index);
						// System.out.println(oldArgTypes);
						Type oldT = oldArgTypes.get(index);
						// System.out.println(t + " vs " + oldT);
						if (t instanceof Integer
								&& oldT.getSort() != Type.OBJECT
								&& oldT.getSort() != Type.ARRAY) {
							// Had shadow int, still need that, still OK
						} else if (t instanceof String
								&& oldT.getSort() == Type.ARRAY
								&& oldT.getDimensions() == 1
								&& oldT.getElementType().getSort() != Type.OBJECT) {
							// Had shadow array still ok
						} else if (t instanceof Integer
								&& (oldT.getSort() == Type.ARRAY
									&& oldT.getDimensions() == 1
									&& oldT.getElementType().getSort() != Type.OBJECT)) {
							// Had a shodow array, need shadow int
							setFrameLocal(index-1, Configuration.TAINT_TAG_STACK_TYPE);
						} else if (t instanceof String
								&& oldT.getSort() != Type.ARRAY
								&& oldT.getSort() != Type.OBJECT) {
							// Had shadow int, need shadow array
							setFrameLocal(index-1, Configuration.TAINT_TAG_ARRAY_STACK_TYPE);
						} else {
							// Had nothing, need something
							if (!varToShadowVar.containsKey(newVar))
								shadowVar = newShadowLV(typ, newVar);
							else
								shadowVar = varToShadowVar.get(newVar);
							// System.out.println("Adding storage for " + newVar + " at  " + shadowVar);
							setFrameLocal(shadowVar, shadowType);
						}
					}
				}
			}
			index += size;
		}

		for (int i : varToShadowVar.keySet()) {
			if(i < newLocals.length
					&& newLocals[i] == null
					&& varToShadowVar.get(i) < newLocals.length) {
				newLocals[varToShadowVar.get(i)] = Opcodes.TOP;
			} else if (i < newLocals.length
					&& !TaintAdapter.isPrimitiveStackType(newLocals[i])
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
		mv.visitFrame(type, number, newLocals, nStack, stack);
		// System.out.println("fin" + Arrays.toString(newLocals));

		// restores original value of 'newLocals'
		newLocals = oldLocals;
	}

	public void disable() {
		disabled = true;
	}
}
