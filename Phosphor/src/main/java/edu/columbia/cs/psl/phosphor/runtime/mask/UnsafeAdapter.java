package edu.columbia.cs.psl.phosphor.runtime.mask;

import java.security.ProtectionDomain;

public class UnsafeAdapter {
    public Class<?> defineClass(String name, byte[] b, int off, int len, ClassLoader loader,
                                ProtectionDomain protectionDomain) {
        return null;
    }
}
