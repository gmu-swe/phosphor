package edu.columbia.cs.psl.test.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.test.phosphor.BaseMultiTaintClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;

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
    
    /* Sets the specified Object's value at the specified offset to be a new value of the specified Class that is tainted
     * if taint is true. */
    public void setValue(Object obj, long offset, boolean taint, Class<?> clazz) {
        if (boolean.class.equals(clazz)) {
            unsafe.putBoolean(obj, offset, supplier.getBoolean(taint));
        } else if (byte.class.equals(clazz)) {
            unsafe.putByte(obj, offset, supplier.getByte(taint));
        } else if (char.class.equals(clazz)) {
            unsafe.putChar(obj, offset, supplier.getChar(taint));
        } else if (double.class.equals(clazz)) {
            unsafe.putDouble(obj, offset, supplier.getDouble(taint));
        } else if (float.class.equals(clazz)) {
            unsafe.putFloat(obj, offset, supplier.getFloat(taint));
        } else if (int.class.equals(clazz)) {
            unsafe.putInt(obj, offset, supplier.getInt(taint));
        } else if (long.class.equals(clazz)) {
            unsafe.putLong(obj, offset, supplier.getLong(taint));
        } else if (short.class.equals(clazz)) {
            unsafe.putShort(obj, offset, supplier.getShort(taint));
        } else if (clazz.isArray()) {
            unsafe.putObject(obj, offset, supplier.getArray(taint, clazz));
        } else {
            // Cycle through primitive array classes
            Class<?> next = primArrayTypes.pop();
            unsafe.putObject(obj, offset, supplier.getArray(taint, next));
            primArrayTypes.add(next);
        }
    }

    /* Uses Unsafe to set the specified object's fields. */
    @Override
    public void setFields(Object obj, Field[] fields, boolean taint) {
        for(Field field : fields) {
            setValue(obj, unsafe.objectFieldOffset(field), taint, field.getType());
        }
    }

    /* Gets the specified Object's value of the specified type at the specified offset and checks if it is tainted. */
    public void checkValue(Object obj, long offset, boolean tainted, Class<?> clazz) {
        Taint t;
        if(boolean.class.equals(clazz)) {
            t = MultiTainter.getTaint(unsafe.getBoolean(obj, offset));
        } else if(byte.class.equals(clazz)) {
            t = MultiTainter.getTaint(unsafe.getByte(obj, offset));
        } else if(char.class.equals(clazz)) {
            t = MultiTainter.getTaint(unsafe.getChar(obj, offset));
        } else if(double.class.equals(clazz)) {
            t = MultiTainter.getTaint(unsafe.getDouble(obj, offset));
        } else if(float.class.equals(clazz)) {
            t = MultiTainter.getTaint(unsafe.getFloat(obj, offset));
        } else if(int.class.equals(clazz)) {
            t = MultiTainter.getTaint(unsafe.getInt(obj, offset));
        } else if(long.class.equals(clazz)) {
            t = MultiTainter.getTaint(unsafe.getLong(obj, offset));
        } else if(short.class.equals(clazz)) {
            t = MultiTainter.getTaint(unsafe.getShort(obj, offset));
        } else {
            t = MultiTainter.getMergedTaint(unsafe.getObject(obj, offset));
        }
        if(tainted) {
            BaseMultiTaintClass.assertNonNullTaint(t);
        } else {
            BaseMultiTaintClass.assertNullOrEmpty(t);
        }
    }

    /* Uses Unsafe to get the specified object's fields. */
    @Override
    public void checkFields(Object obj, Field[] fields, boolean tainted) {
        for(Field field : fields) {
            checkValue(obj, unsafe.objectFieldOffset(field), tainted, field.getType());
        }
    }

    @Test(timeout = 3000L)
    public void testCAS2DCharArray(){
        Object[] ar = new Object[2];
        char[] charArr = new char[10];
        ar[0] = charArr;
        unsafe.getAndSetObject(ar,unsafe.arrayBaseOffset(Object[].class), null);
    }

    /* Checks that setting primitive arrays' non-tainted elements to tainted values results in the primitive arrays' elements
     * being tainted. */
    @Test
    public void testSetTaintedPrimitiveArrayElement() {
        for(Class<?> clazz : primArrayTypes) {
            long baseOffset = unsafe.arrayBaseOffset(clazz);
            Object arr = supplier.getArray(false, clazz);
            setValue(arr, baseOffset, true, clazz.getComponentType());
            assertNonNullTaint(MultiTainter.getMergedTaint(arr));
        }
    }

    /* Checks that setting primitive arrays' tainted elements to non-tainted values results in the primitive arrays' elements
     * being non-tainted. */
    @Test
    public void testSetNonTaintedPrimitiveArrayElement() {
        for(Class<?> clazz : primArrayTypes) {
            long baseOffset = unsafe.arrayBaseOffset(clazz);
            Object arr = supplier.getArray(true, clazz);
            setValue(arr, baseOffset, false, clazz.getComponentType());
            assertNullOrEmpty(MultiTainter.getMergedTaint(arr));
        }
    }

    /* Checks that getting primitive arrays' tainted elements return tainted primitives. */
    @Test
    public void testGetTaintedPrimitiveArrayElement() {
        for(Class<?> clazz : primArrayTypes) {
            long baseOffset = unsafe.arrayBaseOffset(clazz);
            Object arr = supplier.getArray(true, clazz);
            checkValue(arr, baseOffset, true, clazz.getComponentType());
        }
    }

    /* Checks that getting primitive arrays' non-tainted elements return non-tainted primitives. */
    @Test
    public void testGetNonTaintedPrimitiveArrayElement() {
        for(Class<?> clazz : primArrayTypes) {
            long baseOffset = unsafe.arrayBaseOffset(clazz);
            Object arr = supplier.getArray(false, clazz);
            checkValue(arr, baseOffset, false, clazz.getComponentType());
        }
    }

    /* Creates a PrimitiveArrayHolder with fields that are tainted if tainted is false and tries to set the holder's fields
     * to new arrays whose elements are tainted if tainted is true using compareAndSwapObject. The swap should be successful,
     * i.e. the comparison should match, only if swap is true. */
    private void checkCompareAndSwapArrays(boolean tainted, boolean swap) throws NoSuchFieldException {
        PrimitiveArrayHolder holder = new PrimitiveArrayHolder(!tainted);
        Field[] fields = PrimitiveArrayHolder.fields();
        assertEquals(swap, unsafe.compareAndSwapObject(holder, unsafe.objectFieldOffset(fields[0]), swap ? holder.getIa() : null, supplier.getIntArray(tainted, 1)));
        assertEquals(swap, unsafe.compareAndSwapObject(holder, unsafe.objectFieldOffset(fields[1]), swap ? holder.getJa() : null, supplier.getLongArray(tainted, 1)));
        assertEquals(swap, unsafe.compareAndSwapObject(holder, unsafe.objectFieldOffset(fields[2]), swap ? holder.getZa() : null, supplier.getBooleanArray(tainted, 1)));
        assertEquals(swap, unsafe.compareAndSwapObject(holder, unsafe.objectFieldOffset(fields[3]), swap ? holder.getSa() : null, supplier.getShortArray(tainted, 1)));
        assertEquals(swap, unsafe.compareAndSwapObject(holder, unsafe.objectFieldOffset(fields[4]), swap ? holder.getDa() : null, supplier.getDoubleArray(tainted, 1)));
        assertEquals(swap, unsafe.compareAndSwapObject(holder, unsafe.objectFieldOffset(fields[5]), swap ? holder.getBa() : null, supplier.getByteArray(tainted, 1)));
        assertEquals(swap, unsafe.compareAndSwapObject(holder, unsafe.objectFieldOffset(fields[6]), swap ? holder.getCa() : null, supplier.getCharArray(tainted, 1)));
        assertEquals(swap, unsafe.compareAndSwapObject(holder, unsafe.objectFieldOffset(fields[7]), swap ? holder.getFa() : null, supplier.getFloatArray(tainted, 1)));
        if((tainted && swap) || (!tainted && !swap)) {
            holder.checkFieldsAreTainted();
        } else {
            holder.checkFieldsAreNotTainted();
        }
    }

    /* Checks that using Unsafe.compareAndSwapObject to potentially update primitive array fields updates the primitive array's
     * taint tags only if the swap occurs. */
    @Test
    public void testCompareAndSwapObjectArrayFields() throws NoSuchFieldException {
        // Check successfully swapping arrays with tainted elements
        checkCompareAndSwapArrays(true, true);
        // Check successfully swapping arrays with non-tainted elements
        checkCompareAndSwapArrays(false, true);
        // Check unsuccessfully swapping arrays with tainted elements
        checkCompareAndSwapArrays(true, false);
        // Check unsuccessfully swapping arrays with non-tainted elements
        checkCompareAndSwapArrays(false, false);
    }

    /* Creates a PrimitiveHolder with fields that are tainted if tainted is false and tries to set the holder's int field
     * to a new int that is tainted if tainted is true using compareAndSwapInt. The swap should be successful,
     * i.e. the comparison should match, only if swap is true. */
    private void checkCompareAndSwapInt(boolean tainted, boolean swap) throws NoSuchFieldException {
        PrimitiveHolder holder = new PrimitiveHolder(!tainted);
        long offset =  unsafe.objectFieldOffset(PrimitiveHolder.fields()[0]);
        assertEquals(swap, unsafe.compareAndSwapInt(holder, offset, swap ? holder.getI() : holder.getI() - 1, supplier.getInt(tainted)));
        if((tainted && swap) || (!tainted && !swap)) {
            assertNonNullTaint(MultiTainter.getTaint(holder.getI()));
        } else {
            assertNullOrEmpty(MultiTainter.getTaint(holder.getI()));
        }
    }

    /* Checks that using Unsafe.compareAndSwapInt to potentially update an int field updates the int field's taint tag only
     * if the swap occurs. */
    @Test
    public void testCompareAndSwapIntField() throws NoSuchFieldException {
        // Check successfully swapping tainted int
        checkCompareAndSwapInt(true, true);
        // Check successfully swapping non-tainted int
        checkCompareAndSwapInt(false, true);
        // Check unsuccessfully swapping tainted int
        checkCompareAndSwapInt(true, false);
        // Check unsuccessfully swapping non-tainted int
        checkCompareAndSwapInt(false, false);
    }

    /* Creates a PrimitiveHolder with fields that are tainted if tainted is false and tries to set the holder's long field
     * to a new long that is tainted if tainted is true using compareAndSwapLong. The swap should be successful,
     * i.e. the comparison should match, only if swap is true. */
    private void checkCompareAndSwapLong(boolean tainted, boolean swap) throws NoSuchFieldException {
        PrimitiveHolder holder = new PrimitiveHolder(!tainted);
        long offset = unsafe.objectFieldOffset(PrimitiveHolder.fields()[1]);
        assertEquals(swap, unsafe.compareAndSwapLong(holder, offset, swap ? holder.getJ() : holder.getJ() - 1, supplier.getLong(tainted)));
        if((tainted && swap) || (!tainted && !swap)) {
            assertNonNullTaint(MultiTainter.getTaint(holder.getJ()));
        } else {
            assertNullOrEmpty(MultiTainter.getTaint(holder.getJ()));
        }
    }

    /* Checks that using Unsafe.compareAndSwapLong to potentially update a long field updates the int field's taint tag only
     * if the swap occurs. */
    @Test
    public void testCompareAndSwapLongField() throws NoSuchFieldException {
        // Check successfully swapping tainted long
        checkCompareAndSwapLong(true, true);
        // Check successfully swapping non-tainted long
        checkCompareAndSwapLong(false, true);
        // Check unsuccessfully swapping tainted long
        checkCompareAndSwapLong(true, false);
        // Check unsuccessfully swapping non-tainted long
        checkCompareAndSwapLong(false, false);
    }

    /* Creates a int array whose elements are tainted if tainted is false and tries to set the first element of the array
     * to a new int that is tainted if tainted is true using compareAndSwapInt. The swap should be successful,
     * i.e. the comparison should match, only if swap is true. */
    private void checkCompareAndSwapIntArrayElement(boolean tainted, boolean swap)  {
        int[] arr = (int[])supplier.getIntArray(!tainted, 1);
        long offset = Unsafe.ARRAY_INT_BASE_OFFSET;
        assertEquals(swap, unsafe.compareAndSwapInt(arr, offset, swap ? arr[0] : arr[0] - 1 , supplier.getInt(tainted)));
        if((tainted && swap) || (!tainted && !swap)) {
            assertNonNullTaint(MultiTainter.getTaint(arr[0]));
        } else {
            assertNullOrEmpty(MultiTainter.getTaint(arr[0]));
        }
    }

    /* Checks that using Unsafe.compareAndSwapInt to potentially update an element of a int array updates the element's
     * taint tag only if the swap occurs. */
    @Test
    public void testCompareAndSwapIntArrayElement() {
        // Check successfully swapping tainted int array
        checkCompareAndSwapIntArrayElement(true, true);
        // Check successfully swapping non-tainted int array
        checkCompareAndSwapIntArrayElement(false, true);
        // Check unsuccessfully swapping tainted int array
        checkCompareAndSwapIntArrayElement(true, false);
        // Check unsuccessfully swapping non-tainted int array
        checkCompareAndSwapIntArrayElement(false, false);
    }


    /* Creates a long array whose elements are tainted if tainted is false and tries to set the first element of the array
     * to a new long that is tainted if tainted is true using compareAndSwapLong. The swap should be successful,
     * i.e. the comparison should match, only if swap is true. */
    private void checkCompareAndSwapLongArrayElement(boolean tainted, boolean swap)  {
        long[] arr = (long[])supplier.getLongArray(!tainted, 1);
        long offset = Unsafe.ARRAY_LONG_BASE_OFFSET;
        assertEquals(swap, unsafe.compareAndSwapLong(arr, offset, swap ? arr[0] : arr[0] - 1 , supplier.getLong(tainted)));
        if((tainted && swap) || (!tainted && !swap)) {
            assertNonNullTaint(MultiTainter.getTaint(arr[0]));
        } else {
            assertNullOrEmpty(MultiTainter.getTaint(arr[0]));
        }
    }

    /* Checks that using Unsafe.compareAndSwapLong to potentially update an element of a long array updates the element's
     * taint tag only if the swap occurs. */
    @Test
    public void testCompareAndSwapLongArrayElement() {
        // Check successfully swapping tainted long array
        checkCompareAndSwapLongArrayElement(true, true);
        // Check successfully swapping non-tainted long array
        checkCompareAndSwapLongArrayElement(false, true);
        // Check unsuccessfully swapping tainted long array
        checkCompareAndSwapLongArrayElement(true, false);
        // Check unsuccessfully swapping non-tainted long array
        checkCompareAndSwapLongArrayElement(false, false);
    }
}
