package edu.columbia.cs.psl.test.phosphor;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

public class FakeEnumTest extends BasePhosphorTest{

public static void main(String[] args) {
	System.out.println(Foo.A);
	String a = "A";
	MultiTainter.taintedObject(a, Taint.withLabel("Z"));
    System.out.println(Foo.valueOf(a));
	System.out.println(MultiTainter.getTaint(Foo.valueOf(a)));
	System.out.println(MultiTainter.getTaint(a));
	System.out.println(MultiTainter.getTaint(Foo.A));

}
	
	enum Foo {
		A,B,C;
		String baz;
	}
}
