package edu.columbia.cs.psl.phosphor.instrumenter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Label;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Type;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.commons.LocalVariablesSorter;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.LabelNode;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.tree.LocalVariableNode;

public class LocalVariableManager extends LocalVariablesSorter implements Opcodes {
	private NeverNullArgAnalyzerAdapter analyzer;
	private static final boolean DEBUG = false;
	int createdLVIdx = 0;
	HashSet<LocalVariableNode> createdLVs = new HashSet<LocalVariableNode>();
	HashMap<Integer, LocalVariableNode> curLocalIdxToLVNode = new HashMap<Integer, LocalVariableNode>();
	MethodVisitor uninstMV;

	Type returnType;
	int lastArg;
	
	public HashMap<Integer, Integer> varToShadowVar = new HashMap<Integer, Integer>();
	public LocalVariableManager(int access, String desc, MethodVisitor mv, NeverNullArgAnalyzerAdapter analyzer, MethodVisitor uninstMV) {
		super(ASM5, access, desc, mv);
		this.analyzer = analyzer;
		this.uninstMV = uninstMV;
		returnType = Type.getReturnType(desc);
		Type[] args = Type.getArgumentTypes(desc);
		if((access & Opcodes.ACC_STATIC) == 0) lastArg++;
		for (int i = 0; i < args.length; i++) {
			lastArg += args[i].getSize();
		}
		lastArg--;
		end = new Label();
//		System.out.println("New LVS");
//		System.out.println("LVS thinks its at " + lastArg);
		preAllocedReturnTypes.put(returnType,lastArg);
	}

	public void freeTmpLV(int idx) {
		for (TmpLV v : tmpLVs) {
			if (v.idx == idx && v.inUse) {
				Label lbl = new Label();
				super.visitLabel(lbl);
				curLocalIdxToLVNode.get(v.idx).end = new LabelNode(lbl);
				v.inUse = false;
				v.owner = null;
				return;
			}
		}
		//		System.err.println(tmpLVs);
		throw new IllegalArgumentException("asked to free tmp lv " + idx + " but couldn't find it?");
	}

	@Deprecated
	public int newLocal(Type type) {
		int idx = super.newLocal(type);
		Label lbl = new Label();
		super.visitLabel(lbl);
//		new Exception().printStackTrace();

		LocalVariableNode newLVN = new LocalVariableNode("phosphorShadowLV" + createdLVIdx, type.getDescriptor(), null, new LabelNode(lbl), new LabelNode(end), idx);
		createdLVs.add(newLVN);
		curLocalIdxToLVNode.put(idx, newLVN);
		createdLVIdx++;

		return idx;
	}
	
	HashMap<Integer, Integer> origLVMap = new HashMap<Integer, Integer>();
	HashMap<Integer, Integer> shadowLVMap = new HashMap<Integer, Integer>();

	HashMap<Integer, Object> shadowLVMapType = new HashMap<Integer, Object>();
	public int newShadowLV(Type type, int shadows) {
		int idx = super.newLocal(type);
		Label lbl = new Label();
		super.visitLabel(lbl);
		
		LocalVariableNode newLVN = new LocalVariableNode("phosphorShadowLVFor" + shadows+"XX"+createdLVIdx, type.getDescriptor(), null, new LabelNode(lbl), new LabelNode(end), idx);
		createdLVs.add(newLVN);
		curLocalIdxToLVNode.put(idx, newLVN);
		createdLVIdx++;
		shadowLVMap.put(idx, origLVMap.get(shadows));
		
		varToShadowVar.put(shadows, idx);
		return idx;
	}

