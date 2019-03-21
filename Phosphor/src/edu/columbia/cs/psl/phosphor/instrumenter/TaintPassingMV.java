package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.Instrumenter;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.TaggedValue;
import edu.columbia.cs.psl.phosphor.instrumenter.asm.OffsetPreservingLabel;
import edu.columbia.cs.psl.phosphor.runtime.*;
import edu.columbia.cs.psl.phosphor.struct.*;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArrayWithObjTag;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;

import java.util.ArrayList;
import java.util.*;
import java.util.LinkedList;

@SuppressWarnings("ALL")
public class TaintPassingMV extends TaintAdapter implements Opcodes {

	public int lastArg;
	Type originalMethodReturnType;
	Type newReturnType;

	private String name;
	private boolean isStatic = true;
	public Type[] paramTypes;


	private boolean isSuperUninit;
	public void setArrayAnalyzer(PrimitiveArrayAnalyzer primitiveArrayFixer) {
		this.arrayAnalyzer = primitiveArrayFixer;
}

	public int controlTaintArray = -1;

	Label endLabel = new Label();

	Label popAllLabel = new Label();


	private int sizeOfControlTaintArray = -1;
	/**
	 * Calls "push" on controltainttagstack, possibly requiring loading the exception data
	 */
	public void callPushControlTaint(int idx) {
		super.push(idx);
		super.push(sizeOfControlTaintArray);
		if (lvs.idxOfMasterExceptionLV >= 0) {
			super.visitVarInsn(ALOAD, lvs.idxOfMasterExceptionLV);
			super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(ControlTaintTagStack.class), "push", "(" + Configuration.TAINT_TAG_DESC + "[III" + Type.getDescriptor(ExceptionalTaintData.class) + ")[I", false);

		} else
			super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(ControlTaintTagStack.class), "push", "(" + Configuration.TAINT_TAG_DESC + "[III" + ")[I", false);
		super.visitVarInsn(ASTORE, controlTaintArray);
	}

	public void callPushControlTaintObj(int idx) {
		super.push(idx);
		super.push(sizeOfControlTaintArray);
		if (lvs.idxOfMasterExceptionLV >= 0) {
			super.visitVarInsn(ALOAD, lvs.idxOfMasterExceptionLV);
			super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(ControlTaintTagStack.class), "push", "(Ljava/lang/Object;[III" + Type.getDescriptor(ExceptionalTaintData.class) + ")[I", false);

		} else
			super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(ControlTaintTagStack.class), "push", "(Ljava/lang/Object;[III" + ")[I", false);
		super.visitVarInsn(ASTORE, controlTaintArray);
	}

	public void callPopControlTaint(MethodVisitor _mv, int idx) {
		if(idx < 0)
			callPopAllControlTaint(_mv);
		else {
			if(idx > Byte.MAX_VALUE)
				_mv.visitIntInsn(SIPUSH,idx);
			else
				_mv.visitIntInsn(BIPUSH, idx);
			if (lvs.idxOfMasterExceptionLV >= 0) {
				_mv.visitVarInsn(ALOAD, lvs.idxOfMasterExceptionLV);
				_mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(ControlTaintTagStack.class), "pop", "([" + "II" + Type.getDescriptor(ExceptionalTaintData.class) + ")V", false);
			} else {
				_mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(ControlTaintTagStack.class), "pop", "([" + "II" + ")V", false);
			}
		}
	}
	public void callPopAllControlTaint(MethodVisitor _mv) {
		if (lvs.idxOfMasterExceptionLV >= 0) {
			_mv.visitVarInsn(ALOAD,lvs.idxOfMasterExceptionLV);
			_mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(ControlTaintTagStack.class), "pop", "(" + "[I" +Type.getDescriptor(ExceptionalTaintData.class)+ ")V", false);
		} else {
			_mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(ControlTaintTagStack.class), "pop", "(" + "[I" + ")V", false);
		}
	}

	int idxOfCaughtExceptionTaint;
	@Override
	public void visitCode() {
		if (this.isExcludedFromControlTrack && this.arrayAnalyzer.isEmptyMethod) {
			this.isExcludedFromControlTrack = false;
		}

//		System.out.println("TPMVStart" + name);
		super.visitCode();

		firstLabel = new Label();
		if(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_LIGHT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING)
		{
			if (lvs.idxOfMasterControlLV < 0) {
				int tmpLV = lvs.createMasterControlTaintLV();
				super.visitTypeInsn(NEW, Type.getInternalName(ControlTaintTagStack.class));
				super.visitInsn(DUP);
				if(arrayAnalyzer.nJumps > Byte.MAX_VALUE)
					super.visitIntInsn(SIPUSH, arrayAnalyzer.nJumps);
				else
					super.visitIntInsn(BIPUSH, arrayAnalyzer.nJumps);
				if (name.equals("<clinit>") || Configuration.IMPLICIT_LIGHT_TRACKING)
					super.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(ControlTaintTagStack.class), "<init>", "(I)V", false);
				super.visitVarInsn(ASTORE, tmpLV);
			}
			else{
				LocalVariableNode newLVN = new LocalVariableNode("phosphorJumpControlTag", Type.getDescriptor(ControlTaintTagStack.class), null, new LabelNode(lvs.start), new LabelNode(lvs.end), lvs.idxOfMasterControlLV);
				lvs.createdLVs.add(newLVN);
			}
			if(Configuration.IMPLICIT_EXCEPTION_FLOW && this.arrayAnalyzer.nTryCatch > 0)
			{
				this.idxOfCaughtExceptionTaint = lvs.newControlExceptionTaintLV();
				super.visitInsn(Opcodes.ACONST_NULL);
				super.visitVarInsn(ASTORE,this.idxOfCaughtExceptionTaint);
			}
			if(Configuration.IMPLICIT_EXCEPTION_FLOW && this.arrayAnalyzer.nThrow > 0){
				//Create the LV for the exception data
				int id = lvs.createExceptionTaintLV();
				super.visitTypeInsn(NEW,Type.getInternalName(ExceptionalTaintData.class));
				super.visitInsn(DUP);
				super.visitMethodInsn(INVOKESPECIAL,Type.getInternalName(ExceptionalTaintData.class), "<init>","()V",false);
				super.visitVarInsn(ASTORE,id);
			}
			if(arrayAnalyzer.nJumps + arrayAnalyzer.nTryCatch > 0){
				controlTaintArray = lvs.newControlTaintLV();
				this.sizeOfControlTaintArray = arrayAnalyzer.nJumps+arrayAnalyzer.nTryCatch+2;
				super.visitInsn(Opcodes.ACONST_NULL);
				super.visitVarInsn(Opcodes.ASTORE,controlTaintArray);
			}
			if(this.isExcludedFromControlTrack) {
				super.visitVarInsn(ALOAD, lvs.idxOfMasterControlLV);
				super.visitInsn(ICONST_1);
				super.visitFieldInsn(PUTFIELD, Type.getInternalName(ControlTaintTagStack.class), "isDisabled", "Z");
			}
		}

		if(Configuration.IMPLICIT_TRACKING && !arrayAnalyzer.hasFinally && arrayAnalyzer.nTryCatch == 0 && !isSuperUninit){
			super.visitTryCatchBlock(firstLabel,endLabel,popAllLabel,null);
			super.visitLabel(firstLabel);
		}
		Configuration.taintTagFactory.methodEntered(className, name, desc, passthruMV, lvs, this);
		//		if (arrayAnalyzer != null) {
		//			this.bbsToAddACONST_NULLto = arrayAnalyzer.getbbsToAddACONST_NULLto();
		//			this.bbsToAddChecktypeObjectto = arrayAnalyzer.getBbsToAddChecktypeObject();
		//		}
		//		if (TaintUtils.DEBUG_FRAMES)
		//			System.out.println("Need to dup " + Arrays.toString(bbsToAddACONST_NULLto) + " nulls based onthe analyzer result");
	}

	@Override
	public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
		super.visitTryCatchBlock(start, end, handler, type);
		arrayAnalyzer.nTryCatch--;
		if(Configuration.IMPLICIT_TRACKING && !arrayAnalyzer.hasFinally && arrayAnalyzer.nTryCatch == 0 && !isSuperUninit){
			super.visitTryCatchBlock(firstLabel,endLabel,popAllLabel,null);
			super.visitLabel(firstLabel);
		}
	}

	int curLabel = -1;

	@Override
	public void visitLabel(Label label) {
		if (isIgnoreAllInstrumenting) {
			super.visitLabel(label);
			return;
		}
		if(Configuration.READ_AND_SAVE_BCI && label instanceof OffsetPreservingLabel)
			Configuration.taintTagFactory.insnIndexVisited(((OffsetPreservingLabel)label).getOriginalPosition());
		//		if (curLabel >= 0 && curLabel < bbsToAddChecktypeObjectto.length && bbsToAddChecktypeObjectto[curLabel] == 1) {
		//			visitTypeInsn(CHECKCAST, "java/lang/Object");
		//			bbsToAddChecktypeObjectto[curLabel] = 0;
		//		}
		//		if (curLabel >= 0 && curLabel < bbsToAddACONST_NULLto.length && bbsToAddACONST_NULLto[curLabel] == 1) {
		//			if (analyzer.stack.size() == 0 || topStackElIsNull())
		//			{
		//				if(TaintUtils.DEBUG_FRAMES)
		//					System.out.println("Pre add extra null: " + analyzer.stack);
		//				super.visitInsn(ACONST_NULL);
		//			}
		//			bbsToAddACONST_NULLto[curLabel] = 0;
		//		}
		super.visitLabel(label);
		curLabel++;
		//		System.out.println("CurLabel" + curLabel);
	}

	
	private String className;
	private String desc;
	private MethodVisitor passthruMV;
	int idxOfPassedControlInfo = 0;
	private boolean rewriteLVDebug = false;

	private boolean isLambda;


	private HashSet<MethodNode> wrapperMethodsToAdd;

	private boolean isExcludedFromControlTrack = false;
	public TaintPassingMV(MethodVisitor mv, int access, String className, String name, String desc, String signature, String[] exceptions, String originalDesc, NeverNullArgAnalyzerAdapter analyzer,MethodVisitor passthruMV, HashSet<MethodNode> wrapperMethodsToAdd) {
		//		super(Opcodes.ASM4,mv,access,name,desc);
		super(access, className,name,desc,  signature, exceptions, mv, analyzer);
		Configuration.taintTagFactory.instrumentationStarting(access, name, desc); 
//				System.out.println("TPMV "+ className+"."+name+desc);
		this.isLambda = className.contains("$Lambda$");
		this.name = name;
		this.className = className;
		this.wrapperMethodsToAdd = wrapperMethodsToAdd;
		Type[] newArgTypes = Type.getArgumentTypes(desc);
		lastArg = 0;
		for (Type t : newArgTypes) {
			lastArg += t.getSize();
		}
		if ((access & Opcodes.ACC_STATIC) == 0) {
			lastArg++;//take account for arg[0]=this
			isStatic = false;
		}
		originalMethodReturnType = Type.getReturnType(originalDesc);
		newReturnType = Type.getReturnType(desc);
		paramTypes = new Type[lastArg + 1];
		int n = (isStatic ? 0 : 1);
		if (TaintUtils.DEBUG_LOCAL)
			System.out.println("New desc is " + Arrays.toString(newArgTypes));
		for (int i = 0; i < newArgTypes.length; i++) {
			if (TaintUtils.DEBUG_LOCAL)
				System.out.println("ARG TYPE: " + newArgTypes[i]);
			paramTypes[n] = newArgTypes[i];
			if(newArgTypes[i].getDescriptor().equals(Type.getDescriptor(ControlTaintTagStack.class)))
				idxOfPassedControlInfo = n;
			n += newArgTypes[i].getSize();
		}
		this.passthruMV = passthruMV;
		this.desc = desc;
		this.rewriteLVDebug = this.className.equals("java/lang/invoke/MethodType");
		this.isSuperUninit = this.name.equals("<init>");

		if(this.isLambda)
			this.isIgnoreAllInstrumenting = true;

		if(Configuration.IMPLICIT_TRACKING)
			this.isExcludedFromControlTrack = Instrumenter.isIgnoredFromControlTrack(className, name);
	}

	protected Type getLocalType(int n) {
		if (n >= analyzer.locals.size())
			return null;
		Object t = analyzer.locals.get(n);
		if (t == Opcodes.TOP || t == Opcodes.NULL)
			return null;
		return getTypeForStackType(t);
	}

	public void visitIincInsn(int var, int increment) {
		Configuration.taintTagFactory.iincOp(var, increment, mv, lvs, this);
		mv.visitIincInsn(var, increment);

		nextLoadisTracked = false;
	}
	
	HashMap<Integer, Object> varTypes = new HashMap<Integer, Object>();
	HashSet<Integer> questionableShadows = new HashSet<Integer>();

	HashSet<Integer> boxAtNextJump = new HashSet<Integer>();

	public int branchStarting;
	HashSet<Integer> forceCtrlAdd = new HashSet<Integer>();

	@SuppressWarnings("unused")
	@Override
	public void visitVarInsn(int opcode, int var) {
//		if(opcode < 200 && var < analyzer.locals.size())
//			System.out.println(nextLoadisTracked +" " +Printer.OPCODES[opcode] + var +" - " + analyzer.locals.get(var) + "\t"+analyzer.locals);
		if(nextLoadisTracked && opcode == Opcodes.ALOAD)
		{
			if(analyzer.locals.get(var) != Opcodes.NULL && !TaintUtils.isPrimitiveOrPrimitiveArrayType(getTypeForStackType(analyzer.locals.get(var)))){
				nextLoadisTracked = false;
			}
		}
		if (!nextLoadisTracked && opcode < 200) {
			if((Configuration.IMPLICIT_LIGHT_TRACKING || Configuration.IMPLICIT_TRACKING) && opcode == Opcodes.ASTORE)
			{
				super.visitInsn(DUP);
				super.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
				super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTagsOnObject",
						"(Ljava/lang/Object;Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)V", false);
			}
			if(opcode == Opcodes.ASTORE){
//			System.out.println(this.name + " " + Printer.OPCODES[opcode] + " on " + var + " last arg" + lastArg +", stack: " + analyzer.stack + ";"+analyzer.locals);
//			System.out.println(analyzer.stackTagStatus);
			if(topCarriesTaint())
				throw new IllegalStateException();
			}

			super.visitVarInsn(opcode, var);
			switch(opcode)
			{
			case Opcodes.ILOAD:
			case Opcodes.FLOAD:
			case Opcodes.ALOAD:
				analyzer.stackTagStatus.set(analyzer.stack.size() - 1, analyzer.stack.get(analyzer.stack.size()-1));
				break;
			case Opcodes.LLOAD:
			case Opcodes.DLOAD:
				analyzer.stackTagStatus.set(analyzer.stack.size() - 2, analyzer.stack.get(analyzer.stack.size()-2));
				break;
			}

			return;
		}
		nextLoadisTracked = false;
		if (opcode == TaintUtils.NEVER_AUTOBOX) {
			System.out.println("Never autobox: " + var);
			varsNeverToForceBox.add(var);
			return;
		}
		
		//Following 2 special cases are notes left by the post-dominator analysis for implicit taint tracking
		if(opcode == TaintUtils.BRANCH_START)
		{
			branchStarting = var;
			return;
		}
		if(opcode == TaintUtils.BRANCH_END)
		{
			if(controlTaintArray >= 0) {
				passthruMV.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
				passthruMV.visitVarInsn(ALOAD, controlTaintArray);
				callPopControlTaint(passthruMV, var);
			}
			analyzer.clearLabels();
			return;
		}
		if (opcode == TaintUtils.ALWAYS_AUTOBOX && analyzer.locals.size() > var && analyzer.locals.get(var) instanceof String) {
			Type t = Type.getObjectType((String) analyzer.locals.get(var));
			if (t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT && lvs.varToShadowVar.containsKey(var)) {
				//				System.out.println("Restoring " + var + " to be boxed");
				super.visitVarInsn(ALOAD, lvs.varToShadowVar.get(var));
				super.visitVarInsn(ASTORE, var);
			}
			return;
		}
		if (opcode == TaintUtils.ALWAYS_BOX_JUMP) {
			boxAtNextJump.add(var);
			return;
		}
		if (isIgnoreAllInstrumenting) {
			if(opcode != TaintUtils.FORCE_CTRL_STORE)
				super.visitVarInsn(opcode, var);
			return;
		}

		int shadowVar = -1;
		if (TaintUtils.DEBUG_LOCAL)
			System.out.println(this.name + " " + Printer.OPCODES[opcode] + " on " + var + " last arg" + lastArg +", stack: " + analyzer.stackTagStatus);

		if (opcode == Opcodes.ASTORE && TaintUtils.DEBUG_FRAMES) {
			System.out.println(this.name + " ASTORE " + var + ", shadowvar contains " + lvs.varToShadowVar.get(var) + " oldvartype " + varTypes.get(var));
		}
		boolean boxIt = false;

		if((Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_LIGHT_TRACKING) && !Configuration.WITHOUT_PROPOGATION)
		{
			switch(opcode)
			{
			case ISTORE:
			case FSTORE:
				super.visitInsn(SWAP);
				super.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
				if(!Configuration.MULTI_TAINTING)
					super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags", "(ILedu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)I", false);
				else
				{
					super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags", "("+Configuration.TAINT_TAG_DESC+"Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)"+Configuration.TAINT_TAG_DESC, false);
				}
				super.visitInsn(SWAP);
				break;
			case DSTORE:
			case LSTORE:
				super.visitInsn(DUP2_X1);
				super.visitInsn(POP2);
				super.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
				if(!Configuration.MULTI_TAINTING)
					super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags", "(ILedu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)I", false);
				else
				{
					super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags", "("+Configuration.TAINT_TAG_DESC+"Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)"+Configuration.TAINT_TAG_DESC, false);
				}
				super.visitInsn(DUP_X2);
				super.visitInsn(POP);
				break;
			case ASTORE:
				//if (!topOfStackIsNull()) {
					super.visitInsn(DUP);
					super.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
					super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTagsOnObject",
							"(Ljava/lang/Object;Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)V", false);
				//}
				break;
			case TaintUtils.FORCE_CTRL_STORE:
				forceCtrlAdd.add(var);
				return;
			}
		}

		if (var == 0 && !isStatic) {
			//accessing "this" so no-op, die here so we never have to worry about uninitialized this later on.
			super.visitVarInsn(opcode, var);
			return;
		} else if (var < lastArg && paramTypes[var]!= null && TaintUtils.getShadowTaintType(paramTypes[var].getDescriptor()) != null) {
			//accessing an arg; remap it
			Type localType = paramTypes[var];
			if (TaintUtils.DEBUG_LOCAL)
				System.out.println(Arrays.toString(paramTypes) + ",,," + var);
			if (TaintUtils.getShadowTaintType(localType.getDescriptor()) != null)
				shadowVar = var - 1;
		} else {
			//not accessing an arg
			
			Object oldVarType = varTypes.get(var);
			if (lvs.varToShadowVar.containsKey(var))
				shadowVar = lvs.varToShadowVar.get(var);
//						System.out.println(name+" "+Printer.OPCODES[opcode] + " "+var + " old " + oldVarType + " shadow " + shadowVar + " Last " + lastArg);
//						System.out.println(Arrays.toString(paramTypes));
			if (oldVarType != null) {
				//In this case, we already have a shadow for this. Make sure that it's the right kind of shadow though.
				if (TaintUtils.DEBUG_LOCAL)
					System.out.println(name + Textifier.OPCODES[opcode] + " " + var + " old type is " + varTypes.get(var) + " shadow is " + shadowVar);
				//First: If we thought this was NULL before, but it's not NULL now (but instead another type), then update that.
				if (opcode == ALOAD && oldVarType == Opcodes.NULL && analyzer.locals.get(var) instanceof String) {
					varTypes.put(var, analyzer.locals.get(var));
				}
				if ((oldVarType == Opcodes.NULL || oldVarType instanceof String) && opcode != ASTORE && opcode != ALOAD) {
					//Went from a TYPE to a primitive.
					if (opcode == ISTORE || opcode == FSTORE || opcode == DSTORE || opcode == LSTORE)
						varTypes.put(var, getTopOfStackObject());
					else
						varTypes.put(var, Configuration.TAINT_TAG_STACK_TYPE);
					if (shadowVar > -1) {
						while(shadowVar >= analyzer.locals.size())
						{
							analyzer.locals.add(Opcodes.TOP);
						}
						analyzer.locals.set(shadowVar, Configuration.TAINT_TAG_STACK_TYPE);
						lvs.remapLocal(shadowVar, Type.getType(Configuration.TAINT_TAG_DESC));
					}
				} else if (oldVarType instanceof Integer && oldVarType != Opcodes.NULL && (opcode == ASTORE || opcode == ALOAD)) {
					//Went from primitive to TYPE
					if (opcode == ASTORE)
						varTypes.put(var, getTopOfStackObject());
					else
						varTypes.put(var, "Lunidentified;");

					if (shadowVar > -1){
						while(shadowVar >= analyzer.locals.size())
						{
							analyzer.locals.add(Opcodes.TOP);
						}
						
//						if(shadowVar == 9)
//							System.err.println("Setting the local type for " + shadowVar + analyzer.stack);
						if (opcode == ASTORE) {
							String shadow = TaintUtils.getShadowTaintType(getTopOfStackType().getDescriptor());
							if (shadow == null) {
								shadow = Configuration.TAINT_TAG_ARRAYDESC;
								analyzer.locals.set(shadowVar, Opcodes.TOP);
							} else if (shadow.equals(Configuration.TAINT_TAG_DESC))
								analyzer.locals.set(shadowVar, Configuration.TAINT_TAG_STACK_TYPE);
							else
								analyzer.locals.set(shadowVar, shadow);
							lvs.remapLocal(shadowVar, Type.getType(shadow));
						} else {
							lvs.remapLocal(shadowVar, Type.getType(Configuration.TAINT_TAG_ARRAYDESC));
							analyzer.locals.set(shadowVar, Configuration.TAINT_TAG_ARRAYDESC);
						}
					}
				}

				if (opcode == ASTORE && !topOfStackIsNull() && !oldVarType.equals(getTopOfStackObject())) {
					varTypes.put(var, getTopOfStackObject());
				}
			}
			if (shadowVar < 0) {
				//We don't have a shadowvar for this yet. Do we need one?
				if (opcode == ALOAD) {
					if (analyzer.locals.size() > var && analyzer.locals.get(var) instanceof String) {
						Type localType = Type.getObjectType((String) analyzer.locals.get(var));
						if (TaintUtils.isPrimitiveArrayType(localType)) {
							lvs.varToShadowVar.put(var, lvs.newShadowLV(MultiDTaintedArray.getTypeForType(localType),var));
							varTypes.put(var, Opcodes.NULL);
							shadowVar = lvs.varToShadowVar.get(var);
							if (shadowVar == analyzer.locals.size())
								analyzer.locals.add(Opcodes.NULL);
						}
					}
				} else if (opcode == ASTORE) {
					if (topCarriesTaint()) {
						// That's easy.
						if(getTopOfStackObject() == Opcodes.NULL)
						{
							lvs.varToShadowVar.put(var, lvs.newShadowLV(Type.getType(Configuration.TAINT_TAG_ARRAYDESC), var));
						}
						else
							lvs.varToShadowVar.put(var, lvs.newShadowLV(MultiDTaintedArray.getTypeForType(getTopOfStackType()), var));
						varTypes.put(var, getTopOfStackObject());
						shadowVar = lvs.varToShadowVar.get(var);
					}
				} else {
					lvs.varToShadowVar.put(var, lvs.newShadowLV(Type.getType(Configuration.TAINT_TAG_DESC),var));
					varTypes.put(var, Configuration.TAINT_TAG_STACK_TYPE);
					shadowVar = lvs.varToShadowVar.get(var);
					if (opcode == ILOAD || opcode == FLOAD || opcode == DLOAD || opcode == LLOAD) {
						if (shadowVar == analyzer.locals.size())
							analyzer.locals.add(Configuration.TAINT_TAG_STACK_TYPE);
					}
				}
			}
			
			if (opcode == Opcodes.ASTORE && TaintUtils.DEBUG_FRAMES) {
				System.out.println("ASTORE " + var + ", shadowvar contains " + lvs.varToShadowVar.get(var));
			}
			if (shadowVar > -1 && TaintUtils.DEBUG_LOCAL) {
				System.out.println("using shadow " + shadowVar + "for " + var);
				System.out.println("LVS: " + analyzer.locals);
			}
		}
