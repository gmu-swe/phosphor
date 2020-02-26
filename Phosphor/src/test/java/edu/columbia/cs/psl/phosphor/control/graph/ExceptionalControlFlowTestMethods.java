package edu.columbia.cs.psl.phosphor.control.graph;

import java.io.FileNotFoundException;
import java.io.IOException;

@SuppressWarnings("unused")
public class ExceptionalControlFlowTestMethods {

    // Used to identify which basic block a particular code chunk ended up in
    private static int blockID = -1;

    public int implicitNPE(int[] a) {
        blockID = 0;
        int i = 0;
        try {
            a[0] = 5;
            blockID = 1;
            i = 7;
        } catch(NullPointerException e) {
            blockID = 2;
            e.printStackTrace();
        }
        blockID = 3;
        return i;
    }

    public void exceptionThrowingMethod() throws IOException {
        throw new FileNotFoundException();
    }

    public void callsExceptionThrowingMethod() {
        blockID = 0;
        try {
            exceptionThrowingMethod();
            blockID = 1;
        } catch(FileNotFoundException e) {
            blockID = 2;
        } catch(IOException e) {
            blockID = 3;
        }
        blockID = 4;
    }

    public void nestedHandlers(Object obj) {
        blockID = 0;
        try {
            try {
                int i = 0;
                String s = obj.toString();
                blockID = 1;
            } catch(NullPointerException e) {
                blockID = 2;
                e.printStackTrace();
            }
            blockID = 3;
        } catch(RuntimeException e) {
            blockID = 4;
        }
        blockID = 5;
    }

    public int explicitlyThrowCaughtException(int[] a) {
        blockID = 0;
        int i = 0;
        try {
            if(a[0] < 2) {
                blockID = 1;
                throw new IOException();
            }
            blockID = 2;
        } catch(IOException e) {
            blockID = 3;
            e.printStackTrace();
        }
        blockID = 4;
        return i;
    }

    public int explicitlyThrowUncaughtException(int[] a, IOException e) throws IOException {
        blockID = 0;
        int i = 0;
        try {
            if(a[0] < 2) {
                blockID = 1;
                throw e;
            }
            blockID = 2;
        } catch(IllegalArgumentException e2) {
            blockID = 3;
        }
        blockID = 4;
        return i;
    }
}