	@Override
	protected int remap(int var, Type type) {
		
		int ret = super.remap(var, type);
//		System.out.println("var -> " + ret);
		origLVMap.put(ret, var);
		Object objType = "[I";
		switch(type.getSort()){
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
//		System.out.println("End is going to be " + end);
		LocalVariableNode newLVN = new LocalVariableNode("phosphorReturnPreAlloc" + createdLVIdx, type.getDescriptor(), null, new LabelNode(lbl), new LabelNode(end), idx);
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

		LocalVariableNode newLVN = new LocalVariableNode("phosphorShadowLV" + createdLVIdx, type.getDescriptor(), null, new LabelNode(lbl), new LabelNode(end), local);
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
		//		System.out.println("gettmplv " + obj);
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
					if (analyzer.locals != null && lv.idx < analyzer.locals.size()) {
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
		if (DEBUG) {
			newLV.owner = new IllegalStateException("Unclosed tmp lv created at:");
			newLV.owner.fillInStackTrace();
		}
		return newLV.idx;
	}

	ArrayList<TmpLV> tmpLVs = new ArrayList<LocalVariableManager.TmpLV>();

	boolean endVisited = false;
	@Override
	public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
		super.visitLocalVariable(name, desc, signature, start, end, index);
		if (createdLVs.size() > 0) {
			if(!endVisited)
			{
				super.visitLabel(this.end);
				endVisited = true;
			}
			for (LocalVariableNode n : createdLVs) {
				uninstMV.visitLocalVariable(n.name, n.desc, n.signature, n.start.getLabel(), n.end.getLabel(), n.index);
			}
			createdLVs.clear();
		}
	}

	Label end;

	@Override
	public void visitEnd() {
		if(!endVisited)
		{
			super.visitLabel(end);
			endVisited = true;
		}
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
		for(Type t : primitiveArrayFixer.wrapperTypesToPreAlloc)
		{
			if(t.equals(returnType))
			{
				preAllocedReturnTypes.put(t, lastArg);
			}
			else
			{
				int lv = newPreAllocedReturnType(t);
				preAllocedReturnTypes.put(t,lv);
				super.visitTypeInsn(NEW, t.getInternalName());
				super.visitInsn(DUP);
				super.visitMethodInsn(INVOKESPECIAL, t.getInternalName(),"<init>", "()V",false);
				mv.visitVarInsn(ASTORE, lv);
//				System.out.println("Created LV Storage at " + lv);
			}
		}
	}

	HashMap<Type, Integer> preAllocedReturnTypes = new HashMap<Type, Integer>();
	PrimitiveArrayAnalyzer primitiveArrayFixer;

	public void setPrimitiveArrayAnalyzer(PrimitiveArrayAnalyzer primitiveArrayFixer) {
		this.primitiveArrayFixer = primitiveArrayFixer;

	}

	public int getPreAllocedReturnTypeVar(Type newReturnType) {
//		System.out.println(preAllocedReturnTypes);
		if(!preAllocedReturnTypes.containsKey(newReturnType))
			throw new IllegalArgumentException("Got " + newReturnType + " but have " + preAllocedReturnTypes);
		return preAllocedReturnTypes.get(newReturnType);
	}
	
	@Override
    public void visitFrame(final int type, final int nLocal,
            final Object[] local, final int nStack, final Object[] stack) {
		if(type == TaintUtils.RAW_INSN)
		{
//			System.out.println("ZZ");
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
//        System.out.println("nlocal " + nLocal);
//        System.out.println(Arrays.toString(local));
//        System.out.println(Arrays.toString(newLocals));
        // creates a copy of newLocals
        Object[] oldLocals = new Object[newLocals.length];
        System.arraycopy(newLocals, 0, oldLocals, 0, oldLocals.length);

        updateNewLocals(newLocals);
       
        for(int i = 0; i < newLocals.length; i++)
        	newLocals[i] = Opcodes.TOP;
      
        // copies types from 'local' to 'newLocals'
        // 'newLocals' currently empty

        for(Type t : preAllocedReturnTypes.keySet())
        {
//        	System.out.println(t);
        	if(t.getSort() != Type.OBJECT)
        		continue;
        	int idx = preAllocedReturnTypes.get(t);
        	if(idx >= 0)
        	{
//        		System.out.println(idx + "->" + t);
        		setFrameLocal(idx, t.getInternalName());
        	}
        }
//        System.out.println(Arrays.toString(newLocals));
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
            	if(t instanceof Integer)
            	{
            		shadowType = Opcodes.INTEGER;
            	}
            	else if(t instanceof String)
            	{
            		Type _t = Type.getType((String) t);
            		if(_t.getSort() == Type.ARRAY && _t.getDimensions() == 1 && _t.getElementType().getSort() != Type.OBJECT)
            			shadowType = "[I";
            	}
            	if(shadowType != null)
            	{

            		int newVar = remap(index, typ);
            		int shadowVar = 0;
					if (newVar > lastArg) {
						if (!varToShadowVar.containsKey(newVar))
							shadowVar = newShadowLV(typ, newVar);
						else
							shadowVar = varToShadowVar.get(newVar);
						//            		System.out.println("Adding storage for " + newVar + " at  " + shadowVar);
						setFrameLocal(shadowVar, shadowType);
					}
            	}
            }
            index += size;
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
//        System.out.println(Arrays.toString(newLocals));
        
        // restores original value of 'newLocals'
        newLocals = oldLocals;
    }
}
