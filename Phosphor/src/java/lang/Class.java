package java.lang;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedReturnHolderWithIntTag;

public class Class<T> {
	public Class PHOSPHOR_TAGclass;

	public native Field[] getDeclaredFields();

	public native Method getMethod(String name, Class<?>... parameterTypes) throws NoSuchMethodException;

	public native Class<?> getComponentType();

	public native String getName();

	public native boolean isArray();

	public native boolean isPrimitive();

	public native boolean isAnnotation();

	public static native Class forName(String name) throws ClassNotFoundException;

	public static native Class forName(String name, boolean z, ClassLoader ldr) throws ClassNotFoundException;

	public native Field getDeclaredField(String field) throws NoSuchFieldException;

	public native Constructor<T> getConstructor();

	//    public static native boolean forName(String name, );

	public boolean isAssignableFrom(Class returnType) {
		return false;
	}

	public Object newInstance() throws InstantiationException, IllegalAccessException {
		throw new UnsupportedOperationException();
	}

	public Constructor getDeclaredConstructor(Class[] args) throws NoSuchMethodException {
		throw new UnsupportedOperationException();
	}

	public Field getField(String string) throws NoSuchFieldException {
		throw new UnsupportedOperationException();
	}

	public ClassLoader getClassLoader() {
		throw new UnsupportedOperationException();
	}

	public boolean isInterface() {
		throw new UnsupportedOperationException();
	}

	public Class<?> getSuperclass() {
		throw new UnsupportedOperationException();
	}

	public Constructor getConstructor(Class... types) throws NoSuchMethodException {
		throw new UnsupportedOperationException();
	}

	public Method[] getMethods() {
		throw new UnsupportedOperationException();
	}

	public Method[] getDeclaredMethods() {
		throw new UnsupportedOperationException();
	}

	public Class[] getInterfaces() {
		throw new UnsupportedOperationException();
	}

	public Method getDeclaredMethod(String string, Class... type) throws NoSuchMethodException {
		throw new UnsupportedOperationException();
	}

	public Constructor[] getDeclaredConstructors() {
		// TODO Auto-generated method stub
		return null;
	}

	public Constructor[] getConstructors() {
		// TODO Auto-generated method stub
		return null;
	}

	public Field[] getFields() {
		// TODO Auto-generated method stub
		return null;
	}

	public TaintedBooleanWithIntTag isPrimitive$$PHOSPHORTAGGED(TaintedReturnHolderWithIntTag prealloc) {
		// TODO Auto-generated method stub
		return null;
	}

	public TaintedBooleanWithIntTag isArray$$PHOSPHORTAGGED(TaintedReturnHolderWithIntTag prealloc) {
		// TODO Auto-generated method stub
		return null;
	}

	public Class<?> getComponentType$$PHOSPHORTAGGED(TaintedReturnHolderWithIntTag prealloc) {
		// TODO Auto-generated method stub
		return null;
	}

	public TaintedBooleanWithIntTag isAnnotation$$PHOSPHORTAGGED(TaintedReturnHolderWithIntTag prealloc) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getName$$PHOSPHORTAGGED(TaintedReturnHolderWithIntTag prealloc) {
		// TODO Auto-generated method stub
		return null;
	}

	public TaintedBooleanWithIntTag equals$$PHOSPHORTAGGED(Object tAINT_TAG_OBJ_CLASS, TaintedReturnHolderWithIntTag prealloc) {
		// TODO Auto-generated method stub
		return null;
	}

	public Method getDeclaredMethod$$PHOSPHORTAGGED(String string, Class[] args, TaintedReturnHolderWithIntTag prealloc) throws NoSuchMethodException{
		// TODO Auto-generated method stub
		return null;
	}
}