//		System.out.println(this.name + this.desc + Printer.OPCODES[opcode] + var+"using shadow " + shadowVar + "for " + var);
		
		if (shadowVar >= 0) {
			switch (opcode) {
			case Opcodes.ILOAD:
			case Opcodes.FLOAD:
			case Opcodes.LLOAD:
			case Opcodes.DLOAD:
				super.visitVarInsn((!Configuration.MULTI_TAINTING ? ILOAD: ALOAD), shadowVar);
				super.visitVarInsn(opcode, var);
				analyzer.setTopOfStackTagged();
				return;
			case Opcodes.ALOAD:
				if (TaintUtils.DEBUG_LOCAL)
					System.out.println("PRE ALOAD " + var);
				if (TaintUtils.DEBUG_LOCAL)
					System.out.println("Locals: " + analyzer.locals);
				Type localType = null;
				if (var >= analyzer.locals.size()) {
					System.err.println(analyzer.locals);
					System.err.println(className);
					IllegalStateException ex = new IllegalStateException("Trying to load an arg (" + var + ") past end of analyzer locals");
					throw ex;
				}
				localType = Type.getType(Configuration.TAINT_TAG_ARRAYDESC);

				if (analyzer.locals.get(var) == Opcodes.NULL) {
					if (TaintUtils.DEBUG_LOCAL)
						System.out.println("Ignoring shadow " + shadowVar + " on ALOAD " + var + " because var is null");
					super.visitInsn(ACONST_NULL);
					super.visitVarInsn(opcode, var);
					analyzer.setTopOfStackTagged();
					return;
				}
				if (analyzer.locals.get(var) instanceof Integer) {
					System.out.println(className + "." + name);
					System.out.println("ALOAD " + var + " but found " + analyzer.locals.get(var));
					System.out.println(analyzer.locals);
					throw new IllegalArgumentException();
				}
				if (analyzer.locals.get(var) instanceof Label) {
					// this var is uninitilaized obj. def not an array or
					// anythign we care about.
					super.visitVarInsn(opcode, var);
					return;
				}
				if(analyzer.locals.get(var) instanceof TaggedValue)
					localType = Type.getType((String) ((TaggedValue) analyzer.locals.get(var)).v);
				else
					localType = Type.getType((String) analyzer.locals.get(var));
				if (TaintUtils.DEBUG_LOCAL)
					System.out.println("Pre ALOAD " + var + "localtype " + localType);
				if (localType.getSort() == Type.ARRAY && localType.getDimensions() == 1) {
					switch (localType.getElementType().getSort()) {
					case Type.ARRAY:
					case Type.OBJECT:
						super.visitVarInsn(opcode, var);
						return;
					default:
						super.visitVarInsn(ALOAD, shadowVar);
						super.visitVarInsn(opcode, var);
						analyzer.setTopOfStackTagged();
						if (TaintUtils.DEBUG_LOCAL)
							System.out.println("POST ALOAD " + var);
						if (TaintUtils.DEBUG_LOCAL)
							System.out.println("Locals: " + analyzer.locals);
						return;
					}
				} else {
					System.out.println(var + ", sahdow " + shadowVar);
					super.visitVarInsn(opcode, var);
					System.out.println(analyzer.stackTagStatus);
					System.out.println(localType);
					System.out.println(analyzer.locals.get(shadowVar));
					throw new IllegalStateException("ALOAD " + var + "Shadow " + shadowVar);
//					return;
				}
			case Opcodes.ISTORE:
			case Opcodes.LSTORE:
			case Opcodes.FSTORE:
			case Opcodes.DSTORE:
				super.visitVarInsn(opcode, var);
				if(Configuration.MULTI_TAINTING)
				{
					super.visitMethodInsn(Opcodes.INVOKESTATIC, Configuration.TAINT_TAG_INTERNAL_NAME, "copyTaint", "("+Configuration.TAINT_TAG_DESC+")"+Configuration.TAINT_TAG_DESC, false);
				}
				super.visitVarInsn((!Configuration.MULTI_TAINTING ? ISTORE : ASTORE), shadowVar);
				return;
			case Opcodes.ASTORE:
				Object stackEl = analyzer.stack.get(analyzer.stack.size() - 1);
				if (stackEl == Opcodes.NULL) {
					super.visitVarInsn(ASTORE, shadowVar);
					super.visitVarInsn(opcode, var);
					if (TaintUtils.DEBUG_LOCAL)
						System.out.println("stack top was null, now POST ASTORE " + var + ": stack is " + analyzer.stack + " lvs " + analyzer.locals);
					return;
				}
				if (!(stackEl instanceof String)) {
					super.visitVarInsn(opcode, var);
					IllegalStateException ex = new IllegalStateException("Doing ASTORE but top of stack isn't a type, it's " + stackEl);
					ex.printStackTrace();
					return;
				}
				Type stackType = Type.getType((String) stackEl);

				//				if (TaintUtils.DEBUG_LOCAL)
				//					System.out.println("ASTORE " + var + ": stack is " + analyzer.stack + " StackType is " + stackType + " lvs " + analyzer.locals);
				if (stackType.getSort() == Type.ARRAY && stackType.getDimensions() == 1) {
					switch (stackType.getElementType().getSort()) {
					case Type.ARRAY:
						super.visitVarInsn(opcode, var);
						super.visitVarInsn(ASTORE, shadowVar);
						if (TaintUtils.DEBUG_LOCAL)
							System.out.println("POST ASTORE " + var + ": stack is " + analyzer.stack + " lvs " + analyzer.locals);
						return;
					case Type.OBJECT:
						super.visitVarInsn(opcode, var);
						if (TaintUtils.DEBUG_LOCAL)
							System.out.println("POST ASTORE " + var + ": stack is " + analyzer.stack + " lvs " + analyzer.locals);
						return;
					default:
						super.visitVarInsn(opcode, var);
						super.visitVarInsn(ASTORE, shadowVar);
						if (TaintUtils.DEBUG_LOCAL)
							System.out.println("POST ASTORE " + var + ": stack is " + analyzer.stack + " lvs " + analyzer.locals);
						return;
					}
				}
				//				System.out.println("Ignoring shadow, because top of stack is " + stackType);
				super.visitVarInsn(opcode, var);
				return;
			case Opcodes.RET:
				break;
			}
		} else {
			if (opcode == ASTORE && TaintUtils.isPrimitiveArrayType(getTopOfStackType())) {
				System.out.println("box astore " + var);
				registerTaintedArray();
				super.visitVarInsn(opcode, var);
			} else
				super.visitVarInsn(opcode, var);
			if (TaintUtils.DEBUG_LOCAL)
				System.out.println("(no shadow) POST " + opcode + " " + var + ": stack is " + analyzer.stack + " lvs " + analyzer.locals);
		}
	}

	public List<Field> forceCtrlStoreFields = new LinkedList<>();

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		Type descType = Type.getType(desc);
		if (opcode == TaintUtils.FORCE_CTRL_STORE) {
			forceCtrlStoreFields.add(new Field(false,owner,name,desc));
			return;
		}
		else if(opcode == TaintUtils.FORCE_CTRL_STORE_SFIELD)
		{
			forceCtrlStoreFields.add(new Field(true,owner,name,desc));
			return;
		}
		if (isIgnoreAllInstrumenting) {
			super.visitFieldInsn(opcode, owner, name, desc);
			return;
		}
		
		boolean dispatched = false;
		if (descType.getSort() == Type.ARRAY && descType.getElementType().getSort() != Type.OBJECT && descType.getDimensions() > 1) {
			desc = MultiDTaintedArray.getTypeForType(descType).getInternalName();
		}
		if(Instrumenter.isUninstrumentedField(owner,name))
		{
			String helperClass = "edu/columbia/cs/psl/phosphor/struct/multid/MultiDTaintedArrayWithObjTag";
			if(!Configuration.MULTI_TAINTING)
				helperClass = "edu/columbia/cs/psl/phosphor/struct/multid/MultiDTaintedArrayWithIntTag";
			switch(opcode)
			{
				case GETFIELD:
				case GETSTATIC:
					//need to turn into a wrapped type
					super.visitFieldInsn(opcode, owner, name, desc);
					super.visitMethodInsn(INVOKESTATIC,Type.getInternalName(MultiDTaintedArray.class),"boxIfNecessary","(Ljava/lang/Object;)Ljava/lang/Object;");
					break;
				case PUTFIELD:
				case PUTSTATIC:
					//Need to drop the taint array, if it's there
					if(topCarriesTaint())
					{
						super.visitInsn(SWAP);
						super.visitInsn(POP);
						super.visitFieldInsn(opcode,owner,name,desc);
						return;
					}
					else
					{
						//need to unbox this thing
						super.visitTypeInsn(CHECKCAST,helperClass);
						super.visitMethodInsn(INVOKEVIRTUAL,helperClass,"getVal","()Ljava/lang/Object;");
						super.visitFieldInsn(opcode, owner, name, desc);
						return;
					}
			}
		}
		if((!nextLoadisTracked && (opcode == GETSTATIC || opcode == GETFIELD)) ||
				(opcode == PUTSTATIC && !analyzer.isTopOfStackTagged() && getTopOfStackType().getSort() == Type.ARRAY))
		{
			Configuration.taintTagFactory.fieldOp(opcode, owner, name, desc, mv, lvs, this, nextLoadisTracked);
			if (opcode == PUTSTATIC && owner.equals(className) && descType.getSort() == Type.ARRAY
					&& descType.getDimensions() == 1 && descType.getElementType().getSort() != Type.OBJECT) {
				String wrap = (String)TaintUtils.getShadowTaintTypeForFrame(desc);
				super.visitTypeInsn(NEW, wrap);
				super.visitInsn(DUP_X1);
				super.visitInsn(SWAP);
				super.visitMethodInsn(INVOKESPECIAL, wrap, "<init>", "()V", false);
				super.visitFieldInsn(opcode, owner, name+TaintUtils.TAINT_FIELD, wrap);
				super.visitFieldInsn(opcode, owner, name, desc);
			}
			else
			{
				super.visitFieldInsn(opcode, owner, name, desc);
			}
			return;
		}
		boolean thisIsTracked = nextLoadisTracked;
//		System.out.println(this.name);
//		System.out.println(nextLoadisTracked);
//		System.out.println(Printer.OPCODES[opcode] + name+owner+desc + "TRACKED");
//		System.out.println(analyzer.stackTagStatus);
		nextLoadisTracked = false;

		if((Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_LIGHT_TRACKING) && !Configuration.WITHOUT_PROPOGATION)
		{
			switch(opcode)
			{
			case PUTFIELD:
				//taint the object owner of this field
				if(!isSuperUninit) {
					if (descType.getSize() == 1) {
						if(topCarriesTaint())
						{
							super.visitInsn(DUP2_X1);
							super.visitInsn(POP2);
							super.visitInsn(DUP_X2);
						}
						else {
							super.visitInsn(SWAP);
							super.visitInsn(DUP_X1);
						}
						super.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
						super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTagsOnObject", "(Ljava/lang/Object;Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)V", false);
					} else {
						//Obj Taint ValVal
						int tmp = lvs.getTmpLV(descType);
						super.visitVarInsn((descType.getSort() == Type.DOUBLE ? DSTORE : LSTORE), tmp);
						super.visitInsn(SWAP);
						super.visitInsn(DUP_X1);

						super.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
						super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTagsOnObject", "(Ljava/lang/Object;Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)V", false);

						super.visitVarInsn(descType.getSort() == Type.DOUBLE ? DLOAD: LLOAD, tmp);
						lvs.freeTmpLV(tmp);
					}

				}
			case PUTSTATIC:
				dispatched = true;
				Configuration.taintTagFactory.fieldOp(opcode, owner, name, desc, mv, lvs, this, thisIsTracked);
				if (descType.getSize() == 1) {
					if (descType.getSort() == Type.OBJECT) {
						super.visitInsn(DUP);
						super.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
						super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTagsOnObject", "(Ljava/lang/Object;Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)V", false);						
					} else if( descType.getSort() != Type.ARRAY){
						super.visitInsn(SWAP);
						super.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
						if(!Configuration.MULTI_TAINTING)
							super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags", "(ILedu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)I", false);
						else
						{
							super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags", "("+Configuration.TAINT_TAG_DESC+"Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)"+Configuration.TAINT_TAG_DESC, false);
						}
						super.visitInsn(SWAP);
					}
				} else {
					super.visitInsn(DUP2_X1);
					super.visitInsn(POP2);
					super.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
					if(!Configuration.MULTI_TAINTING)
						super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags", "(ILedu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)I", false);
					else
					{
						super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags", "("+Configuration.TAINT_TAG_DESC+"Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)"+Configuration.TAINT_TAG_DESC, false);
					}
					super.visitInsn(DUP_X2);
					super.visitInsn(POP);
				}
				break;
			case ASTORE:
				break;
			}
		}


		boolean isIgnoredTaint = Instrumenter.isIgnoredClass(owner);

		if(!dispatched)
		{
			dispatched = true;
			Configuration.taintTagFactory.fieldOp(opcode, owner, name, desc, mv, lvs, this, thisIsTracked);
		}
		switch (opcode) {
		case Opcodes.GETSTATIC:
			if (TaintUtils.isPrimitiveOrPrimitiveArrayType(descType)) {
				if (isIgnoredTaint) {
					Configuration.taintTagFactory.generateEmptyTaint(mv);
					super.visitFieldInsn(opcode, owner, name, desc);
				} else {
					super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_FIELD, TaintUtils.getShadowTaintType(descType.getDescriptor()));
					super.visitFieldInsn(opcode, owner, name, desc);
				}
				analyzer.setTopOfStackTagged();
			} else
				super.visitFieldInsn(opcode, owner, name, desc);
			break;
		case Opcodes.GETFIELD:
			String shadowType = TaintUtils.getShadowTaintType(desc);
			if (shadowType != null) {
				if (isIgnoredTaint) {
//					System.out.println("IGNORED " + owner+name+desc);
					super.visitFieldInsn(opcode, owner, name, desc);
					if(desc.startsWith("["))
						retrieveTopOfStackTaintArray();
					else
						Configuration.taintTagFactory.generateEmptyTaint(mv);
					super.visitInsn(SWAP);
				} else {
					super.visitInsn(DUP);
					super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_FIELD, shadowType);
					super.visitInsn(SWAP);
					super.visitFieldInsn(opcode, owner, name, desc);
				}
				analyzer.setTopOfStackTagged();

			} else
				super.visitFieldInsn(opcode, owner, name, desc);
			break;
		case Opcodes.PUTSTATIC:
			if (getTopOfStackObject() != Opcodes.NULL && getTopOfStackType().getSort() == Type.OBJECT 
			&& descType.getSort() == Type.ARRAY 
			&& descType.getDimensions() == 1 
			&& descType.getElementType().getSort() != Type.OBJECT)
				retrieveTaintedArray(desc);
			shadowType = TaintUtils.getShadowTaintType(desc);
			Type onStack = getTopOfStackType();
//			if (onStack.getSort() == Type.ARRAY && onStack.getElementType().getSort() != Type.OBJECT) {
//				registerTaintedArray(desc);
//				super.visitFieldInsn(opcode, owner, name, desc);
//			} else {
				super.visitFieldInsn(opcode, owner, name, desc);
				if (TaintUtils.isPrimitiveOrPrimitiveArrayType(descType)) {
					if (isIgnoredTaint)
						super.visitInsn(POP);
					else
					{
						super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_FIELD, shadowType);
					}
				}
