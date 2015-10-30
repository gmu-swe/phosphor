package edu.columbia.cs.psl.phosphor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class PartialInstrumentationInferencerMV extends MethodVisitor {

	MethodDescriptor desc;
	Map<MethodDescriptor, List<MethodDescriptor>> map = new HashMap<MethodDescriptor, List<MethodDescriptor>>();
	List<String> superClass;
	
	public PartialInstrumentationInferencerMV(int api, MethodDescriptor desc, MethodVisitor next, Map<MethodDescriptor, List<MethodDescriptor>> map, List<String> superClass) {
		super(api, next);
		this.desc = desc;
		this.map = map;
		this.superClass = superClass;
	}
	
	@Override
	public void visitMethodInsn(int opcode, String owner, String name,
			String desc, boolean itf) {
		if(name.equals("getResourceAsStream") && owner.equals("java/lang/Class")) {
			if (TaintUtils.DEBUG_CALLS)
				System.out.println("[PTI] adding " + this.desc);
			//AdditionalMethodsToTaint.methodsWithGetAsSrtream.add(this.desc);
			SelectiveInstrumentationManager.methodsToInstrument.add(this.desc);
			for(String spr : superClass)
				SelectiveInstrumentationManager.methodsToInstrument.add(new MethodDescriptor(this.desc.getName(), spr, this.desc.getDesc()));
		}
		MethodDescriptor caller = this.desc;
		MethodDescriptor callee = new MethodDescriptor(name, owner, desc);
		
		Type calleeType = Type.getReturnType(desc);
		Type[] argTypes = Type.getArgumentTypes(desc);
		
		if(SelectiveInstrumentationManager.methodsToInstrument.contains(caller) 
				&& !SelectiveInstrumentationManager.methodsToInstrument.contains(callee)) {
			for(Type t : argTypes) {
				if((t.getSort() == Type.ARRAY && t.getDimensions() > 1) || t.getDescriptor().equals("Ljava/lang/Object;")) {
					SelectiveInstrumentationManager.methodsToInstrument.add(callee);
					for(String spr : superClass)
						SelectiveInstrumentationManager.methodsToInstrument.add(new MethodDescriptor(callee.getName(), spr, callee.getDesc()));
					break;
				}
			}
		}
		
		if(SelectiveInstrumentationManager.methodsToInstrument.contains(caller) 
				&& !callee.getOwner().startsWith("java/") && !callee.getOwner().startsWith("sun/") 
				&& !callee.getOwner().startsWith("javax/")) {
			if(calleeType.getSort() == Type.ARRAY && calleeType.getDimensions() > 1 
					&& !SelectiveInstrumentationManager.methodsToInstrument.contains(callee)) {
				if (TaintUtils.DEBUG_CALLS)
					System.out.println("[PTI] adding " + callee);
	//			if(PartialInstrumentationInferencerCV.classesSeenTillNow.contains(callee.getOwner()))
	//				System.out.println("Noooooo " + callee);
				SelectiveInstrumentationManager.methodsToInstrument.add(callee);
				for(String spr : superClass)
					SelectiveInstrumentationManager.methodsToInstrument.add(new MethodDescriptor(callee.getName(), spr, callee.getDesc()));
			}
			
		}
		map.get(this.desc).add(new MethodDescriptor(name, owner, desc));
		super.visitMethodInsn(opcode, owner, name, desc, itf);
	}
	
	@Override
	public void visitFieldInsn(int opcode, String owner, String name,
			String desc) {
		Type fieldType = Type.getType(desc);
		
		FieldDescriptor fdesc = new FieldDescriptor(name, owner, desc);
		if((opcode == Opcodes.PUTFIELD || opcode == Opcodes.PUTSTATIC) 
				&& fieldType.getSort() == Type.ARRAY && fieldType.getDimensions() == 1) {
			// check if field in current class
			//if(singledim_array_fields.contains(fdesc) || PartialInstrumentationInferencerCV.singledim_array_fields_non_private.contains(fdesc)) {
				if (TaintUtils.DEBUG_CALLS)
					System.out.println("[PTI] adding " + this.desc);
				//AdditionalMethodsToTaint.methodsAccessingMultiDimensionalArrays.add(this.desc);
				SelectiveInstrumentationManager.methodsToInstrument.add(this.desc);
				for(String spr : superClass)
					SelectiveInstrumentationManager.methodsToInstrument.add(new MethodDescriptor(this.desc.getName(), spr, this.desc.getDesc()));
			//}
		}

		// TODO test if the second case works
		/*
		 * putfield 
		 * putstatic
		 * getfield
		 * getstatic
		 */
		if(fieldType.getSort() == Type.ARRAY && fieldType.getDimensions() > 1)  {
			if (TaintUtils.DEBUG_CALLS)
				System.out.println("[PTI] adding " + this.desc);
			//AdditionalMethodsToTaint.methodsAccessingMultiDimensionalArrays.add(this.desc);
			SelectiveInstrumentationManager.methodsToInstrument.add(this.desc);
			for(String spr : superClass)
				SelectiveInstrumentationManager.methodsToInstrument.add(new MethodDescriptor(this.desc.getName(), spr, this.desc.getDesc()));
		}
		super.visitFieldInsn(opcode, owner, name, desc);
	}
}
