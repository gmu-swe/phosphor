package edu.columbia.cs.psl.phosphor;

import edu.columbia.cs.psl.phosphor.runtime.NonModifiableClassException;

public interface InstrumentationHelper {
    void addTransformer(PhosphorBaseTransformer classSupertypeReadingTransformer);
    void addTransformer(PhosphorBaseTransformer classSupertypeReadingTransformer, boolean canRedefineClasses);

    void retransformClasses(Class<?> clazz) throws NonModifiableClassException;

    Class<?>[] getAllLoadedClasses();
}
