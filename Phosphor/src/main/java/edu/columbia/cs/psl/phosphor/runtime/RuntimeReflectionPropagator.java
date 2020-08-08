package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.control.ControlFlowStack;
import edu.columbia.cs.psl.phosphor.struct.*;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.WeakHashMap;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArrayWithObjTag;
import org.objectweb.asm.Type;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class RuntimeReflectionPropagator {

    private static Unsafe unsafe;
    private static WeakHashMap<Field, Field> fieldToField = new WeakHashMap<>();

    private RuntimeReflectionPropagator() {
        // Prevents this class from being instantiated
    }

    private static Unsafe getUnsafe() {
        if(unsafe == null) {
            unsafe = Unsafe.getUnsafe();
        }
        return unsafe;
    }

    public static TaintedReferenceWithObjTag getType$$PHOSPHORTAGGED(Field f, Taint fieldtaint, ControlFlowStack ctrl, TaintedReferenceWithObjTag ret, Class erasedReturn) {
        return getType$$PHOSPHORTAGGED(f, fieldtaint, ret, erasedReturn);
    }

    public static TaintedReferenceWithObjTag getType$$PHOSPHORTAGGED(Field f, Taint fieldTaint, TaintedReferenceWithObjTag ret, Class erasedReturn) {
        String name = f.getName();
        ret.taint = fieldTaint;
        if(f.getName().endsWith(TaintUtils.TAINT_FIELD) || f.getName().endsWith(TaintUtils.TAINT_WRAPPER_FIELD) || f.getDeclaringClass().getName().contains("edu.columbia.cs.psl.phosphor")) {
            ret.val = f.getType();
            return ret;
        }
        Class<?> _ret = f.getType();
        Class<?> component = _ret;
        while(component.isArray()) {
            component = component.getComponentType();
        }
        if(LazyArrayObjTags.class.isAssignableFrom(component)) {
            String d = Type.getDescriptor(_ret);

            String newType = "[";
            char[] c = d.toCharArray();
            for(int i = 0; i < c.length && c[i] == '['; i++) {
                newType += "[";
            }
            newType += MultiDTaintedArrayWithObjTag.getPrimitiveTypeForWrapper(component);
            try {
                _ret = Class.forName(newType);
            } catch(ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        ret.val = _ret;
        return ret;
    }

    public static TaintedReferenceWithObjTag get$$PHOSPHORTAGGED(Field f, Taint fieldTaint, Object obj, Taint objTaint, ControlFlowStack ctrl, TaintedReferenceWithObjTag ret, Object erasedReturn) throws IllegalArgumentException, IllegalAccessException {
        if(f.getType().isPrimitive()) {
            return get$$PHOSPHORTAGGED(f, fieldTaint, obj, objTaint, ret, erasedReturn);
        }
        f.setAccessible(true);
        f.get$$PHOSPHORTAGGED(fieldTaint, obj, objTaint, ctrl, ret, erasedReturn);
        return ret;
    }

    public static TaintedReferenceWithObjTag get$$PHOSPHORTAGGED(Field f, Taint fieldTaint, Object obj, Taint objTaint, TaintedReferenceWithObjTag _ret, Object erasedReturn) throws IllegalArgumentException, IllegalAccessException {
        f.setAccessible(true);
        _ret.taint = objTaint;
        if(f.getType().isPrimitive()) {
            if(f.getType() == Boolean.TYPE) {
                TaintedBooleanWithObjTag r = getBoolean$$PHOSPHORTAGGED(f, fieldTaint, obj, objTaint, new TaintedBooleanWithObjTag());
                _ret.taint = r.taint;
                _ret.val = r.val;
            } else if(f.getType() == Byte.TYPE) {
                TaintedByteWithObjTag r = getByte$$PHOSPHORTAGGED(f, fieldTaint, obj, objTaint, new TaintedByteWithObjTag());
                _ret.taint = r.taint;
                _ret.val = r.val;
            } else if(f.getType() == Character.TYPE) {
                TaintedCharWithObjTag r = getChar$$PHOSPHORTAGGED(f, fieldTaint, obj, objTaint, new TaintedCharWithObjTag());
                _ret.taint = r.taint;
                _ret.val = r.val;
            } else if(f.getType() == Double.TYPE) {
                TaintedDoubleWithObjTag r = getDouble$$PHOSPHORTAGGED(f, fieldTaint, obj, objTaint, new TaintedDoubleWithObjTag());
                _ret.taint = r.taint;
                _ret.val = r.val;
            } else if(f.getType() == Float.TYPE) {
                TaintedFloatWithObjTag r = getFloat$$PHOSPHORTAGGED(f, fieldTaint, obj, objTaint, new TaintedFloatWithObjTag());
                _ret.taint = r.taint;
                _ret.val = r.val;
            } else if(f.getType() == Long.TYPE) {
                TaintedLongWithObjTag r = getLong$$PHOSPHORTAGGED(f, fieldTaint, obj, objTaint, new TaintedLongWithObjTag());
                _ret.taint = r.taint;
                _ret.val = r.val;
            } else if(f.getType() == Integer.TYPE) {
                TaintedIntWithObjTag r = getInt$$PHOSPHORTAGGED(f, fieldTaint, obj, objTaint, new TaintedIntWithObjTag());
                _ret.taint = r.taint;
                _ret.val = r.val;
            } else if(f.getType() == Short.TYPE) {
                TaintedShortWithObjTag r = getShort$$PHOSPHORTAGGED(f, fieldTaint, obj, objTaint, new TaintedShortWithObjTag());
                _ret.taint = r.taint;
                _ret.val = r.val;
            } else {
                throw new IllegalArgumentException();
            }
        } else {
            f.get$$PHOSPHORTAGGED(fieldTaint, obj, objTaint, _ret, erasedReturn);
        }
        return _ret;
    }

    public static TaintedBooleanWithObjTag getBoolean$$PHOSPHORTAGGED(Field f, Taint fieldTaint, Object obj, Taint objTaint, ControlFlowStack ctrl, TaintedBooleanWithObjTag ret) throws IllegalArgumentException, IllegalAccessException {
        return getBoolean$$PHOSPHORTAGGED(f, fieldTaint, obj, objTaint, ret);
    }

    public static TaintedByteWithObjTag getByte$$PHOSPHORTAGGED(Field f, Taint fieldTaint, Object obj, Taint objTaint, ControlFlowStack ctrl, TaintedByteWithObjTag ret) throws IllegalArgumentException, IllegalAccessException {
        return getByte$$PHOSPHORTAGGED(f, fieldTaint, obj, objTaint, ret);
    }

    public static TaintedCharWithObjTag getChar$$PHOSPHORTAGGED(Field f, Taint fieldTaint, Object obj, Taint objTaint, ControlFlowStack ctrl, TaintedCharWithObjTag ret) throws IllegalArgumentException, IllegalAccessException {
        return getChar$$PHOSPHORTAGGED(f, fieldTaint, obj, objTaint, ret);
    }

    public static TaintedDoubleWithObjTag getDouble$$PHOSPHORTAGGED(Field f, Taint fieldTaint, Object obj, Taint objTaint, ControlFlowStack ctrl, TaintedDoubleWithObjTag ret) throws IllegalArgumentException, IllegalAccessException {
        return getDouble$$PHOSPHORTAGGED(f, fieldTaint, obj, objTaint, ret);
    }

    public static TaintedFloatWithObjTag getFloat$$PHOSPHORTAGGED(Field f, Taint fieldTaint, Object obj, Taint objTaint, ControlFlowStack ctrl, TaintedFloatWithObjTag ret) throws IllegalArgumentException, IllegalAccessException {
        return getFloat$$PHOSPHORTAGGED(f, fieldTaint, obj, objTaint, ret);
    }

    public static TaintedIntWithObjTag getInt$$PHOSPHORTAGGED(Field f, Taint fieldTaint, Object obj, Taint objTaint, ControlFlowStack ctrl, TaintedIntWithObjTag ret) throws IllegalArgumentException, IllegalAccessException {
        return getInt$$PHOSPHORTAGGED(f, fieldTaint, obj, objTaint, ret);
    }

    public static TaintedLongWithObjTag getLong$$PHOSPHORTAGGED(Field f, Taint fieldTaint, Object obj, Taint objTaint, ControlFlowStack ctrl, TaintedLongWithObjTag ret) throws IllegalArgumentException, IllegalAccessException {
        return getLong$$PHOSPHORTAGGED(f, fieldTaint, obj, objTaint, ret);
    }

    public static TaintedShortWithObjTag getShort$$PHOSPHORTAGGED(Field f, Taint fieldTaint, Object obj, Taint objTaint, ControlFlowStack ctrl, TaintedShortWithObjTag ret) throws IllegalArgumentException, IllegalAccessException {
        return getShort$$PHOSPHORTAGGED(f, fieldTaint, obj, objTaint, ret);
    }

    public static TaintedBooleanWithObjTag getBoolean$$PHOSPHORTAGGED(Field f, Taint fieldTaint, Object obj, Taint objTaint, TaintedBooleanWithObjTag ret) throws IllegalArgumentException, IllegalAccessException {
        f.setAccessible(true);
        ret.val = f.getBoolean(obj);
        try {
            Field taintField;
            if(fieldToField.containsKey(f)) {
                taintField = fieldToField.get(f);
            } else {
                taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
                taintField.setAccessible(true);
                fieldToField.put(f, taintField);
            }
            Object t = taintField.get(obj);
            if(t instanceof Taint) {
                ret.taint = (Taint) t;
            }
            if(t instanceof Integer) {
                ret.taint = (Taint) HardcodedBypassStore.get(((Integer) t).intValue());
            }
        } catch(NoSuchFieldException e) {
            //
        } catch(SecurityException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static TaintedByteWithObjTag getByte$$PHOSPHORTAGGED(Field f, Taint fieldTaint, Object obj, Taint objTaint, TaintedByteWithObjTag ret) throws IllegalArgumentException, IllegalAccessException {
        f.setAccessible(true);
        ret.val = f.getByte(obj);
        try {
            Field taintField;
            if(fieldToField.containsKey(f)) {
                taintField = fieldToField.get(f);
            } else {
                taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
                taintField.setAccessible(true);
                fieldToField.put(f, taintField);
            }
            Object t = taintField.get(obj);
            if(t instanceof Taint) {
                ret.taint = (Taint) t;
            }
            if(t instanceof Integer) {
                ret.taint = (Taint) HardcodedBypassStore.get(((Integer) t).intValue());
            }
        } catch(NoSuchFieldException e) {
            //
        } catch(SecurityException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static TaintedCharWithObjTag getChar$$PHOSPHORTAGGED(Field f, Taint fieldTaint, Object obj, Taint objTaint, TaintedCharWithObjTag ret) throws IllegalArgumentException, IllegalAccessException {
        f.setAccessible(true);
        ret.val = f.getChar(obj);
        try {
            Field taintField;
            if(fieldToField.containsKey(f)) {
                taintField = fieldToField.get(f);
            } else {
                taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
                taintField.setAccessible(true);
                fieldToField.put(f, taintField);
            }
            Object t = taintField.get(obj);
            if(t instanceof Taint) {
                ret.taint = (Taint) t;
            }
            if(t instanceof Integer) {
                ret.taint = (Taint) HardcodedBypassStore.get(((Integer) t).intValue());
            }
        } catch(NoSuchFieldException e) {
            //
        } catch(SecurityException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static TaintedDoubleWithObjTag getDouble$$PHOSPHORTAGGED(Field f, Taint fieldTaint, Object obj, Taint objTaint, TaintedDoubleWithObjTag ret) throws IllegalArgumentException, IllegalAccessException {
        f.setAccessible(true);
        ret.val = f.getDouble(obj);
        try {
            Field taintField;
            if(fieldToField.containsKey(f)) {
                taintField = fieldToField.get(f);
            } else {
                taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
                taintField.setAccessible(true);
                fieldToField.put(f, taintField);
            }
            Object t = taintField.get(obj);
            if(t instanceof Taint) {
                ret.taint = (Taint) t;
            }
            if(t instanceof Integer) {
                ret.taint = (Taint) HardcodedBypassStore.get(((Integer) t).intValue());
            }
        } catch(NoSuchFieldException e) {
            //
        } catch(SecurityException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static TaintedFloatWithObjTag getFloat$$PHOSPHORTAGGED(Field f, Taint fieldTaint, Object obj, Taint objTaint, TaintedFloatWithObjTag ret) throws IllegalArgumentException, IllegalAccessException {
        f.setAccessible(true);
        ret.val = f.getFloat(obj);
        try {
            Field taintField;
            if(fieldToField.containsKey(f)) {
                taintField = fieldToField.get(f);
            } else {
                taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
                taintField.setAccessible(true);
                fieldToField.put(f, taintField);
            }
            Object t = taintField.get(obj);
            if(t instanceof Taint) {
                ret.taint = (Taint) t;
            }
            if(t instanceof Integer) {
                ret.taint = (Taint) HardcodedBypassStore.get(((Integer) t).intValue());
            }
        } catch(NoSuchFieldException e) {
            //
        } catch(SecurityException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static TaintedIntWithObjTag getInt$$PHOSPHORTAGGED(Field f, Taint fieldTaint, Object obj, Taint objTaint, TaintedIntWithObjTag ret) throws IllegalArgumentException, IllegalAccessException {
        f.setAccessible(true);
        ret.val = f.getInt(obj);
        try {
            Field taintField;
            if(fieldToField.containsKey(f)) {
                taintField = fieldToField.get(f);
            } else {
                taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
                taintField.setAccessible(true);
                fieldToField.put(f, taintField);
            }
            Object t = taintField.get(obj);
            if(t instanceof Taint) {
                ret.taint = (Taint) t;
            }
            if(t instanceof Integer) {
                ret.taint = (Taint) HardcodedBypassStore.get(((Integer) t).intValue());
            }

        } catch(SecurityException e) {
            e.printStackTrace();
        } catch(NoSuchFieldException e) {
            //
        }
        return ret;
    }

    public static TaintedLongWithObjTag getLong$$PHOSPHORTAGGED(Field f, Taint fieldTaint, Object obj, Taint objTaint, TaintedLongWithObjTag ret) throws IllegalArgumentException, IllegalAccessException {
        f.setAccessible(true);
        ret.val = f.getLong(obj);
        try {
            Field taintField;
            if(fieldToField.containsKey(f)) {
                taintField = fieldToField.get(f);
            } else {
                taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
                taintField.setAccessible(true);
                fieldToField.put(f, taintField);
            }
            Object t = taintField.get(obj);
            if(t instanceof Taint) {
                ret.taint = (Taint) t;
            }
            if(t instanceof Integer) {
                ret.taint = (Taint) HardcodedBypassStore.get(((Integer) t).intValue());
            }
        } catch(NoSuchFieldException | SecurityException e) {
            //
        }
        return ret;
    }

    public static TaintedShortWithObjTag getShort$$PHOSPHORTAGGED(Field f, Taint fieldTaint, Object obj, Taint objTaint, TaintedShortWithObjTag ret) throws IllegalArgumentException, IllegalAccessException {
        f.setAccessible(true);
        ret.val = f.getShort(obj);
        try {
            Field taintField;
            if(fieldToField.containsKey(f)) {
                taintField = fieldToField.get(f);
            } else {
                taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
                taintField.setAccessible(true);
                fieldToField.put(f, taintField);
            }
            Object t = taintField.get(obj);
            if(t instanceof Taint) {
                ret.taint = (Taint) t;
            }
            if(t instanceof Integer) {
                ret.taint = (Taint) HardcodedBypassStore.get(((Integer) t).intValue());
            }
        } catch(NoSuchFieldException e) {
            //
        } catch(SecurityException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static void setAccessible$$PHOSPHORTAGGED(Field f, Taint<?> tag, boolean flag, Taint<?> flagTag) {
        f.setAccessible(flag);
        if(isPrimitiveOrPrimitiveArrayType(f.getType())) {
            try {
                f.getDeclaringClass().getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD).setAccessible(flag);
            } catch(SecurityException | NoSuchFieldException e) {
                //
            }
        }
    }

    public static void setAccessible$$PHOSPHORTAGGED(Field f, Taint<?> tag, boolean flag, Taint<?> flagTag, ControlFlowStack ctrl) {
        f.setAccessible(flag);
        if(isPrimitiveOrPrimitiveArrayType(f.getType())) {
            try {
                f.getDeclaringClass().getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD).setAccessible(flag);
            } catch(SecurityException | NoSuchFieldException e) {
                //
            }
        }
    }

    public static void setBoolean$$PHOSPHORTAGGED(Field f, Taint fieldTaint, Object obj, Taint objTaint, boolean val, Taint<?> tag, ControlFlowStack ctrl) throws IllegalArgumentException, IllegalAccessException {
        tag = Taint.combineTags(tag, ctrl);
        setBoolean$$PHOSPHORTAGGED(f, fieldTaint, obj, objTaint, val, tag);
    }

    public static void setByte$$PHOSPHORTAGGED(Field f, Taint fieldTaint, Object obj, Taint objTaint, byte val, Taint<?> tag, ControlFlowStack ctrl) throws IllegalArgumentException, IllegalAccessException {
        tag = Taint.combineTags(tag, ctrl);
        setByte$$PHOSPHORTAGGED(f, fieldTaint, obj, objTaint, val, tag);
    }

    public static void setChar$$PHOSPHORTAGGED(Field f, Taint fieldTaint, Object obj, Taint objTaint, char val, Taint<?> tag, ControlFlowStack ctrl) throws IllegalArgumentException, IllegalAccessException {
        tag = Taint.combineTags(tag, ctrl);
        setChar$$PHOSPHORTAGGED(f, fieldTaint, obj, objTaint, val, tag);
    }

    public static void setDouble$$PHOSPHORTAGGED(Field f, Taint fieldTaint, Object obj, Taint objTaint, double val, Taint<?> tag, ControlFlowStack ctrl) throws IllegalArgumentException, IllegalAccessException {
        tag = Taint.combineTags(tag, ctrl);
        setDouble$$PHOSPHORTAGGED(f, fieldTaint, obj, objTaint, val, tag);
    }

    public static void setFloat$$PHOSPHORTAGGED(Field f, Taint fieldTaint, Object obj, Taint objTaint, float val, Taint<?> tag, ControlFlowStack ctrl) throws IllegalArgumentException, IllegalAccessException {
        tag = Taint.combineTags(tag, ctrl);
        setFloat$$PHOSPHORTAGGED(f, fieldTaint, obj, objTaint, val, tag);
    }

    public static void setInt$$PHOSPHORTAGGED(Field f, Taint fieldTaint, Object obj, Taint objTaint, int val, Taint<?> tag, ControlFlowStack ctrl) throws IllegalArgumentException, IllegalAccessException {
        tag = Taint.combineTags(tag, ctrl);
        setInt$$PHOSPHORTAGGED(f, fieldTaint, obj, objTaint, val, tag);
    }

    public static void setLong$$PHOSPHORTAGGED(Field f, Taint fieldTaint, Object obj, Taint objTaint, long val, Taint<?> tag, ControlFlowStack ctrl) throws IllegalArgumentException, IllegalAccessException {
        tag = Taint.combineTags(tag, ctrl);
        setLong$$PHOSPHORTAGGED(f, fieldTaint, obj, objTaint, val, tag);
    }

    public static void setShort$$PHOSPHORTAGGED(Field f, Taint fieldTaint, Object obj, Taint objTaint, short val, Taint<?> tag, ControlFlowStack ctrl) throws IllegalArgumentException, IllegalAccessException {
        tag = Taint.combineTags(tag, ctrl);
        setShort$$PHOSPHORTAGGED(f, fieldTaint, obj, objTaint, val, tag);
    }

    public static void setBoolean$$PHOSPHORTAGGED(Field f, Taint fieldTaint, Object obj, Taint objTaint, int tag, boolean val) throws IllegalArgumentException, IllegalAccessException {
        f.setAccessible(true);
        f.setBoolean(obj, val);
        try {
            Field taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
            taintField.setAccessible(true);
            taintField.setInt(obj, tag);
        } catch(NoSuchFieldException e) {
            //
        } catch(SecurityException e) {
            e.printStackTrace();
        }
    }

    public static void setByte$$PHOSPHORTAGGED(Field f, Taint fieldTaint, Object obj, Taint objTaint, int tag, byte val) throws IllegalArgumentException, IllegalAccessException {
        f.setAccessible(true);
        f.setByte(obj, val);
        try {
            Field taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
            taintField.setAccessible(true);
            taintField.setInt(obj, tag);
        } catch(NoSuchFieldException e) {
            //
        } catch(SecurityException e) {
            e.printStackTrace();
        }
    }

    public static void setChar$$PHOSPHORTAGGED(Field f, Taint fieldTaint, Object obj, Taint objTaint, int tag, char val) throws IllegalArgumentException, IllegalAccessException {
        f.setAccessible(true);
        f.setChar(obj, val);
        try {
            Field taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
            taintField.setAccessible(true);
            taintField.setInt(obj, tag);
        } catch(NoSuchFieldException e) {
            //
        } catch(SecurityException e) {
            e.printStackTrace();
        }
    }

    public static void setDouble$$PHOSPHORTAGGED(Field f, Taint fieldTaint, Object obj, Taint objTaint, int tag, double val) throws IllegalArgumentException, IllegalAccessException {
        f.setAccessible(true);
        f.setDouble(obj, val);
        try {
            Field taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
            taintField.setAccessible(true);
            taintField.setInt(obj, tag);
        } catch(NoSuchFieldException e) {
            //
        } catch(SecurityException e) {
            e.printStackTrace();
        }
    }

    public static void setFloat$$PHOSPHORTAGGED(Field f, Taint fieldTaint, Object obj, Taint objTaint, int tag, float val) throws IllegalArgumentException, IllegalAccessException {
        f.setAccessible(true);
        f.setFloat(obj, val);
        try {
            Field taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
            taintField.setAccessible(true);
            taintField.setInt(obj, tag);
        } catch(NoSuchFieldException e) {
            //
        } catch(SecurityException e) {
            e.printStackTrace();
        }
    }

    public static void setInt$$PHOSPHORTAGGED(Field f, Taint fieldTaint, Object obj, Taint objTaint, int tag, int val) throws IllegalArgumentException, IllegalAccessException {
        f.setAccessible(true);
        f.setInt(obj, val);
        try {
            Field taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
            taintField.setAccessible(true);
            taintField.setInt(obj, tag);
        } catch(NoSuchFieldException e) {
            //
        } catch(SecurityException e) {
            e.printStackTrace();
        }
    }

    public static void setLong$$PHOSPHORTAGGED(Field f, Taint fieldTaint, Object obj, Taint objTaint, int tag, long val) throws IllegalArgumentException, IllegalAccessException {
        f.setAccessible(true);
        f.setLong(obj, val);
        try {
            Field taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
            taintField.setAccessible(true);
            taintField.setInt(obj, tag);
        } catch(NoSuchFieldException e) {
            //
        } catch(SecurityException e) {
            e.printStackTrace();
        }
    }

    public static void setShort$$PHOSPHORTAGGED(Field f, Taint fieldTaint, Object obj, Taint objTaint, int tag, short val) throws IllegalArgumentException, IllegalAccessException {
        f.setAccessible(true);
        f.setShort(obj, val);
        try {
            Field taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
            taintField.setAccessible(true);
            taintField.setInt(obj, tag);
        } catch(NoSuchFieldException e) {
            //
        } catch(SecurityException e) {
            e.printStackTrace();
        }
    }

    public static void setBoolean$$PHOSPHORTAGGED(Field f, Taint fieldTaint, Object obj, Taint objTaint, boolean val, Taint<?> tag) throws IllegalArgumentException, IllegalAccessException {
        f.setAccessible(true);
        f.setBoolean(obj, val);
        try {
            Field taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
            taintField.setAccessible(true);
            taintField.set(obj, tag);
        } catch(NoSuchFieldException e) {
            //
        } catch(SecurityException e) {
            e.printStackTrace();
        }
    }

    public static void setByte$$PHOSPHORTAGGED(Field f, Taint fieldTaint, Object obj, Taint objTaint, byte val, Taint<?> tag) throws IllegalArgumentException, IllegalAccessException {
        f.setAccessible(true);
        f.setByte(obj, val);
        try {
            Field taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
            taintField.setAccessible(true);
            taintField.set(obj, tag);
        } catch(NoSuchFieldException e) {
            //
        } catch(SecurityException e) {
            e.printStackTrace();
        }
    }

    public static void setChar$$PHOSPHORTAGGED(Field f, Taint fieldTaint, Object obj, Taint objTaint, char val, Taint<?> tag) throws IllegalArgumentException, IllegalAccessException {
        f.setAccessible(true);
        f.setChar(obj, val);
        try {
            Field taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
            taintField.setAccessible(true);
            taintField.set(obj, tag);
        } catch(NoSuchFieldException e) {
            //
        } catch(SecurityException e) {
            e.printStackTrace();
        }
    }

    public static void setDouble$$PHOSPHORTAGGED(Field f, Taint fieldTaint, Object obj, Taint objTaint, double val, Taint<?> tag) throws IllegalArgumentException, IllegalAccessException {
        f.setAccessible(true);
        f.setDouble(obj, val);
        try {
            Field taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
            taintField.setAccessible(true);
            taintField.set(obj, tag);
        } catch(NoSuchFieldException e) {
            //
        } catch(SecurityException e) {
            e.printStackTrace();
        }
    }

    public static void setFloat$$PHOSPHORTAGGED(Field f, Taint fieldTaint, Object obj, Taint objTaint, float val, Taint<?> tag) throws IllegalArgumentException, IllegalAccessException {
        f.setAccessible(true);
        f.setFloat(obj, val);
        try {
            Field taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
            taintField.setAccessible(true);
            taintField.set(obj, tag);
        } catch(NoSuchFieldException e) {
            //
        } catch(SecurityException e) {
            e.printStackTrace();
        }
    }

    public static void setInt$$PHOSPHORTAGGED(Field f, Taint fieldTaint, Object obj, Taint objTaint, int val, Taint<?> tag) throws IllegalArgumentException, IllegalAccessException {
        f.setAccessible(true);
        f.setInt(obj, val);
        try {
            Field taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
            taintField.setAccessible(true);
            taintField.set(obj, tag);
        } catch(NoSuchFieldException e) {
            //
        } catch(SecurityException e) {
            e.printStackTrace();
        }
    }

    public static void setLong$$PHOSPHORTAGGED(Field f, Taint fieldTaint, Object obj, Taint objTaint, long val, Taint<?> tag) throws IllegalArgumentException, IllegalAccessException {
        f.setAccessible(true);
        f.setLong(obj, val);
        try {
            Field taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
            taintField.setAccessible(true);
            taintField.set(obj, tag);
        } catch(NoSuchFieldException e) {
            //
        } catch(SecurityException e) {
            e.printStackTrace();
        }
    }

    public static void setShort$$PHOSPHORTAGGED(Field f, Taint fieldTaint, Object obj, Taint objTaint, short val, Taint<?> tag) throws IllegalArgumentException, IllegalAccessException {
        f.setAccessible(true);
        f.setShort(obj, val);
        try {
            Field taintField = f.getDeclaringClass().getField(f.getName() + TaintUtils.TAINT_FIELD);
            taintField.setAccessible(true);
            taintField.set(obj, tag);
        } catch(NoSuchFieldException e) {
            //
        } catch(SecurityException e) {
            e.printStackTrace();
        }
    }

    private static Taint getTagObj(Object val) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        Object ret = val.getClass().getField("value" + TaintUtils.TAINT_FIELD).get(val);
        if(ret instanceof Integer) {
            ret = HardcodedBypassStore.get(((Integer) ret).intValue());
        }
        return (Taint) ret;
    }

    private static int getTag(Object val) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        return val.getClass().getField("value" + TaintUtils.TAINT_FIELD).getInt(val);
    }

    public static void set$$PHOSPHORTAGGED(Field f, Taint fieldTaint, Object obj, Taint objTaint, Object val, Taint valTaint, ControlFlowStack ctrl) throws IllegalArgumentException, IllegalAccessException {
        if(f.getType().isPrimitive()) {
            if(val instanceof Integer && f.getType().equals(Integer.TYPE)) {
                Integer i = (Integer) val;
                f.setAccessible(true);
                f.setInt(obj, i.intValue());
                try {
                    Field tf = f.getDeclaringClass().getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD);
                    tf.setAccessible(true);
                    tf.set(obj, Taint.combineTags(getTagObj(val), ctrl));
                } catch(NoSuchFieldException | SecurityException e) {
                    e.printStackTrace();
                }
                return;
            } else if(val instanceof Boolean && f.getType().equals(Boolean.TYPE)) {
                Boolean i = (Boolean) val;
                f.setAccessible(true);
                f.setBoolean(obj, i.booleanValue());
                return;
            } else if(val instanceof Byte && f.getType().equals(Byte.TYPE)) {
                Byte i = (Byte) val;
                f.setAccessible(true);
                f.setByte(obj, i.byteValue());
                try {
                    Field tf = f.getDeclaringClass().getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD);
                    tf.setAccessible(true);
                    tf.set(obj, Taint.combineTags(getTagObj(val), ctrl));
                } catch(SecurityException | NoSuchFieldException e) {
                    e.printStackTrace();
                }
                return;
            } else if(val instanceof Character && f.getType().equals(Character.TYPE)) {
                Character i = (Character) val;
                f.setAccessible(true);
                f.setChar(obj, i.charValue());
                try {
                    Field tf = f.getDeclaringClass().getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD);
                    tf.setAccessible(true);
                    tf.set(obj, Taint.combineTags(getTagObj(val), ctrl));
                } catch(SecurityException | NoSuchFieldException e) {
                    e.printStackTrace();
                }
                return;
            } else if(val instanceof Double && f.getType().equals(Double.TYPE)) {
                Double i = (Double) val;
                f.setAccessible(true);
                f.setDouble(obj, i.doubleValue());
                try {
                    Field tf = f.getDeclaringClass().getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD);
                    tf.setAccessible(true);
                    tf.set(obj, Taint.combineTags(getTagObj(val), ctrl));
                } catch(SecurityException | NoSuchFieldException e) {
                    e.printStackTrace();
                }
                return;
            } else if(val instanceof Float && f.getType().equals(Float.TYPE)) {
                Float i = (Float) val;
                f.setAccessible(true);
                f.setFloat(obj, i.floatValue());
                try {
                    Field tf = f.getDeclaringClass().getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD);
                    tf.setAccessible(true);
                    tf.set(obj, Taint.combineTags(getTagObj(val), ctrl));
                } catch(SecurityException | NoSuchFieldException e) {
                    e.printStackTrace();
                }
                return;
            } else if(val instanceof Long && f.getType().equals(Long.TYPE)) {
                Long i = (Long) val;
                f.setAccessible(true);
                f.setLong(obj, i.longValue());
                try {
                    Field tf = f.getDeclaringClass().getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD);
                    tf.setAccessible(true);
                    tf.set(obj, Taint.combineTags(getTagObj(val), ctrl));
                } catch(SecurityException | NoSuchFieldException e) {
                    e.printStackTrace();
                }
                return;
            } else if(val instanceof Short && f.getType().equals(Short.TYPE)) {
                Short i = (Short) val;
                f.setAccessible(true);
                f.setShort(obj, i.shortValue());
                try {
                    Field tf = f.getDeclaringClass().getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD);
                    tf.setAccessible(true);
                    tf.set(obj, Taint.combineTags(getTagObj(val), ctrl));
                } catch(SecurityException | NoSuchFieldException e) {
                    e.printStackTrace();
                }
                return;
            }
        }
        Taint.combineTagsOnObject(obj, ctrl);
        f.setAccessible(true);
        f.set(obj, val);
        if(f.getType().isArray() && val instanceof LazyArrayObjTags) {
            try {
                Field taintField = f.getDeclaringClass().getDeclaredField(f.getName() + TaintUtils.TAINT_WRAPPER_FIELD);
                Unsafe u = getUnsafe();
                u.putObject(obj, u.objectFieldOffset(taintField), val);
            } catch(NoSuchFieldException | SecurityException e) {
                //
            }
        }
    }

    public static void set$$PHOSPHORTAGGED(Field f, Taint fieldTaint, Object obj, Taint objTaint, Object val, Taint valTaint) throws IllegalArgumentException, IllegalAccessException {
        if(f.getType().isPrimitive()) {
            if(val instanceof Integer && f.getType().equals(Integer.TYPE)) {
                Integer i = (Integer) val;
                f.setAccessible(true);
                f.setInt(obj, i.intValue());
                try {
                    Field tf = f.getDeclaringClass().getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD);
                    tf.setAccessible(true);
                    tf.set(obj, valTaint);
                } catch(NoSuchFieldException | SecurityException e) {
                    e.printStackTrace();
                }
                return;
            } else if(val instanceof Boolean && f.getType().equals(Boolean.TYPE)) {
                Boolean i = (Boolean) val;
                f.setAccessible(true);
                f.setBoolean(obj, i.booleanValue());
                try {
                    Field tf = f.getDeclaringClass().getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD);
                    tf.setAccessible(true);
                    tf.set(obj, valTaint);
                } catch(NoSuchFieldException | SecurityException e) {
                    e.printStackTrace();
                }
                return;
            } else if(val instanceof Byte && f.getType().equals(Byte.TYPE)) {
                Byte i = (Byte) val;
                f.setAccessible(true);
                f.setByte(obj, i.byteValue());
                try {
                    Field tf = f.getDeclaringClass().getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD);
                    tf.setAccessible(true);
                    tf.set(obj, valTaint);
                } catch(NoSuchFieldException | SecurityException e) {
                    e.printStackTrace();
                }
                return;
            } else if(val instanceof Character && f.getType().equals(Character.TYPE)) {
                Character i = (Character) val;
                f.setAccessible(true);
                f.setChar(obj, i.charValue());
                try {
                    Field tf = f.getDeclaringClass().getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD);
                    tf.setAccessible(true);
                    tf.set(obj, valTaint);
                } catch(NoSuchFieldException | SecurityException e) {
                    e.printStackTrace();
                }
                return;
            } else if(val instanceof Double && f.getType().equals(Double.TYPE)) {
                Double i = (Double) val;
                f.setAccessible(true);
                f.setDouble(obj, i.doubleValue());
                try {
                    Field tf = f.getDeclaringClass().getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD);
                    tf.setAccessible(true);
                    tf.set(obj, valTaint);
                } catch(NoSuchFieldException | SecurityException e) {
                    e.printStackTrace();
                }
                return;
            } else if(val instanceof Float && f.getType().equals(Float.TYPE)) {
                Float i = (Float) val;
                f.setAccessible(true);
                f.setFloat(obj, i.floatValue());
                try {
                    Field tf = f.getDeclaringClass().getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD);
                    tf.setAccessible(true);
                    tf.set(obj, valTaint);
                } catch(NoSuchFieldException | SecurityException e) {
                    e.printStackTrace();
                }
                return;
            } else if(val instanceof Long && f.getType().equals(Long.TYPE)) {
                Long i = (Long) val;
                f.setAccessible(true);
                f.setLong(obj, i.longValue());
                try {
                    Field tf = f.getDeclaringClass().getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD);
                    tf.setAccessible(true);
                    tf.set(obj, valTaint);
                } catch(NoSuchFieldException | SecurityException e) {
                    e.printStackTrace();
                }
                return;
            } else if(val instanceof Short && f.getType().equals(Short.TYPE)) {
                Short i = (Short) val;
                f.setAccessible(true);
                f.setShort(obj, i.shortValue());
                try {
                    Field tf = f.getDeclaringClass().getDeclaredField(f.getName() + TaintUtils.TAINT_FIELD);
                    tf.setAccessible(true);
                    tf.set(obj, valTaint);
                } catch(NoSuchFieldException | SecurityException e) {
                    e.printStackTrace();
                }
                return;
            }
        }
        f.setAccessible(true);
        f.set(obj, val);
        if(f.getType().isArray() && val instanceof LazyArrayObjTags) {
            try {
                Field taintField = f.getDeclaringClass().getDeclaredField(f.getName() + TaintUtils.TAINT_WRAPPER_FIELD);
                Unsafe u = getUnsafe();
                if(Modifier.isStatic(f.getModifiers())) {
                    u.putObject(u.staticFieldBase(taintField), u.staticFieldOffset(taintField), val);
                } else {
                    u.putObject(obj, u.objectFieldOffset(taintField), val);
                }
            } catch(NoSuchFieldException | SecurityException e) {
                //
            }
        }
    }

    private static int getNumberOfDimensions(Class<?> clazz) {
        String name = clazz.getName();
        return name.lastIndexOf('[') + 1;
    }

    private static boolean isPrimitiveOrPrimitiveArrayType(Class<?> clazz) {
        if(clazz.isPrimitive()) {
            return true;
        }
        if(clazz.isArray() && getNumberOfDimensions(clazz) == 1) {
            return isPrimitiveOrPrimitiveArrayType(clazz.getComponentType());
        }
        return false;
    }
}
