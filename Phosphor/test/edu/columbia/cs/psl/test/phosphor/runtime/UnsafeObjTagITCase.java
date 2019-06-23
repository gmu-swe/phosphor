package edu.columbia.cs.psl.test.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.test.phosphor.BaseMultiTaintClass;
import org.junit.ClassRule;
import org.junit.rules.ExternalResource;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class UnsafeObjTagITCase extends MaskingBaseTest {

    private static Unsafe unsafe;

    @ClassRule
    public static ExternalResource resource= new ExternalResource() {
        @Override
        protected void before() throws Throwable {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            unsafe = (Unsafe)unsafeField.get(null);
        }
    };

    /* Uses Unsafe to set the specified object's fields. */
    @Override
    public void setFields(Object obj, Field[] fields, boolean taint) {
        for(Field field : fields) {
            if(boolean.class.equals(field.getType())) {
                unsafe.putBoolean(obj, unsafe.objectFieldOffset(field), supplier.getBoolean(taint));
            } else if(byte.class.equals(field.getType())) {
                unsafe.putByte(obj, unsafe.objectFieldOffset(field), supplier.getByte(taint));
            } else if(char.class.equals(field.getType())) {
                unsafe.putChar(obj, unsafe.objectFieldOffset(field), supplier.getChar(taint));
            } else if(double.class.equals(field.getType())) {
                unsafe.putDouble(obj, unsafe.objectFieldOffset(field), supplier.getDouble(taint));
            } else if(float.class.equals(field.getType())) {
                unsafe.putFloat(obj, unsafe.objectFieldOffset(field), supplier.getFloat(taint));
            } else if(int.class.equals(field.getType())) {
                unsafe.putInt(obj, unsafe.objectFieldOffset(field), supplier.getInt(taint));
            } else if(long.class.equals(field.getType())) {
                unsafe.putLong(obj, unsafe.objectFieldOffset(field), supplier.getLong(taint));
            } else if(short.class.equals(field.getType())) {
                unsafe.putShort(obj, unsafe.objectFieldOffset(field), supplier.getShort(taint));
            } else if(field.getType().isArray()) {
                unsafe.putObject(obj, unsafe.objectFieldOffset(field), supplier.getArray(taint, field.getType()));
            } else {
                // Cycle through primitive array classes
                Class<?> next = primArrayTypes.pop();
                unsafe.putObject(obj, unsafe.objectFieldOffset(field), supplier.getArray(taint, next));
                primArrayTypes.add(next);
            }
        }
    }

    /* Uses reflection to get the specified object's fields. */
    @Override
    public void checkFields(Object obj, Field[] fields, boolean tainted) {
        for(Field field : fields) {
            Taint t;
            if(boolean.class.equals(field.getType())) {
                t = MultiTainter.getTaint(unsafe.getBoolean(obj, unsafe.objectFieldOffset(field)));
            } else if(byte.class.equals(field.getType())) {
                t = MultiTainter.getTaint(unsafe.getByte(obj, unsafe.objectFieldOffset(field)));
            } else if(char.class.equals(field.getType())) {
                t = MultiTainter.getTaint(unsafe.getChar(obj, unsafe.objectFieldOffset(field)));
            } else if(double.class.equals(field.getType())) {
                t = MultiTainter.getTaint(unsafe.getDouble(obj, unsafe.objectFieldOffset(field)));
            } else if(float.class.equals(field.getType())) {
                t = MultiTainter.getTaint(unsafe.getFloat(obj, unsafe.objectFieldOffset(field)));
            } else if(int.class.equals(field.getType())) {
                t = MultiTainter.getTaint(unsafe.getInt(obj, unsafe.objectFieldOffset(field)));
            } else if(long.class.equals(field.getType())) {
                t = MultiTainter.getTaint(unsafe.getLong(obj, unsafe.objectFieldOffset(field)));
            } else if(short.class.equals(field.getType())) {
                t = MultiTainter.getTaint(unsafe.getShort(obj, unsafe.objectFieldOffset(field)));
            } else {
                t = MultiTainter.getMergedTaint(unsafe.getObject(obj, unsafe.objectFieldOffset(field)));
            }
            if(tainted) {
                BaseMultiTaintClass.assertNonNullTaint(t);
            } else {
                BaseMultiTaintClass.assertNullOrEmpty(t);
            }
        }
    }
}
