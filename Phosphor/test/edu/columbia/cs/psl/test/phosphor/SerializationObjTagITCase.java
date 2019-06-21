package edu.columbia.cs.psl.test.phosphor;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;


public class SerializationObjTagITCase extends BaseMultiTaintClass {

    public static class ArrayHolderObjField implements Serializable {
        private static final long serialVersionUID = 2710507305915502837L;
        private Object arr;

        public ArrayHolderObjField(Object arr) {
            this.arr = arr;
        }

        private void writeObject(ObjectOutputStream oos) throws IOException {

            oos.defaultWriteObject();
        }
    }

    public static class ArrayHolder implements Serializable {
        private static final long serialVersionUID = 7515681563331750671L;
        private byte[] arr;
        public ArrayHolder(byte[] arr) {
            this.arr = arr;
        }

        public byte[] getArr() {
            return arr;
        }
    }

    public static class ArrayHolderChild extends ArrayHolder {
        private static final long serialVersionUID = 10585365320351952L;
        public ArrayHolderChild(byte[] arr) {
            super(arr);
        }
    }

    public static class PrimitiveHolder implements  Serializable {
        private static final long serialVersionUID = -7447282366633906624L;
        private int x;
        public PrimitiveHolder(int x) {
            this.x = x;
        }
    }

    /* Checks that when an object with a tainted primitive array field is serialized and then deserialized the primitive
     * array of the deserialized object is also tainted. */
    @Test
    public void testSerializeObjectWithTaintedPrimitiveArrayField() throws Exception {
        byte[] arr = new byte[3];
        for(int i = 0; i < arr.length; i++) {
            arr[i] = MultiTainter.taintedByte((byte)i, "label");
        }
        ArrayHolder holderInput = new ArrayHolder(arr);
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream outStream = new ObjectOutputStream(byteStream);
        outStream.writeObject(holderInput);
        outStream.close();
        ArrayHolder holderOutput = (ArrayHolder) new ObjectInputStream(new ByteArrayInputStream(byteStream.toByteArray())).readObject();
        assertArrayEquals(holderInput.arr, holderOutput.arr);
        for(int el : holderOutput.arr) {
            assertNonNullTaint(MultiTainter.getTaint(el));
        }
    }

    /* Checks that when an object whose superclass has a tainted primitive array field is serialized and then deserialized the primitive
     * array of the deserialized object is also tainted. */
    @Test
    public void testSerializeSubclassObjectWithTaintedPrimitiveArrayField() throws Exception {
        byte[] arr = new byte[3];
        for(int i = 0; i < arr.length; i++) {
            arr[i] = MultiTainter.taintedByte((byte)i, "label");
        }
        ArrayHolderChild holderInput = new ArrayHolderChild(arr);
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream outStream = new ObjectOutputStream(byteStream);
        outStream.writeObject(holderInput);
        outStream.close();
        ArrayHolderChild holderOutput = (ArrayHolderChild) new ObjectInputStream(new ByteArrayInputStream(byteStream.toByteArray())).readObject();
        assertArrayEquals(holderInput.getArr(), holderOutput.getArr());
        for(int el : holderOutput.getArr()) {
            assertNonNullTaint(MultiTainter.getTaint(el));
        }
    }

    public void testSerializeObjectWithTaintedPrimitiveArrayInObjectField() throws Exception {
        int[] arr = new int[3];
        for(int i = 0; i < arr.length; i++) {
            arr[i] = MultiTainter.taintedInt(i, "label");
        }
        ArrayHolderObjField holderInput = new ArrayHolderObjField(arr);
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream outStream = new ObjectOutputStream(byteStream);
        outStream.writeObject(holderInput);
        outStream.close();
        ArrayHolderObjField holderOutput = (ArrayHolderObjField) new ObjectInputStream(new ByteArrayInputStream(byteStream.toByteArray())).readObject();
        assertArrayEquals((int[]) holderInput.arr, (int[]) holderOutput.arr);
        for (int el : ((int[]) (holderOutput.arr))) {
            assertNonNullTaint(MultiTainter.getTaint(el));
        }
    }

    /* Checks that when an object with a tainted primitive field is serialized and then deserialized the primitive of the
     * deserialized object is also tainted. */
    @Test
    public void testSerializeObjectWithTaintedPrimitiveField() throws Exception {
        PrimitiveHolder holderInput = new PrimitiveHolder(MultiTainter.taintedInt(37, "label"));
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream outStream = new ObjectOutputStream(byteStream);
        outStream.writeObject(holderInput);
        outStream.close();
        PrimitiveHolder holderOutput = (PrimitiveHolder) new ObjectInputStream(new ByteArrayInputStream(byteStream.toByteArray())).readObject();
        assertEquals(holderInput.x, holderOutput.x);
        assertNonNullTaint(MultiTainter.getTaint(holderOutput.x));
    }

