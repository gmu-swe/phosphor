package edu.columbia.cs.psl.phosphor.runtime.mask;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that calls to the annotated method should be added during instrumentation to
 * replace calls to replace compatible method calls.
 * Methods with this annotation must be static.
 * A method call is compatible with a mask if the method being called is owned by the mask's specified
 * {@link Mask#owner}, has the same name as the method with the mask annotation, and it descriptor is compatible
 * with that the of method with the mask annotation.
 * If {@link Mask#isStatic()} is true then a descriptor is compatible if it is equal to descriptor of the method with
 * the mask annotation.
 * Otherwise, a descriptor is compatible if it is equal to the descriptor of the method with the mask annotation with
 * the first parameter removed.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface Mask {
    Class<?> owner();

    boolean isStatic() default false;
}