//			}
			break;
		case Opcodes.PUTFIELD:
			//get an extra copy of the field owner
			//			System.out.println("PUTFIELD " + owner+"."+name+desc + analyzer.stack);
			if (getTopOfStackObject() != Opcodes.NULL && getTopOfStackType().getSort() == Type.OBJECT && descType.getSort() == Type.ARRAY && descType.getDimensions() == 1 && descType.getElementType().getSort() != Type.OBJECT)
				retrieveTaintedArray(desc);
			shadowType = TaintUtils.getShadowTaintType(desc);

			if (shadowType != null) {
				if (Type.getType(desc).getSize() == 2) {

					// R T VV
					super.visitInsn(DUP2_X2);
					super.visitInsn(POP2); // VV R T
					super.visitInsn(SWAP);// VV T R
					super.visitInsn(DUP_X1); // VV R T R
					super.visitInsn(SWAP);// VV R R T
					if (isIgnoredTaint)
						super.visitInsn(POP2);
					else
						super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_FIELD, shadowType);
					super.visitInsn(DUP_X2);//VV R
					super.visitInsn(POP);// R VV R
					super.visitFieldInsn(opcode, owner, name, desc);// R VV
				} else {
					//Are we storing ACONST_NULL to a primitive array field? If so, there won't be a taint!
					if (Type.getType(desc).getSort() == Type.ARRAY && Type.getType(desc).getElementType().getSort() != Type.OBJECT && analyzer.stack.get(analyzer.stack.size() - 1) == Opcodes.NULL) {
						super.visitInsn(POP2);
						super.visitInsn(DUP);
						super.visitInsn(ACONST_NULL);
						super.visitFieldInsn(opcode, owner, name, desc);
						super.visitInsn(ACONST_NULL);
						if (isIgnoredTaint)
							super.visitInsn(POP2);
						else
							super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_FIELD, shadowType);
					} else {
						super.visitInsn(DUP2_X1);
						super.visitInsn(POP2);
						super.visitInsn(DUP_X2);
						super.visitInsn(SWAP);
						super.visitFieldInsn(opcode, owner, name, desc);
						if (isIgnoredTaint)
							super.visitInsn(POP2);
						else
							super.visitFieldInsn(opcode, owner, name + TaintUtils.TAINT_FIELD, shadowType);
					}
				}
			} else {
				onStack = getTopOfStackType();
				if (shadowType == null && onStack.getSort() == Type.ARRAY && onStack.getElementType().getSort() != Type.OBJECT && onStack.getDimensions() == 1) {
					registerTaintedArray();
					super.visitFieldInsn(opcode, owner, name, desc);
				} else {
					super.visitFieldInsn(opcode, owner, name, desc);
				}
			}

			break;
		default:
			throw new IllegalArgumentException();
		}
	}

	HashSet<Integer> varsNeverToForceBox = new HashSet<Integer>();
	HashSet<Integer> varsAlwaysToForceBox = new HashSet<Integer>();


	public void doForceCtrlStores(){

		if(Configuration.WITHOUT_BRANCH_NOT_TAKEN) {
			forceCtrlStoreFields.clear();
			forceCtrlAdd.clear();
			return;
		}

		MethodVisitor ta = mv;

		for(Field f : forceCtrlStoreFields) {
			Type descType = Type.getType(f.description);
			if (!f.isStatic) {
				ta.visitVarInsn(ALOAD, 0);
				if (descType.getSort() == Type.OBJECT || descType.getSort() == Type.ARRAY) {
					if (descType.getSort() == Type.ARRAY && descType.getElementType().getSort() != Type.OBJECT && descType.getDimensions() > 1) {
						f.description = MultiDTaintedArray.getTypeForType(descType).getInternalName();
					}

					ta.visitFieldInsn(GETFIELD, f.owner, f.name, f.description);
					ta.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
					ta.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTagsOnObject", "(Ljava/lang/Object;Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)V",
							false);
				} else {
					ta.visitInsn(DUP);
					ta.visitFieldInsn(GETFIELD, f.owner, f.name + TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_DESC);
					ta.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
					ta.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags", "(" + Configuration.TAINT_TAG_DESC
							+ "Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)" + Configuration.TAINT_TAG_DESC, false);
					ta.visitFieldInsn(PUTFIELD, f.owner, f.name + TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_DESC);

				}
			} else {
				if (descType.getSort() == Type.OBJECT || descType.getSort() == Type.ARRAY) {
					if (descType.getSort() == Type.ARRAY && descType.getElementType().getSort() != Type.OBJECT && descType.getDimensions() > 1) {
						f.description = MultiDTaintedArray.getTypeForType(descType).getInternalName();
					}
					ta.visitFieldInsn(GETSTATIC, f.owner, f.name, f.description);
					ta.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
					ta.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTagsOnObject", "(Ljava/lang/Object;Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)V",
							false);
				} else {
					ta.visitFieldInsn(GETSTATIC, f.owner, f.name + TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_DESC);
					ta.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
					ta.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags", "(" + Configuration.TAINT_TAG_DESC
							+ "Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)" + Configuration.TAINT_TAG_DESC, false);
					ta.visitFieldInsn(PUTSTATIC, f.owner, f.name + TaintUtils.TAINT_FIELD, Configuration.TAINT_TAG_DESC);

				}
			}
		}
		forceCtrlStoreFields.clear();
		for (int var : forceCtrlAdd) {
			int shadowVar = -1;
			if (analyzer.locals.size() <= var || analyzer.locals.get(var) == Opcodes.TOP)
				continue;
			if (var < lastArg && TaintUtils.getShadowTaintType(paramTypes[var].getDescriptor()) != null) {
				//accessing an arg; remap it
				Type localType = paramTypes[var];
				if (localType.getSort() != Type.OBJECT && localType.getSort() != Type.ARRAY) {
					shadowVar = var - 1;
				} else if (localType.getSort() == Type.ARRAY)
					continue;
			} else {
				if (lvs.varToShadowVar.containsKey(var)) {
					shadowVar = lvs.varToShadowVar.get(var);
					if (analyzer.locals.get(var) instanceof String && ((String) analyzer.locals.get(var)).startsWith("["))
						continue;
					if (shadowVar >= analyzer.locals.size() || analyzer.locals.get(shadowVar) instanceof Integer || ((String) analyzer.locals.get(shadowVar)).startsWith("["))
						continue;
				}
			}
			if(shadowVar >= 0)
			{
				ta.visitVarInsn(ALOAD, shadowVar);
				ta.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
				ta.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags", "("+Configuration.TAINT_TAG_DESC+"Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)"+Configuration.TAINT_TAG_DESC, false);
				ta.visitVarInsn(ASTORE, shadowVar);
			}
			else
			{
				if(!(analyzer.locals.get(var) instanceof Integer)) {
					ta.visitVarInsn(ALOAD, var);
					ta.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
					ta.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTagsOnObject", "(Ljava/lang/Object;Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)V", false);
				}
			}
		}
		forceCtrlAdd.clear();
	}

	@Override
	public void visitIntInsn(int opcode, int operand) {
		if (isIgnoreAllInstrumenting) {
			super.visitIntInsn(opcode, operand);
			return;
		}

		switch (opcode) {
			case TaintUtils.IGNORE_EVERYTHING:
				isIgnoreAllInstrumenting =true;
				break;
		case Opcodes.BIPUSH:
		case Opcodes.SIPUSH:
			if(nextLoadisTracked)
			{
				if(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_LIGHT_TRACKING)
				{
					super.visitVarInsn(ALOAD, lvs.idxOfMasterControlLV);
					super.visitMethodInsn(INVOKEVIRTUAL, "edu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack", "copyTag"+(Configuration.IMPLICIT_EXCEPTION_FLOW ? "Exceptions":""), "()"+Configuration.TAINT_TAG_DESC, false);
				}
				else
					super.visitInsn(Configuration.NULL_TAINT_LOAD_OPCODE);
				nextLoadisTracked = false;
				super.visitIntInsn(opcode, operand);
				analyzer.setTopOfStackTagged();
			}
			else
				super.visitIntInsn(opcode, operand);

			break;
		case Opcodes.NEWARRAY:
			super.visitIntInsn(opcode, operand);
//			Configuration.taintTagFactory.intOp(opcode, operand, mv, lvs, this);
			if(nextLoadisTracked)
			{
				String arType = null;
				String desc = null;
				switch(operand)
				{
				case T_BOOLEAN:
					desc = "[Z";
					arType = "edu/columbia/cs/psl/phosphor/struct/LazyBooleanArray"+(Configuration.MULTI_TAINTING?"Obj":"Int")+"Tags";
					break;
				case T_INT:
					desc = "[I";
					arType = "edu/columbia/cs/psl/phosphor/struct/LazyIntArray"+(Configuration.MULTI_TAINTING?"Obj":"Int")+"Tags";
					break;
				case T_BYTE:
					desc = "[B";
					arType = "edu/columbia/cs/psl/phosphor/struct/LazyByteArray"+(Configuration.MULTI_TAINTING?"Obj":"Int")+"Tags";
					break;
				case T_CHAR:
					desc = "[C";
					arType = "edu/columbia/cs/psl/phosphor/struct/LazyCharArray"+(Configuration.MULTI_TAINTING?"Obj":"Int")+"Tags";
					break;
				case T_DOUBLE:
					desc = "[D";
					arType = "edu/columbia/cs/psl/phosphor/struct/LazyDoubleArray"+(Configuration.MULTI_TAINTING?"Obj":"Int")+"Tags";
					break;
				case T_FLOAT:
					desc = "[F";
					arType = "edu/columbia/cs/psl/phosphor/struct/LazyFloatArray"+(Configuration.MULTI_TAINTING?"Obj":"Int")+"Tags";
					break;
				case T_LONG:
					desc = "[J";
					arType = "edu/columbia/cs/psl/phosphor/struct/LazyLongArray"+(Configuration.MULTI_TAINTING?"Obj":"Int")+"Tags";
					break;
				case T_SHORT:
					desc = "[S";
					arType = "edu/columbia/cs/psl/phosphor/struct/LazyShortArray"+(Configuration.MULTI_TAINTING?"Obj":"Int")+"Tags";
					break;
					default:
						throw new IllegalArgumentException();
				}
				if(Configuration.ARRAY_LENGTH_TRACKING)
				{
					super.visitInsn(DUP_X1); // AR LT AR
					super.visitTypeInsn(NEW, arType); // AR LT AR T
					super.visitInsn(DUP_X2);// AR T LT AR T
					super.visitInsn(DUP_X2);// AR T T LT AR T
					super.visitInsn(POP);// AR T T LT AR
					super.visitMethodInsn(INVOKESPECIAL, arType, "<init>", "(" + Configuration.TAINT_TAG_DESC+desc + ")V", false);
					super.visitInsn(SWAP);
				} else {
					super.visitInsn(DUP); // AR AR
					super.visitTypeInsn(NEW, arType); // AR AR T
					super.visitInsn(DUP_X1);// AR T AR T
					super.visitInsn(SWAP);
					super.visitMethodInsn(INVOKESPECIAL, arType, "<init>", "(" + desc + ")V", false);
					super.visitInsn(SWAP);
				}
				analyzer.setTopOfStackTagged();
				nextLoadisTracked = false;
			}
			break;
		default:
			throw new IllegalArgumentException();
		}
	}

	@Override
	public void visitMultiANewArrayInsn(String desc, int dims) {
		if (isIgnoreAllInstrumenting) {
			super.visitMultiANewArrayInsn(desc, dims);
			return;
		}
		if(nextLoadisTracked)
			nextLoadisTracked = false;
		Type arrayType = Type.getType(desc);
		Type origType = arrayType;
		boolean needToHackDims = false;
		int tmp = 0;
		int tmp2 = 0;
		Type tagType = Type.getType(Configuration.TAINT_TAG_DESC);
		if (arrayType.getElementType().getSort() != Type.OBJECT) {
			if (dims == arrayType.getDimensions()) {
				needToHackDims = true;
				dims--;
				tmp = lvs.getTmpLV(Type.INT_TYPE);
				super.visitVarInsn(Opcodes.ISTORE, tmp);

				if (Configuration.ARRAY_LENGTH_TRACKING) {
					tmp2 = lvs.getTmpLV(tagType);
					super.visitVarInsn(tagType.getOpcode(Opcodes.ISTORE), tmp2);
				}
			}
			arrayType = MultiDTaintedArray.getTypeForType(arrayType);
			//Type.getType(MultiDTaintedArray.getClassForComponentType(arrayType.getElementType().getSort()));
			desc = arrayType.getInternalName();
		}
		int[] dimsLvs;
		int taintsLvs = -1;
		if(Configuration.ARRAY_LENGTH_TRACKING)
		{
			super.visitIntInsn(BIPUSH, dims);
			if(Configuration.MULTI_TAINTING)
				super.visitTypeInsn(ANEWARRAY, tagType.getInternalName());
			else
				super.visitIntInsn(NEWARRAY, Opcodes.T_INT);
			taintsLvs = lvs.getTmpLV(Type.getType(Configuration.TAINT_TAG_ARRAYDESC));
			super.visitVarInsn(ASTORE, taintsLvs);
			dimsLvs = new int[dims];
			for (int i = 0; i < dims; i++) {
				dimsLvs[i] = lvs.getTmpLV(Type.INT_TYPE);
				super.visitVarInsn(ISTORE, dimsLvs[i]);
				super.visitVarInsn(ALOAD, taintsLvs);
				super.visitInsn(SWAP);
				super.visitIntInsn(BIPUSH, i);
				super.visitInsn(SWAP);
				super.visitInsn(tagType.getOpcode(IASTORE));
			}
			for(int i = 0; i < dims; i++)
			{
				super.visitVarInsn(ILOAD, dimsLvs[dims-i-1]);
				lvs.freeTmpLV(dimsLvs[dims-i-1]);
			}
		}
		if (dims == 1) {
			//It's possible that we dropped down to a 1D object type array
			super.visitTypeInsn(ANEWARRAY, arrayType.getElementType().getInternalName());
		} else
			super.visitMultiANewArrayInsn(desc, dims);
		
		if (needToHackDims) {
			super.visitInsn(DUP);
			if (Configuration.ARRAY_LENGTH_TRACKING)
			{
				super.visitVarInsn(tagType.getOpcode(ILOAD), tmp2);
				lvs.freeTmpLV(tmp2);
			}
			super.visitVarInsn(ILOAD, tmp);
			lvs.freeTmpLV(tmp);
			super.visitIntInsn(BIPUSH, origType.getElementType().getSort());
			super.visitMethodInsn(INVOKESTATIC, Type.getInternalName((Configuration.MULTI_TAINTING ? MultiDTaintedArrayWithObjTag.class : MultiDTaintedArrayWithIntTag.class)), "initLastDim", "([Ljava/lang/Object;" + (Configuration.ARRAY_LENGTH_TRACKING ? Configuration.TAINT_TAG_DESC : "") + "II)V",
					false);

		}
		if(Configuration.ARRAY_LENGTH_TRACKING)
		{
			super.visitInsn(DUP);
			super.visitVarInsn(ALOAD, taintsLvs);
			super.visitIntInsn(BIPUSH, dims);
			super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTagsOnArrayInPlace", "([Ljava/lang/Object;[" + Configuration.TAINT_TAG_DESC + "I)V", false);
			lvs.freeTmpLV(taintsLvs);
		}
	}

	@Override
	public void visitLdcInsn(Object cst) {
		if(nextLoadisTracked)
		{
			if(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_LIGHT_TRACKING)
			{
				super.visitVarInsn(ALOAD, lvs.idxOfMasterControlLV);
				super.visitMethodInsn(INVOKEVIRTUAL, "edu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack", "copyTag" +(Configuration.IMPLICIT_EXCEPTION_FLOW ? "Exceptions":""), "()"+Configuration.TAINT_TAG_DESC, false);
			}
			else
				super.visitInsn(Configuration.NULL_TAINT_LOAD_OPCODE);
			nextLoadisTracked = false;
			super.visitLdcInsn(cst);
			analyzer.setTopOfStackTagged();
		}
		else
			super.visitLdcInsn(cst);
	}
	HashSet<String> exceptionsToMaybeThrow = new HashSet<>();
	@Override
	public void visitTypeInsn(int opcode, String type) {
		if (isIgnoreAllInstrumenting) {
			super.visitTypeInsn(opcode, type);
			return;
		}
		switch (opcode) {
			case TaintUtils.EXCEPTION_HANDLER_START:
				if(type == null) {
					//Special sentinel to say that this is the start of the exception handler and that a list of covered types will follow
					super.visitInsn(DUP);
					super.visitVarInsn(ALOAD, lvs.idxOfMasterControlLV);
					super.visitInsn(SWAP);
					super.visitVarInsn(ALOAD, this.idxOfCaughtExceptionTaint);
					super.visitMethodInsn(INVOKEVIRTUAL, "edu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack", "exceptionHandlerStart", "(Ljava/lang/Throwable;" + Type.getDescriptor(EnqueuedTaint.class) + ")" + Type.getDescriptor(EnqueuedTaint.class),false);
					super.visitVarInsn(ASTORE,this.idxOfCaughtExceptionTaint);

					doForceCtrlStores();
					return;
				}
				super.visitVarInsn(ALOAD, lvs.idxOfMasterControlLV);
				super.visitLdcInsn(Type.getObjectType(type));
				super.visitMethodInsn(INVOKEVIRTUAL, "edu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack", "exceptionHandlerStart", "(Ljava/lang/Class;)V", false);

				break;
			case TaintUtils.EXCEPTION_HANDLER_END:
				if(type == null) {
					//end of handler
					super.visitVarInsn(ALOAD, lvs.idxOfMasterControlLV);
					super.visitVarInsn(ALOAD, this.idxOfCaughtExceptionTaint);
					super.visitMethodInsn(INVOKEVIRTUAL, "edu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack", "exceptionHandlerEnd", "(" + Type.getDescriptor(EnqueuedTaint.class) + ")V", false);
				}
				else
				{
					//end of try block
					doForceCtrlStores();
					super.visitVarInsn(ALOAD, lvs.idxOfMasterControlLV);
					super.visitLdcInsn(Type.getObjectType(type));
					super.visitMethodInsn(INVOKEVIRTUAL, "edu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack", "tryBlockEnd", "(Ljava/lang/Class;)V", false);

				}

				break;
			case TaintUtils.UNTHROWN_EXCEPTION:
				//We want to note that we are returning and might instead have thrown an exception
				super.visitVarInsn(ALOAD,lvs.idxOfMasterControlLV);
				super.visitVarInsn(ALOAD,lvs.idxOfMasterExceptionLV);
				super.visitLdcInsn(Type.getObjectType(type));
				super.visitMethodInsn(INVOKEVIRTUAL, "edu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack","addUnthrownException","("+Type.getDescriptor(ExceptionalTaintData.class)+"Ljava/lang/Class;)V",false);
				break;
			case TaintUtils.UNTHROWN_EXCEPTION_CHECK:
				super.visitVarInsn(ALOAD,lvs.idxOfMasterControlLV);
				super.visitLdcInsn(Type.getObjectType(type));
				super.visitMethodInsn(INVOKEVIRTUAL, "edu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack","applyPossiblyUnthrownExceptionToTaint","(Ljava/lang/Class;)V",false);
				break;
		case Opcodes.ANEWARRAY:
			if (Configuration.ARRAY_LENGTH_TRACKING && !Configuration.WITHOUT_PROPOGATION) {
				Type t = Type.getObjectType(type);
				if (t.getSort() == Type.ARRAY && t.getElementType().getDescriptor().length() == 1) {
					//e.g. [I for a 2 D array -> MultiDTaintedIntArray
					type = MultiDTaintedArray.getTypeForType(t).getInternalName();
				}
				//L TL
				super.visitTypeInsn(opcode, type);
				//A TL
				super.visitInsn(DUP_X1);
				//A TL A
				super.visitInsn(SWAP);
				//TL A A
				super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTagsInPlace", "(Ljava/lang/Object;" + Configuration.TAINT_TAG_DESC + ")V", false);
			} else {
				Type t = Type.getObjectType(type);
				if (t.getSort() == Type.ARRAY && t.getElementType().getDescriptor().length() == 1) {
					//e.g. [I for a 2 D array -> MultiDTaintedIntArray
					type = MultiDTaintedArray.getTypeForType(t).getInternalName();
				}
				super.visitTypeInsn(opcode, type);
				
			}
			nextLoadisTracked = false;
			break;
		case Opcodes.NEW:
			super.visitTypeInsn(opcode, type);			
			break;
		case Opcodes.CHECKCAST:
			Type t = Type.getObjectType(type);
			if (nextLoadisTracked) {
				checkCast(type);
				nextLoadisTracked = false;
				analyzer.setTopOfStackTagged();
			} else if (TaintUtils.isPrimitiveArrayType(t)) {
				if (getTopOfStackType().getSort() == Type.ARRAY) {
					// ...
					super.visitTypeInsn(opcode, type);
					return;
				} else {
					retrieveTaintedArrayWithoutTags(type);
					super.visitTypeInsn(opcode, type);
					return;
				}
			}
			else
				checkCast(type);
			break;
		case Opcodes.INSTANCEOF:
			if(nextLoadisTracked)
			{
				if (Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_LIGHT_TRACKING) {
					super.visitInsn(DUP);
					super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintUtils.class), "getTaintObj", "(Ljava/lang/Object;)"+Configuration.TAINT_TAG_DESC, false);
					super.visitInsn(SWAP);
					instanceOf(type);

				} else {
					instanceOf(type);
					super.visitInsn(Configuration.NULL_TAINT_LOAD_OPCODE);
					super.visitInsn(SWAP);
				}
				nextLoadisTracked = false;
				analyzer.setTopOfStackTagged();
			}
			else
			{
				instanceOf(type);
			}
			break;
		default:
			throw new IllegalArgumentException();
		}
	}
	
	private void instanceOf(String type){
		Type t = Type.getObjectType(type);

		boolean doIOR = false;
		if (t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT) {
			if (t.getDimensions() > 1) {
				type = MultiDTaintedArray.getTypeForType(t).getDescriptor();
			} else if (!topCarriesTaint()) {
				doIOR = true;
				//Maybe we have it boxed on the stack, maybe we don't - how do we know? Who cares, just check both...
				super.visitInsn(DUP);
				super.visitTypeInsn(INSTANCEOF, Type.getInternalName(MultiDTaintedArray.getClassForComponentType(t.getElementType().getSort())));
				super.visitInsn(SWAP);
			}
		}
		super.visitTypeInsn(INSTANCEOF, type);
		if (doIOR) {
			super.visitInsn(IOR);
		}
	}

	private void checkCast(String type){
		Type t = Type.getObjectType(type);
		int opcode = CHECKCAST;
		if (t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT) {
			if (t.getDimensions() > 1) {
				//Hahaha you thought you could cast to a primitive multi dimensional array!

				super.visitTypeInsn(opcode, MultiDTaintedArray.getTypeForType(Type.getType(type)).getDescriptor());
				return;
			} else {
				//what is on the top of the stack that we are checkcast'ing?
				Object o = analyzer.stack.get(analyzer.stack.size() - 1);
				if (o instanceof String) {
					Type zz = TaintAdapter.getTypeForStackType(o);
					if (zz.getSort() == Type.ARRAY && zz.getElementType().getSort() != Type.OBJECT) {
						super.visitTypeInsn(opcode, type);
						return;
					}
				}
				//cast of Object[] or Object to char[] or int[] etc.
				if (o == Opcodes.NULL) {
					super.visitInsn(SWAP);
					super.visitTypeInsn(CHECKCAST, MultiDTaintedArray.getTypeForType(t).getInternalName());
					super.visitInsn(SWAP);
				} else
				{
					Type wrap = MultiDTaintedArray.getTypeForType(t);
					super.visitTypeInsn(CHECKCAST, wrap.getInternalName());
					retrieveTaintedArray(type);
				}
				super.visitTypeInsn(opcode, type);
			}
		} else {

			//What if we are casting an array to Object?
			if (topStackElCarriesTaints()) {
				//Casting array to non-array type

				//Register the taint array for later.
				registerTaintedArray();
			}
			super.visitTypeInsn(opcode, type);
		}
	}
	/**
	 * Pre: A Post: TA A
	 * 
	 * @param type
	 */
	public void retrieveTaintedArray(String type) {
		//A
		Label isNull = new Label();
		Label isDone = new Label();

		Type toCastTo = MultiDTaintedArray.getTypeForType(Type.getType(type));

		super.visitInsn(DUP);
		if(!isIgnoreAllInstrumenting)
		super.visitInsn(TaintUtils.IGNORE_EVERYTHING);
		super.visitJumpInsn(IFNULL, isNull);
		if(!isIgnoreAllInstrumenting)
		super.visitInsn(TaintUtils.IGNORE_EVERYTHING);

//				System.out.println("unbox: " + getTopOfStackType() + " type passed is " + type);

		super.visitTypeInsn(CHECKCAST, toCastTo.getInternalName());
		FrameNode fn = getCurrentFrameNode();

		super.visitInsn(DUP);

		Type arrayDesc = Type.getType(type);
		//		System.out.println("Get tainted array from " + arrayDesc);
		//A A
		super.visitFieldInsn(GETFIELD, toCastTo.getInternalName(), "val", type);
		FrameNode fn2 = getCurrentFrameNode();
		super.visitJumpInsn(GOTO, isDone);
		super.visitLabel(isNull);
		acceptFn(fn);
		super.visitTypeInsn(CHECKCAST, toCastTo.getInternalName());

		super.visitInsn(ACONST_NULL);
//		if (arrayDesc.getElementType().getSort() == Type.OBJECT)
//			super.visitTypeInsn(CHECKCAST, "[Ljava/lang/Object;");
//		else
//			super.visitTypeInsn(CHECKCAST, Type.getType(TaintUtils.getShadowTaintType(arrayDesc.getDescriptor())).getInternalName());
		super.visitTypeInsn(CHECKCAST, type);
		super.visitLabel(isDone);
		acceptFn(fn2);
	}
	
	public void retrieveTaintedArrayWithoutTags(String type) {
		//A
		Label isNull = new Label();
		Label isDone = new Label();

		FrameNode fn = getCurrentFrameNode();

		super.visitInsn(DUP);
		if(!isIgnoreAllInstrumenting)
		super.visitInsn(TaintUtils.IGNORE_EVERYTHING);
		super.visitJumpInsn(IFNULL, isNull);
		if(!isIgnoreAllInstrumenting)
		super.visitInsn(TaintUtils.IGNORE_EVERYTHING);

		//		System.out.println("unbox: " + onStack + " type passed is " + type);

		Class boxType = MultiDTaintedArray.getClassForComponentType(Type.getType(type).getElementType().getSort());
		super.visitTypeInsn(CHECKCAST, Type.getInternalName(boxType));


		Type arrayDesc = Type.getType(type);
		//		System.out.println("Get tainted array from " + arrayDesc);
		super.visitFieldInsn(GETFIELD, Type.getInternalName(boxType), "val", type);
		FrameNode fn2 = getCurrentFrameNode();
		super.visitJumpInsn(GOTO, isDone);
		super.visitLabel(isNull);
		acceptFn(fn);
		super.visitTypeInsn(CHECKCAST, type);
		super.visitLabel(isDone);
		acceptFn(fn2);
	}

	/**
	 * 
	 * @param descOfDest
	 */
	public void registerTaintedArray() {
		super.visitInsn(SWAP);
		Label isnull = new Label();
		Label ok = new Label();
		FrameNode fn2 = getCurrentFrameNode();

		super.visitInsn(DUP);
		super.visitJumpInsn(IFNULL, isnull);
		super.visitInsn(DUP_X1);
		super.visitInsn(SWAP);
		Type onTop = getTopOfStackType();
		String wrapper = (String) TaintUtils.getShadowTaintTypeForFrame(onTop.getDescriptor());
		super.visitMethodInsn(INVOKEVIRTUAL, wrapper, "ensureVal", "("+onTop.getDescriptor()+")V", false);
		FrameNode fn = getCurrentFrameNode();
		super.visitJumpInsn(GOTO, ok);
		super.visitLabel(isnull);
		acceptFn(fn2);
		super.visitInsn(SWAP);
		super.visitInsn(POP);
		super.visitLabel(ok);
		acceptFn(fn);
		//A
	}

	/**
	 * Will insert a NULL after the nth element from the top of the stack
	 * 
	 * @param n
	 */
	void insertNullAt(int n) {
		switch (n) {
		case 0:
			super.visitInsn(ACONST_NULL);
			break;
		case 1:
			super.visitInsn(ACONST_NULL);
			super.visitInsn(SWAP);
			break;
		case 2:
			super.visitInsn(ACONST_NULL);
			super.visitInsn(DUP_X2);
			super.visitInsn(POP);
			break;
		default:
			LocalVariableNode[] d = storeToLocals(n);
			super.visitInsn(ACONST_NULL);
			for (int i = n - 1; i >= 0; i--) {
				loadLV(i, d);
			}
			freeLVs(d);

		}
	}

	/**
	 * Pop at n means pop the nth element down from the top (pop the top is n=0)
	 * 
	 * @param n
	 */
	void popAt(int n) {
		if (TaintUtils.DEBUG_DUPSWAP)
			System.out.println(name + " POP AT " + n + " from " + analyzer.stack);
		switch (n) {
		case 0:
			Object top = analyzer.stack.get(analyzer.stack.size() - 1);
			if (top == Opcodes.LONG || top == Opcodes.DOUBLE || top == Opcodes.TOP)
				super.visitInsn(POP2);
			else
				super.visitInsn(POP);
			break;
		case 1:
			top = analyzer.stack.get(analyzer.stack.size() - 1);
			if (top == Opcodes.LONG || top == Opcodes.DOUBLE || top == Opcodes.TOP) {
				Object second = analyzer.stack.get(analyzer.stack.size() - 3);
				//				System.out.println("Second is " + second);
				if (second == Opcodes.LONG || second == Opcodes.DOUBLE || second == Opcodes.TOP) {
					//VV VV
					super.visitInsn(DUP2_X2);
					super.visitInsn(POP2);
					super.visitInsn(POP2);
				} else {
					//V VV
					super.visitInsn(DUP2_X1);
					super.visitInsn(POP2);
					super.visitInsn(POP);
				}
			} else {
				Object second = analyzer.stack.get(analyzer.stack.size() - 2);
				if (second == Opcodes.LONG || second == Opcodes.DOUBLE || second == Opcodes.TOP) {
					//VV V
					super.visitInsn(DUP_X2);
					super.visitInsn(POP);
					super.visitInsn(POP2);
				} else {
					//V V
					super.visitInsn(SWAP);
					super.visitInsn(POP);
				}
			}
			break;
		case 2:

		default:
			LocalVariableNode[] d = storeToLocals(n);

			//			System.out.println("POST load the top " + n + ":" + analyzer.stack);

			super.visitInsn(POP);
			for (int i = n - 1; i >= 0; i--) {
				loadLV(i, d);
			}
			freeLVs(d);

		}
		if (TaintUtils.DEBUG_CALLS)
			System.out.println("POST POP AT " + n + ":" + analyzer.stack);

	}

	/**
	 * Store at n means pop the nth element down from the top and store it to
	 * our arraystore (pop the top is n=0)
	 * 
	 * @param n
	 */
	void storeTaintArrayAt(int n, String descAtDest) {
		if (TaintUtils.DEBUG_DUPSWAP)
			System.out.println(name + " POP AT " + n + " from " + analyzer.stack);
		switch (n) {
		case 0:
			throw new IllegalStateException("Not supposed to ever pop the top like this");
			//			break;
		case 1:
			Object top = analyzer.stack.get(analyzer.stack.size() - 1);
			if (top == Opcodes.LONG || top == Opcodes.DOUBLE || top == Opcodes.TOP) {
				throw new IllegalStateException("Not supposed to ever pop the top like this");
			} else {
				Object second = analyzer.stack.get(analyzer.stack.size() - 2);
				if (second == Opcodes.LONG || second == Opcodes.DOUBLE || second == Opcodes.TOP) {
					throw new IllegalStateException("Not supposed to ever pop the top like this");
				} else {
					//V V
					registerTaintedArray();
				}
			}
			break;
		case 2:

		default:
			LocalVariableNode[] d = storeToLocals(n - 1);

			//						System.out.println("POST load the top " + n + ":" + analyzer.stack);
			registerTaintedArray();
			for (int i = n - 2; i >= 0; i--) {
				loadLV(i, d);
			}
			freeLVs(d);

		}
		if (TaintUtils.DEBUG_CALLS)
			System.out.println("POST POP AT " + n + ":" + analyzer.stack);

	}

	void unboxTaintArrayAt(int n, String descAtDest) {
		if (TaintUtils.DEBUG_DUPSWAP)
			System.out.println(name + " unbox AT " + n + " from " + analyzer.stack);
		switch (n) {
		case 0:
			retrieveTaintedArray(descAtDest);
		case 1:
			Object top = analyzer.stack.get(analyzer.stack.size() - 1);
			if (top == Opcodes.LONG || top == Opcodes.DOUBLE || top == Opcodes.TOP) {
				throw new IllegalStateException("Not supposed to ever pop the top like this");
			} else {
				Object second = analyzer.stack.get(analyzer.stack.size() - 2);
				if (second == Opcodes.LONG || second == Opcodes.DOUBLE || second == Opcodes.TOP) {
					throw new IllegalStateException("Not supposed to ever pop the top like this");
				} else {
					//V
					retrieveTaintedArray(descAtDest);
				}
			}
			break;
		case 2:

		default:
			LocalVariableNode[] d = storeToLocals(n - 1);

			//						System.out.println("POST load the top " + n + ":" + analyzer.stack);
			retrieveTaintedArray(descAtDest);
			for (int i = n - 2; i >= 0; i--) {
				loadLV(i, d);
			}
			freeLVs(d);

		}
		if (TaintUtils.DEBUG_CALLS)
			System.out.println("POST POP AT " + n + ":" + analyzer.stack);

	}
	
	@Override
	public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
        String owner = bsm.getOwner();
        boolean hasNewName = !TaintUtils.remapMethodDesc(desc).equals(desc);
        String newDesc = TaintUtils.remapMethodDesc(desc);
        boolean isPreAllocedReturnType = TaintUtils.isPreAllocReturnType(desc);
        boolean isIgnoredForTaints = Configuration.WITH_SELECTIVE_INST && Instrumenter.isIgnoredMethodFromOurAnalysis(owner, name, desc);
        int opcode = INVOKEVIRTUAL;
		if (bsm.getTag() == Opcodes.H_INVOKESTATIC)
			opcode = INVOKESTATIC;

