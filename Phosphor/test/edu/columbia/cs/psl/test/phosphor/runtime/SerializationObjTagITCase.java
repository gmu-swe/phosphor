package edu.columbia.cs.psl.test.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.test.phosphor.BaseMultiTaintClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.lang.reflect.Array;

import static org.junit.Assert.*;


public class SerializationObjTagITCase extends BaseMultiTaintClass {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    /* Class containing Object fields used to store primitive arrays. */
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

    public static class PrimitiveHolder implements Serializable {
        private static final long serialVersionUID = -7447282366633906624L;
        private int x;
        public PrimitiveHolder(int x) {
            this.x = x;
        }
    }

    /* Checks that the value and taint tags of the specified output matches the specified input . */
    private static void checkValueAndTagsMatch(Object input, Object output) {
        if(input == null) {
            assertNull(output);
            return;
        }
        assertEquals(input.getClass(), output.getClass());
        if(input.getClass().isArray()) {
            assertTrue(output.getClass().isArray());
            checkArrayValuesAndTagsMatch(input, output);
        } else {
            assertEquals(input, output);
            assertEquals(MultiTainter.getTaint(input), MultiTainter.getTaint(output));
        }
    }

    /* Checks that the values and tags of the elements of the specified output array match the values and tags of the
     * elements of the specified input array. */
    private static void checkArrayValuesAndTagsMatch(Object inputArray, Object outputArray) {
        int length = Array.getLength(inputArray);
        assertEquals(length, Array.getLength(outputArray));
        for(int i = 0; i < length; i++) {
            checkValueAndTagsMatch(Array.get(inputArray, i), Array.get(outputArray, i));
        }
    }

    /* Checks that when a tainted primitive is serialized to a file output stream and then deserialized the primitive
     * deserialized is tainted. */
    @Test
    public void testSerializeTaintedPrimitiveToFileObjectStream() throws Exception {
        File file = folder.newFile();
        ObjectOutputStream outStream = new ObjectOutputStream(new FileOutputStream(folder.newFile()));
        int input = MultiTainter.taintedInt(11, "label");
        outStream.writeInt(input);
        outStream.close();
        ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(file));
        int output = inputStream.readInt();
        inputStream.close();
        assertEquals(input, output);
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

    @Test
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