    /* Checks that when a tainted primitive array field is serialized and then deserialized the deserialized primitive
     * array is also tainted. */
    @Test
    public void testSerializeTaintedPrimitiveArray() throws Exception {
        int[] inputArr = new int[3];
        for(int i = 0; i < inputArr.length; i++) {
            inputArr[i] = MultiTainter.taintedInt(i, "label");
        }
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream outStream = new ObjectOutputStream(byteStream);
        outStream.writeObject(inputArr);
        outStream.close();
        int[] outputArr = (int[]) new ObjectInputStream(new ByteArrayInputStream(byteStream.toByteArray())).readObject();
        assertArrayEquals(inputArr, outputArr);
        for(int el : outputArr) {
            assertNonNullTaint(MultiTainter.getTaint(el));
        }
    }

    /* Checks that when a tainted primitive is serialized and then deserialized the deserialized primitive is also tainted. */
    @Test
    public void testSerializeTaintedPrimitive() throws Exception {
        int input = MultiTainter.taintedInt(37, "label");
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream outStream = new ObjectOutputStream(byteStream);
        outStream.writeObject(input);
        outStream.close();
        int output = (int) new ObjectInputStream(new ByteArrayInputStream(byteStream.toByteArray())).readObject();
        assertEquals(input, output);
        assertNonNullTaint(MultiTainter.getTaint(output));
    }

    /* Checks that when an object with a non-tainted primitive array field is serialized and then deserialized the primitive
     * array of the deserialized object is not tainted. */
    @Test
    public void testSerializeObjectWithNonTaintedPrimitiveArrayField() throws Exception {
        byte[] arr = new byte[3];
        for(int i = 0; i < arr.length; i++) {
            arr[i] = (byte)i;
        }
        ArrayHolder holderInput = new ArrayHolder(arr);
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream outStream = new ObjectOutputStream(byteStream);
        outStream.writeObject(holderInput);
        outStream.close();
        ArrayHolder holderOutput = (ArrayHolder) new ObjectInputStream(new ByteArrayInputStream(byteStream.toByteArray())).readObject();
        assertArrayEquals(holderInput.arr, holderOutput.arr);
        for(int el : holderOutput.arr) {
            assertNullOrEmpty(MultiTainter.getTaint(el));
        }
    }

    /* Checks that when an object with a non-tainted primitive field is serialized and then deserialized the primitive of the
     * deserialized object is not tainted. */
    @Test
    public void testSerializeObjectWithNonTaintedPrimitiveField() throws Exception {
        PrimitiveHolder holderInput = new PrimitiveHolder(37);
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream outStream = new ObjectOutputStream(byteStream);
        outStream.writeObject(holderInput);
        outStream.close();
        PrimitiveHolder holderOutput = (PrimitiveHolder) new ObjectInputStream(new ByteArrayInputStream(byteStream.toByteArray())).readObject();
        assertEquals(holderInput.x, holderOutput.x);
        assertNullOrEmpty(MultiTainter.getTaint(holderOutput.x));
    }

    /* Checks that when a non-tainted primitive array field is serialized and then deserialized the deserialized primitive
     * array is not tainted. */
    @Test
    public void testSerializeNonTaintedPrimitiveArray() throws Exception {
        int[] inputArr = new int[3];
        for(int i = 0; i < inputArr.length; i++) {
            inputArr[i] = i;
        }
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream outStream = new ObjectOutputStream(byteStream);
        outStream.writeObject(inputArr);
        outStream.close();
        int[] outputArr = (int[]) new ObjectInputStream(new ByteArrayInputStream(byteStream.toByteArray())).readObject();
        assertArrayEquals(inputArr, outputArr);
        for(int el : outputArr) {
            assertNullOrEmpty(MultiTainter.getTaint(el));
        }
    }

    /* Checks that when a non-tainted primitive is serialized and then deserialized the deserialized primitive is not tainted. */
    @Test
    public void testSerializeNonTaintedPrimitive() throws Exception {
        int input = 37;
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream outStream = new ObjectOutputStream(byteStream);
        outStream.writeObject(input);
        outStream.close();
        int output = (int) new ObjectInputStream(new ByteArrayInputStream(byteStream.toByteArray())).readObject();
        assertEquals(input, output);
        assertNullOrEmpty(MultiTainter.getTaint(output));
    }
}