//		if(bsm != null && bsm.getOwner().equals("java/lang/invoke/LambdaMetafactory"))
//		{
//			bsm = new Handle(bsm.getTag(),"edu/columbia/cs/psl/phosphor/runtime/LambdaMetafactory",bsm.getName(),bsm.getDesc(),bsm.isInterface());
//		}

		if (Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING) {
			hasNewName = true;
			newDesc = TaintUtils.remapMethodDescAndIncludeReturnHolderNoControlStack(desc);
		}
        if (name.equals("<init>") && !newDesc.equals(desc)) {
            //Add the taint sentinel to the desc
            super.visitInsn(ACONST_NULL);
            newDesc = newDesc.substring(0, newDesc.indexOf(")")) + Type.getDescriptor(TaintSentinel.class) + ")" + Type.getReturnType(newDesc).getDescriptor();
        }
        if (isPreAllocedReturnType) {
            Type t = Type.getReturnType(newDesc);
            newDesc = newDesc.substring(0, newDesc.indexOf(")")) + t.getDescriptor() + ")" + t.getDescriptor();
            super.visitVarInsn(ALOAD, lvs.getPreAllocedReturnTypeVar(t));
        }
        Type origReturnType = Type.getReturnType(desc);
        Type returnType = TaintUtils.getContainerReturnType(Type.getReturnType(desc));
        if (TaintUtils.DEBUG_CALLS)
            System.out.println("Remapped call from " + owner + "." + name + desc + " to " + owner + "." + name + newDesc);
        if (!name.contains("<") && hasNewName)// && !isMetaLambda)
            name += TaintUtils.METHOD_SUFFIX;
        if (TaintUtils.DEBUG_CALLS) {
            System.out.println("Calling w/ stack: " + analyzer.stack);
        }

        //if you call a method and instead of passing a primitive array you pass ACONST_NULL, we need to insert another ACONST_NULL in the stack
        //for the taint for that array
        Type[] args = Type.getArgumentTypes(newDesc);
        Type[] argsInReverse = new Type[args.length];
        int argsSize = 0;
        for (int i = 0; i < args.length; i++) {
            argsInReverse[args.length - i - 1] = args[i];
            argsSize += args[i].getSize();
        }
        int i = 1;
        int n = 1;
        boolean ignoreNext = false;

//                                      System.out.println("1z2zdw23 Calling "+owner+"."+name+newDesc + "with " + analyzer.stack);
        for (Type t : argsInReverse) {
            if (analyzer.stack.get(analyzer.stack.size() - i) == Opcodes.TOP)
                i++;

            //          if(ignoreNext)
            //              System.out.println("ignore next i");
            Type onStack = getTypeForStackType(analyzer.stack.get(analyzer.stack.size() - i));
//                      System.out.println(name+ " ONStack: " + onStack + " and t = " + t);

            if (!ignoreNext && t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT) {
//                              System.out.println("t " + t + " and " + analyzer.stack.get(analyzer.stack.size() - i) + " j23oij4oi23");

                //Need to check to see if there's a null on the stack in this position
                if (analyzer.stack.get(analyzer.stack.size() - i) == Opcodes.NULL) {
                    if (TaintUtils.DEBUG_CALLS)
                        System.err.println("Adding a null in call at " + n);
                    insertNullAt(n);
                } else if (onStack.getSort() == Type.OBJECT && (!isIgnoredForTaints || t.getDimensions() == 1)) {
                    //Unbox this
                    unboxTaintArrayAt(n, t.getDescriptor());
                }
            } else if (!ignoreNext && onStack.getSort() == Type.ARRAY && onStack.getElementType().getSort() != Type.OBJECT) {
                //There is an extra taint on the stack at this position
                if (TaintUtils.DEBUG_CALLS)
                    System.err.println("removing taint array in call at " + n);
                storeTaintArrayAt(n, onStack.getDescriptor());
            }
			if ((t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT) || (t.getDescriptor().startsWith("Ledu/columbia/cs/psl/phosphor/struct/Lazy")))
                ignoreNext = !ignoreNext;
            n++;
            i++;
        }

        //      System.out.println("Args size: " + argsSize + " nargs " + args.length);
//        if (TaintUtils.DEBUG_CALLS)
//            System.out.println("No more changes: calling " + owner + "." + name + newDesc + " w/ stack: " + analyzer.stack);

        boolean isCalledOnAPrimitiveArrayType = false;
        if (opcode == INVOKEVIRTUAL) {
            if (analyzer.stack.get(analyzer.stack.size() - argsSize - 1) == null)
                System.out.println("NULL on stack for calllee???" + analyzer.stack + " argsize " + argsSize);
            Type callee = getTypeForStackType(analyzer.stack.get(analyzer.stack.size() - argsSize - 1));
            if (TaintUtils.DEBUG_CALLS)
                System.out.println("CALLEE IS " + callee);
            if (callee.getSort() == Type.ARRAY && callee.getElementType().getSort() != Type.OBJECT)
                isCalledOnAPrimitiveArrayType = true;
        }

//        super.visitMethodInsn(opcode, owner, name, newDesc, itfc);
        if(bsmArgs != null)
        {
        	boolean instOK = false;

        	//Are we remapping something with a primitive return that gets ignored?
	        if(bsm.getName().equals("metafactory"))
	        {
	            Handle implMethod = (Handle) bsmArgs[1];
		        if (!Instrumenter.isIgnoredClass(implMethod.getOwner()) && !Instrumenter.isIgnoredMethod(implMethod.getOwner(), implMethod.getName(), implMethod.getDesc()) && !TaintUtils.remapMethodDescAndIncludeReturnHolder(implMethod.getDesc()).equals(implMethod.getDesc())) {
			        Type uninstSamMethodType = (Type) bsmArgs[0];

			        bsmArgs[0] = Type.getMethodType(TaintUtils.remapMethodDescAndIncludeReturnHolder(((Type) bsmArgs[0]).getDescriptor()));
			        //make sure that we can directly make this call without needing to add additional parameters
			        String implMethodDesc = implMethod.getDesc();

			        Type samMethodType = (Type) bsmArgs[0];

			        String remappedImplDesc = TaintUtils.remapMethodDescAndIncludeReturnHolder(implMethodDesc);

			        Type instantiatedType = (Type) bsmArgs[2];
			        Type[] instantiatedMethodArgs = instantiatedType.getArgumentTypes();
			        Type[] samMethodArgs = samMethodType.getArgumentTypes();


			        String remappedInstantiatedDesc = null;
			        if (implMethod.getName().equals("<init>"))
				        remappedInstantiatedDesc = TaintUtils.remapMethodDescAndIncludeReturnHolderInit(instantiatedType.getDescriptor());
			        else
				        remappedInstantiatedDesc = TaintUtils.remapMethodDescAndIncludeReturnHolder(instantiatedType.getDescriptor());
			        boolean needToAddUnWrapper = samMethodType.getArgumentTypes().length != Type.getMethodType(remappedInstantiatedDesc).getArgumentTypes().length;

			        boolean isStatic = (implMethod.getTag() == Opcodes.H_INVOKESTATIC);
			        boolean isNEW = (implMethod.getTag() == Opcodes.H_NEWINVOKESPECIAL);
			        boolean isVirtual = (implMethod.getTag() == Opcodes.H_INVOKEVIRTUAL) || implMethod.getTag() == Opcodes.H_INVOKESPECIAL || implMethod.getTag() == Opcodes.H_INVOKEINTERFACE;
			        int nOrigArgs = Type.getArgumentTypes(desc).length;
			        if(isVirtual)
			        	nOrigArgs--;

			        //OR if there are primitives in the impl but not in the sam!
			        int nPrimsImpl = 0;
			        int nPrimsSAM = 0;
			        for (Type t : uninstSamMethodType.getArgumentTypes())
				        if (TaintUtils.isPrimitiveType(t))
					        nPrimsSAM++;

			        int nArgsSAM = nOrigArgs;
			        for (Type t : Type.getArgumentTypes(implMethodDesc)) {
				        nArgsSAM--;
			        	if(nArgsSAM >= 0)
			        		continue;
				        if (TaintUtils.isPrimitiveType(t))
					        nPrimsImpl++;
			        }
			        if (nPrimsImpl != nPrimsSAM) {
				        needToAddUnWrapper = true;
			        }

			        //OR If there is a wrapped return type on the impl but not in the sam
			        if(TaintUtils.isPrimitiveType(Type.getReturnType(implMethodDesc)) && !TaintUtils.isPrimitiveType(uninstSamMethodType.getReturnType()))
			        {
			        	needToAddUnWrapper = true;
			        }

			        if (needToAddUnWrapper) {

				        ArrayList<Type> newWrapperArgs = new ArrayList<>();
				        Type wrapperDesc = Type.getMethodType(implMethod.getDesc());
				        Type[] implMethodArgs = Type.getArgumentTypes(implMethod.getDesc());
				        int extraArgs = Type.getArgumentTypes(implMethodDesc).length - instantiatedType.getArgumentTypes().length;

				        if(isVirtual)
				        	newWrapperArgs.add(Type.getObjectType(implMethod.getOwner()));

				        for (int j = 0; j < implMethodArgs.length; j++) {
					        if (j < extraArgs) {
						        //Add as-is
						        newWrapperArgs.add(implMethodArgs[j]);
					        } else if (TaintUtils.isPrimitiveOrPrimitiveArrayType(implMethodArgs[j]) &&
							        samMethodArgs[j - extraArgs].getDescriptor().equals("Ljava/lang/Object;")) {
						        newWrapperArgs.add(samMethodArgs[j - extraArgs]);
					        } else {
						        newWrapperArgs.add(implMethodArgs[j]);
					        }
				        }

				        boolean boxPrimitiveReturn = false;
				        boolean unboxPrimitiveReturn = false;
				        //Are we returning a primitive that needs to be autoboxed?
				        Type newReturnType = TaintUtils.getContainerReturnType(wrapperDesc.getReturnType());
				        Type originalReturnType = wrapperDesc.getReturnType();
				        boolean addReturnWrapper = false;
				        if (TaintUtils.isPrimitiveType(wrapperDesc.getReturnType())) {
					        if (TaintUtils.isPrimitiveType(uninstSamMethodType.getReturnType())) {
					        	addReturnWrapper = true;
					        	newWrapperArgs.add(newReturnType);
					        }
					        else{
						        boxPrimitiveReturn = true;
						        switch (wrapperDesc.getReturnType().getSort()) {
							        case Type.CHAR:
								        newReturnType = Type.getType("Ljava/lang/Character;");
								        break;
							        case Type.BOOLEAN:
								        newReturnType = Type.getType("Ljava/lang/Boolean;");
								        break;
							        case Type.DOUBLE:
								        newReturnType = Type.getType("Ljava/lang/Double;");
								        break;
							        case Type.FLOAT:
								        newReturnType = Type.getType("Ljava/lang/Float;");
								        break;
							        case Type.LONG:
								        newReturnType = Type.getType("Ljava/lang/Long;");
								        break;
							        case Type.INT:
								        newReturnType = Type.getType("Ljava/lang/Integer;");
								        break;
							        case Type.SHORT:
								        newReturnType = Type.getType("Ljava/lang/Short;");
								        break;
							        case Type.BYTE:
								        newReturnType = Type.getType("Ljava/lang/Byte;");
						        }
					        }
				        } else if (isNEW) {
				        	newReturnType = Type.getObjectType(implMethod.getOwner());
				        } else if(TaintUtils.isPrimitiveType(uninstSamMethodType.getReturnType()) && !TaintUtils.isPrimitiveType(wrapperDesc.getReturnType())){
				        	//IMPL method returns boxed type, SAM returns primitive
//					        newWrapperArgs.add(TaintUtils.getContainerReturnType(uninstSamMethodType.getReturnType()));
					        newReturnType = uninstSamMethodType.getReturnType();
					        unboxPrimitiveReturn = true;
				        }
				        wrapperDesc = Type.getMethodType(newReturnType, newWrapperArgs.toArray(new Type[newWrapperArgs.size()]));

				        String wrapperName = "phosphorWrapInvokeDymnamic" + wrapperMethodsToAdd.size();

				        MethodNode mn = new MethodNode(Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, wrapperName, wrapperDesc.getDescriptor(), null, null);

				        GeneratorAdapter ga = new GeneratorAdapter(mn, Opcodes.ACC_STATIC, wrapperName, wrapperDesc.getDescriptor());
				        ga.visitCode();

				        if(isNEW){
				        	ga.visitTypeInsn(Opcodes.NEW, implMethod.getOwner());
				        	ga.visitInsn(DUP);
				        }
				        int offset = 0;
				        if (isVirtual) {
					        ga.visitVarInsn(ALOAD, 0);
					        offset = 1;
					        newWrapperArgs.remove(0);
				        }
				        if(addReturnWrapper)
				        {
				        	newWrapperArgs.remove(newWrapperArgs.size()-1);
				        }
				        for (int j = 0; j < newWrapperArgs.size(); j++) {
					        ga.visitVarInsn(newWrapperArgs.get(j).getOpcode(Opcodes.ILOAD), offset);
					        offset += newWrapperArgs.get(j).getSize();
					        if(!implMethodArgs[j].getDescriptor().equals(newWrapperArgs.get(j).getDescriptor())) {
					        	if(implMethodArgs[j].getSort() == Type.ARRAY)
						            ga.visitTypeInsn(Opcodes.CHECKCAST, implMethodArgs[j].getInternalName());
					        	else
						        {
						        	//Cast to box type, then unbox
							        ga.unbox(implMethodArgs[j]);
						        }
					        }
				        }
				        int opToCall = INVOKESTATIC;
				        boolean isInterface = false;
				        switch(implMethod.getTag()){
					        case H_INVOKESTATIC:
					        	opToCall = INVOKESTATIC;
					        	break;
					        case H_INVOKEVIRTUAL:
					        	opToCall = INVOKEVIRTUAL;
					        	break;
					        case H_INVOKESPECIAL:
					        case H_NEWINVOKESPECIAL:
					        	opToCall = INVOKESPECIAL;
					        	break;
					        case H_INVOKEINTERFACE:
					        	isInterface = true;
					        	opToCall = INVOKEINTERFACE;
					        	break;
					        default:
					        	throw new UnsupportedOperationException();
				        }
				        if(!boxPrimitiveReturn && !unboxPrimitiveReturn)
				            ga.visitInsn(TaintUtils.NO_TAINT_STORE_INSN);
				        ga.visitMethodInsn(opToCall, implMethod.getOwner(), implMethod.getName(), implMethod.getDesc(), isInterface);

				        if(boxPrimitiveReturn)
				        	ga.box(originalReturnType);
				        if(unboxPrimitiveReturn)
				        	ga.unbox(uninstSamMethodType.getReturnType());
				        ga.returnValue();
				        ga.visitMaxs(0, 0);
				        ga.visitEnd();
				        mn.maxLocals = 100;

				        wrapperMethodsToAdd.add(mn);

				        //Change the bsmArgs to point to the new wrapper (which may or may not get the suffix)

				        String taintedWrapperDesc = TaintUtils.remapMethodDescAndIncludeReturnHolder(wrapperDesc.getDescriptor());

				        String targetName = wrapperName;
				        if (!taintedWrapperDesc.equals(wrapperDesc.getDescriptor()))
					        targetName = targetName + "$$PHOSPHORTAGGED";
				        bsmArgs[1] = new Handle(Opcodes.H_INVOKESTATIC, className, targetName, taintedWrapperDesc);

				        //build the new instantiated desc
				        for (int j = 0; j < instantiatedMethodArgs.length; j++) {
					        if (TaintUtils.isPrimitiveArrayType(instantiatedMethodArgs[j])) {
						        instantiatedMethodArgs[j] = MultiDTaintedArray.getTypeForType(instantiatedMethodArgs[j]);
					        }
				        }

				        Type instantiatedReturnType = null;
				        if(isNEW || unboxPrimitiveReturn || boxPrimitiveReturn)
				        	instantiatedReturnType = newReturnType;
				        else
				        	instantiatedReturnType = originalReturnType;
				        if(samMethodType.getReturnType().getSort() == Type.VOID)
				        	instantiatedReturnType = Type.VOID_TYPE;
				        bsmArgs[2] = Type.getMethodType(TaintUtils.remapMethodDescAndIncludeReturnHolder(Type.getMethodType(instantiatedReturnType, instantiatedMethodArgs).getDescriptor()));

			        } else {
				        bsmArgs[1] = new Handle(implMethod.getTag(), implMethod.getOwner(), implMethod.getName() + (implMethod.getName().equals("<init>") ? "" : TaintUtils.METHOD_SUFFIX), remappedImplDesc);
				        bsmArgs[2] = Type.getMethodType(TaintUtils.remapMethodDescAndIncludeReturnHolder(instantiatedType.getDescriptor()));
			        }

		        }
	        }
	        else {
		        if(bsmArgs[1] instanceof Handle)
		        {
			        Type t = Type.getMethodType(((Handle)bsmArgs[1]).getDesc());
			        if(TaintUtils.isPrimitiveType(t.getReturnType()))
			        {
				        Type _t = (Type) bsmArgs[0];
				        if(_t.getReturnType().getSort() == Type.VOID){
					        //Manually add the return type here;
					        String nd = "(";
					        for(Type a : _t.getArgumentTypes())
					        {
						        nd += a.getDescriptor();
					        }
					        nd += TaintUtils.getContainerReturnType(t.getReturnType()) +")V";
					        bsmArgs[0] = Type.getMethodType(nd);
				        }
				        _t = (Type) bsmArgs[2];
				        if(_t.getReturnType().getSort() == Type.VOID){
					        //Manually add the return type here;
					        String nd = "(";
					        for(Type a : _t.getArgumentTypes())
					        {
						        nd += a.getDescriptor();
					        }
					        nd += TaintUtils.getContainerReturnType(t.getReturnType()) +")V";
					        bsmArgs[2] = Type.getMethodType(nd);
				        }
			        }
		        }
		        for (int k = 0; k < bsmArgs.length; k++) {
			        Object o = bsmArgs[k];
			        if (o instanceof Handle) {
				        String nameH = ((Handle) o).getName();
				        if (!Instrumenter.isIgnoredClass(((Handle) o).getOwner()) && !Instrumenter.isIgnoredMethod(((Handle) o).getOwner(), nameH, ((Handle) o).getDesc()) &&
						        !TaintUtils.remapMethodDescAndIncludeReturnHolder(((Handle) o).getDesc()).equals(((Handle) o).getDesc())) {
					        bsmArgs[k] = new Handle(((Handle) o).getTag(), ((Handle) o).getOwner(), nameH + (nameH.equals("<init>") ? "" : TaintUtils.METHOD_SUFFIX), TaintUtils.remapMethodDescAndIncludeReturnHolder(((Handle) o).getDesc()));
				        }
			        } else if (o instanceof Type) {
				        Type t = (Type) o;
				        bsmArgs[k] = Type.getMethodType(TaintUtils.remapMethodDescAndIncludeReturnHolder(t.getDescriptor()));
			        }
		        }
	        }
        }
		if (hasNewName && !Instrumenter.isIgnoredClass(bsm.getOwner())) {
			if (!Instrumenter.isIgnoredMethod(bsm.getOwner(), bsm.getName(), bsm.getDesc()) && !TaintUtils.remapMethodDescAndIncludeReturnHolder(bsm.getDesc()).equals(bsm.getDesc()))
				bsm = new Handle(bsm.getTag(), bsm.getOwner(), bsm.getName()+TaintUtils.METHOD_SUFFIX, TaintUtils.remapMethodDescAndIncludeReturnHolder(bsm.getDesc()));
        }
        super.visitInvokeDynamicInsn(name, newDesc, bsm, bsmArgs);
        //      System.out.println("asdfjas;dfdsf  post invoke " + analyzer.stack);
//      System.out.println("Now: " + analyzer.stack);
        if (isCalledOnAPrimitiveArrayType) {
            if (TaintUtils.DEBUG_CALLS)
                System.out.println("Post invoke stack: " + analyzer.stack);
            if (Type.getReturnType(desc).getSort() == Type.VOID) {
                super.visitInsn(POP);
            } else if (analyzer.stack.size() >= 2) {// && analyzer.stack.get(analyzer.stack.size() - 2).equals("[I")) {
                //this is so dumb that it's an array type.
                super.visitInsn(SWAP);
                super.visitInsn(POP);
            }

        }
