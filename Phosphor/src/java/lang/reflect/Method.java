package java.lang.reflect;

import sun.reflect.MethodAccessor;

import java.lang.annotation.Annotation;

public final class Method extends AccessibleObject implements GenericDeclaration, Member {
	public Method PHOSPHOR_TAGmethod;
	public boolean PHOSPHOR_TAGmarked;

	public Class<?> getDeclaringClass() {
		return null;
	}

	public String getName() {
		return null;
	}

	public int getModifiers() {
		return 0;
	}

	public TypeVariable<Method>[] getTypeParameters() {
		return null;
	}

	public Class<?> getReturnType() {
		return null;
	}

	public Type getGenericReturnType() {
		return null;
	}

	public Class<?>[] getParameterTypes() {
		return null;
	}

	public Type[] getGenericParameterTypes() {
		return null;
	}

	public Class<?>[] getExceptionTypes() {
		return null;
	}

	public Type[] getGenericExceptionTypes() {
		return null;
	}

	public boolean equals(Object obj) {
		return super.equals(obj);
	}

	public int hashCode() {
		return super.hashCode();
	}

	public String toString() {
		return super.toString();
	}

	public String toGenericString() {
		return null;
	}

	public Object invoke(Object obj, Object... args) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		return null;
	}

	public boolean isBridge() {
		return false;
	}

	public boolean isVarArgs() {
		return false;
	}

	public boolean isSynthetic() {
		return false;
	}

	MethodAccessor getMethodAccessor() {
		return null;
	}

	void setMethodAccessor(MethodAccessor accessor) {
	}

	public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
		return null;
	}

	public Annotation[] getDeclaredAnnotations() {
		return null;
	}

	public Object getDefaultValue() {
		return null;
	}

	public Annotation[][] getParameterAnnotations() {
		return null;
	}

	public boolean isAccessible() {
		return super.isAccessible();
	}

	public void setAccessible(boolean flag) throws SecurityException {
		super.setAccessible(flag);
	}

	public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
		return super.isAnnotationPresent(annotationClass);
	}

	public Annotation[] getAnnotations() {
		return super.getAnnotations();
	}

	void checkAccess(Class<?> caller, Class<?> clazz, Object obj, int modifiers) throws IllegalAccessException {
		super.checkAccess(caller, clazz, obj, modifiers);
	}

	void slowCheckMemberAccess(Class<?> caller, Class<?> clazz, Object obj, int modifiers, Class<?> targetClass) throws IllegalAccessException {
		super.slowCheckMemberAccess(caller, clazz, obj, modifiers, targetClass);
	}

	protected Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	protected void finalize() throws Throwable {
		super.finalize();
	}
}
