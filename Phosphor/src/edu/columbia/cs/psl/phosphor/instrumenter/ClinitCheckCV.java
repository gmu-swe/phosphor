package edu.columbia.cs.psl.phosphor.instrumenter;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ClinitCheckCV extends ClassVisitor {
	public ClinitCheckCV(int api, ClassVisitor cv)
	{
		super(api,cv);
	}
	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		if(name.equals("<clinit>") && desc.length()>3)
			throw new IllegalStateException("Desc " + desc + "on clinit!");
//		if(name.equals("defineClass$$PHOSPHORTAGGED"))
//		{
//			System.err.println(name+desc + ", " + signature);
//			System.out.println("Abtract?"+((Opcodes.ACC_ABSTRACT & access) != 0));
//			new Exception().printStackTrace();
//		}
		return super.visitMethod(access, name, desc, signature, exceptions);
	}
}