//      System.out.println("after: " + analyzer.stack);

        if (dontUnboxTaints) {
            dontUnboxTaints = false;
            return;
        }
        String taintType = TaintUtils.getShadowTaintType(Type.getReturnType(desc).getDescriptor());
        if (taintType != null) {
            super.visitInsn(DUP);
			super.visitFieldInsn(GETFIELD, returnType.getInternalName(), "taint", taintType);
            super.visitInsn(SWAP);
            super.visitFieldInsn(GETFIELD, returnType.getInternalName(), "val", origReturnType.getDescriptor());
            if(nextLoadisTracked)
            	throw new UnsupportedOperationException();
        }
        if (TaintUtils.DEBUG_CALLS)
            System.out.println("Post invoke stack post swap pop maybe: " + analyzer.stack);
	}

	private Type[] prepend(Type objectType, Type[] argumentTypes) {
		Type[] ret = new Type[argumentTypes.length + 1];
		System.arraycopy(argumentTypes, 0, ret, 1, argumentTypes.length);
		ret[0] = objectType;
		return ret;
	}

	static final boolean isInternalTaintingMethod(String owner)
	{
		return owner.startsWith("edu/columbia/cs/psl/phosphor/runtime/")
				|| Configuration.taintTagFactory.isInternalTaintingClass(owner);
	}
	
	static final String BYTE_NAME = "java/lang/Byte";
	static final String BOOLEAN_NAME = "java/lang/Boolean";
	static final String INTEGER_NAME = "java/lang/Integer";
	static final String FLOAT_NAME = "java/lang/Float";
	static final String LONG_NAME = "java/lang/Long";
	static final String CHARACTER_NAME = "java/lang/Character";
	static final String DOUBLE_NAME = "java/lang/Double";
	static final String SHORT_NAME = "java/lang/Short";

	static final boolean isBoxUnboxMethodToWrap(String owner, String name, String desc) {
		if (Configuration.ignoredMethods.contains(new Configuration.Method(name, owner))) {
			return false;
		}
		
		if(name.equals("valueOf") && desc.startsWith("(Ljava/lang/String"))
			return Instrumenter.isIgnoredClass(owner);//All of these no matter what will get caught by parseXXX
		if ((owner.equals(INTEGER_NAME) || owner.equals(BYTE_NAME) || owner.equals(BOOLEAN_NAME) || owner.equals(CHARACTER_NAME) || owner.equals(SHORT_NAME) || owner.equals(LONG_NAME) || owner.equals(FLOAT_NAME) || owner.equals(DOUBLE_NAME))
				&& (name.equals("toString") || name.equals("toHexString") || name.equals("toOctalString") || name.equals("toBinaryString") || name.equals("toUnsignedString"))) {
			return true;
		} else if ((owner.equals(BOOLEAN_NAME) && (name.equals("parseBoolean")))
				|| (owner.equals(BYTE_NAME)    && (name.equals("parseByte")))
				|| (owner.equals(INTEGER_NAME) && (name.equals("parseInt") || name.equals("parseUnsignedInt")))
				|| (owner.equals(SHORT_NAME)   && (name.equals("parseShort")))
				|| (owner.equals(LONG_NAME)    && (name.equals("parseLong") || name.equals("parseUnsignedLong")))
				|| (owner.equals(FLOAT_NAME)   && (name.equals("parseFloat")))
				|| (owner.equals(DOUBLE_NAME)  && (name.equals("parseDouble")))) {
			return true;
		}
		else if(name.equals("getChars") && (owner.equals(INTEGER_NAME) || owner.equals(LONG_NAME)))
			return true;
		else if (owner.equals(CHARACTER_NAME) && (name.equals("digit") /*|| name.equals("isWhitespace")*/))
			return true;
		return false;
	}
	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itfc) {
		if (isIgnoreAllInstrumenting || isRawInsns) {
			super.visitMethodInsn(opcode, owner, name, desc, itfc);
			return;
		}
		//Stupid workaround for eclipse benchmark
		if (name.equals("getProperty") && className.equals("org/eclipse/jdt/core/tests/util/Util")) {
			owner = Type.getInternalName(ReflectionMasker.class);
			System.out.println("Fixing getproperty");
			name = "getPropertyHideBootClasspath";
		}

		if (opcode == INVOKESTATIC && isBoxUnboxMethodToWrap(owner, name, desc)) {
				if(name.equals("valueOf") && desc.startsWith("(Ljava/lang/String;"))
				switch(owner)
				{
				case BYTE_NAME:
					name = "valueOfB";
					break;
				case BOOLEAN_NAME:
					name = "valueOfZ";
					break;
				case CHARACTER_NAME:
					name = "valueOfC";
					break;
				case SHORT_NAME:
					name = "valueOfS";
					break;
					default:
						throw new UnsupportedOperationException(owner);
				}
				owner = "edu/columbia/cs/psl/phosphor/runtime/RuntimeBoxUnboxPropogator";
		}
		boolean isPreAllocedReturnType = TaintUtils.isPreAllocReturnType(desc);
		if (Instrumenter.isClassWithHashmapTag(owner) && name.equals("valueOf")) {
			Type[] args = Type.getArgumentTypes(desc);
			if (args[0].getSort() != Type.OBJECT) {
				if(!Configuration.MULTI_TAINTING)
				{
				owner = Type.getInternalName(BoxedPrimitiveStoreWithIntTags.class);
				desc = "(I" + desc.substring(1);
				}
				else
				{
					owner = Type.getInternalName(BoxedPrimitiveStoreWithObjTags.class);
					desc = "("+Configuration.TAINT_TAG_DESC + desc.substring(1);
				}
				super.visitMethodInsn(Opcodes.INVOKESTATIC, owner, name, desc,false);
				return;
			}
		} else if (Instrumenter.isClassWithHashmapTag(owner) && (name.equals("byteValue") || name.equals("booleanValue") || name.equals("charValue") || name.equals("shortValue"))) {
			Type returnType = Type.getReturnType(desc);
			Type boxedReturn = TaintUtils.getContainerReturnType(returnType);
			desc = "(L" + owner + ";)" + boxedReturn.getDescriptor();
			if(!Configuration.MULTI_TAINTING)
				owner = Type.getInternalName(BoxedPrimitiveStoreWithIntTags.class);
			else
				owner = Type.getInternalName(BoxedPrimitiveStoreWithObjTags.class);
			super.visitMethodInsn(Opcodes.INVOKESTATIC, owner, name, desc,false);
			if (nextLoadisTracked) {
				super.visitInsn(DUP);
				getTaintFieldOfBoxedType(boxedReturn.getInternalName());
				super.visitInsn(SWAP);
			}
			super.visitFieldInsn(GETFIELD, boxedReturn.getInternalName(), "val", returnType.getDescriptor());
			if(nextLoadisTracked)
			{
				analyzer.setTopOfStackTagged();
				nextLoadisTracked = false;
			}
			return;
		}

		Type ownerType = Type.getObjectType(owner);
		if (opcode == INVOKEVIRTUAL && ownerType.getSort() == Type.ARRAY && ownerType.getElementType().getSort() != Type.OBJECT && ownerType.getDimensions() > 1) {
			//			System.out.println("got to change the owner on primitive array call from " +owner+" to " + MultiDTaintedArray.getTypeForType(ownerType));
			owner = MultiDTaintedArray.getTypeForType(ownerType).getInternalName();
		}

		//		Type ownerType = Type.getType(owner + ";");
		boolean isCallToPrimitiveArrayClone = opcode == INVOKEVIRTUAL && desc.equals("()Ljava/lang/Object;") && name.equals("clone") && getTopOfStackType().getSort() == Type.ARRAY
				&& getTopOfStackType().getElementType().getSort() != Type.OBJECT;
		//When you call primitive array clone, we should first clone the taint array, then register that taint array to the cloned object after calling clone
		Type primitiveArrayType = null;
		if (isCallToPrimitiveArrayClone) {
			//			System.out.println("Call to primitive array clone: " + analyzer.stack + " " + owner);
			registerTaintedArray();
			primitiveArrayType = getTopOfStackType();
			Configuration.taintTagFactory.methodOp(opcode, primitiveArrayType.getInternalName(), "clone", "()Ljava/lang/Object", false, mv, lvs, this);
			super.visitMethodInsn(opcode, primitiveArrayType.getInternalName(), "clone", "()Ljava/lang/Object;",false);
			return;
		}
		if ((owner.equals("java/lang/System") || owner.equals("java/lang/VMSystem") || owner.equals("java/lang/VMMemoryManager"))&& name.equals("arraycopy")
				&&! desc.equals("(Ljava/lang/Object;ILjava/lang/Object;IILjava/lang/DCompMarker;)V")) {
//			System.out.println("12dw23 Calling "+owner+"."+name+desc + "with " + analyzer.stack);

			if(Instrumenter.IS_KAFFE_INST)
				name = "arraycopyVM";
			else if(Instrumenter.IS_HARMONY_INST)
				name= "arraycopyHarmony";
			owner = Type.getInternalName(TaintUtils.class);
			//We have several scenarios here. src/dest may or may not have shadow arrays on the stack
			boolean destIsPrimitve = false;
			Type destType = getStackTypeAtOffset(4);
//			System.out.println(analyzer.stack);
			destIsPrimitve = !stackElIsNull(4) &&destType.getSort() != Type.OBJECT && destType.getElementType().getSort() != Type.OBJECT;
			int srcOffset = 7;
			if (destIsPrimitve)
				srcOffset++;
//						System.out.println("Sysarracopy with " + analyzer.stack);
			Type srcType = getStackTypeAtOffset(srcOffset);
			boolean srcIsPrimitive = srcType.getSort() != Type.OBJECT && srcType.getElementType().getSort() != Type.OBJECT&& !stackElIsNull(srcOffset);
			if(!Configuration.MULTI_TAINTING)
			{
				if (srcIsPrimitive) {
					if (destIsPrimitve) {
						desc = "(Ljava/lang/Object;Ljava/lang/Object;IILjava/lang/Object;Ljava/lang/Object;IIII)V";
						if(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING)
							name = "arraycopyControlTrack";
					} else {
						desc = "(Ljava/lang/Object;Ljava/lang/Object;IILjava/lang/Object;IIII)V";
					}
				} else {
					if (destIsPrimitve) {
						desc = "(Ljava/lang/Object;IILjava/lang/Object;Ljava/lang/Object;IIII)V";
					} else {
						desc = "(Ljava/lang/Object;IILjava/lang/Object;IIII)V";

					}
				}
			}
			else
			{
				if (srcIsPrimitive) {
					if (destIsPrimitve) {
						desc = "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;ILjava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;ILjava/lang/Object;I)V";
						if(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_HEADERS_NO_TRACKING)
							name = "arraycopyControlTrack";
					} else {
						desc = "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;ILjava/lang/Object;Ljava/lang/Object;ILjava/lang/Object;I)V";
					}
				} else {
					if (destIsPrimitve) {
						desc = "(Ljava/lang/Object;Ljava/lang/Object;ILjava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;ILjava/lang/Object;I)V";
					} else {
						desc = "(Ljava/lang/Object;Ljava/lang/Object;ILjava/lang/Object;Ljava/lang/Object;ILjava/lang/Object;I)V";

					}
				}
				if(Configuration.IMPLICIT_TRACKING)
				{
					desc = desc.substring(0,desc.length()-2)+Type.getDescriptor(ControlTaintTagStack.class)+")V";
					super.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
				}
			}
		}
		if (owner.startsWith("edu/columbia/cs/psl/phosphor") && !name.equals("printConstraints") && !name.equals("hasNoDependencies") && !desc.equals("(I)V") && !owner.endsWith("Tainter") && !owner.endsWith("CharacterUtils")
				&&!name.equals("getPHOSPHOR_TAG") && !name.equals("setPHOSPHOR_TAG") && !owner.equals("edu/columbia/cs/psl/phosphor/runtime/RuntimeBoxUnboxPropogator")
		&& !owner.equals(Configuration.TAINT_TAG_INTERNAL_NAME)
		) {
			Configuration.taintTagFactory.methodOp(opcode, owner, name, desc, itfc, mv, lvs, this);
			super.visitMethodInsn(opcode, owner, name, desc, itfc);
			return;
		}
		if(Configuration.IMPLICIT_TRACKING && opcode == Opcodes.INVOKEVIRTUAL && owner.equals("java/lang/Object") && (name.equals("equals") || name.equals("hashCode")))
		{
			Type callee = getTopOfStackType();
			if(name.equals("equals"))
				callee = getStackTypeAtOffset(1);

			if(callee.getSort() == Type.OBJECT) {
				String calledOn = callee.getInternalName();

				try {
					Class in = Class.forName(calledOn.replace('/', '.'), false, TaintPassingMV.class.getClassLoader());
					if (!in.isInterface() && !Instrumenter.isIgnoredClass(calledOn)) {
						owner = calledOn;
					}
				} catch (Throwable t) {
					//if not ignored, can still make an invokeinterface
					if (!Instrumenter.isIgnoredClass(calledOn)) {
						owner = Type.getInternalName(TaintedWithObjTag.class);
						opcode = INVOKEINTERFACE;
						itfc = true;
//						t.printStackTrace();
					}

				}

			}
		}
		if(opcode == INVOKEVIRTUAL && Configuration.WITH_HEAVY_OBJ_EQUALS_HASHCODE && (name.equals("equals") || name.equals("hashCode")) && owner.equals("java/lang/Object"))
		{
			opcode = INVOKESTATIC;
			owner = Type.getInternalName(NativeHelper.class);
			if(name.equals("equals"))
				desc= "(Ljava/lang/Object;Ljava/lang/Object;)Z";
			else
				desc = "(Ljava/lang/Object;)I";
		}
		//to reduce how much we need to wrap, we will only rename methods that actually have a different descriptor
		boolean hasNewName = !TaintUtils.remapMethodDesc(desc).equals(desc);
		if(isCallToPrimitiveArrayClone)
		{
			hasNewName = false;
		}
		boolean isIgnoredForTaints = Configuration.WITH_SELECTIVE_INST && 
				Instrumenter.isIgnoredMethodFromOurAnalysis(owner, name, desc);
		if ((Instrumenter.isIgnoredClass(owner) || isIgnoredForTaints || Instrumenter.isIgnoredMethod(owner, name, desc))  && !isInternalTaintingMethod(owner)){
			Type[] args = Type.getArgumentTypes(desc);
			if (TaintUtils.DEBUG_CALLS) {
				System.out.println("Calling non-inst: " + owner + "." + name + desc + " stack " + analyzer.stack);
			}
			int argsSize = 0;
			for (int i = 0; i < args.length; i++) {
				argsSize += args[args.length - i - 1].getSize();
								if (TaintUtils.DEBUG_CALLS)
				System.out.println(i + ", " + analyzer.stack.get(analyzer.stack.size() - argsSize) + " " + args[args.length - i - 1]);
				if(args[args.length - i - 1].getSort() == Type.ARRAY && args[args.length - i - 1].getElementType().getSort() != Type.OBJECT && args[args.length - i - 1].getDimensions() > 1)
				{
					if(!isIgnoredForTaints)
					ensureUnBoxedAt(i, args[args.length-i-1]);
//					unboxTaintArrayAt(i+1, args[args.length - i - 1].getDescriptor());
				}
				else if (isPrimitiveType(args[args.length - i - 1])
						|| (args[args.length - i - 1].equals(Type.getType(Object.class)) && isPrimitiveStackType(analyzer.stack.get(analyzer.stack.size() - argsSize)))) {
					//Wooahhh let's do nothing here if it's a null on the stack
					if (isPrimitiveType(args[args.length - i - 1]) && analyzer.stack.get(analyzer.stack.size() - argsSize) == Opcodes.NULL) {

					} else
						popAt(i + 1);
				}
			}
//			System.out.println("After modifying, Calling non-inst: " + owner + "." + name + desc + " stack " + analyzer.stack);

			boolean isCalledOnAPrimitiveArrayType = false;
			if (opcode == INVOKEVIRTUAL) {
				Type callee = getTypeForStackType(analyzer.stack.get(analyzer.stack.size() - argsSize - 1));
				if (TaintUtils.DEBUG_CALLS)
					System.out.println("CALLEE IS " + callee);
				if (callee.getSort() == Type.ARRAY && callee.getElementType().getSort() != Type.OBJECT)
					isCalledOnAPrimitiveArrayType = true;
			}

			if(isIgnoredForTaints && !owner.startsWith("[") && !Instrumenter.isIgnoredClass(owner))
			{
				if(name.equals("<init>"))
				{
					super.visitInsn(Opcodes.ACONST_NULL);
					desc = desc.substring(0,desc.indexOf(')'))+Type.getDescriptor(UninstrumentedTaintSentinel.class)+")"+desc.substring(desc.indexOf(')')+1);
				}
				else
					name += TaintUtils.METHOD_SUFFIX_UNINST;
			}
			Configuration.taintTagFactory.methodOp(opcode, owner, name, TaintUtils.remapMethodDescForUninst(desc), itfc, mv, lvs, this);

			super.visitMethodInsn(opcode, owner, name, TaintUtils.remapMethodDescForUninst(desc), itfc);
			if (isCallToPrimitiveArrayClone) {
				//Now we have cloned (but not casted) array, and a clopned( but not casted) taint array
				//TA A
				super.visitTypeInsn(CHECKCAST, primitiveArrayType.getInternalName());
				registerTaintedArray();
			} else if (isCalledOnAPrimitiveArrayType) {
				if (TaintUtils.DEBUG_CALLS)
					System.out.println("Post invoke stack: " + analyzer.stack);
				if (Type.getReturnType(desc).getSort() == Type.VOID) {
					super.visitInsn(POP);
				} else if (analyzer.stack.size() >= 2) {// && analyzer.stack.get(analyzer.stack.size() - 2).equals("[I")) {
					//this is so dumb that it's an array type.
					super.visitInsn(SWAP);
					super.visitInsn(POP); //This is the case that we are calling a method on a primitive array type so need to pop the taint
				}
			}
			
			Type returnType = Type.getReturnType(desc);
			if (dontUnboxTaints && isIgnoredForTaints) {
				dontUnboxTaints = false;
				if(returnType.getSize() == 2)
				{
					super.visitInsn(POP2);
					super.visitInsn(ACONST_NULL);
					return;
				}
				else
				{
					super.visitInsn(POP);
					super.visitInsn(ACONST_NULL);
					return;
				}
			}

			if (isPrimitiveType(returnType)) {
				if(!nextLoadisTracked)
				{
					return;
				}
				nextLoadisTracked = false;
				if (returnType.getSort() == Type.ARRAY) {
					generateEmptyTaintArray(returnType.getDescriptor());
				} else if (returnType.getSize() == 2) {
					generateUnconstrainedTaint(0);
					super.visitInsn(DUP_X2);
					super.visitInsn(POP);
				} else {
					generateUnconstrainedTaint(0);
					super.visitInsn(SWAP);
				}
				analyzer.setTopOfStackTagged();
//				System.out.println("Post" + name + desc + analyzer.stackTagStatus);
			}
			else if(returnType.getDescriptor().endsWith("Ljava/lang/Object;"))
			{
				super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArray.class), "boxIfNecessary", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
				super.visitTypeInsn(Opcodes.CHECKCAST, returnType.getInternalName());
			}
			if (TaintUtils.DEBUG_CALLS)
				System.out.println("Post invoke stack post swap pop maybe: " + analyzer.stack);
			

			
			return;
		}
		String newDesc = TaintUtils.remapMethodDesc(desc);
		if((Configuration.IMPLICIT_HEADERS_NO_TRACKING || Configuration.IMPLICIT_TRACKING))
		{
			if((isInternalTaintingMethod(owner) || owner.startsWith("[")) && !name.equals("getControlFlow") && !name.startsWith("hashCode") && !name.startsWith("equals")){
				newDesc = newDesc.replace(Type.getDescriptor(ControlTaintTagStack.class), "");
			}
			else
				super.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
			if(owner.startsWith("["))
				hasNewName = false;
		}
		//		boolean pushNull = false;
		if (name.equals("<init>") && !newDesc.equals(desc)) {
			//Add the taint sentinel to the desc
			super.visitInsn(ACONST_NULL);
			newDesc = newDesc.substring(0, newDesc.indexOf(")")) + Type.getDescriptor(TaintSentinel.class) + ")" + Type.getReturnType(newDesc).getDescriptor();
		}
		if (isPreAllocedReturnType) {
//			System.out.println("\t\tAdding stuff for " + owner + "." + name + newDesc);
			Type t = Type.getReturnType(newDesc);
			newDesc = newDesc.substring(0, newDesc.indexOf(")")) + t.getDescriptor() + ")" + t.getDescriptor();
			super.visitVarInsn(ALOAD, lvs.getPreAllocedReturnTypeVar(t));
//			System.out.println("n: " + lvs.getPreAllocedReturnTypeVar(t));
//			System.out.println("Analyzer lcoal is: " + analyzer.locals.get(lvs.getPreAllocedReturnTypeVar(t)));
		}
		Type origReturnType = Type.getReturnType(desc);
		Type returnType = TaintUtils.getContainerReturnType(Type.getReturnType(desc));
		if (TaintUtils.DEBUG_CALLS)
			System.out.println("Remapped call from " + owner + "." + name + desc + " to " + owner + "." + name + newDesc);
		if (!name.contains("<") && hasNewName)
			name += TaintUtils.METHOD_SUFFIX;
		if (TaintUtils.DEBUG_CALLS) {
			System.out.println("Calling w/ stack: " + analyzer.stack);
		}

		//if you call a method and instead of passing a primitive array you pass ACONST_NULL, we need to insert another ACONST_NULL in the stack
		//for the taint for that array
		Type[] args = Type.getArgumentTypes(newDesc);
		Type[] argsInReverse = new Type[args.length];
		int argsSize = 0;
		for (int i = 0; i < args.length; i++) {
			argsInReverse[args.length - i - 1] = args[i];
			argsSize += args[i].getSize();
		}
		int i = 1;
		int n = 1;
		boolean ignoreNext = false;

//										System.out.println(this.name+this.desc+"12dw23 Calling "+owner+"."+name+newDesc + "with " + analyzer.stack);
		for (Type t : argsInReverse) {
			if (analyzer.stack.get(analyzer.stack.size() - i) == Opcodes.TOP)
				i++;

//						if(ignoreNext)
//							System.out.println("ignore next i");
			Type onStack = getTypeForStackType(analyzer.stack.get(analyzer.stack.size() - i));
//						System.out.println(name+ " ONStack: " + onStack + " and t = " + t);

			if (!ignoreNext && t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT) {
//								System.out.println("t " + t + " and " + analyzer.stack.get(analyzer.stack.size() - i) + " j23oij4oi23");
//System.out.println(onStack);
				//Need to check to see if there's a null on the stack in this position
				if (analyzer.stack.get(analyzer.stack.size() - i) == Opcodes.NULL) {
//					if (TaintUtils.DEBUG_CALLS)
//						System.err.println("Adding a null in call at " + n);
//					insertNullAt(n);
				} else if (onStack.getSort() == Type.OBJECT && (!isIgnoredForTaints || t.getDimensions() == 1)) {
					//Unbox this
					unboxTaintArrayAt(n, t.getDescriptor());
				}
			} else if (!ignoreNext && onStack.getSort() == Type.ARRAY && onStack.getElementType().getSort() != Type.OBJECT) {
				//There is an extra taint on the stack at this position
				if (TaintUtils.DEBUG_CALLS)
					System.err.println("removing taint array in call at " + n);
				storeTaintArrayAt(n, onStack.getDescriptor());
			}
			if ((t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT) || (t.getDescriptor().startsWith("Ledu/columbia/cs/psl/phosphor/struct/Lazy")))
				ignoreNext = !ignoreNext;
			n++;
			i++;
		}

		//		System.out.println("Args size: " + argsSize + " nargs " + args.length);
//		if (TaintUtils.DEBUG_CALLS)
//			System.out.println("No more changes: calling " + owner + "." + name + newDesc + " w/ stack: " + analyzer.stack);

		boolean isCalledOnAPrimitiveArrayType = false;
		if (opcode == INVOKEVIRTUAL) {
			if (analyzer.stack.get(analyzer.stack.size() - argsSize - 1) == null)
				System.out.println("NULL on stack for calllee???" + analyzer.stack + " argsize " + argsSize);
			Type callee = getTypeForStackType(analyzer.stack.get(analyzer.stack.size() - argsSize - 1));
			if (TaintUtils.DEBUG_CALLS)
				System.out.println("CALLEE IS " + callee);
			if (callee.getSort() == Type.ARRAY && callee.getElementType().getSort() != Type.OBJECT)
				isCalledOnAPrimitiveArrayType = true;
		}
		Configuration.taintTagFactory.methodOp(opcode, owner, name, newDesc, itfc, mv, lvs, this);

		super.visitMethodInsn(opcode, owner, name, newDesc, itfc);

		if(Configuration.IMPLICIT_TRACKING && !arrayAnalyzer.hasFinally && arrayAnalyzer.nTryCatch == 0 && !isSuperUninit && this.isSuperUninit && opcode == INVOKESPECIAL && name.equals("<init>"))
		{
			super.visitTryCatchBlock(firstLabel,endLabel,popAllLabel,null);
			super.visitLabel(firstLabel);
			this.isSuperUninit = false;
		}
//				System.out.println("asdfjas;dfdsf  post invoke " + analyzer.stack);
//		System.out.println("Now: " + analyzer.stack);
		if (isCallToPrimitiveArrayClone) {
			//Now we have cloned (but not casted) array, and a clopned( but not casted) taint array
			//TA A
			//			System.out.println(owner + name + newDesc + ": " + analyzer.stack);
			super.visitTypeInsn(CHECKCAST, primitiveArrayType.getInternalName());
			//			System.out.println(owner + name + newDesc + ": " + analyzer.stack);
			registerTaintedArray();
		} else if (isCalledOnAPrimitiveArrayType) {
			if (TaintUtils.DEBUG_CALLS)
				System.out.println("Post invoke stack: " + analyzer.stack);
			if (Type.getReturnType(desc).getSort() == Type.VOID) {
				super.visitInsn(POP);
			} else if (analyzer.stack.size() >= 2) {// && analyzer.stack.get(analyzer.stack.size() - 2).equals("[I")) {
				//this is so dumb that it's an array type.
				super.visitInsn(SWAP);
				super.visitInsn(POP);
			}

		}
//		System.out.println("after: " + analyzer.stack);

		if (isTaintlessArrayStore) {
			isTaintlessArrayStore = false;
			return;
		}
		if (dontUnboxTaints) {
			dontUnboxTaints = false;
			return;
		}
		String taintType = TaintUtils.getShadowTaintType(Type.getReturnType(desc).getDescriptor());
		if (taintType != null) {
			if(nextLoadisTracked)
			{
				FrameNode fn = getCurrentFrameNode();
				fn.type = Opcodes.F_NEW;
				super.visitInsn(DUP);
				String taintTypeRaw = Configuration.TAINT_TAG_DESC;
				if (Type.getReturnType(desc).getSort() == Type.ARRAY)
				{
					nextLoadisTracked = false;
					Label ok = new Label();
					Label isnull = new Label();
					
					super.visitJumpInsn(IFNULL, isnull);
					super.visitInsn(DUP);
					super.visitFieldInsn(GETFIELD, returnType.getInternalName(), "val", origReturnType.getDescriptor());
					FrameNode fn2 = getCurrentFrameNode();
					fn2.type = Opcodes.F_NEW;
					super.visitJumpInsn(GOTO, ok);
					super.visitLabel(isnull);
					fn.accept(this);
					super.visitInsn(ACONST_NULL);
					super.visitTypeInsn(CHECKCAST, origReturnType.getInternalName());
					super.visitLabel(ok);
					fn2.accept(this);
					analyzer.setTopOfStackTagged();
				}
				else
				{
					super.visitFieldInsn(GETFIELD, returnType.getInternalName(), "taint", taintTypeRaw);
					super.visitInsn(SWAP);		
					nextLoadisTracked = false;
					super.visitFieldInsn(GETFIELD, returnType.getInternalName(), "val", origReturnType.getDescriptor());
					analyzer.setTopOfStackTagged();
				}
			}
			else
			{
				if (Type.getReturnType(desc).getSort() == Type.ARRAY)
				{
					FrameNode fn = getCurrentFrameNode();
					fn.type = Opcodes.F_NEW;

					nextLoadisTracked = false;
					Label ok = new Label();
					Label isnull = new Label();

					super.visitInsn(DUP);
					super.visitJumpInsn(IFNULL, isnull);
					super.visitFieldInsn(GETFIELD, returnType.getInternalName(), "val", origReturnType.getDescriptor());
					FrameNode fn2 = getCurrentFrameNode();
					fn2.type = Opcodes.F_NEW;
					super.visitJumpInsn(GOTO, ok);
					super.visitLabel(isnull);
					fn.accept(this);
					super.visitTypeInsn(CHECKCAST, origReturnType.getInternalName());
					super.visitLabel(ok);
					fn2.accept(this);
				}
				else
					super.visitFieldInsn(GETFIELD, returnType.getInternalName(), "val", origReturnType.getDescriptor());
			}

		}
		if (TaintUtils.DEBUG_CALLS)
			System.out.println("Post invoke stack post swap pop maybe: " + analyzer.stack);
	}
	private Label firstLabel;
	private HashSet<LocalVariableNode> lvsFromLastFrame = new HashSet<LocalVariableNode>();
	@Override
	public void visitMaxs(int maxStack, int maxLocals) {
		if(rewriteLVDebug)
		{
			Label end = new Label();
			super.visitLabel(end);
//			Label thisFrameStart = new Label();
//			for(LocalVariableNode lv : lvsFromLastFrame)
//			{
//				lv.end = new LabelNode(thisFrameStart);
//				lv.accept(mv);
//			}
//			lvsFromLastFrame.clear();
		}
		if(Configuration.IMPLICIT_TRACKING && !arrayAnalyzer.hasFinally){
			super.visitLabel(endLabel);
			super.visitLabel(popAllLabel);

			if(this.isExcludedFromControlTrack) {
				super.visitVarInsn(ALOAD,lvs.idxOfMasterControlLV);
				super.visitInsn(ICONST_0);
				super.visitFieldInsn(PUTFIELD,Type.getInternalName(ControlTaintTagStack.class),"isDisabled","Z");
			}

			int maxLV = lvs.idxOfMasterControlLV;
			if(lvs.idxOfMasterExceptionLV > maxLV)
				maxLV = lvs.idxOfMasterExceptionLV;
			if (controlTaintArray > maxLV)
				maxLV = controlTaintArray;

			Object[] baseLvs = new Object[maxLV+1];
			for(int i = 0; i < baseLvs.length;i++)
				baseLvs[i] = TOP;
			String ctrl = "edu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack";
			baseLvs[lvs.idxOfMasterControlLV] = ctrl;
			if(lvs.idxOfMasterExceptionLV >= 0)
				baseLvs[lvs.idxOfMasterExceptionLV] = Type.getInternalName(ExceptionalTaintData.class);
			if(controlTaintArray > 0)
			baseLvs[controlTaintArray] = "[I";
//			if(this.name.equals("<init>"))
//				baseLvs[0] = UNINITIALIZED_THIS;
			super.visitFrame(F_NEW, baseLvs.length, baseLvs,1,new Object[]{"java/lang/Throwable"});
			if(controlTaintArray >= 0) {
				passthruMV.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
				passthruMV.visitVarInsn(ALOAD, controlTaintArray);
				callPopAllControlTaint(passthruMV);
			}
			super.visitInsn(ATHROW);
		}
		super.visitMaxs(maxStack, maxLocals);
	}
	@Override
	public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
