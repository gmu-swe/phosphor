package edu.columbia.cs.psl.test.phosphor;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.junit.Test;

import edu.columbia.cs.psl.phosphor.runtime.Tainter;

public class SerializationIntTagITCase extends BasePhosphorTest {

	static class ArrayHolder implements Serializable {
		int[] ar;
	}

//	@Test
//	public void testSerializedArrayThenTaint() throws Exception {
//		ArrayHolder ah = new ArrayHolder();
//		ah.ar = new int[10];
//		for(int i = 0; i < 10; i++)
//			ah.ar[i] = 40;
//		ObjectOutputStream s = null;
//		ByteArrayOutputStream bos = new ByteArrayOutputStream();
//		try {
//			s = new ObjectOutputStream(bos);
//			s.writeObject(ah);
//			s.close();
//		} catch (IOException e) {
//
//		}
//
//		try {
//			ArrayHolder k = (ArrayHolder) new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray())).readObject();
//			k.ar = Tainter.taintedIntArray(k.ar, 3);
//		} catch (IOException e) {
//			e.printStackTrace();
//		} catch (ClassNotFoundException e) {
//			e.printStackTrace();
//		}
//	}
	
//	@Test
	public void testTaintedArraySerialized() throws Exception {
		ArrayHolder ah = new ArrayHolder();
		ah.ar = new int[10];
		for(int i = 0; i < 10; i++)
			ah.ar[i] = 40;
		ah.ar = Tainter.taintedIntArray(ah.ar, 3);
		assertEquals(3, Tainter.getTaint(ah.ar[0]));

		ObjectOutputStream s = null;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			s = new ObjectOutputStream(bos);
			s.writeObject(ah);
			s.close();
		} catch (IOException e) {

		}

		try {
			ArrayHolder k = (ArrayHolder) new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray())).readObject();
			assertEquals(3, Tainter.getTaint(k.ar[0]));
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
}
