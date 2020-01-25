package edu.columbia.cs.psl.test.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.struct.PowerSetTree;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.math.BigInteger;

import static org.junit.Assert.*;


public class SerializationObjTagITCase extends FieldHolderBaseTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @After
    public void reset() {
        PowerSetTree.getInstance().reset();
    }

    public static class PrimitiveArrayHolderChild extends PrimitiveArrayHolder {

        private static final long serialVersionUID = 2460391570566213782L;

        public PrimitiveArrayHolderChild(boolean taintFields) {
            super(taintFields);
        }

    }

    public static class EmptyHolder implements Serializable {

        private static final long serialVersionUID = 9061758314720341782L;
    }

    /* Serializes the specified input and then deserializes it. Returns the object deserialized. */
    @SuppressWarnings("unchecked")
    private <T> T roundTripSerialize(T input) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream byteStream =new ByteArrayOutputStream();
        ObjectOutputStream outputStream = new ObjectOutputStream(byteStream);
        outputStream.writeObject(input);
        outputStream.close();
        ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(byteStream.toByteArray()));
        T result = (T)inputStream.readObject();
        inputStream.close();
        return result;
    }

    /* Checks that when an object with tainted primitive fields is serialized and then deserialized the primitive
     * fields of the deserialized object are tainted. */
    @Test
    public void testSerializeTaintedPrimitiveFields() throws Exception {
        PrimitiveHolder input = new PrimitiveHolder(true);
        PrimitiveHolder output = roundTripSerialize(input);
        assertEquals(input, output);
        output.checkFieldsAreTainted();
    }

    /* Checks that when an object with non-tainted primitive fields is serialized and then deserialized the primitive
     * fields of the deserialized object are non-tainted. */
    @Test
    public void testSerializeNonTaintedPrimitiveFields() throws Exception {
        PrimitiveHolder input = new PrimitiveHolder(false);
        PrimitiveHolder output = roundTripSerialize(input);
        assertEquals(input, output);
        output.checkFieldsAreNotTainted();
    }

    /* Checks that when an object with tainted boxed primitive fields is serialized and then deserialized the boxed
     * primitive fields of the deserialized object are tainted. */
    @Test
    public void testSerializeTaintedBoxedPrimitiveFields() throws Exception {
        BoxedPrimitiveHolder input = new BoxedPrimitiveHolder(true);
        BoxedPrimitiveHolder output = roundTripSerialize(input);
        assertEquals(input, output);
        output.checkFieldsAreTainted();
    }

    /* Checks that when an object with non-tainted boxed primitive fields is serialized and then deserialized the boxed
     * primitive fields of the deserialized object are non-tainted. */
    @Test
    public void testSerializeNonTaintedBoxedPrimitiveFields() throws Exception {
        BoxedPrimitiveHolder input = new BoxedPrimitiveHolder(false);
        BoxedPrimitiveHolder output = roundTripSerialize(input);
        assertEquals(input, output);
        output.checkFieldsAreNotTainted();
    }

    /* Checks that when an object with tainted primitive array fields is serialized and then deserialized the primitive
     * arrays of the deserialized object are tainted. */
    @Test
    public void testSerializeTaintedPrimitiveArrayFields() throws Exception {
        PrimitiveArrayHolder input = new PrimitiveArrayHolder(true);
        PrimitiveArrayHolder output = roundTripSerialize(input);
        assertEquals(input, output);
        output.checkFieldsAreTainted();
    }

    /* Checks that when an object with non-tainted primitive array fields is serialized and then deserialized the primitive
     * arrays of the deserialized object are non-tainted. */
    @Test
    public void testSerializeNonTaintedPrimitiveArrayFields() throws Exception {
        PrimitiveArrayHolder input = new PrimitiveArrayHolder(false);
        PrimitiveArrayHolder output = roundTripSerialize(input);
        assertEquals(input, output);
        output.checkFieldsAreNotTainted();
    }

    /* Checks that when an object with tainted primitive array fields declared in a superclass is serialized and then
     * deserialized the primitive arrays of the deserialized object are tainted. */
    @Test
    public void testSerializeTaintedSuperClassPrimitiveArrayFields() throws Exception {
        PrimitiveArrayHolderChild input = new PrimitiveArrayHolderChild(true);
        PrimitiveArrayHolderChild output = roundTripSerialize(input);
        assertEquals(input, output);
        output.checkFieldsAreTainted();
    }

    /* Checks that when an object with non-tainted primitive array fields declared in a superclass is serialized and then
     * deserialized the primitive arrays of the deserialized object are non-tainted. */
    @Test
    public void testSerializeNonTaintedSuperClassPrimitiveArrayFields() throws Exception {
        PrimitiveArrayHolderChild input = new PrimitiveArrayHolderChild(false);
        PrimitiveArrayHolderChild output = roundTripSerialize(input);
        assertEquals(input, output);
        output.checkFieldsAreNotTainted();
    }

    /* Checks that when an object with tainted primitive arrays in object fields is serialized and then deserialized the
     * primitive arrays of the deserialized object are tainted. */
    @Test
    public void testSerializeTaintedPrimitiveArrayObjFields() throws Exception {
        PrimitiveArrayObjHolder input = new PrimitiveArrayObjHolder(true);
        PrimitiveArrayObjHolder output = roundTripSerialize(input);
        output.checkFieldsAreTainted();
    }

    /* Checks that when an object with non-tainted primitive array in object fields is serialized and then deserialized the
     * primitive arrays of the deserialized object are non-tainted. */
    @Test
    public void testSerializeNonTaintedPrimitiveArrayObjFields() throws Exception {
        PrimitiveArrayObjHolder input = new PrimitiveArrayObjHolder(false);
        PrimitiveArrayObjHolder output = roundTripSerialize(input);
        output.checkFieldsAreNotTainted();
    }

    /* Checks that when an object with tainted 2D primitive array fields is serialized and then deserialized the 2D primitive
     * arrays of the deserialized object are tainted. */
    @Test
    public void testSerializeTainted2DPrimitiveArrayFields() throws Exception {
        Primitive2DArrayHolder input = new Primitive2DArrayHolder(true);
        Primitive2DArrayHolder output = roundTripSerialize(input);
        assertEquals(input, output);
        output.checkFieldsAreTainted();
    }

    /* Checks that when an object with non-tainted 2D primitive array fields is serialized and then deserialized the 2D primitive
     * arrays of the deserialized object are non-tainted. */
    @Test
    public void testSerializeNonTainted2DPrimitiveArrayFields() throws Exception {
        Primitive2DArrayHolder input = new Primitive2DArrayHolder(false);
        Primitive2DArrayHolder output = roundTripSerialize(input);
        assertEquals(input, output);
        output.checkFieldsAreNotTainted();
    }

    /* Checks that when a tainted BigInteger is serialized and then deserialized the deserialized BigInteger is tainted. */
    @Test
    public void testSerializeTaintedBigInt() throws Exception {
        byte[] b = (byte[])supplier.getByteArray(true, 1);
        BigInteger input = new BigInteger(b);
        BigInteger output = roundTripSerialize(input);
        assertEquals(input, output);
        assertNonNullTaint(MultiTainter.getTaint(output.longValue()));
    }

    /* Checks that when a non-tainted BigInteger is serialized and then deserialized the deserialized BigInteger is non-tainted. */
    @Test
    public void testSerializeNonTaintedBigInt() throws Exception {
        BigInteger input = BigInteger.valueOf(5);
        BigInteger output = roundTripSerialize(input);
        assertEquals(input, output);
        assertNullOrEmpty(MultiTainter.getTaint(output.longValue()));
    }

    /* Checks that when tainted primitive arrays are serialized and then deserialized the deserialized primitive arrays
     * are tainted. */
    @Test
    public void testSerializeTaintedPrimitiveArrays() throws Exception {
        for(Class<?> clazz : primArrayTypes) {
            Object input = supplier.getArray(true, clazz);
            Object output = roundTripSerialize(input);
            assertNonNullTaint(MultiTainter.getMergedTaint(output));
        }
    }

    /* Checks that when non-tainted primitive arrays are serialized and then deserialized the deserialized primitive arrays
     * are non-tainted. */
    @Test
    public void testSerializeNonTaintedPrimitiveArrays() throws Exception {
        for(Class<?> clazz : primArrayTypes) {
            Object input = supplier.getArray(false, clazz);
            Object output = roundTripSerialize(input);
            assertNullOrEmpty(MultiTainter.getMergedTaint(output));
        }
    }

    /* Serializes primitives that are tainted if taint is true and non-tainted otherwise. Check that primitive deserializes
     * from the serialized primitives are tainted only if the originals are tainted. */
    private void checkSerializePrimitives(boolean taint) throws IOException {
        // Create the input primitives
        int i = supplier.getInt(taint);
        long j = supplier.getLong(taint);
        boolean z = supplier.getBoolean(taint);
        short s = supplier.getShort(taint);
        double d = supplier.getDouble(taint);
        byte b = supplier.getByte(taint);
        char c = supplier.getChar(taint);
        float f = supplier.getFloat(taint);
        // Write the primitives to the stream
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream outStream = new ObjectOutputStream(byteStream);
        outStream.writeInt(i);
        outStream.writeLong(j);
        outStream.writeBoolean(z);
        outStream.writeShort(s);
        outStream.writeDouble(d);
        outStream.writeByte(b);
        outStream.writeChar(c);
        outStream.writeFloat(f);
        outStream.close();
        // Read primitive from the stream
        ObjectInputStream inStream = new ObjectInputStream(new ByteArrayInputStream(byteStream.toByteArray()));
        int outI = inStream.readInt();
        long outJ = inStream.readLong();
        boolean outZ = inStream.readBoolean();
        short outS = inStream.readShort();
        double outD = inStream.readDouble();
        byte outB = inStream.readByte();
        char outC = inStream.readChar();
        float outF = inStream.readFloat();
        inStream.close();
        // Check the values of the output primitives match the input primitives
        assertEquals(i, outI);
        assertEquals(j, outJ);
        assertEquals(z, outZ);
        assertEquals(s, outS);
        assertEquals(d, outD, 0.001);
        assertEquals(b, outB);
        assertEquals(c, outC);
        assertEquals(f, outF, 0.001);
        // Check the taint tags of the output primitives
        if(taint) {
            assertNonNullTaint(MultiTainter.getTaint(outI));
            assertNonNullTaint(MultiTainter.getTaint(outJ));
            assertNonNullTaint(MultiTainter.getTaint(outZ));
            assertNonNullTaint(MultiTainter.getTaint(outS));
            assertNonNullTaint(MultiTainter.getTaint(outD));
            assertNonNullTaint(MultiTainter.getTaint(outB));
            assertNonNullTaint(MultiTainter.getTaint(outC));
            assertNonNullTaint(MultiTainter.getTaint(outF));
        } else {
            assertNullOrEmpty(MultiTainter.getTaint(outI));
            assertNullOrEmpty(MultiTainter.getTaint(outJ));
            assertNullOrEmpty(MultiTainter.getTaint(outZ));
            assertNullOrEmpty(MultiTainter.getTaint(outS));
            assertNullOrEmpty(MultiTainter.getTaint(outD));
            assertNullOrEmpty(MultiTainter.getTaint(outB));
            assertNullOrEmpty(MultiTainter.getTaint(outC));
            assertNullOrEmpty(MultiTainter.getTaint(outF));
        }
    }

    /* Checks that when tainted primitives are serialized and then deserialized the deserialized primitives are tainted. */
    @Test
    public void testSerializeTaintedPrimitives() throws Exception {
        checkSerializePrimitives(true);
    }

    /* Checks that when non-tainted primitives are serialized and then deserialized the deserialized primitives are non-tainted. */
    @Test
    public void testSerializeNonTaintedPrimitives() throws Exception {
        checkSerializePrimitives(false);
    }

    /* Checks that when a tainted Object is serialized to a file output stream and then deserialized the Object
     * deserialized is tainted. */
    @Test
    public void testSerializeTaintedObject() throws Exception {
        EmptyHolder input = new EmptyHolder();
        input = MultiTainter.taintedReference(input, "label");
        EmptyHolder output = roundTripSerialize(input);
        assertNonNullTaint(output);
    }
}
