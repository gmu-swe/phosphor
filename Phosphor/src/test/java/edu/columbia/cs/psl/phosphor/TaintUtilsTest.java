package edu.columbia.cs.psl.phosphor;

import com.sun.beans.decoder.DocumentHandler;
import com.sun.xml.internal.bind.v2.runtime.reflect.Lister;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Theories.class)
public class TaintUtilsTest {

    /**
     * Checks that a method descriptor remapped with TaintUtils.remapMethodDescAndIncludeReturnHolder
     * is restoring via TaintUtils.getOriginalMethodDesc.
     */
    @Theory
    public void checkRoundTripMapMethodDescriptor(Method method) {
        boolean isStatic = Modifier.isStatic(method.getModifiers());
        String originalDesc = Type.getMethodDescriptor(method);
        String taintedDesc = TaintUtils.remapMethodDescAndIncludeReturnHolder(!isStatic, originalDesc);
        String restoredDesc = TaintUtils.getOriginalMethodDescWithoutReturn(taintedDesc);
        String originalWithoutReturn = originalDesc.substring(0, originalDesc.lastIndexOf(')') + 1);
        String restoredWithoutReturn = restoredDesc.substring(0, restoredDesc.lastIndexOf(')') + 1);
        assertEquals(originalWithoutReturn, restoredWithoutReturn);
    }

    @Theory
    public void checkIsSource(Method method) {
        boolean isStatic = Modifier.isStatic(method.getModifiers());
        String originalDesc = Type.getMethodDescriptor(method);
        String taintedDesc = TaintUtils.remapMethodDescAndIncludeReturnHolder(!isStatic, originalDesc);
        String owner = Type.getInternalName(method.getDeclaringClass());
        String originalSignature = owner +  "." + method.getName() + originalDesc;
        String taintedName = method.getName();
        if (!originalDesc.equals(taintedDesc)) {
            taintedName += TaintUtils.METHOD_SUFFIX;
        }
        List<String> previousSources = BasicSourceSinkManager.replaceAutoTaintMethods(
                Collections.singleton(originalSignature),
                BasicSourceSinkManager.AutoTaint.SOURCE);
        assertTrue(BasicSourceSinkManager.getInstance().isSource(owner, taintedName, taintedDesc));
        // Restore the sources
        BasicSourceSinkManager.replaceAutoTaintMethods(previousSources,
                BasicSourceSinkManager.AutoTaint.SOURCE);
    }

    @DataPoints
    public static Method[] methods() {
        List<Method> methods = new LinkedList<>();
        List<Class<?>> targetClasses = Arrays.asList(String.class, HashMap.class, LinkedList.class, Lister.class,
                DocumentHandler.class, GregorianCalendar.class, TaintUtilsTestMethods.class);
        for (Class<?> targetClass : targetClasses) {
            methods.addAll(Arrays.asList(targetClass.getMethods()));
        }
        return methods.toArray(new Method[0]);
    }
}