package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.struct.LinkedList;
import edu.columbia.cs.psl.phosphor.struct.TaintedIntWithIntTag;

public class TaintedWithIntReturnHolderPool {
	static LinkedList<TaintedIntWithIntTag> pool = new LinkedList<TaintedIntWithIntTag>();
	static
	{
		pool.add(new TaintedIntWithIntTag());
		pool.add(new TaintedIntWithIntTag());
		pool.add(new TaintedIntWithIntTag());
		pool.add(new TaintedIntWithIntTag());
		pool.add(new TaintedIntWithIntTag());
		pool.add(new TaintedIntWithIntTag());
	}
	public static TaintedIntWithIntTag getTaintedInt()
	{
//		synchronized (pool) {
//			if(pool.size == 0)
//			{
//				System.out.println("Out of pool");
//				return new TaintedIntWithIntTag();
//			}
//			return pool.pop();			
//		}
		return new TaintedIntWithIntTag();
	}
	public static void releaseTaintedInt(TaintedIntWithIntTag i)
	{
//		synchronized (pool) {
//			pool.addFast(i);
//		}
	}
}
