package edu.columbia.cs.psl.phosphor.struct.multid;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import edu.columbia.cs.psl.phosphor.runtime.Taint;
import org.objectweb.asm.Type;

import sun.security.pkcs11.wrapper.CK_ATTRIBUTE;
import edu.columbia.cs.psl.phosphor.struct.LazyArrayIntTags;
import edu.columbia.cs.psl.phosphor.struct.LazyArrayObjTags;

import static edu.columbia.cs.psl.phosphor.Configuration.MULTI_TAINTING;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Opcodes.T_LONG;

public abstract class MultiDTaintedArray {

    public static CK_ATTRIBUTE[] unboxCK_ATTRIBUTE(CK_ATTRIBUTE[] in) throws NoSuchFieldException, SecurityException, ClassNotFoundException, IllegalArgumentException, IllegalAccessException {
		if(in == null || in[0] == null) {
			return null;
		}
        boolean needsFix = false;
        Field f = in[0].getClass().getDeclaredField("pValue");
        for(Object a : in) {
            Object v = f.get(a);
			if(v instanceof LazyArrayIntTags) {
				f.set(a, MultiDTaintedArrayWithIntTag.unboxRaw(v));
			} else if(v instanceof LazyArrayObjTags) {
				f.set(a, MultiDTaintedArrayWithObjTag.unboxRaw(v));
			}
        }
        return in;
    }

    public static Object unbox1D(final Object in) {
		if(in instanceof LazyArrayObjTags) {
			return ((LazyArrayObjTags) in).getVal();
		} else if(in instanceof LazyArrayIntTags) {
			return ((LazyArrayIntTags) in).getVal();
		}
        return in;
    }

    /* If the specified object is a one dimensional array of primitives, boxes and returns the specified object. Otherwise
     * returns the specified object. */
    public static Object boxOnly1D(final Object obj) {
        if(MULTI_TAINTING) {
            return MultiDTaintedArrayWithObjTag.boxOnly1D(obj);
        } else {
            return MultiDTaintedArrayWithIntTag.boxOnly1D(obj);
        }
    }

    public static Object maybeUnbox(final Object in) {
		if(in == null) {
			return null;
		}
		if(null != isPrimitiveBoxClass(in.getClass())) {
			return unboxRaw(in);
		}
        return in;
    }

    public static Type getTypeForType(final Type originalElementType) {
		if(!MULTI_TAINTING) {
			return MultiDTaintedArrayWithIntTag.getTypeForType(originalElementType);
		}
        return MultiDTaintedArrayWithObjTag.getTypeForType(originalElementType);
    }

    public static String isPrimitiveBoxClass(Class c) {
		if(!MULTI_TAINTING) {
			return MultiDTaintedArrayWithIntTag.isPrimitiveBoxClass(c);
		} else {
			return MultiDTaintedArrayWithObjTag.isPrimitiveBoxClass(c);
		}
    }

    public static String getPrimitiveTypeForWrapper(Class c) {
		if(!MULTI_TAINTING) {
			return MultiDTaintedArrayWithIntTag.getPrimitiveTypeForWrapper(c);
		} else {
			return MultiDTaintedArrayWithObjTag.getPrimitiveTypeForWrapper(c);
		}

    }

    public static Class getUnderlyingBoxClassForUnderlyingClass(Class c) {
		if(!MULTI_TAINTING) {
			return MultiDTaintedArrayWithIntTag.getUnderlyingBoxClassForUnderlyingClass(c);
		} else {
			return MultiDTaintedArrayWithObjTag.getUnderlyingBoxClassForUnderlyingClass(c);
		}

    }

    public static Class getClassForComponentType(final int componentSort) {
		if(!MULTI_TAINTING) {
			return MultiDTaintedArrayWithIntTag.getClassForComponentType(componentSort);
		} else {
			return MultiDTaintedArrayWithObjTag.getClassForComponentType(componentSort);
		}
    }

    public static Object unboxRaw(final Object in) {
		if(!MULTI_TAINTING) {
			return MultiDTaintedArrayWithIntTag.unboxRaw(in);
		} else {
			return MultiDTaintedArrayWithObjTag.unboxRaw(in);
		}
    }

