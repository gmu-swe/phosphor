package sun.misc;

import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;
import edu.columbia.cs.psl.phosphor.struct.TaintedBoolean;

public class VM {
	public static TaintedBoolean isBooted$$PHOSPHORTAGGED(TaintedBoolean in)
	{
		return in;
	}
	public static TaintedBoolean isBooted$$PHOSPHORTAGGED(ControlTaintTagStack z, TaintedBoolean in)
	{
		return in;
	}
	public static boolean isBooted()
	{
		return false;
	}
}
