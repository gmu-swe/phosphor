package edu.columbia.cs.psl.phosphor.control.type;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.Value;

public final class TypeValue implements Value {

    public static final TypeValue UNINITIALIZED_VALUE = new TypeValue(null);
    public static final TypeValue INT_VALUE = new TypeValue(Type.INT_TYPE);
    public static final TypeValue FLOAT_VALUE = new TypeValue(Type.FLOAT_TYPE);
    public static final TypeValue LONG_VALUE = new TypeValue(Type.LONG_TYPE);
    public static final TypeValue DOUBLE_VALUE = new TypeValue(Type.DOUBLE_TYPE);
    public static final TypeValue INT_ARRAY = new TypeValue(Type.getType("[I"));
    public static final TypeValue FLOAT_ARRAY = new TypeValue(Type.getType("[F"));
    public static final TypeValue DOUBLE_ARRAY = new TypeValue(Type.getType("[D"));
    public static final TypeValue LONG_ARRAY = new TypeValue(Type.getType("[J"));
    public static final TypeValue BYTE_ARRAY = new TypeValue(Type.getType("[B"));
    public static final TypeValue CHAR_ARRAY = new TypeValue(Type.getType("[C"));
    public static final TypeValue BOOLEAN_ARRAY = new TypeValue(Type.getType("[Z"));
    public static final TypeValue SHORT_ARRAY = new TypeValue(Type.getType("[S"));
    static final Type nullType = Type.getObjectType("null");
    public static final TypeValue NULL_VALUE = new TypeValue(nullType);

    private final Type type;

    private TypeValue(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    @Override
    public int getSize() {
        return type == Type.LONG_TYPE || type == Type.DOUBLE_TYPE ? 2 : 1;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        } else if(!(o instanceof TypeValue)) {
            return false;
        }
        TypeValue typeValue = (TypeValue) o;
        return type != null ? type.equals(typeValue.type) : typeValue.type == null;
    }

    @Override
    public int hashCode() {
        return type != null ? type.hashCode() : 0;
    }

    @Override
    public String toString() {
        return type == null ? "." : type.toString();
    }

    public static TypeValue getInstance(Type type) {
        if(type == null) {
            return TypeValue.UNINITIALIZED_VALUE;
        }
        switch(type.getSort()) {
            case Type.VOID:
                return null;
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
                return INT_VALUE;
            case Type.FLOAT:
                return FLOAT_VALUE;
            case Type.LONG:
                return LONG_VALUE;
            case Type.DOUBLE:
                return DOUBLE_VALUE;
            case Type.OBJECT:
                return new TypeValue(type);
            case Type.ARRAY:
                if(type.getDimensions() > 1 || type.getElementType().getSort() == Type.OBJECT) {
                    return new TypeValue(type);
                } else {
                    switch(type.getElementType().getSort()) {
                        case Type.BOOLEAN:
                            return TypeValue.BOOLEAN_ARRAY;
                        case Type.BYTE:
                            return TypeValue.BYTE_ARRAY;
                        case Type.CHAR:
                            return TypeValue.CHAR_ARRAY;
                        case Type.DOUBLE:
                            return TypeValue.DOUBLE_ARRAY;
                        case Type.FLOAT:
                            return TypeValue.FLOAT_ARRAY;
                        case Type.INT:
                            return TypeValue.INT_ARRAY;
                        case Type.LONG:
                            return TypeValue.LONG_ARRAY;
                        case Type.SHORT:
                            return TypeValue.SHORT_ARRAY;
                        default:
                            throw new IllegalArgumentException();
                    }
                }
            default:
                throw new IllegalArgumentException();
        }
    }
}
