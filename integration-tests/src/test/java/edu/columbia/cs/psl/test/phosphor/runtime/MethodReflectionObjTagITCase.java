package edu.columbia.cs.psl.test.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.test.phosphor.BaseMultiTaintClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.*;

@RunWith(Enclosed.class)
public class MethodReflectionObjTagITCase extends BaseMultiTaintClass {

    private static PrimitiveSupplier supplier;

    @ClassRule
    public static ExternalResource resource = new ExternalResource() {
        @Override
        protected void before() {
            supplier = new PrimitiveSupplier();
        }
    };

    @RunWith(Theories.class)
    public static class MethodInvokeTheoryTests {

        @DataPoints
        public static Class<?>[][] types = new Class<?>[][] {
                {Boolean.TYPE},
                {boolean[].class},
                {Object.class}
        };

        @DataPoints
        public static Boolean[] taintArguments = new Boolean[]{true, false};

        /* Invokes a method using reflection. Checks that the invocation succeeded and returned an expected value.
         * Checks that any taint tags associated with the arguments are also passed to the method call. */
        @Theory
        public void testInvokeMethod(Boolean taintArguments, Class<?>[] types) throws Exception {
            MethodHolder holder = new MethodHolder(taintArguments);
            Method method = MethodHolder.class.getMethod("example", types);
            Object[] args = new Object[types.length];
            for(int i = 0; i < types.length; i++) {
                if (types[i] == Object.class) {
                    args[i] = supplier.getArray(taintArguments, boolean[].class);
                } else {
                    args[i] = types[i].isArray() ? supplier.getArray(taintArguments, types[i]) : supplier.getBoxedPrimitive(taintArguments, types[i]);
                }
            }
            Object result = method.invoke(holder, args);
            assertTrue("Expected integer to be returned from reflected method.", result instanceof Integer);
            int i = (Integer)result;
            assertEquals(MethodHolder.RET_VALUE, i);
        }
    }

    @Test
    public void testReflectionMethodWithoutTaint() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        MethodHolder obj2 = new MethodHolder(false);
        Method m = MethodHolder.class.getMethod("getLast", int.class,
                int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class,
                int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class,
                int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class,
                int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class,
                int.class, int.class, int.class);
        int ret = (int) m.invoke(obj2, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14,
                15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39);
        assertEquals(39, ret);
    }

    @Test
    public void testReflectionMethodTaintPropagation() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        MethodHolder obj2 = new MethodHolder(true);
        Method m = MethodHolder.class.getMethod("getLast", int.class,
                int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class,
                int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class,
                int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class,
                int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class,
                int.class, int.class, int.class);
        int i39 = MultiTainter.taintedInt(39, "tainted");
        int ret = (int) m.invoke(obj2, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14,
                15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, i39);
        assertTaintHasOnlyLabel(MultiTainter.getTaint(ret), "tainted");
    }

    public static class StandardTests {

        /* Checks that parameters passed to getDeclaredMethod and invoke for ignored classes like Boolean are not
         * remapped. */
        @Test
        public void testBooleanIgnoredMethod() throws Exception {
            Method method = Boolean.class.getDeclaredMethod("toString", boolean.class);
            String result = (String)method.invoke(null, false);
            assertNotNull(result);
        }

        /* Checks that a NoSuchMethodException is thrown for classes which do not declare an equals method even if a
         * synthetic one was added by Phosphor. */
        @Test(expected = NoSuchMethodException.class)
        public void testGetDeclaredMethodEquals() throws NoSuchMethodException {
            MethodHolder.class.getDeclaredMethod("equals", Object.class);
        }

        /* Checks that a NoSuchMethodException is thrown for classes which do not declare a hashCode method even if a
         * synthetic one was added by Phosphor. */
        @Test(expected = NoSuchMethodException.class)
        public void testGetDeclaredMethodHashCode() throws NoSuchMethodException {
            MethodHolder.class.getDeclaredMethod("hashCode");
        }

        /* Checks that a class' original equals method is returned by Class.getMethod even if a synthetic one was added
         * by Phosphor. */
        @Test
        public void testGetMethodEquals() throws NoSuchMethodException {
            Method actual = MethodHolder.class.getMethod("equals", Object.class);
            Method expected = Object.class.getMethod("equals", Object.class);
            assertEquals(expected, actual);
        }

        /* Checks that a class' original hashCode method is returned by Class.getMethod even if a synthetic one was added
         * by Phosphor. */
        @Test
        public void testGetMethodHashCode() throws NoSuchMethodException {
            Method actual = MethodHolder.class.getMethod("hashCode");
            Method expected = Object.class.getMethod("hashCode");
            assertEquals(expected, actual);
        }

        /* Checks that synthetic equals and hashcode methods added by Phosphor are replaced by Object.equals and
         * Object.hashCode for Class.getMethods. */
        @Test
        public void testHashCodeAndEqualsReplacedInGetMethods() throws NoSuchMethodException {
            HashSet<Method> expected = new HashSet<>();
            expected.add(MethodHolder.class.getDeclaredMethod("example", Boolean.TYPE));
            expected.add(MethodHolder.class.getDeclaredMethod("example", boolean[].class));
            expected.add(MethodHolder.class.getDeclaredMethod("example", Object.class));
            expected.add(MethodHolder.class.getDeclaredMethod("getLast", int.class, int.class,
                    int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class,
                    int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class,
                    int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class,
                    int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class,
                    int.class, int.class, int.class, int.class, int.class, int.class));
            expected.addAll(Arrays.asList(Object.class.getMethods()));
            Method[] methods = MethodHolder.class.getMethods();
            HashSet<Method> actual = new HashSet<>(Arrays.asList(methods));
            assertEquals(expected, actual);
        }

        /* Checks that synthetic equals and hashcode methods added by Phosphor are hidden from Class.getDeclaredMethods. */
        @Test
        public void testHashCodeAndEqualsHiddenFromGetDeclaredMethods() {
            String[] methodNames = new String[]{"example", "example", "example", "getLast"};
            Class<?>[] returnTypes = new Class<?>[]{Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE};
            Class<?>[][] paramTypes = new Class<?>[][]{
                    new Class<?>[]{Boolean.TYPE},
                    new Class<?>[]{boolean[].class},
                    new Class<?>[]{Object.class},
                    new Class<?>[] {int.class,
                            int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class,
                            int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class,
                            int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class,
                            int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class,
                            int.class, int.class, int.class}
            };
            Method[] methods = MethodHolder.class.getDeclaredMethods();
            assertEquals(methodNames.length, methods.length);
            for(Method method : methods) {
                boolean methodMatchesExpected = false;
                for(int i = 0; i < methodNames.length; i++) {
                    if(method.getName().equals(methodNames[i]) && method.getReturnType().equals(returnTypes[i])
                            && Arrays.equals(method.getParameterTypes(), paramTypes[i])) {
                        methodMatchesExpected = true;
                        break;
                    }
                }
                assertTrue(methodMatchesExpected);
            }
        }

        /* Checks that the methods returned by getMethod and getDeclaredMethod are equal. */
        @Test
        public void testGetMethodEqualsGetDeclaredMethod() throws NoSuchMethodException {
            Method m = MethodHolder.class.getMethod("example", Boolean.TYPE);
            Method mDeclared = MethodHolder.class.getDeclaredMethod("example", Boolean.TYPE);
            assertEquals(mDeclared, m);
        }

    }

}
