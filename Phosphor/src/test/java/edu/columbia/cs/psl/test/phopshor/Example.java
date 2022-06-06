package edu.columbia.cs.psl.test.phopshor;

import com.sun.istack.internal.NotNull;

public class Example {
    @SuppressWarnings("unused")
    public int parameters(@NotNull Object o, double d, float f, @RuntimeAnnotation long j, int i,
                          @RuntimeAnnotation short s) {
        return o.toString().length();
    }
}