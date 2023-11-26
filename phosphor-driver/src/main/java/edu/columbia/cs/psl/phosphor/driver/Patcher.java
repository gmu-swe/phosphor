package edu.columbia.cs.psl.phosphor.driver;

import java.io.IOException;

public interface Patcher {
    byte[] patch(String classFileName, byte[] classFileBuffer) throws IOException;
}
