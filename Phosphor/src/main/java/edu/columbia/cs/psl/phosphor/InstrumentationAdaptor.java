package edu.columbia.cs.psl.phosphor;

import edu.columbia.cs.psl.phosphor.runtime.NonModifiableClassException;

public interface InstrumentationAdaptor {
    void addTransformer(PhosphorBaseTransformer transformer);

    Class<?>[] getAllLoadedClasses();

    void addTransformer(PhosphorBaseTransformer transformer, boolean canRedefineClasses);

    void retransformClasses(Class<?> clazz) throws NonModifiableClassException;
}
