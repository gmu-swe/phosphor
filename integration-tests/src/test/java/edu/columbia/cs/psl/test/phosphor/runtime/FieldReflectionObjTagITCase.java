package edu.columbia.cs.psl.test.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.test.phosphor.BaseMultiTaintClass;

import java.lang.reflect.Field;
import java.util.LinkedList;

public class FieldReflectionObjTagITCase extends MaskingBaseTest {

    /* Uses reflection to set the specified object's fields. */
    @Override
    public void setFields(Object obj, Field[] fields, boolean taint) throws IllegalAccessException {
        for(Field field : fields) {
            if(boolean.class.equals(field.getType())) {
                field.setBoolean(obj, supplier.getBoolean(taint));
            } else if(byte.class.equals(field.getType())) {
                field.setByte(obj, supplier.getByte(taint));
            } else if(char.class.equals(field.getType())) {
                field.setChar(obj, supplier.getChar(taint));
            } else if(double.class.equals(field.getType())) {
                field.setDouble(obj, supplier.getDouble(taint));
            } else if(float.class.equals(field.getType())) {
                field.setFloat(obj, supplier.getFloat(taint));
            } else if(int.class.equals(field.getType())) {
                field.setInt(obj, supplier.getInt(taint));
            } else if(long.class.equals(field.getType())) {
                field.setLong(obj, supplier.getLong(taint));
            } else if(short.class.equals(field.getType())) {
                field.setShort(obj, supplier.getShort(taint));
            } else if(field.getType().isArray()) {
                field.set(obj, supplier.getArray(taint, field.getType()));
            } else {
                // Cycle through primitive array classes
                Class<?> next = primArrayTypes.pop();
                field.set(obj, supplier.getArray(taint, next));
                primArrayTypes.add(next);
            }
        }
    }

    /* Uses reflection to get the specified object's fields. */
    @Override
    public void checkFields(Object obj, Field[] fields, boolean tainted) throws IllegalAccessException {
        for(Field field : fields) {
            Taint t;
            if(boolean.class.equals(field.getType())) {
                t = MultiTainter.getTaint(field.getBoolean(obj));
            } else if(byte.class.equals(field.getType())) {
                t = MultiTainter.getTaint(field.getByte(obj));
            } else if(char.class.equals(field.getType())) {
                t = MultiTainter.getTaint(field.getChar(obj));
            } else if(double.class.equals(field.getType())) {
                t = MultiTainter.getTaint(field.getDouble(obj));
            } else if(float.class.equals(field.getType())) {
                t = MultiTainter.getTaint(field.getFloat(obj));
            } else if(int.class.equals(field.getType())) {
                t = MultiTainter.getTaint(field.getInt(obj));
            } else if(long.class.equals(field.getType())) {
                t = MultiTainter.getTaint(field.getLong(obj));
            } else if(short.class.equals(field.getType())) {
                t = MultiTainter.getTaint(field.getShort(obj));
            } else {
                t = MultiTainter.getMergedTaint(field.get(obj));
            }
            if(tainted) {
                BaseMultiTaintClass.assertNonNullTaint(t);
            } else {
                BaseMultiTaintClass.assertNullOrEmpty(t);
            }
        }
    }
}