//		if (TaintUtils.DEBUG_FRAMES)
//			System.out.println("TMV sees frame: " + type + Arrays.toString(local) + ", stack " + Arrays.toString(stack));
//		System.out.println("TMV sees frame: " + type + Arrays.toString(local) + ", stack " + Arrays.toString(stack));
//			new Exception().printStackTrace();
//		if(rewriteLVDebug)
//		{
//			Label thisFrameStart = new Label();
//			for(LocalVariableNode lv : lvsFromLastFrame)
//			{
//				lv.end = new LabelNode(thisFrameStart);
//				lv.accept(this.passthruMV);
//			}
//			lvsFromLastFrame.clear();
//			int n = 0;
//			for(int i = 0; i < nLocal; i++)
//			{
//				if(local[i] == Opcodes.TOP)
//				{
//					n++;
//					continue;
//				}
//				Type t = getTypeForStackType(local[i]);
//				lvsFromLastFrame.add(new LocalVariableNode("local"+i, t.getDescriptor(), null, new LabelNode(thisFrameStart), null, n));
//				n+=t.getSize();
//			}
//			super.visitLabel(thisFrameStart);
//			super.visitFrame(type, nLocal, local, nStack, stack);
//			lastFrameStart = thisFrameStart;
//		}
//		else
		{
			super.visitFrame(type, nLocal, local, nStack, stack);
		}
		varsNeverToForceBox = new HashSet<Integer>();
	}

	//	private int[] bbsToAddACONST_NULLto;
	//	private int[] bbsToAddChecktypeObjectto;
	private PrimitiveArrayAnalyzer arrayAnalyzer;

	public boolean isTaintlessArrayStore = false;
	public boolean isIgnoreAllInstrumenting = false;
	public boolean isRawInsns = false;
	public boolean dontUnboxTaints;

//	private Object getTopIfConstant() {
//		if (getTopOfStackObject() == Opcodes.TOP) {
//			return analyzer.stackConstantVals.get(analyzer.stackConstantVals.size() - 2);
//		} else
//			return analyzer.stackConstantVals.get(analyzer.stackConstantVals.size() - 1);
//	}

	public boolean isIgnoreEverything = false;
	private boolean nextLoadisTracked = false;
	private boolean nextDupCopiesTaint0 = false;
	private boolean nextDupCopiesTaint1 = false;
	private boolean nextDupCopiesTaint2 = false;
	private boolean nextDupCopiesTaint3 = false;

	public boolean isNextLoadTracked()
	{
		return nextLoadisTracked;
	}
	@Override
	public void visitInsn(int opcode) {
//		if(opcode<200)
//		System.out.println(name + Printer.OPCODES[opcode] + " " + analyzer.stackTagStatus + " raw? " + isRawInsns);
//		if(opcode < 200)
//			System.out.println(name+ " "+ Printer.OPCODES[opcode]);
//		else
//			System.out.println(name+" "+PhosphorTextifier.MORE_OPCODES[opcode-200]);
		if(opcode == TaintUtils.CUSTOM_SIGNAL_1 || opcode == TaintUtils.CUSTOM_SIGNAL_2 || opcode == TaintUtils.CUSTOM_SIGNAL_3 || opcode == TaintUtils.LOOP_HEADER)
		{
			Configuration.taintTagFactory.signalOp(opcode, null);
			super.visitInsn(opcode);
			return;
		}
		if(opcode == TaintUtils.DUP_TAINT_AT_0)
		{
			nextDupCopiesTaint0 = true;
			return;
		}
		if(opcode == TaintUtils.DUP_TAINT_AT_1)
		{
			nextDupCopiesTaint1 = true;
			return;
		}
		if(opcode == TaintUtils.DUP_TAINT_AT_2)
		{
			nextDupCopiesTaint2 = true;
			return;
		}
		if(opcode == TaintUtils.DUP_TAINT_AT_3)
		{
			nextDupCopiesTaint3 = true;
			return;
		}
		if(opcode == TaintUtils.TRACKED_LOAD)
		{
			nextLoadisTracked = true;
			super.visitInsn(opcode);
			return;
		}
		if(this.isExcludedFromControlTrack && ((opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) || opcode == Opcodes.ATHROW)) {
			super.visitVarInsn(ALOAD,lvs.idxOfMasterControlLV);
			super.visitInsn(ICONST_0);
			super.visitFieldInsn(PUTFIELD,Type.getInternalName(ControlTaintTagStack.class),"isDisabled","Z");
		}
		if(isLambda && opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN){
			//Do we need to box?
			Type returnType = Type.getReturnType(this.desc);
			if (returnType.getDescriptor().contains("edu/columbia/cs/psl/phosphor/struct")) {
				String t = null;
				if(returnType.getDescriptor().contains("edu/columbia/cs/psl/phosphor/struct/Tainted"))
				{
					t = returnType.getDescriptor().replace("Ledu/columbia/cs/psl/phosphor/struct/Tainted","");
					t = t.replace("WithIntTag","");
					t = t.replace("WithObjTag","");
				}
				else if(returnType.getDescriptor().contains("edu/columbia/cs/psl/phosphor/struct/Lazy"))
				{
					t = returnType.getDescriptor().replace("Ledu/columbia/cs/psl/phosphor/struct/Lazy","");
					t = t.replace("IntTags","");
					t = t.replace("ObjTags","");
					t = "["+t;
				}
				t = t.replace(";","").replace("Int","I")
						.replace("Byte","B")
						.replace("Short","S")
						.replace("Long","J")
						.replace("Boolean","Z")
						.replace("Float","F")
						.replace("Double","D");
				//Probably need to box...
				int returnHolder = lastArg -1;
				if(getTopOfStackType().getSize() == 2){
					super.visitVarInsn(ALOAD,returnHolder);
					super.visitInsn(DUP_X2);
					super.visitInsn(POP);
				}
				else
				{
					super.visitVarInsn(ALOAD,returnHolder);
					super.visitInsn(SWAP);
				}

				super.visitFieldInsn(PUTFIELD, returnType.getInternalName(),"value",t);
				super.visitVarInsn(ALOAD,returnHolder);
				super.visitInsn(DUP);
				if(t.startsWith("[")) {
					super.visitInsn(ACONST_NULL);
					super.visitFieldInsn(PUTFIELD, returnType.getInternalName(), "taints", "[" + Configuration.TAINT_TAG_DESC);
				}
				else {

					super.visitInsn(Configuration.NULL_TAINT_LOAD_OPCODE);
					super.visitFieldInsn(PUTFIELD, returnType.getInternalName(), "taint", Configuration.TAINT_TAG_DESC);
				}
				super.visitInsn(ARETURN);
			}
			else
				super.visitInsn(opcode);
			return;
		}
		if (opcode == TaintUtils.FORCE_CTRL_STORE) {
			if(Configuration.WITHOUT_BRANCH_NOT_TAKEN) {
				return;
			}

			//If there is anything on the stack right now, apply the current marker to it
			if (analyzer.stack.isEmpty() || topOfStackIsNull())
				return;
			Type onStack = getTopOfStackType();
			if (onStack.getSort() != Type.OBJECT && onStack.getSort() != Type.ARRAY) {
				if (onStack.getSize() == 1) {
					super.visitInsn(SWAP);
					super.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
					super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags", "(" + Configuration.TAINT_TAG_DESC
							+ "Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)" + Configuration.TAINT_TAG_DESC, false);
					super.visitInsn(SWAP);
				} else {
					super.visitInsn(DUP2_X1);
					super.visitInsn(POP2);
					super.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
					super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags", "(" + Configuration.TAINT_TAG_DESC
							+ "Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)" + Configuration.TAINT_TAG_DESC, false);
					super.visitInsn(DUP_X2);
					super.visitInsn(POP);
				}
			} else {
				super.visitInsn(DUP);
				super.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
				super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTagsOnObject", "(Ljava/lang/Object;Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)V",
						false);
			}
			return;
		}
		if (opcode == TaintUtils.RAW_INSN) {
			isRawInsns = !isRawInsns;
			return;
		}
		if (opcode == TaintUtils.IGNORE_EVERYTHING) {
//			System.err.println("VisitInsn is ignoreverything! in  " + name);
//			new Exception().printStackTrace();
			isIgnoreAllInstrumenting = !isIgnoreAllInstrumenting;
			isIgnoreEverything = !isIgnoreEverything;
			Configuration.taintTagFactory.signalOp(opcode, null);
			super.visitInsn(opcode);
			return;
		}
		if (opcode == TaintUtils.NO_TAINT_STORE_INSN) {
			isTaintlessArrayStore = true;
			return;
		}
		if (isIgnoreAllInstrumenting || isRawInsns) {
			super.visitInsn(opcode);
			return;
		}
		
		if(Configuration.IMPLICIT_TRACKING && !Configuration.WITHOUT_PROPOGATION)
		{
			switch (opcode) {
//			case IRETURN:
//			case IASTORE:
//			case FRETURN:
//			case RETURN:
//			case ARETURN:
//			case DRETURN:
//			case LRETURN:
			case ATHROW:
				if (controlTaintArray >= 0) {
					passthruMV.visitInsn(DUP);
					passthruMV.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
					passthruMV.visitMethodInsn(INVOKESTATIC, Type.getInternalName(Taint.class), "combineTagsOnObject", "(Ljava/lang/Object;" + Type.getDescriptor(ControlTaintTagStack.class) + ")V", false);

					passthruMV.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
					passthruMV.visitVarInsn(ALOAD, controlTaintArray);
					callPopAllControlTaint(passthruMV);
				}
				break;
//			case FASTORE:
//			case BASTORE:
//			case CASTORE:
//			case SASTORE:
//				super.visitInsn(SWAP);
//				super.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
//				if (!Configuration.MULTI_TAINTING)
//					super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags", "(ILedu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)I", false);
//				else {
//					super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags",
//							"("+Configuration.TAINT_TAG_DESC+"Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)"+Configuration.TAINT_TAG_DESC, false);
//				}
//				super.visitInsn(SWAP);
//				break;
//			case DASTORE:
//			case LASTORE:
//			case DRETURN:
//			case LRETURN:
//				super.visitInsn(DUP2_X1);
//				super.visitInsn(POP2);
//				super.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
//				if (!Configuration.MULTI_TAINTING)
//					super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags", "(ILedu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)I", false);
//				else {
//					super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTags",
//							"("+Configuration.TAINT_TAG_DESC+"Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)"+Configuration.TAINT_TAG_DESC, false);
//				}
//				super.visitInsn(DUP_X2);
//				super.visitInsn(POP);
//				break;
//			case AASTORE:
//			case ARETURN:
//				break;
			}
		}
		
		switch (opcode) {
		case Opcodes.NOP:
			super.visitInsn(opcode);
			break;
		case Opcodes.ACONST_NULL:
			//			System.out.println("NULL at" + curLabel);
			//			System.out.println("NULL IN " + curLabel);
			super.visitInsn(opcode);
			if(nextLoadisTracked)
			{
				nextLoadisTracked = false;
				super.visitInsn(ACONST_NULL);
				analyzer.setTopOfStackTagged();
			}
			break;
		case Opcodes.ICONST_M1:
		case Opcodes.ICONST_0:
		case Opcodes.ICONST_1:
		case Opcodes.ICONST_2:
		case Opcodes.ICONST_3:
		case Opcodes.ICONST_4:
		case Opcodes.ICONST_5:
		case Opcodes.LCONST_0:
		case Opcodes.LCONST_1:
		case Opcodes.FCONST_0:
		case Opcodes.FCONST_1:
		case Opcodes.FCONST_2:
		case Opcodes.DCONST_0:
		case Opcodes.DCONST_1:
			if(nextLoadisTracked)
			{
				if(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_LIGHT_TRACKING)
				{
					super.visitVarInsn(ALOAD, lvs.idxOfMasterControlLV);
					super.visitMethodInsn(INVOKEVIRTUAL, "edu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack", "copyTag"+(Configuration.IMPLICIT_EXCEPTION_FLOW ? "Exceptions":""), "()"+Configuration.TAINT_TAG_DESC, false);
				}
				else
					super.visitInsn(Configuration.NULL_TAINT_LOAD_OPCODE);
				nextLoadisTracked = false;
				super.visitInsn(opcode);
				analyzer.setTopOfStackTagged();
			}
			else
				super.visitInsn(opcode);
			return;
		case Opcodes.LALOAD:
		case Opcodes.DALOAD:
		case Opcodes.IALOAD:
		case Opcodes.FALOAD:
		case Opcodes.BALOAD:
		case Opcodes.CALOAD:
		case Opcodes.SALOAD:
			Type taintType  = Type.getType(Configuration.TAINT_TAG_DESC);
			boolean doingLoadWithIdxTaint = false;
			if (Configuration.ARRAY_INDEX_TRACKING && topCarriesTaint()) {
				if (nextLoadisTracked)
					doingLoadWithIdxTaint = true;
				else {
					super.visitInsn(SWAP);
					super.visitInsn(POP);
					analyzer.clearTopOfStackTagged();
				}
			}
			String elType = null;
			String elName = null;
			switch (opcode) {
			case Opcodes.LALOAD:
				elName = "Long";
				elType = "J";
				break;
			case Opcodes.DALOAD:
				elName = "Double";
				elType = "D";
				break;
			case Opcodes.IALOAD:
				elName = "Int";
				elType = "I";
				break;
			case Opcodes.FALOAD:
				elName = "Float";
				elType = "F";
				break;
			case Opcodes.BALOAD:
				elName = "Byte";
				// System.out.println("BALOAD " + analyzer.stack);
				if (analyzer.stack.get(analyzer.stack.size() - (doingLoadWithIdxTaint ? 3 :2)) instanceof Integer)
					elType = "B";
				else {
					elType = Type.getType((String) analyzer.stack.get(analyzer.stack.size() - (doingLoadWithIdxTaint ? 3 :2))).getElementType().getDescriptor();
				}
				break;
			case Opcodes.CALOAD:
				elName = "Char";
				elType = "C";
				break;
			case Opcodes.SALOAD:
				elName = "Short";
				elType = "S";
				break;
			}

			if (TaintUtils.DEBUG_FRAMES)
				System.out.println(name+desc+"PRE XALOAD " + elType + ": " + analyzer.stack + "; " + analyzer.locals);
			if (analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - (doingLoadWithIdxTaint ? 3 : 2)) instanceof TaggedValue
					|| nextLoadisTracked) {
				// TA A T I
				if (elType.equals("Z"))
					elName = "Boolean";
				Type retType = Type.getObjectType("edu/columbia/cs/psl/phosphor/struct/Tainted" + elName + "With"
						+ (Configuration.MULTI_TAINTING ? "Obj" : "Int") + "Tag");

				int prealloc = lvs.getPreAllocedReturnTypeVar(retType);
				super.visitVarInsn(ALOAD, prealloc);
				String methodName = "get";
				if (Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_LIGHT_TRACKING)
				{
					super.visitVarInsn(ALOAD, lvs.idxOfMasterControlLV);
				}
				super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "edu/columbia/cs/psl/phosphor/struct/Lazy"+elName+"Array"+(Configuration.MULTI_TAINTING ? "Obj":"Int")+"Tags", methodName,
						"(" + "[" + elType + (doingLoadWithIdxTaint ? Configuration.TAINT_TAG_DESC : "")+ "I" + retType.getDescriptor() + (Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_LIGHT_TRACKING ? "Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;":"")+")" + retType.getDescriptor(), false);
				if (nextLoadisTracked) {
					super.visitInsn(DUP);
					super.visitFieldInsn(GETFIELD, retType.getInternalName(), "taint", Configuration.TAINT_TAG_DESC);
					super.visitInsn(SWAP);
					super.visitFieldInsn(GETFIELD, retType.getInternalName(), "val", elType);
					nextLoadisTracked = false;
					analyzer.setTopOfStackTagged();
				}
				else
				{
					super.visitFieldInsn(GETFIELD, retType.getInternalName(), "val", elType);
				}
			} else {
				super.visitInsn(opcode);
			}
			break;
		case Opcodes.AALOAD:
			int lvForIdxtaint = -1;
			taintType  = Type.getType(Configuration.TAINT_TAG_DESC);
			if(Configuration.ARRAY_INDEX_TRACKING && topCarriesTaint())
			{
				super.visitInsn(SWAP);
				lvForIdxtaint = lvs.getTmpLV(taintType);
				super.visitVarInsn(taintType.getOpcode(ISTORE), lvForIdxtaint);
				analyzer.clearTopOfStackTagged();
			}
			//?TA A I
//			System.out.println("AALOAD " + analyzer.stackTagStatus);
			Object arrayType = analyzer.stack.get(analyzer.stack.size() - 2);
			Type t = getTypeForStackType(arrayType);
			if (t.getSort() == Type.ARRAY && t.getDimensions() == 1 && t.getElementType().getDescriptor().startsWith("Ledu/columbia/cs/psl/phosphor/struct/Lazy")) {
				super.visitInsn(opcode);
				try {
					retrieveTaintedArray("[" + (MultiDTaintedArray.getPrimitiveTypeForWrapper(Class.forName(t.getElementType().getInternalName().replace("/", ".")))));
					if(!nextLoadisTracked)
					{
						super.visitInsn(SWAP);
						super.visitInsn(POP);
					}
					else
						analyzer.setTopOfStackTagged();
					nextLoadisTracked = false;
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else
				super.visitInsn(opcode);
			if(lvForIdxtaint >= 0)
			{
				super.visitInsn(DUP);
				super.visitVarInsn(taintType.getOpcode(ILOAD), lvForIdxtaint);
				super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTagsInPlace", "(Ljava/lang/Object;" + Configuration.TAINT_TAG_DESC + ")V", false);
				lvs.freeTmpLV(lvForIdxtaint);
			}
			break;
		case Opcodes.AASTORE:
			arrayType = analyzer.stack.get(analyzer.stack.size() - 1);
			t = getTypeForStackType(arrayType);
//			Type taintArrayType = getTypeForStackType(analyzer.stack.get(analyzer.stack.size() - 2));
//									System.out.println("AASTORE of " + arrayType + " ONTO ...");
//									System.out.println(analyzer.stack);
			//better look to see if we are storing a NULL into a multidemnsional array...

			boolean idxTainted = Configuration.ARRAY_INDEX_TRACKING && analyzer.stackTagStatus.get(analyzer.stack.size() - (topCarriesTaint() ? 1 : 0) - 2) instanceof TaggedValue;
			if (arrayType == Opcodes.NULL) {
				Object theArray = analyzer.stack.get(analyzer.stack.size() - 3 - (idxTainted ? 1 : 0));
				t = getTypeForStackType(theArray);
				//				System.out.println(theArray);
				if (theArray != Opcodes.NULL && t.getElementType().getSort() != Type.OBJECT)
					super.visitInsn(ACONST_NULL);
			}
			if (t.getSort() == Type.ARRAY && t.getElementType().getDescriptor().length() == 1) {
//				if (TaintUtils.DEBUG_FRAMES)
//					System.out.println("PRE-AASTORE w/ mutlid array " + t + ": " + analyzer.stack);
				//this is a multi-d array. make it work, even if it's nasty.
				if(!analyzer.isTopOfStackTagged())
				{
					super.visitTypeInsn(NEW, MultiDTaintedArray.getTypeForType(t).getInternalName());
					super.visitInsn(DUP_X1);
					super.visitInsn(SWAP);
					super.visitMethodInsn(INVOKESPECIAL, MultiDTaintedArray.getTypeForType(t).getInternalName(), "<init>", "("+t+")V", false);
					nextLoadisTracked = false;
//					analyzer.setTopOfStackTagged();
				}
				else
					registerTaintedArray();
				if(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_LIGHT_TRACKING)
				{
					super.visitInsn(DUP);
					super.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
					super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTagsOnObject", "(Ljava/lang/Object;Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)V", false);						
				}
				if(idxTainted)
				{
					//Array Taint Index Val
					super.visitInsn(DUP2_X1);
					//Array I V T I V
					super.visitInsn(SWAP);
					super.visitInsn(POP);
					//Array I V T V
					super.visitInsn(SWAP);
					//Array  I V V T
					super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTagsInPlace", "(Ljava/lang/Object;" + Configuration.TAINT_TAG_DESC + ")V", false);
				}
				super.visitInsn(opcode);
			} else {
				if(Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_LIGHT_TRACKING)
				{
					super.visitInsn(DUP);
					super.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
					super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTagsOnObject", "(Ljava/lang/Object;Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)V", false);						
				}
				if(idxTainted)
				{
					//Array T I V
					super.visitInsn(DUP2_X1);
					//Array I V T I V
					super.visitInsn(SWAP);
					super.visitInsn(POP);
					//Array I V T V
					super.visitInsn(SWAP);
					//Array  I V V T
					super.visitMethodInsn(INVOKESTATIC, Configuration.MULTI_TAINT_HANDLER_CLASS, "combineTagsInPlace", "(Ljava/lang/Object;" + Configuration.TAINT_TAG_DESC + ")V", false);
				}
				super.visitInsn(opcode);
			}
			break;
		case Opcodes.IASTORE:
		case Opcodes.LASTORE:
		case Opcodes.FASTORE:
		case Opcodes.DASTORE:
		case Opcodes.BASTORE:
		case Opcodes.CASTORE:
		case Opcodes.SASTORE:
			//	public static void XASTORE(int[] TArray, int[] Array, int idxTaint, int idx, int valTaint, int val) {
			int valStoreOpcode;
			int valLoadOpcode;
			Object beingStored = analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - (opcode == LASTORE || opcode == DASTORE ? 2:1));
			int offsetToArray = 4;
			int ob = (opcode == LASTORE || opcode == DASTORE ? 2 : 1) + ((beingStored instanceof TaggedValue) ? 1 : 0) + 1;
			boolean tagIsTracked = Configuration.ARRAY_INDEX_TRACKING && analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - ob) instanceof TaggedValue;
			if(tagIsTracked)
				offsetToArray++;
			switch (opcode) {
			case Opcodes.LASTORE:
				valStoreOpcode = LSTORE;
				valLoadOpcode = LLOAD;
				elType = "J";
				offsetToArray++;
				break;
			case Opcodes.DASTORE:
				valStoreOpcode = DSTORE;
				valLoadOpcode = DLOAD;
				elType = "D";
				offsetToArray++;
				break;
			case Opcodes.IASTORE:
				valStoreOpcode = ISTORE;
				valLoadOpcode = ILOAD;
				elType = "I";
				break;
			case Opcodes.FASTORE:
				valStoreOpcode = FSTORE;
				valLoadOpcode = FLOAD;
				elType = "F";
				break;
			case Opcodes.BASTORE:
				elType = "B";
				if(beingStored instanceof TaggedValue)
				{
					elType = Type.getType((String) analyzer.stack.get(analyzer.stack.size() - offsetToArray)).getElementType().getDescriptor();
					if(elType.equals("Z")) 
						elName= "Boolean";					
				}
				
				valStoreOpcode = ISTORE;
				valLoadOpcode = ILOAD;
				break;
			case Opcodes.CASTORE:
				valLoadOpcode = ILOAD;
				valStoreOpcode = ISTORE;
				elType = "C";
				break;
			case Opcodes.SASTORE:
				valLoadOpcode = ILOAD;
				valStoreOpcode = ISTORE;
				elType = "S";
				break;
			default:
				valLoadOpcode = -1;
				valStoreOpcode = -1;
				elType = null;
			}

			if (TaintUtils.DEBUG_FRAMES) {
				System.out.println("XASTORE>>>" + elType);
				System.out.println(beingStored);
				System.out.println(analyzer.stackTagStatus);
				System.out.println(analyzer.stack);
			}


			if(!(beingStored instanceof TaggedValue))
			{
//				System.out.println(analyzer.stackTagStatus);
//				System.out.println(offsetToArray);
				if (tagIsTracked) {
					String typ = (String) analyzer.stack.get(analyzer.stack.size() - offsetToArray);
					super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, typ, "set", "([" + elType + Configuration.TAINT_TAG_DESC + "I" + elType+")V", false);
				}
				else
					super.visitInsn(opcode);
			} else if (analyzer.stackTagStatus.get(analyzer.stack.size() - offsetToArray) instanceof TaggedValue) {
				String typ = (String) analyzer.stack.get(analyzer.stack.size() - offsetToArray - 1);
				if (Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_LIGHT_TRACKING) {
					super.visitVarInsn(ALOAD, lvs.idxOfMasterControlLV);
					if (tagIsTracked)
						super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, typ, "set", "([" + elType + Configuration.TAINT_TAG_DESC + "I" + Configuration.TAINT_TAG_DESC + elType + "Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)V", false);
					else
						super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, typ, "set", "([" + elType + "I" + Configuration.TAINT_TAG_DESC + elType + "Ledu/columbia/cs/psl/phosphor/struct/ControlTaintTagStack;)V", false);
				} 
				else if (tagIsTracked)
					super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, typ, "set", "([" + elType + Configuration.TAINT_TAG_DESC + "I" + Configuration.TAINT_TAG_DESC  + elType+")V", false);
				else
					super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, typ, "set", "([" + elType + "I" + Configuration.TAINT_TAG_DESC + elType + ")V", false);
			}
			else
			{
				if(tagIsTracked)
					throw new IllegalStateException(analyzer.stackTagStatus.toString());
				super.visitInsn(opcode);
			}
			isTaintlessArrayStore = false;
			break;
		case Opcodes.POP:
			if(topCarriesTaint())
			{
				super.visitInsn(POP2);
				return;
			}
			super.visitInsn(opcode);
			return;
		case Opcodes.POP2:
			if(topCarriesTaint())
			{
				super.visitInsn(POP2);
				if(topCarriesTaint())
					super.visitInsn(POP2);
				else
					super.visitInsn(POP);
				return;
			}
			else if(analyzer.stackTagStatus.get(analyzer.stackTagStatus.size()-2) instanceof TaggedValue)
			{
				super.visitInsn(POP);
				super.visitInsn(POP2);
			}
			else
				super.visitInsn(opcode);
			return;
		case Opcodes.DUP:
