package edu.columbia.cs.psl.test.phosphor;

public class PartiallyInstrumentedClass {
	public static void foo(){
		
	}
	public static void inst(int a)
	{
		
	}
	public static boolean uninst(){
		return false;
	}
	@Override
	public boolean equals(Object obj) {
		// TODO Auto-generated method stub
		return super.equals(obj);
	}
	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return super.hashCode();
	}
}
