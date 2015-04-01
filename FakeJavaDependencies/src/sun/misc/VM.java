package sun.misc;

import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;
import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanWithObjTag;

public class VM {
	public static TaintedBooleanWithIntTag isBooted$$PHOSPHORTAGGED(TaintedBooleanWithIntTag in)
	{
		return in;
	}
	public static TaintedBooleanWithIntTag isBooted$$PHOSPHORTAGGED(ControlTaintTagStack z, TaintedBooleanWithIntTag in)
	{
		return in;
	}
	
	public static TaintedBooleanWithObjTag isBooted$$PHOSPHORTAGGED(TaintedBooleanWithObjTag in)
	{
		return in;
	}
	public static TaintedBooleanWithObjTag isBooted$$PHOSPHORTAGGED(ControlTaintTagStack z, TaintedBooleanWithObjTag in)
	{
		return in;
	}
	public static boolean isBooted()
	{
		return false;
	}
}
