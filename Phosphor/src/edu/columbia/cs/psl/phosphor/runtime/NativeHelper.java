package edu.columbia.cs.psl.phosphor.runtime;

import java.util.Collection;

import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Type;
import edu.columbia.cs.psl.phosphor.struct.TaintedBoolean;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;

public final class NativeHelper {
	public static final void nativeInvoked(String desc)
	{
//		if(VM.isBooted$$PHOSPHORTAGGED(new TaintedBoolean()).val)
//			System.out.println("Invoke native: " + desc);
	}
	public static final void wrapperInvoked(String desc)
	{
//		if(VM.isBooted$$PHOSPHORTAGGED(new TaintedBoolean()).val)
//			System.out.println("Invoke wrapper: " + desc);
	}
	@SuppressWarnings("rawtypes")
	public static final Collection ensureIsBoxed(Collection in)
	{
		if(in != null)
		{
//			if(VM.isBooted())
//			{
//				System.out.println("in:" + in);
//			}
			Collection tmp = null;
			for(Object o : in)
			{
				if(o == null)
					break;
				Type t = Type.getType(o.getClass());
				if(t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT)
				{
					if(tmp == null)
					{
						try{
						tmp = (Collection) in.getClass().getConstructor().newInstance(null);
						}
						catch(Exception ex)
						{
							ex.printStackTrace();
						}
					}
					tmp.add$$PHOSPHORTAGGED(MultiDTaintedArray.boxIfNecessary(o), new TaintedBoolean());
				}
				else
					break;
			}
			if(tmp != null)
			{
				in.clear();
				in.addAll$$PHOSPHORTAGGED(tmp, new TaintedBoolean());
			}
		}
//		if(VM.isBooted())
//		{
//			System.out.println("boxed:" + in);
//		}
		return in;
	}
	public static final Collection ensureIsUnBoxed(Collection in)
	{
//		if(VM.isBooted())
//		{
//			System.out.println("unbox in:" + in);
//		}
		if(in != null)
		{
			Collection tmp = null;
			for(Object o : in)
			{
				if(o != null && MultiDTaintedArray.isPrimitiveBoxClass(o.getClass()) != null)
				{
					if(tmp == null)
					{
						try{
						tmp = (Collection) in.getClass().getConstructor().newInstance(null);
						}
						catch(Exception ex)
						{
							ex.printStackTrace();
						}
					}
					tmp.add$$PHOSPHORTAGGED(MultiDTaintedArray.unboxRaw(o), new TaintedBoolean());
				}
				else
					break;
			}
			if(tmp != null)
			{
				in.clear();
				in.addAll$$PHOSPHORTAGGED(tmp, new TaintedBoolean());
			}
		}
//		if(VM.isBooted())
//		{
//			System.out.println("unbox out:" + in);
//		}
		return in;
	}
}
