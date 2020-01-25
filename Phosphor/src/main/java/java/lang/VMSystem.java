package java.lang;

public class VMSystem {
    public static native void arraycopy(Object src, int srcPos,
                                        Object dest, int destPos,
                                        int length);

    public static void arraycopy0(Object src, int srcPos, Object dest, int destPos, int length) {
        // TODO Auto-generated method stub

    }
}
