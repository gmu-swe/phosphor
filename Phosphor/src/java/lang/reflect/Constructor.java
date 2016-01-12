package java.lang.reflect;

import edu.columbia.cs.psl.phosphor.struct.TaintedReturnHolderWithIntTag;

public final class Constructor<T>  {
	public Constructor PHOSPHOR_TAGconstructor;
	public boolean PHOSPHOR_TAGmarked;

	public T newInstance(Object ... initargs)
	        throws InstantiationException, IllegalAccessException,
	               IllegalArgumentException, InvocationTargetException
	    {
	    return null;
	    }
	 public Class<?>[] getParameterTypes() {
	        return null;
	    }
	 public Class getDeclaringClass()
	 {
		 return null;
	 }
	public void setAccessible(boolean b) {
		// TODO Auto-generated method stub
		
	}
	public Class[] getParameterTypes$$PHOSPHORTAGGED(TaintedReturnHolderWithIntTag prealloc) {
		// TODO Auto-generated method stub
		return null;
	}
}
