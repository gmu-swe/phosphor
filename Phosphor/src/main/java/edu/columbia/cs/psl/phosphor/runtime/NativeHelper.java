package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.struct.TaggedArray;
import edu.columbia.cs.psl.phosphor.struct.TaggedReferenceArray;
import org.objectweb.asm.Type;

import java.util.Collection;

public final class NativeHelper {

    private NativeHelper() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static Class<?> getClassOrWrapped(Object in) {
        if(in instanceof TaggedReferenceArray) {
            return ((TaggedReferenceArray) in).getUnderlyingClass();
        }
        if(in instanceof TaggedArray) {
            return ((TaggedArray) in).getVal().getClass();
        }
        return in.getClass();
    }

    @SuppressWarnings("rawtypes")
    public static Collection ensureIsBoxedObjTags(Collection in) {
        if(in != null) {
            Collection tmp = null;
            for(Object o : in) {
                if(o == null) {
                    break;
                }
                Type t = Type.getType(o.getClass());
                if(t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT) {
                    if(tmp == null) {
                        try {
                            tmp = in.getClass().getConstructor().newInstance(null);
                        } catch(Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                    tmp.add(MultiDArrayUtils.boxIfNecessary(o));
                } else {
                    break;
                }
            }
            if(tmp != null) {
                in.clear();
                tmp.add(tmp);
            }
        }
        return in;
    }

    public static Collection ensureIsUnBoxedObjTags(Collection in) {
        if(in != null) {
            Collection tmp = null;
            for(Object o : in) {
                if(o != null && MultiDArrayUtils.isPrimitiveBoxClass(o.getClass()) != null) {
                    if(tmp == null) {
                        try {
                            tmp = in.getClass().getConstructor().newInstance(null);
                        } catch(Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                    tmp.add(MultiDArrayUtils.unboxRaw(o));
                } else {
                    break;
                }
            }
            if(tmp != null) {
                in.clear();
                tmp.add(tmp);
            }
        }
        return in;
    }
}
