package edu.columbia.cs.psl.phosphor;

import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

import static org.junit.Assert.assertTrue;

@RunWith(Theories.class)
public class TaintUtilsTest {

    /**
     * Checks that a method descriptor remapped with
     * TaintUtils.remapMethodDescAndIncludeReturnHolder
     * is restoring via TaintUtils.getOriginalMethodDesc.
     */

    @Theory
    public void checkIsSource(Method method) {
        boolean isStatic = Modifier.isStatic(method.getModifiers());
        String originalDesc = Type.getMethodDescriptor(method);
        String owner = Type.getInternalName(method.getDeclaringClass());
        String originalSignature = owner + "." + method.getName() + originalDesc;
        String taintedName = method.getName();
        List<String> previousSources = BasicSourceSinkManager.replaceAutoTaintMethods(
                Collections.singleton(originalSignature),
                BasicSourceSinkManager.AutoTaint.SOURCE);
        assertTrue(BasicSourceSinkManager.getInstance().isSource(owner, taintedName, originalDesc));
        // Restore the sources
        BasicSourceSinkManager.replaceAutoTaintMethods(previousSources,
                BasicSourceSinkManager.AutoTaint.SOURCE);
    }

    @DataPoints
    public static Method[] methods() {
        List<Method> methods = new LinkedList<>();
        List<Class<?>> targetClasses = Arrays.asList(String.class, HashMap.class, LinkedList.class,
                GregorianCalendar.class, TaintUtilsTestMethods.class);
        for (Class<?> targetClass : targetClasses) {
            methods.addAll(Arrays.asList(targetClass.getMethods()));
        }
        return methods.toArray(new Method[0]);
    }
}