    public static Object unboxVal(final Object _in, final int componentType, final int dims) {
		if(!MULTI_TAINTING) {
			return MultiDTaintedArrayWithIntTag.unboxVal(_in, componentType, dims);
		} else {
			return MultiDTaintedArrayWithObjTag.unboxVal(_in, componentType, dims);
		}
    }

    public static int getSortForBoxClass(Class c) {
		if(!MULTI_TAINTING) {
			return MultiDTaintedArrayWithIntTag.getSortForBoxClass(c);
		} else {
			return MultiDTaintedArrayWithObjTag.getSortForBoxClass(c);
		}
    }

    public static int getSort(Class c) {
		if(!MULTI_TAINTING) {
			return MultiDTaintedArrayWithIntTag.getSort(c);
		} else {
			return MultiDTaintedArrayWithObjTag.getSort(c);
		}

    }

    public static Object boxIfNecessary(final Object in) {
		if(!MULTI_TAINTING) {
			return MultiDTaintedArrayWithIntTag.boxIfNecessary(in);
		} else {
			return MultiDTaintedArrayWithObjTag.boxIfNecessary(in);
		}
    }

    public static Object initWithEmptyTaints(final Object[] ar, final int componentType, final int dims) {
		if(!MULTI_TAINTING) {
			return MultiDTaintedArrayWithIntTag.initWithEmptyTaints(ar, componentType, dims);
		} else {
			return MultiDTaintedArrayWithObjTag.initWithEmptyTaints(ar, componentType, dims);
		}
    }

    @SuppressWarnings("unused")
    public static void initLastDim(final Object[] ar, final int lastDimSize, final int componentType) {
		if(!MULTI_TAINTING) {
			MultiDTaintedArrayWithIntTag.initLastDim(ar, lastDimSize, componentType);
		} else {
			MultiDTaintedArrayWithObjTag.initLastDim(ar, lastDimSize, componentType);
		}
    }

    @SuppressWarnings("unused")
	public static void initLastDim(final Object[] ar, final Taint<?> dimTaint, final int lastDimSize, final int componentType) {
    	MultiDTaintedArrayWithObjTag.initLastDim(ar, dimTaint, lastDimSize, componentType);
	}

    public static Type getPrimitiveTypeForWrapper(String internalName) {
		if(!MULTI_TAINTING) {
			return MultiDTaintedArrayWithIntTag.getPrimitiveTypeForWrapper(internalName);
		} else {
			return MultiDTaintedArrayWithObjTag.getPrimitiveTypeForWrapper(internalName);
		}
    }

    @SuppressWarnings("unused")
    public static Object unboxMethodReceiverIfNecessary(Method m, Object obj) {
        if(LazyArrayObjTags.class.isAssignableFrom(m.getDeclaringClass()) || LazyArrayIntTags.class.isAssignableFrom(m.getDeclaringClass())) {
            return obj; // No unboxing is necessary
        } else {
            return unboxRaw(obj);
        }
    }

    /**
     * @param typeOperand the type operand of a NEWARRAY instruction
     * @return the internal name of the taint array type associated with the specified type operand
     * @throws IllegalArgumentException if the specified int is not a type operand
     */
    public static String getTaintArrayInternalName(int typeOperand) {
        return MULTI_TAINTING ? MultiDTaintedArrayWithObjTag.getTaintArrayInternalName(typeOperand) :
                MultiDTaintedArrayWithIntTag.getTaintArrayInternalName(typeOperand);
    }

	/**
	 * @param typeOperand the type operand of a NEWARRAY instruction
	 * @return the descriptor of the array type associated with the specified type operand
	 * @throws IllegalArgumentException if the specified int is not a type operand
	 */
	public static String getArrayDescriptor(int typeOperand) {
		switch(typeOperand) {
			case T_BOOLEAN:
				return "[Z";
			case T_INT:
				return "[I";
			case T_BYTE:
				return "[B";
			case T_CHAR:
				return "[C";
			case T_DOUBLE:
				return "[D";
			case T_FLOAT:
				return "[F";
			case T_LONG:
				return "[J";
			case T_SHORT:
				return "[S";
			default:
				throw new IllegalArgumentException();
		}
	}
}
