package edu.gmu.swe.phosphor.instrument;

import java.io.IOException;

public interface Patcher {
    byte[] patch(String classFileName, byte[] classFileBuffer) throws IOException;
}
