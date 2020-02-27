package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.type;

@SuppressWarnings("unused")
public class TypeInterpreterTestMethods {

    public void mergeStringAndNull(boolean b) {
        String s = "Hello";
        if(b) {
            s = null;
        }
        String s2 = s;
    }

    public void mergeNullAndString(boolean b) {
        String s = null;
        if(b) {
            s = "Hello";
        }
        String s2 = s;
    }

    public void mergeNullAndNull(boolean b) {
        String s2 = null;
        String s = s2;
        if(b) {
            s = null;
        }
        s2 = s;
    }
}
