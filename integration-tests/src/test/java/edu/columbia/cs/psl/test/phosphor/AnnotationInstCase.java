package edu.columbia.cs.psl.test.phosphor;

import org.junit.Assert;
import org.junit.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

public class AnnotationInstCase extends BasePhosphorTest{
    @Test
    public void testEnum() throws ReflectiveOperationException {
        Method method = Example.class.getDeclaredMethod("x");
        ExampleAnnotation annotation = method.getAnnotation(ExampleAnnotation.class);
        Assert.assertNotNull(annotation);
        Assert.assertEquals(ExampleAnnotation.Color.BLUE, annotation.color());
    }

    public static class Example {
        @ExampleAnnotation(color = ExampleAnnotation.Color.BLUE)
        void x() {}
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ExampleAnnotation {
        Color color();

        enum Color {
            RED,
            BLUE,
            GREEN;
        }
    }
}
