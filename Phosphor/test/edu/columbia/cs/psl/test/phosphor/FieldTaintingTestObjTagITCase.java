package edu.columbia.cs.psl.test.phosphor;

import static org.junit.Assert.*;
import org.junit.Test;

import de.ecspride.Foo;
import de.ecspride.Moo;
import de.ecspride.MyTainter;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;


public class FieldTaintingTestObjTagITCase {

	@Test
	public void classReferenceShouldbeTainted() throws IllegalArgumentException, IllegalAccessException
	{
		Moo m = new Moo(false);
		MyTainter.taintObjects(m);
		Foo f_ = m.getF();
		assertNotNull("f_ should be tainted ", MultiTainter.getTaint(f_));
	}

	@Test
	public void primitiveFieldsShouldbeTainted_1() throws IllegalArgumentException, IllegalAccessException
	{
		Moo m =  new Moo(false);
		MyTainter.taintObjects(m);
		boolean flag_ = m.isFlag();
		assertNotNull("flag_ should be tainted", MultiTainter.getTaint(flag_));
	}

	@Test
	public void primitiveFieldsShouldbeTainted_2() throws IllegalArgumentException, IllegalAccessException
	{
		Moo m =  new Moo(false);
		MyTainter.taintObjects(m);
		int x_ = m.getF().getX();
		assertNotNull("x_ should be tainted",MultiTainter.getTaint(x_));
	}
}