//			System.out.println("DUP " + nextLoadisTracked+ analyzer.stackTagStatus);
//			System.out.println(analyzer.stack);

			if(nextDupCopiesTaint0 && nextDupCopiesTaint1)
			{
				super.visitInsn(Opcodes.DUP2);
			}
			else if(!nextDupCopiesTaint0 && nextDupCopiesTaint1)
			{
				super.visitInsn(DUP_X1);
				analyzer.stackTagStatus.set(analyzer.stack.size()-3, analyzer.stack.get(analyzer.stack.size()-3));
			}
			else if(nextDupCopiesTaint0 && !nextDupCopiesTaint1)
			{
				super.visitInsn(DUP);
				analyzer.stackTagStatus.set(analyzer.stack.size()-1, analyzer.stack.get(analyzer.stack.size()-1));
			}
			else
			{
				//TOP should never be tainted and reach here
				super.visitInsn(Opcodes.DUP);
			}
			nextDupCopiesTaint0 = false;
			nextDupCopiesTaint1 = false;
			nextDupCopiesTaint2 = false;
			nextDupCopiesTaint3 = false;
			break;
		case Opcodes.DUP2:
			Object topOfStack = analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 1);

			//0 1 -> 0 1 2 3
			if (getStackElementSize(topOfStack) == 1) {
				if (nextDupCopiesTaint0) {
					int offset = 2;
					if(topCarriesTaint())
						offset++;
					Object secondOnStack = analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - offset); //3, or 2?
					if (!(secondOnStack instanceof TaggedValue)) {
						if(nextDupCopiesTaint1 && !nextDupCopiesTaint2 && !nextDupCopiesTaint3)
						{
							//Dup the top 2 items, leaving a taint behind for the top item, the second item is not tainted
							//A TB B -> A TB B A TB B
							LocalVariableNode[] lvs = storeToLocals(3);
							loadLV(2,lvs);
							loadLV(1,lvs);
							loadLV(0,lvs);
							loadLV(2,lvs);
							loadLV(1,lvs);
							loadLV(0,lvs);
							freeLVs(lvs);
							return;
						}

						throw new IllegalStateException("Told to copy taint of second thing on stack but got " + secondOnStack);
					}
					if(getStackElementSize(secondOnStack) == 2)
						throw new UnsupportedOperationException();//Should be true always or was invalid already
					if (nextDupCopiesTaint1) {
						if(nextDupCopiesTaint2)
						{
							if(nextDupCopiesTaint3)
							{
								//0, 1, 2, 3
								LocalVariableNode[] lvs = storeToLocals(4);
								loadLV(3, lvs);
								loadLV(2, lvs);
								loadLV(1, lvs);
								loadLV(0, lvs);

								loadLV(3, lvs);
								loadLV(2, lvs);
								loadLV(1, lvs);
								loadLV(0, lvs);
								freeLVs(lvs);
							}
							else
							{
								//0, 1, 2, !3
								LocalVariableNode[] lvs = storeToLocals(4);
								loadLV(3, lvs);
								loadLV(2, lvs);
								loadLV(1, lvs);
								loadLV(0, lvs);

								loadLV(3, lvs);
								loadLV(2, lvs);
//								loadLV(1, lvs);
								loadLV(0, lvs);
								analyzer.clearTopOfStackTagged();
								freeLVs(lvs);
							}
						}
						else
						{
							if(nextDupCopiesTaint3)
							{
								//0, 1, !2, 3
								throw new UnsupportedOperationException();
							}
							else
							{
								//0,1,!2,!3
								throw new UnsupportedOperationException();
							}
						}
					} else { 
						if (nextDupCopiesTaint2) {
							if(nextDupCopiesTaint3)
							{
								//0, !1, 2, 3
								//AB CD -> A CD AB CD (Top)
//								System.out.println(analyzer.stackTagStatus);
								LocalVariableNode[] lvs = storeToLocals(4);
								loadLV(3, lvs);
								loadLV(2, lvs);
								loadLV(0, lvs);
								analyzer.clearTopOfStackTagged();
								loadLV(3, lvs);
								loadLV(2, lvs);
								loadLV(1, lvs);
								loadLV(0, lvs);
								freeLVs(lvs);
//								System.out.println(analyzer.stackTagStatus);
							}
							else
							{
								//0, !1, 2, !3
								//TA ?B -> TA B TA B
								
//								System.out.println("PRE " + analyzer.stack + analyzer.stackTagStatus);
								if (topOfStack instanceof TaggedValue) {
									super.visitInsn(SWAP);
									super.visitInsn(POP);
								}
								//TA B -> TA B TA B

								Type topType = getTypeForStackType(topOfStack);
								int top = lvs.getTmpLV();
								super.visitInsn(TaintUtils.IS_TMP_STORE);
								super.visitVarInsn(topType.getOpcode(ISTORE), top);

								// TA 
								super.visitInsn(DUP2);
								//TA TA
								super.visitVarInsn(topType.getOpcode(ILOAD), top);
								// TA TA B
								super.visitInsn(DUP_X2);
								// AA BB AA BB
								lvs.freeTmpLV(top);
//								analyzer.setTopOfStackTagged();
//								analyzer.stackTagStatus.set(analyzer.stackTagStatus.size() - 3, new TaggedValue(secondOnStack));
								
//								System.out.println("POST " + analyzer.stack + analyzer.stackTagStatus);
							}
						} else { 
							if (nextDupCopiesTaint3) {
								//0, !1, !2, 3
								
								throw new UnsupportedOperationException();

							} else// 0,!1,!2,!3
							{
								throw new UnsupportedOperationException();
							}
						}
					}
				}
				else//!0
				{
					if(nextDupCopiesTaint1)
					{
						if(nextDupCopiesTaint2)
						{
							if(nextDupCopiesTaint3)
							{
								//!0, 1, 2, 3
							}
							else
							{
								//!0, 1,2,!3
							}
						}
					}
					else//!0, !1
					{
						if(nextDupCopiesTaint2)
						{
							if(nextDupCopiesTaint3)
							{
								//!0, !1, 2, 3
								//right here..
								// T V T V -> V V T V T V
								int v1 = lvs.getTmpLV();
								super.visitVarInsn(ISTORE, v1);
								int t1= lvs.getTmpLV();
								super.visitVarInsn(Configuration.TAINT_STORE_OPCODE, t1);
								int v2 = lvs.getTmpLV();
								super.visitVarInsn(ISTORE, v2);
								int t2= lvs.getTmpLV();
								super.visitVarInsn(Configuration.TAINT_STORE_OPCODE, t2);
								
								super.visitVarInsn(ILOAD, v2);
								super.visitVarInsn(ILOAD, v1);
								super.visitVarInsn(Configuration.TAINT_LOAD_OPCODE, t2);
								super.visitVarInsn(ILOAD, v2);
								super.visitVarInsn(Configuration.TAINT_LOAD_OPCODE, t1);
								super.visitVarInsn(ILOAD, v1);
								lvs.freeTmpLV(v1);
								lvs.freeTmpLV(v2);
								lvs.freeTmpLV(t1);
								lvs.freeTmpLV(t2);
								return;
							}
							else
							{
								//!0, !1, 2, !3
							}
						}
						else
						{
							if(nextDupCopiesTaint3)
							{
								//!0, !1, !2, 3
							}
							else
							{
								//!0, !1, !2, !3
								if(getStackElementSize(getTopOfStackObject()) == 2)
								{
									if(analyzer.isTopOfStackTagged())
										throw new UnsupportedOperationException(Printer.OPCODES[opcode] + " " +analyzer.stackTagStatus+" - " + nextDupCopiesTaint0 + " " + nextDupCopiesTaint1 + " " + nextDupCopiesTaint2 + " " + nextDupCopiesTaint3);
									else
									{
										super.visitInsn(opcode);
										return;
									}
								}
								else{
									if(analyzer.isTopOfStackTagged())
										throw new UnsupportedOperationException(Printer.OPCODES[opcode] + " " +analyzer.stackTagStatus+" - " + nextDupCopiesTaint0 + " " + nextDupCopiesTaint1 + " " + nextDupCopiesTaint2 + " " + nextDupCopiesTaint3);
									if(analyzer.stackTagStatus.get(analyzer.stack.size()-2) instanceof TaggedValue)
										throw new UnsupportedOperationException(Printer.OPCODES[opcode] + " " +analyzer.stackTagStatus+" - " + nextDupCopiesTaint0 + " " + nextDupCopiesTaint1 + " " + nextDupCopiesTaint2 + " " + nextDupCopiesTaint3);
									super.visitInsn(opcode);
									return;
								}
							}
						}
					}
					throw new UnsupportedOperationException(Printer.OPCODES[opcode] + " " +analyzer.stackTagStatus+" - " + nextDupCopiesTaint0 + " " + nextDupCopiesTaint1 + " " + nextDupCopiesTaint2 + " " + nextDupCopiesTaint3);
				}
			}
			else //DUP2, top of stack is double
			{
				topOfStack = analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 2);
//				System.out.println("DUP2 " + analyzer.stackTagStatus +", " + nextDupCopiesTaint0+ nextDupCopiesTaint1+ nextDupCopiesTaint2+ nextDupCopiesTaint3);
//				Object secondOnStack = analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 3);
//				System.out.println(topOfStack + ""+secondOnStack);
				if(nextDupCopiesTaint0)
				{
					if(nextDupCopiesTaint1)
					{
						//TVV -> TVVTVV
						Type topType = getTypeForStackType(topOfStack);
						int top = lvs.getTmpLV();
						super.visitInsn(TaintUtils.IS_TMP_STORE);
						super.visitVarInsn(topType.getOpcode(ISTORE), top);
						// T
						super.visitInsn(DUP);
						//TT
						super.visitVarInsn(topType.getOpcode(ILOAD), top);
						analyzer.setTopOfStackTagged();
						// TTVV
						super.visitInsn(DUP2_X1);
						// TVVTVV
						lvs.freeTmpLV(top);
					}
					else
					{
						//TVV -> TVV VV
						super.visitInsn(DUP2);
						analyzer.stackTagStatus.set(analyzer.stackTagStatus.size()-2, analyzer.stack.get(analyzer.stack.size()-2));
					}
				}
				else
				{
					if(nextDupCopiesTaint1)
					{
						//TVV -> VVTVV
						super.visitInsn(DUP2_X1);
						analyzer.stackTagStatus.set(analyzer.stackTagStatus.size()-5, analyzer.stack.get(analyzer.stack.size()-5));
					}
					else
					{
						//VV -> VVVV
						if(topOfStack instanceof TaggedValue)//should not be possible
							throw new UnsupportedOperationException();						
						super.visitInsn(DUP2);
					}
				}
//				System.out.println("POST " + analyzer.stackTagStatus);
			}
			nextDupCopiesTaint0 = false;
			nextDupCopiesTaint1 = false;
			nextDupCopiesTaint2 = false;
			nextDupCopiesTaint3 = false;
			break;
		case Opcodes.DUP_X1:
//			System.out.println("DUP_X1 " + nextDupCopiesTaint + nextDupLeavesTaintOnTop+ analyzer.stackTagStatus);
//			System.out.println(analyzer.stack);
//			new Exception().printStackTrace();
			if(nextDupCopiesTaint0)
			{
				if(!nextDupCopiesTaint1)
					throw new UnsupportedOperationException();
//				System.out.println("DUP_X1 " + analyzer.stackTagStatus);
				topOfStack = analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 1);
				if (topOfStack instanceof TaggedValue) {
					//There is a 1 word element at the top of the stack, we want to dup
					//it and it's taint to go one under so that it's
					//T V X T V
					Object underThisOne = analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 3);
					if (underThisOne instanceof TaggedValue) {
						// X X T V -> T V X X T V
						super.visitInsn(DUP2_X2);
					} else {
						//X T V -> T V X TV
						super.visitInsn(DUP2_X1);
					}
				} else {
					Object underThisOne = analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 2);
					if (underThisOne instanceof TaggedValue) {
						// X X V -> V X X V
						super.visitInsn(DUP_X2);
					} else {
						//X V -> V X V
						super.visitInsn(DUP_X1);
					}
				}
			}
			else if(nextDupCopiesTaint1)
			{
				Object underThisOne = analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 3);
				if(underThisOne instanceof TaggedValue)
				{
					//T1 V1 T2 V2 -> V2 T1 V1 T2 V2
					DUPN_XU(1, 3);
					analyzer.stackTagStatus.set(analyzer.stack.size() - 5, ((TaggedValue)underThisOne).v);
				}
				else {
					super.visitInsn(DUP_X2);
					Object o = analyzer.stack.get(analyzer.stack.size() - 4);
					if (o instanceof TaggedValue)
						o = ((TaggedValue) o).v;
					// need to make sure we clear the tag status on that dude!
					analyzer.stackTagStatus.set(analyzer.stack.size() - 4, o);
					// System.out.println(analyzer.stack);
					// System.out.println(analyzer.stackTagStatus);
				}
			}
			else if(topCarriesTaint())
			{
//				super.visitInsn(DUP_X2);
//				Object o = analyzer.stack.get(analyzer.stack.size() - 4);
//				if(o instanceof TaggedValue)
//					o = ((TaggedValue) o).v;
//				// need to make sure we clear the tag status on that dude!
//				analyzer.stackTagStatus.set(analyzer.stack.size() - 4, o);
				throw new UnsupportedOperationException();
			}
			else if(analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 2) instanceof TaggedValue)
			{
				super.visitInsn(DUP_X2);
			}
			else
				super.visitInsn(DUP_X1);
			nextDupCopiesTaint0 = false;
			nextDupCopiesTaint1 = false;
			nextDupCopiesTaint2 = false;
			nextDupCopiesTaint3 = false;
//			System.out.println("PSOT " + analyzer.stack + "; " + analyzer.stackTagStatus);
			break;
		case Opcodes.DUP_X2:
//			System.out.println("DUP_X2" + analyzer.stackTagStatus + nextDupCopiesTaint0 + nextDupCopiesTaint1);
			if (nextDupCopiesTaint0) {
				if (!nextDupCopiesTaint1) {
					//0, !1
					//aka copythe taint under then delete it from top
					
					System.out.println(analyzer.stackTagStatus);
					throw new UnsupportedOperationException();
				} else {
					//0, 1
					if (getStackElementSize(analyzer.stack.get(analyzer.stack.size() - 3)) == 2) {
						// With long/double under
						if (analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 4) instanceof TaggedValue)
							// Top 2 are tracked
							DUPN_XU(2, 3);
						else
							// 2nd is not tracked.
							DUPN_XU(2, 2);
					} else {
						// With 1word under
						if (analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 3) instanceof TaggedValue) {
							// Top 2 are tracked

							if (analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 5) instanceof TaggedValue)
								DUPN_XU(2, 4);
							else
								DUPN_XU(2, 3);
						} else {
							// 2nd is not tracked.
							if (analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 4) instanceof TaggedValue)
								DUPN_XU(2, 3);
							else
								super.visitInsn(DUP2_X2);
						}
					}
				}
			}
			else
			{
				if (nextDupCopiesTaint1) {
					Object under = analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 3);
					if (under == Opcodes.TOP) {
						// Two byte word
						throw new UnsupportedOperationException();
					} else {
						if (under instanceof TaggedValue) {
							Object twoUnder = analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 5);
							if (twoUnder instanceof TaggedValue) {
								LocalVariableNode[] lvs = storeToLocals(6);
								loadLV(0, lvs);
								analyzer.clearTopOfStackTagged();
								loadLV(5, lvs);
								loadLV(4, lvs);
								loadLV(3, lvs);
								loadLV(2, lvs);
								loadLV(1, lvs);
								loadLV(0, lvs);
								freeLVs(lvs);
							}
							else
							{
//								System.out.println("DUP_X2" + analyzer.stackTagStatus);
								LocalVariableNode[] lvs = storeToLocals(5);
								loadLV(0, lvs);
								analyzer.clearTopOfStackTagged();
								loadLV(4, lvs);
								loadLV(3, lvs);
								loadLV(2, lvs);
								loadLV(1, lvs);
								loadLV(0, lvs);
								freeLVs(lvs);

							}
						}
						else
						{
//							System.out.println(analyzer.stack);
							Object twoUnder = analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 4);
							if(twoUnder instanceof TaggedValue)
							{
								//
								DUPN_XU(1, 4);
//								System.out.println(analyzer.stackTagStatus);
								int off = analyzer.stack.size()-6;
								analyzer.stackTagStatus.set(off, analyzer.stack.get(off));

//								throw new UnsupportedOperationException();
							}
							else
							{
								//TABTC -> CATB
								DUPN_XU(1, 3);
								int off = analyzer.stack.size()-5;
								analyzer.stackTagStatus.set(off, analyzer.stack.get(off));
							}
						}
					}
				}
				else
				{
					//Dont want to copy any taint
					if(topCarriesTaint())
					{
//						System.out.println("DUP_X2" + analyzer.stackTagStatus);
						//Top has a tag, but we don't want to keep it
						if (getStackElementSize(analyzer.stack.get(analyzer.stack.size() - 2)) == 2) {
							//With long/double under
							if(analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 3) instanceof TaggedValue)
								//Top 2 are tracked
								DUPN_XU(1, 4);
							else
								//2nd is not tracked.
								DUPN_XU(1, 3);
						}
						else
						{
							//With 1word under
							if(analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 3) instanceof TaggedValue)
							{
								if(analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 5) instanceof TaggedValue)
									DUPN_XU(1, 5);
								else
									DUPN_XU(1, 4);
							}
							else
							{
								//2nd is not tracked.
								if(analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 4) instanceof TaggedValue)
								{
									DUPN_XU(1, 4);
								}
								else
									DUPN_XU(1, 3);
							}
						}
					}
					else
					{
						//What's under us?
						if(analyzer.stack.get(analyzer.stack.size() - 2) == Opcodes.TOP)
						{
							if(analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 3) instanceof TaggedValue )
							{
								LocalVariableNode d[] = storeToLocals(3);
								loadLV(0, d);
								loadLV(2, d);
								loadLV(1, d);
								loadLV(0, d);
								freeLVs(d);
							}
							else
								super.visitInsn(DUP_X2);
						} else {
							// 1 word under us. is it tagged?
							if (analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 2) instanceof TaggedValue) {
								if (analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 4) instanceof TaggedValue) {
									throw new UnsupportedOperationException();
								} else {
									DUPN_XU(1, 3);
								}
							}
							else if(analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 3) instanceof TaggedValue )
							{
								throw new UnsupportedOperationException();
							}
							else
								super.visitInsn(DUP_X2);
						}
					}
	}
			}
//			System.out.println("post DUP_X2" + analyzer.stackTagStatus);
			nextDupCopiesTaint0 = false;
			nextDupCopiesTaint1 = false;
			nextDupCopiesTaint2 = false;
			nextDupCopiesTaint3 = false;
			break;
		case Opcodes.DUP2_X1:
//			System.out.println("D2X1" + nextDupCopiesTaint0 + " "+nextDupCopiesTaint1 + " "+nextDupCopiesTaint2 + " "+nextDupCopiesTaint3);
			//ABC -> BCABC (0 1 2 3 4)
			if(nextDupCopiesTaint0)
			{
				if (nextDupCopiesTaint1) {
					if(nextDupCopiesTaint2)
					{
						if(nextDupCopiesTaint3)
						{
							//0, 1, 2, 3
							throw new UnsupportedOperationException();
						}
						else
						{
							//0, 1, 2, !3
							throw new UnsupportedOperationException();
						}
					}
					else
					{
						if(nextDupCopiesTaint3)
						{
							//0, 1, !2, 3
							throw new UnsupportedOperationException();
						}
						else
						{
							//0,1,!2,!3
							//ATBTC -> TBTCABC
							//2 word?
							topOfStack = analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() -1);
							if(getStackElementSize(topOfStack) == 2)
							{
								topOfStack = analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 2);
//								System.out.println("DUP2_X1 " + analyzer.stackTagStatus);

								if(!(topOfStack instanceof TaggedValue))
									throw new UnsupportedOperationException("Top must be tagged for this");
								Object secondOnStack = analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 4);
								if(secondOnStack instanceof TaggedValue)
								{
									//TATBB -> TBBTATBB
									throw new UnsupportedOperationException();
								}
								else
								{
									//ATBB -> TBBATBB
									DUPN_XU(2, 1);
								}
//								throw new UnsupportedOperationException();
//								System.out.println(analyzer.stackTagStatus);
							}
							else
							{
								throw new UnsupportedOperationException();
							}
						}
					}
				} else {
					if (nextDupCopiesTaint2) {
						if (nextDupCopiesTaint3) {
							// !0, !1, 2, 3
							throw new UnsupportedOperationException();
						} else {
							// !0, !1, 2, !3
							throw new UnsupportedOperationException();
						}
					} else {
						if (nextDupCopiesTaint3) {
							if (getTopOfStackType().getSize() == 2) {
								//0 !1 !2 3
//								System.out.println(analyzer.stack);
								LocalVariableNode d[] = storeToLocals(3);
//								 System.out.println(analyzer.stackTagStatus);

								loadLV(1, d);
								loadLV(0, d);
//								loadLV(3, d);
								loadLV(2, d);
								loadLV(1, d);
								loadLV(0, d);

								freeLVs(d);

								// System.out.println(nextDupCopiesTaint0
								// +","+nextDupCopiesTaint1+","+nextDupCopiesTaint2+","+nextDupCopiesTaint3);
//								System.out.println(analyzer.stackTagStatus);
//								throw new UnsupportedOperationException();
							} else {
								// !0, !1, !2, 3
//								System.out.println(analyzer.stackTagStatus);
								LocalVariableNode d[] = storeToLocals(4);
								// System.out.println(analyzer.stackTagStatus);

								loadLV(2, d);
								loadLV(0, d);
								analyzer.clearTopOfStackTagged();
								loadLV(3, d);
								loadLV(2, d);
								loadLV(1, d);
								loadLV(0, d);

								freeLVs(d);

								// System.out.println(nextDupCopiesTaint0
								// +","+nextDupCopiesTaint1+","+nextDupCopiesTaint2+","+nextDupCopiesTaint3);
//								System.out.println(analyzer.stackTagStatus);
//								throw new UnsupportedOperationException();
							}
							}
						else
						{
							//!0,!1,!2,!3
							//We don't want to keep the tags anywhere.
							//DUP2_X1
							if(topCarriesTaint())
							{
								if(analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 3) instanceof TaggedValue)
								{
									
								}
								else
								{
//									System.out.println(analyzer.stackTagStatus);
									super.visitInsn(SWAP);
									super.visitInsn(POP);
									analyzer.clearTopOfStackTagged();
									super.visitInsn(opcode);
//									throw new UnsupportedOperationException();
									return;
								}
							}
							System.out.println(analyzer.stackTagStatus);
							throw new UnsupportedOperationException();
						}
					}
				}
			}
			else{
				if (nextDupCopiesTaint1) {
					if(nextDupCopiesTaint2)
					{
						if(nextDupCopiesTaint3)
						{
							//!0, 1, 2, 3
							throw new UnsupportedOperationException();
						}
						else
						{
							//!0, 1, 2, !3
							throw new UnsupportedOperationException();
						}
					}
					else
					{
						if(nextDupCopiesTaint3)
						{
							//!0, 1, !2, 3
							throw new UnsupportedOperationException();
						}
						else
						{
							//!0,1,!2,!3
							topOfStack = analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() -1);
							if(getStackElementSize(topOfStack) == 2)
							{
								//A TVV -> VV A T VV
								super.visitInsn(DUP2_X2);
								int idx = analyzer.stack.size()-6;
								analyzer.stackTagStatus.set(idx, analyzer.stack.get(idx));
							}
							else
							{
//								System.out.println(analyzer.stackTagStatus);
								LocalVariableNode d[] = storeToLocals(4);
//								System.out.println(analyzer.stackTagStatus);

								loadLV(2, d);
								loadLV(1, d);
								loadLV(0, d);
								loadLV(3, d);
								loadLV(2, d);
								loadLV(0, d);
								analyzer.clearTopOfStackTagged();

								freeLVs(d);
								
//								System.out.println(nextDupCopiesTaint0 +","+nextDupCopiesTaint1+","+nextDupCopiesTaint2+","+nextDupCopiesTaint3);
//								System.out.println(analyzer.stackTagStatus);
//								throw new UnsupportedOperationException();
							}
						}
					}
				}
				else {
					if(nextDupCopiesTaint2)
					{
						if(nextDupCopiesTaint3)
						{
							//!0, !1, 2, 3
							throw new UnsupportedOperationException();
						}
						else
						{
							//!0, !1, 2, !3
							throw new UnsupportedOperationException();
						}
					} else {
						if (nextDupCopiesTaint3) {
							// !0, !1, !2, 3
							if (getTopOfStackType().getSize() == 2) {
//								System.out.println(analyzer.stackTagStatus);

								LocalVariableNode d[] = storeToLocals(3);

//								loadLV(2, d);
								loadLV(0, d);
								analyzer.clearTopOfStackTagged();

//								loadLV(3, d);
								loadLV(2, d);
								loadLV(1, d);
								loadLV(0, d);

								freeLVs(d);

								// System.out.println(nextDupCopiesTaint0
								// +","+nextDupCopiesTaint1+","+nextDupCopiesTaint2+","+nextDupCopiesTaint3);
//								System.out.println(analyzer.stackTagStatus);
//								throw new UnsupportedOperationException();
							} else {
//								System.out.println(analyzer.stackTagStatus);

								LocalVariableNode d[] = storeToLocals(4);

								loadLV(2, d);
								loadLV(0, d);
								analyzer.clearTopOfStackTagged();

								loadLV(3, d);
								loadLV(2, d);
								loadLV(1, d);
								loadLV(0, d);

								freeLVs(d);

								// System.out.println(nextDupCopiesTaint0
								// +","+nextDupCopiesTaint1+","+nextDupCopiesTaint2+","+nextDupCopiesTaint3);
//								System.out.println(analyzer.stackTagStatus);
//								throw new UnsupportedOperationException();
							}
						}
						else
						{
							//!0,!1,!2,!3
							if(analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 1) instanceof TaggedValue)
								throw new UnsupportedOperationException();
							else if(analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 2) instanceof TaggedValue)
								throw new UnsupportedOperationException();
							else if(analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 3) instanceof TaggedValue)
							{
								super.visitInsn(DUP2_X2);
								return;
							}
							super.visitInsn(opcode);
						}
					}
				}
			}
