package edu.columbia.cs.psl.phosphor.struct.multid;

import java.lang.reflect.Field;

import org.objectweb.asm.Type;

import sun.security.pkcs11.wrapper.CK_ATTRIBUTE;
import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.struct.LazyArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.LazyArrayObjTags;

public abstract class MultiDTaintedArray {

	public static final CK_ATTRIBUTE[] unboxCK_ATTRIBUTE(CK_ATTRIBUTE[] in) throws NoSuchFieldException, SecurityException, ClassNotFoundException, IllegalArgumentException, IllegalAccessException {
		if (in == null || in[0] == null)
			return null;
		boolean needsFix = false;
		Field f = in[0].getClass().getDeclaredField("pValue");
		for(Object a : in)
		{
			Object v = f.get(a);
			if(v instanceof LazyArrayIntTags)
				f.set(a, MultiDTaintedArrayWithIntTag.unboxRaw(v));
			else if(v instanceof LazyArrayObjTags)
				f.set(a, MultiDTaintedArrayWithObjTag.unboxRaw(v));
		}
		return in;
	}
	public static final Object unbox1D(final Object in)
	{
		if(in instanceof LazyArrayObjTags)
			return ((LazyArrayObjTags) in).getVal();
		else if(in instanceof LazyArrayIntTags)
			return ((LazyArrayIntTags) in).getVal();
		return in;
	}
	public static final Object maybeUnbox(final Object in)
	{
		if(in == null)
			return null;
		if(null != isPrimitiveBoxClass(in.getClass()))
			return unboxRaw(in);
		return in;
	}
	public static final Type getTypeForType(final Type originalElementType) {
		if (!Configuration.MULTI_TAINTING)
			return MultiDTaintedArrayWithIntTag.getTypeForType(originalElementType);
		return MultiDTaintedArrayWithObjTag.getTypeForType(originalElementType);
	}

	public static final String isPrimitiveBoxClass(Class c) {
		if (!Configuration.MULTI_TAINTING)
			return MultiDTaintedArrayWithIntTag.isPrimitiveBoxClass(c);
		else
			return MultiDTaintedArrayWithObjTag.isPrimitiveBoxClass(c);
	}

	public static final String getPrimitiveTypeForWrapper(Class c) {
		if (!Configuration.MULTI_TAINTING)
			return MultiDTaintedArrayWithIntTag.getPrimitiveTypeForWrapper(c);
		else
			return MultiDTaintedArrayWithObjTag.getPrimitiveTypeForWrapper(c);

	}

	public static final Class getUnderlyingBoxClassForUnderlyingClass(Class c) {
		if (!Configuration.MULTI_TAINTING)
			return MultiDTaintedArrayWithIntTag.getUnderlyingBoxClassForUnderlyingClass(c);
		else
			return MultiDTaintedArrayWithObjTag.getUnderlyingBoxClassForUnderlyingClass(c);

	}

	public static final Class getClassForComponentType(final int componentSort) {
		if (!Configuration.MULTI_TAINTING)
			return MultiDTaintedArrayWithIntTag.getClassForComponentType(componentSort);
		else
			return MultiDTaintedArrayWithObjTag.getClassForComponentType(componentSort);
	}

	public static final Object unboxRaw(final Object in) {
		if (!Configuration.MULTI_TAINTING)
			return MultiDTaintedArrayWithIntTag.unboxRaw(in);
		else
			return MultiDTaintedArrayWithObjTag.unboxRaw(in);
	}

	public static final Object unboxVal(final Object _in, final int componentType, final int dims) {
		if (!Configuration.MULTI_TAINTING)
			return MultiDTaintedArrayWithIntTag.unboxVal(_in, componentType, dims);
		else
			return MultiDTaintedArrayWithObjTag.unboxVal(_in, componentType, dims);
	}

	public static int getSortForBoxClass(Class c) {
		if (!Configuration.MULTI_TAINTING)
			return MultiDTaintedArrayWithIntTag.getSortForBoxClass(c);
		else
			return MultiDTaintedArrayWithObjTag.getSortForBoxClass(c);
	}

	public static int getSort(Class c) {
		if (!Configuration.MULTI_TAINTING)
			return MultiDTaintedArrayWithIntTag.getSort(c);
		else
			return MultiDTaintedArrayWithObjTag.getSort(c);

	}

	public static final Object boxIfNecessary(final Object in) {
		if (!Configuration.MULTI_TAINTING)
			return MultiDTaintedArrayWithIntTag.boxIfNecessary(in);
		else
			return MultiDTaintedArrayWithObjTag.boxIfNecessary(in);
	}

	public static final Object initWithEmptyTaints(final Object[] ar, final int componentType, final int dims) {
		if (!Configuration.MULTI_TAINTING)
			return MultiDTaintedArrayWithIntTag.initWithEmptyTaints(ar, componentType, dims);
		else
			return MultiDTaintedArrayWithObjTag.initWithEmptyTaints(ar, componentType, dims);
	}

	public static final void initLastDim(final Object[] ar, final int lastDimSize, final int componentType) {
		if (!Configuration.MULTI_TAINTING)
			MultiDTaintedArrayWithIntTag.initLastDim(ar, lastDimSize, componentType);
		else
			MultiDTaintedArrayWithObjTag.initLastDim(ar, lastDimSize, componentType);

	}

	public static Type getPrimitiveTypeForWrapper(String internalName) {
		if (!Configuration.MULTI_TAINTING)
			return MultiDTaintedArrayWithIntTag.getPrimitiveTypeForWrapper(internalName);
		else
			return MultiDTaintedArrayWithObjTag.getPrimitiveTypeForWrapper(internalName);

	}
}
