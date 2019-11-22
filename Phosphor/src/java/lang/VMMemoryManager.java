package java.lang;

public class VMMemoryManager {
    public static native void arrayCopy(Object src, int srcPos,
                                        Object dest, int destPos,
                                        int length);
}
