package edu.columbia.cs.psl.phosphor.instrumenter.analyzer;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.BasicValue;

public class BasicArrayValue extends BasicValue {
    public static final BasicArrayValue INT_ARRAY = new BasicArrayValue(Type.getType("[I"));
    public static final BasicArrayValue FLOAT_ARRAY = new BasicArrayValue(Type.getType("[F"));
    public static final BasicArrayValue DOUBLE_ARRAY = new BasicArrayValue(Type.getType("[D"));
    public static final BasicArrayValue LONG_ARRAY = new BasicArrayValue(Type.getType("[J"));
    public static final BasicArrayValue BYTE_ARRAY = new BasicArrayValue(Type.getType("[B"));
    public static final BasicArrayValue CHAR_ARRAY = new BasicArrayValue(Type.getType("[C"));
    public static final BasicArrayValue BOOLEAN_ARRAY = new BasicArrayValue(Type.getType("[Z"));
    public static final BasicArrayValue SHORT_ARRAY = new BasicArrayValue(Type.getType("[S"));
    public static final BasicValue NULL_VALUE = new BasicArrayValue(Type.getType("Lnull;"));

    public BasicArrayValue(Type type) {
        super(type);
    }

    @Override
    public String toString() {
        if(this == NULL_VALUE) {
            return "N";
        } else {
            return super.toString();
        }
    }
}
