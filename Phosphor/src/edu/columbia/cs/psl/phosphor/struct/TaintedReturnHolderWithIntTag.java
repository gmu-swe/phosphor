package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.prof.ILogger;
import sun.misc.VM;

public final class TaintedReturnHolderWithIntTag {
	private TaintedIntWithIntTag i;
	private TaintedIntArrayWithIntTag ia;
	private TaintedByteWithIntTag b;
	private TaintedByteArrayWithIntTag ba;
	private TaintedBooleanWithIntTag z;
	private TaintedBooleanArrayWithIntTag za;
	private TaintedCharWithIntTag c;
	private TaintedCharArrayWithIntTag ca;
	private TaintedDoubleWithIntTag d;
	private TaintedDoubleArrayWithIntTag da;
	private TaintedFloatWithIntTag f;
	private TaintedFloatArrayWithIntTag fa;
	private TaintedShortWithIntTag s;
	private TaintedShortArrayWithIntTag sa;
	private TaintedLongWithIntTag j;
	private TaintedLongArrayWithIntTag ja;

	public static ILogger logger;
	public TaintedReturnHolderWithIntTag(){
		if(logger != null)
			logger.hit$$PHOSPHORTAGGED(this);
	}
	public TaintedIntWithIntTag i()
	{
		if(i == null)
			i = new TaintedIntWithIntTag();
		return i;
	}
	public TaintedIntArrayWithIntTag ia()
	{
		if(ia == null)
			ia = new TaintedIntArrayWithIntTag();
		return ia;
	}
	public synchronized TaintedByteWithIntTag b()
	{
		if(b == null)
			b = new TaintedByteWithIntTag();
		return b;
	}
	public TaintedByteArrayWithIntTag ba()
	{
		if(ba == null)
			ba = new TaintedByteArrayWithIntTag();
		return ba;
	}
	public TaintedCharWithIntTag c()
	{
		if(c == null)
			c = new TaintedCharWithIntTag();
		return c;
	}
	public TaintedCharArrayWithIntTag ca()
	{
		if(ca == null)
			ca = new TaintedCharArrayWithIntTag();
		return ca;
	}
	public TaintedDoubleWithIntTag d()
	{
		if(d == null)
			d = new TaintedDoubleWithIntTag();
		return d;
	}
	public TaintedDoubleArrayWithIntTag da()
	{
		if(da == null)
			da = new TaintedDoubleArrayWithIntTag();
		return da;
	}
	public TaintedFloatWithIntTag f()
	{
		if(f == null)
			f = new TaintedFloatWithIntTag();
		return f;
	}
	public TaintedFloatArrayWithIntTag fa()
	{
		if(fa == null)
			fa = new TaintedFloatArrayWithIntTag();
		return fa;
	}
	public TaintedLongWithIntTag j()
	{
		if(j == null)
			j = new TaintedLongWithIntTag();
		return j;
	}
	public TaintedLongArrayWithIntTag ja()
	{
		if(ja == null)
			ja = new TaintedLongArrayWithIntTag();
		return ja;
	}
	public TaintedShortWithIntTag s()
	{
		if(s == null)
			s = new TaintedShortWithIntTag();
		return s;
	}
	public TaintedShortArrayWithIntTag sa()
	{
		if(sa == null)
			sa = new TaintedShortArrayWithIntTag();
		return sa;
	}
	public TaintedBooleanWithIntTag z()
	{
		if(z == null)
			z = new TaintedBooleanWithIntTag();
		return z;
	}
	public TaintedBooleanArrayWithIntTag za()
	{
		if(za == null)
			za = new TaintedBooleanArrayWithIntTag();
		return za;
	}
	
}