//				// Z A B OR Z Z' A B?
//				if (getStackElementSize(analyzer.stack.get(analyzer.stack.size() - 1)) == 2) {
//					// Have two-word el + 1 word taint on top
//					if (analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 4) instanceof TaggedValue) {
//						// Dup the top three words to be under the 2 words
//						// beneath them
//						DUPN_XU(2, 2);
//					} else {
//						// Dup the top three words to be under the word beneath
//						// them
//						DUPN_XU(2, 1);
//					}
//				} else {
//					if (analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 3) instanceof TaggedValue) {
//						// Both this and the guy under are tracked
//						if (analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 5) instanceof TaggedValue) {
//							DUPN_XU(4, 2);
//						} else
//							DUPN_XU(4, 1);
//					} else {
//						// Top is tracked, guy under isn't
//						if (analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 4) instanceof TaggedValue) {
//							DUPN_XU(3, 2);
//						} else
//							DUPN_XU(3, 1);
//
//					}
//				}
//				
			
//			if(topCarriesTaint())
//			{
//				if (getStackElementSize(analyzer.stack.get(analyzer.stack.size() - 1)) == 2) {
//					if(analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 4) instanceof TaggedValue)
//					{
//						//Top is 2 words, then tag, then tagged value. Don't want to copy the tag under htough
//						DUPN_XU(2, 3);
//					}
//					else
//					{
//						//Top is 2 words tagged, then regular value
//						super.visitInsn(DUP2_X2);
//					}
//				}
//				else
//					throw new UnsupportedOperationException();
//			}
//			else if(analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 2) instanceof TaggedValue)
//			{
//				throw new UnsupportedOperationException();
//			}
//			else if(analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 3) instanceof TaggedValue)
//			{
//				throw new UnsupportedOperationException();
//			}
//			else
//				super.visitInsn(DUP2_X1);
			nextDupCopiesTaint0 = false;
			nextDupCopiesTaint1 = false;
			nextDupCopiesTaint2 = false;
			nextDupCopiesTaint3 = false;
			break;
		case Opcodes.DUP2_X2:
			if(nextDupCopiesTaint0)
			{
				if (nextDupCopiesTaint1) {
					if(nextDupCopiesTaint2)
					{
						if(nextDupCopiesTaint3)
						{
							//0, 1, 2, 3
							throw new UnsupportedOperationException();
						}
						else
						{
							//0, 1, 2, !3
							throw new UnsupportedOperationException();
						}
					}
					else
					{
						if(nextDupCopiesTaint3)
						{
							//0, 1, !2, 3
							throw new UnsupportedOperationException();
						}
						else
						{
							//0,1,!2,!3
//							System.out.println("DUP2_X2 "+ analyzer.stackTagStatus);
							if(getStackElementSize(getTopOfStackObject()) == 2)
							{
								//?A?BTC -> TC?A?BTC
								topOfStack = analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() -2);
//								System.out.println(topOfStack);
								Object second = analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() -4);
								if(getStackElementSize(second) == 2)
								{
									//?AATC -> TC?AAC
									throw new UnsupportedOperationException();
								}
								else
								{
									if(second instanceof TaggedValue)
									{
										Object third = analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 6);
										if(third instanceof TaggedValue)
										{
											DUPN_XU(2, 4);
										}
										else
										{
											//ATBTC -> TCATBTC
											DUPN_XU(2, 3);
										}
//										System.out.println(analyzer.stackTagStatus);
//										throw new UnsupportedOperationException();
									}
									else
									{
										Object third = analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 5);
										if (third instanceof TaggedValue) {
											// ATBTC -> TCATBTC
											DUPN_XU(2, 3);
										} else
											throw new UnsupportedOperationException();
//										System.out.println(analyzer.stackTagStatus);
//										throw new UnsupportedOperationException();
									}
								}
							}
							else
								throw new UnsupportedOperationException();
						}
					}
				}
				else {
					if(nextDupCopiesTaint2)
					{
						if(nextDupCopiesTaint3)
						{
							//!0, !1, 2, 3
							throw new UnsupportedOperationException();
						}
						else
						{
							//!0, !1, 2, !3
							throw new UnsupportedOperationException();
						}
					}
					else
					{
						if(nextDupCopiesTaint3)
						{
							//!0, !1, !2, 3
							throw new UnsupportedOperationException();
						}
						else
						{
							//!0,!1,!2,!3
							throw new UnsupportedOperationException();
						}
					}
				}
			}
			else{
				if (nextDupCopiesTaint1) {
					if(nextDupCopiesTaint2)
					{
						if(nextDupCopiesTaint3)
						{
							//!0, 1, 2, 3
							throw new UnsupportedOperationException();
						}
						else
						{
							//!0, 1, 2, !3
							throw new UnsupportedOperationException();
						}
					}
					else
					{
						if(nextDupCopiesTaint3)
						{
							//!0, 1, !2, 3
							throw new UnsupportedOperationException();
						}
						else
						{
							//!0,1,!2,!3
							if(getStackElementSize(getTopOfStackObject()) == 2)
							{
								if(analyzer.stackTagStatus.get(analyzer.stack.size() - 2) instanceof TaggedValue)
								{
									//top 2 words, tagged
									if(getStackElementSize(analyzer.stack.get(analyzer.stack.size()-4)) == 2)
									{
										throw new UnsupportedOperationException();
									}
									else
									{
										//ABTCC -> CCABTCC
										Object second = analyzer.stackTagStatus.get(analyzer.stack.size() - 4);
										if(second instanceof TaggedValue)
										{
//											System.out.println("D2X2" + analyzer.stackTagStatus);
											LocalVariableNode d[] = storeToLocals(6);


											loadLV(0, d);
											analyzer.clearTopOfStackTagged();

											loadLV(5, d);
											loadLV(4, d);
											loadLV(3, d);
											loadLV(2, d);
											loadLV(1, d);
											loadLV(0, d);


											freeLVs(d);
//											System.out.println("D2X2" + analyzer.stackTagStatus);
//											throw new UnsupportedOperationException();
										}
										else
										{
											Object third = analyzer.stackTagStatus.get(analyzer.stack.size() - 5);
											if(third instanceof TaggedValue)
											{
												DUPN_XU(1, 4);
												analyzer.stackTagStatus.set(analyzer.stack.size() - 8, analyzer.stack.get(analyzer.stack.size() - 8));
											}
											else
											{
												throw new UnsupportedOperationException();
											}
										}
									}
								}
								else
								{
									throw new UnsupportedOperationException();
								}
							} else {
								throw new UnsupportedOperationException();
							}
						}
					}
				}
				else {
					if(nextDupCopiesTaint2)
					{
						if(nextDupCopiesTaint3)
						{
							//!0, !1, 2, 3
							throw new UnsupportedOperationException();
						}
						else
						{
							//!0, !1, 2, !3
							throw new UnsupportedOperationException();
						}
					}
					else
					{
						if(nextDupCopiesTaint3)
						{
							//!0, !1, !2, 3
							throw new UnsupportedOperationException();
						}
						else
						{
							//!0,!1,!2,!3
							if (analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 2) instanceof TaggedValue) {
								throw new UnsupportedOperationException();
							} else if (analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 3) instanceof TaggedValue) {
								throw new UnsupportedOperationException();
							} else if (analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 4) instanceof TaggedValue) {
								DUPN_XU(2, 2);
							}
							else
							{
								super.visitInsn(DUP2_X2);
							}
						}
					}
				}
			}
//			if(nextDupCopiesTaint0 || nextDupCopiesTaint1)
//			{
//				throw new UnsupportedOperationException();
//			}
//			else if(topCarriesTaint())
//			{
//				throw new UnsupportedOperationException();
//			}
//			else if(analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 2) instanceof TaggedValue)
//			{
//				throw new UnsupportedOperationException();
//			}
//			else if(analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 3) instanceof TaggedValue)
//			{
//				throw new UnsupportedOperationException();
//			}
//			else
//				super.visitInsn(DUP2_X2);
			nextDupCopiesTaint0 = false;
			nextDupCopiesTaint1 = false;
			nextDupCopiesTaint2 = false;
			nextDupCopiesTaint3 = false;
			break;
		case Opcodes.SWAP:
			if (topCarriesTaint()
					|| analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 1) instanceof TaggedValue) {
				if (nextLoadisTracked) {
					if (analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 3) instanceof TaggedValue)
					{
						super.visitInsn(DUP2_X2);
						super.visitInsn(POP2);
					}
					else
					{
						super.visitInsn(DUP2_X1);
						super.visitInsn(POP2);
					}
				}
				else
				{
					super.visitInsn(DUP2_X1);
					super.visitInsn(POP2);
				}
			}
			else if(analyzer.stackTagStatus.get(analyzer.stackTagStatus.size() - 2) instanceof TaggedValue)
			{
				//Top has no tag, second does
				super.visitInsn(DUP_X2);
				super.visitInsn(POP);
			}
			else
				super.visitInsn(SWAP);
			nextLoadisTracked=false;
			break;
		case Opcodes.FADD:
		case Opcodes.FREM:
		case Opcodes.FSUB:
		case Opcodes.FMUL:
		case Opcodes.FDIV:
			 {
				if (!topCarriesTaint()) {
					super.visitInsn(opcode);
					break;
				} else {
					Configuration.taintTagFactory.stackOp(opcode,mv,lvs,this);
					analyzer.setTopOfStackTagged();
				}
			}
			break;
		case Opcodes.IADD:
		case Opcodes.ISUB:
		case Opcodes.IMUL:
		case Opcodes.IDIV:
		case Opcodes.IREM:
		case Opcodes.ISHL:
		case Opcodes.ISHR:
		case Opcodes.IUSHR:
		case Opcodes.IOR:
		case Opcodes.IAND:
		case Opcodes.IXOR:
//			System.out.println(Printer.OPCODES[opcode] + " "+ analyzer.stackTagStatus);
			if (!topCarriesTaint()) {
				super.visitInsn(opcode);
				break;
			} else {
				Configuration.taintTagFactory.stackOp(opcode, mv, lvs, this);
				analyzer.setTopOfStackTagged();
			}
			break;
		case Opcodes.DADD:
		case Opcodes.DSUB:
		case Opcodes.DMUL:
		case Opcodes.DDIV:
		case Opcodes.DREM:
			{
				if (!topCarriesTaint()) {
					super.visitInsn(opcode);
					break;
				} else {
					Configuration.taintTagFactory.stackOp(opcode,mv,lvs,this);
					analyzer.setTopOfStackTagged();
				}
			}
			break;
		case Opcodes.LSHL:
		case Opcodes.LUSHR:
		case Opcodes.LSHR:
			if (!topCarriesTaint()) {
				super.visitInsn(opcode);
				break;
			} else {
				Configuration.taintTagFactory.stackOp(opcode,mv,lvs,this);
				analyzer.setTopOfStackTagged();
			}
			break;
		case Opcodes.LSUB:
		case Opcodes.LMUL:
		case Opcodes.LADD:
		case Opcodes.LDIV:
		case Opcodes.LREM:
		case Opcodes.LAND:
		case Opcodes.LOR:
		case Opcodes.LXOR: 
			if (!topCarriesTaint()) {
				super.visitInsn(opcode);
				break;
			} else {
				Configuration.taintTagFactory.stackOp(opcode, mv, lvs, this);
				analyzer.setTopOfStackTagged();
			}
			break;
		case Opcodes.INEG:
		case Opcodes.FNEG:
		case Opcodes.LNEG:
		case Opcodes.DNEG:
		case Opcodes.I2L:
		case Opcodes.I2F:
		case Opcodes.I2D:
		case Opcodes.L2I:
		case Opcodes.L2F:
		case Opcodes.L2D:
		case Opcodes.F2I:
		case Opcodes.F2L:
		case Opcodes.F2D:
		case Opcodes.D2I:
		case Opcodes.D2L:
		case Opcodes.D2F:
		case Opcodes.I2B:
		case Opcodes.I2C:
		case Opcodes.I2S:
			Configuration.taintTagFactory.stackOp(opcode,mv,lvs,this);
			nextLoadisTracked = false;
			break;
		case Opcodes.LCMP:
		case Opcodes.DCMPL:
		case Opcodes.DCMPG:
		case Opcodes.FCMPL:
		case Opcodes.FCMPG:
//			System.out.println(Printer.OPCODES[opcode] + analyzer.stack + analyzer.stackTagStatus);
			if (!topCarriesTaint()) {
				super.visitInsn(opcode);
				break;
			} else {
				Configuration.taintTagFactory.stackOp(opcode,mv,lvs,this);
				analyzer.setTopOfStackTagged();
			}
			break;
		case Opcodes.DRETURN:
		case Opcodes.LRETURN:
			int retIdx = lvs.getPreAllocedReturnTypeVar(newReturnType);
			
			super.visitVarInsn(ALOAD, retIdx);
			super.visitInsn(DUP_X2);
			super.visitInsn(POP);
			super.visitFieldInsn(PUTFIELD, newReturnType.getInternalName(), "val", originalMethodReturnType.getDescriptor());
			super.visitVarInsn(ALOAD, retIdx);
			super.visitInsn(SWAP);
			super.visitFieldInsn(PUTFIELD, newReturnType.getInternalName(), "taint", Configuration.TAINT_TAG_DESC);
			super.visitVarInsn(ALOAD, retIdx);
			Configuration.taintTagFactory.stackOp(opcode,mv,lvs,this);
			super.visitInsn(ARETURN);
			break;
		case Opcodes.IRETURN:
		case Opcodes.FRETURN:
			//			super.visitMethodInsn(INVOKESTATIC, TaintUtils.getContainerReturnType(originalMethodReturnType).getInternalName(), "valueOf", "(I" + originalMethodReturnType.getDescriptor() + ")"
			//					+ TaintUtils.getContainerReturnType(originalMethodReturnType).getDescriptor());
			retIdx = lvs.getPreAllocedReturnTypeVar(newReturnType);
			super.visitVarInsn(ALOAD, retIdx);
			super.visitInsn(SWAP);
			super.visitFieldInsn(PUTFIELD, newReturnType.getInternalName(), "val", originalMethodReturnType.getDescriptor());
			super.visitVarInsn(ALOAD, retIdx);
			super.visitInsn(SWAP);
			super.visitFieldInsn(PUTFIELD, newReturnType.getInternalName(), "taint", Configuration.TAINT_TAG_DESC);
			super.visitVarInsn(ALOAD, retIdx);
			Configuration.taintTagFactory.stackOp(opcode,mv,lvs,this);
			super.visitInsn(ARETURN);
			break;
		case Opcodes.ARETURN:
			Type onStack = getTopOfStackType();
			if (originalMethodReturnType.getSort() == Type.ARRAY) {
				if (onStack.getSort() == Type.OBJECT) {
					super.visitInsn(opcode);
					return;
				} else if (originalMethodReturnType.getDimensions() > 1 && (onStack.getSort() != Type.ARRAY || onStack.getElementType().getSort() == Type.OBJECT)) {
					super.visitInsn(opcode);
					return;
				}
				switch (originalMethodReturnType.getElementType().getSort()) {
				case Type.INT:
				case Type.LONG:
				case Type.BOOLEAN:
				case Type.BYTE:
				case Type.CHAR:
				case Type.DOUBLE:
				case Type.FLOAT:
				case Type.SHORT:
					registerTaintedArray();
					Configuration.taintTagFactory.stackOp(opcode,mv,lvs,this);
					super.visitInsn(ARETURN);
					break;
				default:
					Configuration.taintTagFactory.stackOp(opcode,mv,lvs,this);
					super.visitInsn(opcode);
				}
			} else if (onStack.getSort() == Type.ARRAY && onStack.getDimensions() == 1 && onStack.getElementType().getSort() != Type.OBJECT) {
				registerTaintedArray();
				Configuration.taintTagFactory.stackOp(opcode,mv,lvs,this);
				super.visitInsn(opcode);
			} else {
				Configuration.taintTagFactory.stackOp(opcode, mv, lvs, this);
				super.visitInsn(opcode);
			}
			break;
		case Opcodes.RETURN:
			Configuration.taintTagFactory.stackOp(opcode,mv,lvs,this);
			super.visitInsn(opcode);
			break;
		case Opcodes.ARRAYLENGTH:
			if(nextLoadisTracked)
			{
				Configuration.taintTagFactory.stackOp(opcode,mv,lvs,this);
				analyzer.setTopOfStackTagged();
				nextLoadisTracked = false;
			}
			else
				super.visitInsn(opcode);
			break;
		case Opcodes.ATHROW:
			if (TaintUtils.DEBUG_FRAMES)
				System.out.println("ATHROW " + analyzer.stack);
			super.visitInsn(opcode);
			break;
		case Opcodes.MONITORENTER:
		case Opcodes.MONITOREXIT:
			//You can have a monitor on an array type. If it's a primitive array type, pop the taint off!
//			else if(getTopOfStackObject().equals("java/lang/Object"))
//			{
//				//never allow monitor to occur on a multid type
//				super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MultiDTaintedArray.class), "unbox1D", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
//			}
			super.visitInsn(opcode);
			break;
		case TaintUtils.FOLLOWED_BY_FRAME:
			super.visitInsn(opcode);
			break;
		default:
			super.visitInsn(opcode);

			throw new IllegalArgumentException();
		}
	}


	@Override
	public void visitJumpInsn(int opcode, Label label) {
		//		System.out.println(Printer.OPCODES[opcode]);
//		System.out.println("PRE" + name + Printer.OPCODES[opcode] + " " + analyzer.stack);

		if (isIgnoreAllInstrumenting) {
			super.visitJumpInsn(opcode, label);
			return;
		}
		
		
		if ((Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_LIGHT_TRACKING) && !isIgnoreAllInstrumenting && !Configuration.WITHOUT_PROPOGATION) {
			if (!boxAtNextJump.isEmpty()) {
				Label origDest = label;
				Label newDest = new Label();
				Label origFalseLoc = new Label();

				Configuration.taintTagFactory.jumpOp(opcode, branchStarting, newDest, mv, lvs, this);

				FrameNode fn = getCurrentFrameNode();
				super.visitJumpInsn(GOTO, origFalseLoc);
				//			System.out.println("taint passing mv monkeying with jump");
				super.visitLabel(newDest);
				fn.accept(this);
				for (Integer var : boxAtNextJump) {
					super.visitVarInsn(ALOAD, lvs.varToShadowVar.get(var));
					super.visitVarInsn(ASTORE, var);
				}
				super.visitJumpInsn(GOTO, origDest);
				super.visitLabel(origFalseLoc);
				fn.accept(this);
				boxAtNextJump.clear();
			}
			else
				Configuration.taintTagFactory.jumpOp(opcode, branchStarting, label, mv, lvs, this);
		}
		else
		{

			if (!boxAtNextJump.isEmpty() && opcode != Opcodes.GOTO) {
				Label origDest = label;
				Label newDest = new Label();
				Label origFalseLoc = new Label();

				Configuration.taintTagFactory.jumpOp(opcode, branchStarting, newDest, mv, lvs, this);
				FrameNode fn = getCurrentFrameNode();
				fn.type = F_NEW;
				super.visitJumpInsn(GOTO, origFalseLoc);
				//			System.out.println("taint passing mv monkeying with jump");
				super.visitLabel(newDest);
				fn.accept(this);
				for (Integer var : boxAtNextJump) {
					if(lvs.varToShadowVar.get(var) != null) {
						super.visitVarInsn(ALOAD, lvs.varToShadowVar.get(var));
						super.visitVarInsn(ASTORE, var);
					}
				}
				super.visitJumpInsn(GOTO, origDest);
				super.visitLabel(origFalseLoc);
				fn.accept(this);
				boxAtNextJump.clear();
			} else {
					Configuration.taintTagFactory.jumpOp(opcode, branchStarting, label, mv, lvs, this);
			}
		}
	}

	@Override
	public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
		if (isIgnoreAllInstrumenting) {
			super.visitTableSwitchInsn(min, max, dflt, labels);
			return;
		}
		//Need to remove taint
		if (TaintUtils.DEBUG_FRAMES)
			System.out.println("Table switch shows: " + analyzer.stack + ", " + analyzer.locals);
		if (Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_LIGHT_TRACKING) {
			super.visitInsn(SWAP);
			super.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
			super.visitInsn(SWAP);
			super.visitVarInsn(ALOAD, controlTaintArray);
			callPushControlTaint(branchStarting);
		}
		Configuration.taintTagFactory.tableSwitch(min, max, dflt, labels, mv, lvs, this);
	}

	@Override
	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
		if (isIgnoreAllInstrumenting) {
			super.visitLookupSwitchInsn(dflt, keys, labels);
			return;
		}
		//Need to remove taint
		if (Configuration.IMPLICIT_TRACKING || Configuration.IMPLICIT_LIGHT_TRACKING) {
			super.visitInsn(SWAP);
			super.visitVarInsn(ALOAD, lvs.getIdxOfMasterControlLV());
			super.visitInsn(SWAP);
			super.visitVarInsn(ALOAD, controlTaintArray);
			callPushControlTaint(branchStarting);
		}
		Configuration.taintTagFactory.lookupSwitch(dflt, keys, labels, mv, lvs, this);
	}

	int argOffset;

	public void setLVOffset(int newArgOffset) {
		this.argOffset = newArgOffset;
	}


	int line;
	@Override
	public void visitLineNumber(int line, Label start) {
		super.visitLineNumber(line, start);
		this.line = line;
		Configuration.taintTagFactory.lineNumberVisited(line);
	}

}
