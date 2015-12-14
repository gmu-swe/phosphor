package edu.columbia.cs.psl.phosphor.runtime;

import inst.CallProfiler;
import sun.misc.VM;
import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanArrayWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanArrayWithSingleObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedByteArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedByteArrayWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedByteArrayWithSingleObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedByteWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedByteWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedCharArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedCharArrayWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedCharArrayWithSingleObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedCharWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedCharWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedDoubleArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedDoubleArrayWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedDoubleArrayWithSingleObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedDoubleWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedDoubleWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedFloatArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedFloatArrayWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedFloatArrayWithSingleObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedFloatWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedFloatWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedIntArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedIntArrayWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedIntArrayWithSingleObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedIntWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedIntWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedLongArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedLongArrayWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedLongArrayWithSingleObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedLongWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedLongWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedReturnHolderWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedReturnHolderWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedReturnHolderWithSingleObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedShortArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedShortArrayWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedShortArrayWithSingleObjTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedShortWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedShortWithObjTag;

public class PreAllocHelper {
	public static final TaintedReturnHolderWithObjTag[] createPreallocReturnArrayMultiTaint()
	{
				return new TaintedReturnHolderWithObjTag[]{
					new TaintedBooleanWithObjTag(),
					new TaintedByteWithObjTag(),
					new TaintedCharWithObjTag(),
					new TaintedDoubleWithObjTag(),
					new TaintedFloatWithObjTag(),
					new TaintedIntWithObjTag(),
					new TaintedLongWithObjTag(),
					new TaintedShortWithObjTag(),
					new TaintedBooleanArrayWithObjTag(),
					new TaintedByteArrayWithObjTag(),
					new TaintedCharArrayWithObjTag(),
					new TaintedDoubleArrayWithObjTag(),
					new TaintedFloatArrayWithObjTag(),
					new TaintedIntArrayWithObjTag(),
					new TaintedLongArrayWithObjTag(),
					new TaintedShortArrayWithObjTag()};
	}
	public static final TaintedReturnHolderWithSingleObjTag[] createPreallocReturnArrayMultiTaintSingleTag()
	{
		
				return new TaintedReturnHolderWithSingleObjTag[]{
					new TaintedBooleanWithObjTag(),
					new TaintedByteWithObjTag(),
					new TaintedCharWithObjTag(),
					new TaintedDoubleWithObjTag(),
					new TaintedFloatWithObjTag(),
					new TaintedIntWithObjTag(),
					new TaintedLongWithObjTag(),
					new TaintedShortWithObjTag(),
					new TaintedBooleanArrayWithSingleObjTag(),
					new TaintedByteArrayWithSingleObjTag(),
					new TaintedCharArrayWithSingleObjTag(),
					new TaintedDoubleArrayWithSingleObjTag(),
					new TaintedFloatArrayWithSingleObjTag(),
					new TaintedIntArrayWithSingleObjTag(),
					new TaintedLongArrayWithSingleObjTag(),
					new TaintedShortArrayWithSingleObjTag()
			};
	}
	public static final TaintedReturnHolderWithIntTag[] createPreallocReturnArray()
	{
		if(VM.booted)
		{
			Object cache = Thread.currentThread().preallocReturns;
			if(cache == null)
			{
				cache = new TaintedReturnHolderWithIntTag[]{
						new TaintedBooleanWithIntTag(),
						new TaintedByteWithIntTag(),
						new TaintedCharWithIntTag(),
						new TaintedDoubleWithIntTag(),
						new TaintedFloatWithIntTag(),
						new TaintedIntWithIntTag(),
						new TaintedLongWithIntTag(),
						new TaintedShortWithIntTag(),
						new TaintedBooleanArrayWithIntTag(),
						new TaintedByteArrayWithIntTag(),
						new TaintedCharArrayWithIntTag(),
						new TaintedDoubleArrayWithIntTag(),
						new TaintedFloatArrayWithIntTag(),
						new TaintedIntArrayWithIntTag(),
						new TaintedLongArrayWithIntTag(),
						new TaintedShortArrayWithIntTag()
				};
			}
//			CallProfiler.logInvoke$$PHOSPHORTAGGED((TaintedReturnHolderWithIntTag[]) cache);

			return (TaintedReturnHolderWithIntTag[]) cache;
		}
		return new TaintedReturnHolderWithIntTag[]{
				new TaintedBooleanWithIntTag(),
				new TaintedByteWithIntTag(),
				new TaintedCharWithIntTag(),
				new TaintedDoubleWithIntTag(),
				new TaintedFloatWithIntTag(),
				new TaintedIntWithIntTag(),
				new TaintedLongWithIntTag(),
				new TaintedShortWithIntTag(),
				new TaintedBooleanArrayWithIntTag(),
				new TaintedByteArrayWithIntTag(),
				new TaintedCharArrayWithIntTag(),
				new TaintedDoubleArrayWithIntTag(),
				new TaintedFloatArrayWithIntTag(),
				new TaintedIntArrayWithIntTag(),
				new TaintedLongArrayWithIntTag(),
				new TaintedShortArrayWithIntTag()
		};
	}
}
