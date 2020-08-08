package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.control.ControlFlowStack;
import edu.columbia.cs.psl.phosphor.runtime.ReflectionMasker;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.struct.*;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static edu.columbia.cs.psl.phosphor.Configuration.TAINT_TAG_OBJ_CLASS;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

/**
 * Represents some method to which Phosphor adds calls during instrumentation.
 * Stores the information needed by an ASM method visitor to add a call to the method.
 */
public enum TaintMethodRecord implements MethodRecord {

    // Methods from Taint
    COMBINE_TAGS_ON_OBJECT_CONTROL(INVOKESTATIC, Taint.class, "combineTagsOnObject", Void.TYPE, false, Object.class, ControlFlowStack.class),
    COMBINE_TAGS(INVOKESTATIC, Taint.class, "combineTags", TAINT_TAG_OBJ_CLASS, false, TAINT_TAG_OBJ_CLASS, TAINT_TAG_OBJ_CLASS),
    COMBINE_TAGS_CONTROL(INVOKESTATIC, Taint.class, "combineTags", TAINT_TAG_OBJ_CLASS, false, TAINT_TAG_OBJ_CLASS, ControlFlowStack.class),
    NEW_EMPTY_TAINT(INVOKESTATIC, Taint.class, "emptyTaint", TAINT_TAG_OBJ_CLASS, false),
    // Methods from TaintUtils
    GET_TAINT_OBJECT(INVOKESTATIC, TaintUtils.class, "getTaintObj", TAINT_TAG_OBJ_CLASS, false, Object.class),
    GET_TAINT_COPY_SIMPLE(INVOKESTATIC, TaintUtils.class, "getTaintCopySimple", TAINT_TAG_OBJ_CLASS, false, Object.class),
    ENSURE_UNBOXED(INVOKESTATIC, TaintUtils.class, "ensureUnboxed", Object.class, false, Object.class),
    // Methods from ControlFlowStack
    CONTROL_STACK_ENABLE(INVOKEVIRTUAL, ControlFlowStack.class, "enable", Void.TYPE, false),
    CONTROL_STACK_DISABLE(INVOKEVIRTUAL, ControlFlowStack.class, "disable", Void.TYPE, false),
    CONTROL_STACK_POP_FRAME(INVOKEVIRTUAL, ControlFlowStack.class, "popFrame", Void.TYPE, false),
    CONTROL_STACK_PUSH_FRAME(INVOKEVIRTUAL, ControlFlowStack.class, "pushFrame", Void.TYPE, false),
    CONTROL_STACK_COPY_TOP(INVOKEVIRTUAL, ControlFlowStack.class, "copyTop", ControlFlowStack.class, false),
    CONTROL_STACK_UNINSTRUMENTED_WRAPPER(INVOKEVIRTUAL, ControlFlowStack.class, "enteringUninstrumentedWrapper", Void.TYPE, false),
    // Methods from MultiDTaintedArray
    BOX_IF_NECESSARY(INVOKESTATIC, MultiDTaintedArray.class, "boxIfNecessary", Object.class, false, Object.class),
    // Methods from ReflectionMasker
    REMOVE_EXTRA_STACK_TRACE_ELEMENTS(INVOKESTATIC, ReflectionMasker.class, "removeExtraStackTraceElements", TaintedReferenceWithObjTag.class, false, TaintedReferenceWithObjTag.class, Class.class),
    REMOVE_TAINTED_INTERFACES(INVOKESTATIC, ReflectionMasker.class, "removeTaintedInterfaces", Class[].class, false, TaintedReferenceWithObjTag.class, TaintedReferenceWithObjTag.class),
    REMOVE_TAINTED_CONSTRUCTORS(INVOKESTATIC, ReflectionMasker.class, "removeTaintedConstructors", TaintedReferenceWithObjTag.class, false, TaintedReferenceWithObjTag.class),
    REMOVE_TAINTED_METHODS(INVOKESTATIC, ReflectionMasker.class, "removeTaintedMethods", TaintedReferenceWithObjTag.class, false, TaintedReferenceWithObjTag.class, boolean.class),
    REMOVE_TAINTED_FIELDS(INVOKESTATIC, ReflectionMasker.class, "removeTaintedFields", TaintedReferenceWithObjTag.class, false, TaintedReferenceWithObjTag.class),
    GET_ORIGINAL_CLASS(INVOKESTATIC, ReflectionMasker.class, "getOriginalClass", Class.class, false, Class.class),
    GET_ORIGINAL_CLASS_OBJECT_OUTPUT_STREAM(INVOKESTATIC, ReflectionMasker.class, "getOriginalClassObjectOutputStream", Class.class, false, Object.class),
    GET_ORIGINAL_METHOD(INVOKESTATIC, ReflectionMasker.class, "getOriginalMethod", Method.class, false, Method.class),
    GET_ORIGINAL_CONSTRUCTOR(INVOKESTATIC, ReflectionMasker.class, "getOriginalConstructor", Constructor.class, false, Constructor.class),

    FIX_ALL_ARGS_METHOD(INVOKESTATIC, ReflectionMasker.class, "fixAllArgs", MethodInvoke.class, false, Method.class, Taint.class, Object.class, Taint.class, LazyReferenceArrayObjTags.class, Taint.class, TaintedReferenceWithObjTag.class, Object[].class, Object.class),
    FIX_ALL_ARGS_METHOD_CONTROL(INVOKESTATIC, ReflectionMasker.class, "fixAllArgs", MethodInvoke.class, false, Method.class, Taint.class, Object.class, Taint.class, LazyReferenceArrayObjTags.class, Taint.class, ControlFlowStack.class, TaintedReferenceWithObjTag.class, Object[].class, Object.class),
    FIX_ALL_ARGS_CONSTRUCTOR(INVOKESTATIC, ReflectionMasker.class, "fixAllArgs", MethodInvoke.class, false, Constructor.class, Taint.class, LazyReferenceArrayObjTags.class, Taint.class, TaintedReferenceWithObjTag.class, Object[].class, Object.class),
    FIX_ALL_ARGS_CONSTRUCTOR_CONTROL(INVOKESTATIC, ReflectionMasker.class, "fixAllArgs", MethodInvoke.class, false, Constructor.class, Taint.class, LazyReferenceArrayObjTags.class, Taint.class, ControlFlowStack.class, TaintedReferenceWithObjTag.class, Object[].class, Object.class),

    GET_DECLARED_METHOD(INVOKESTATIC, ReflectionMasker.class, "getDeclaredMethod", TaintedReferenceWithObjTag.class, false, Class.class, Taint.class, String.class, Taint.class, LazyReferenceArrayObjTags.class, Taint.class, TaintedReferenceWithObjTag.class, Class[].class, Method.class),
    GET_METHOD(INVOKESTATIC, ReflectionMasker.class, "getMethod", TaintedReferenceWithObjTag.class, false, Class.class, Taint.class, String.class, Taint.class, LazyReferenceArrayObjTags.class, Taint.class, TaintedReferenceWithObjTag.class, Class[].class, Method.class),
    ADD_TYPE_PARAMS(INVOKESTATIC, ReflectionMasker.class, "addTypeParams", LazyReferenceArrayObjTags.class, false, Class.class, LazyReferenceArrayObjTags.class, boolean.class),
    IS_INSTANCE(INVOKESTATIC, ReflectionMasker.class, "isInstance", TaintedBooleanWithObjTag.class, false, Class.class, Taint.class, Object.class, Taint.class, TaintedBooleanWithObjTag.class),
    ENUM_VALUE_OF(INVOKESTATIC, ReflectionMasker.class, "propagateEnumValueOf", TaintedReferenceWithObjTag.class, false, TaintedReferenceWithObjTag.class, Taint.class),
    // Tainted array set methods
    TAINTED_BOOLEAN_ARRAY_SET(INVOKEVIRTUAL, LazyBooleanArrayObjTags.class, "set", Void.TYPE, false, Taint.class, int.class, Taint.class, boolean.class, Taint.class),
    TAINTED_BYTE_ARRAY_SET(INVOKEVIRTUAL, LazyByteArrayObjTags.class, "set", Void.TYPE, false, Taint.class, int.class, Taint.class, byte.class, Taint.class),
    TAINTED_CHAR_ARRAY_SET(INVOKEVIRTUAL, LazyCharArrayObjTags.class, "set", Void.TYPE, false, Taint.class, int.class, Taint.class, char.class, Taint.class),
    TAINTED_DOUBLE_ARRAY_SET(INVOKEVIRTUAL, LazyDoubleArrayObjTags.class, "set", Void.TYPE, false, Taint.class, int.class, Taint.class, double.class, Taint.class),
    TAINTED_FLOAT_ARRAY_SET(INVOKEVIRTUAL, LazyFloatArrayObjTags.class, "set", Void.TYPE, false, Taint.class, int.class, Taint.class, float.class, Taint.class),
    TAINTED_INT_ARRAY_SET(INVOKEVIRTUAL, LazyIntArrayObjTags.class, "set", Void.TYPE, false, Taint.class, int.class, Taint.class, int.class, Taint.class),
    TAINTED_LONG_ARRAY_SET(INVOKEVIRTUAL, LazyLongArrayObjTags.class, "set", Void.TYPE, false, Taint.class, int.class, Taint.class, long.class, Taint.class),
    TAINTED_REFERENCE_ARRAY_SET(INVOKEVIRTUAL, LazyReferenceArrayObjTags.class, "set", Void.TYPE, false, Taint.class, int.class, Taint.class, Object.class, Taint.class),
    TAINTED_SHORT_ARRAY_SET(INVOKEVIRTUAL, LazyShortArrayObjTags.class, "set", Void.TYPE, false, Taint.class, int.class, Taint.class, short.class, Taint.class),
    // Tainted array get methods
    TAINTED_BOOLEAN_ARRAY_GET(INVOKEVIRTUAL, LazyBooleanArrayObjTags.class, "get", TaintedBooleanWithObjTag.class, false, Taint.class, int.class, Taint.class, TaintedBooleanWithObjTag.class),
    TAINTED_BYTE_ARRAY_GET(INVOKEVIRTUAL, LazyByteArrayObjTags.class, "get", TaintedByteWithObjTag.class, false, Taint.class, int.class, Taint.class, TaintedByteWithObjTag.class),
    TAINTED_CHAR_ARRAY_GET(INVOKEVIRTUAL, LazyCharArrayObjTags.class, "get", TaintedCharWithObjTag.class, false, Taint.class, int.class, Taint.class, TaintedCharWithObjTag.class),
    TAINTED_DOUBLE_ARRAY_GET(INVOKEVIRTUAL, LazyDoubleArrayObjTags.class, "get", TaintedDoubleWithObjTag.class, false, Taint.class, int.class, Taint.class, TaintedDoubleWithObjTag.class),
    TAINTED_FLOAT_ARRAY_GET(INVOKEVIRTUAL, LazyFloatArrayObjTags.class, "get", TaintedFloatWithObjTag.class, false, Taint.class, int.class, Taint.class, TaintedFloatWithObjTag.class),
    TAINTED_INT_ARRAY_GET(INVOKEVIRTUAL, LazyIntArrayObjTags.class, "get", TaintedIntWithObjTag.class, false, Taint.class, int.class, Taint.class, TaintedIntWithObjTag.class),
    TAINTED_LONG_ARRAY_GET(INVOKEVIRTUAL, LazyLongArrayObjTags.class, "get", TaintedLongWithObjTag.class, false, Taint.class, int.class, Taint.class, TaintedLongWithObjTag.class),
    TAINTED_REFERENCE_ARRAY_GET(INVOKEVIRTUAL, LazyReferenceArrayObjTags.class, "get", TaintedReferenceWithObjTag.class, false, Taint.class, int.class, Taint.class, TaintedReferenceWithObjTag.class),
    TAINTED_SHORT_ARRAY_GET(INVOKEVIRTUAL, LazyShortArrayObjTags.class, "get", TaintedShortWithObjTag.class, false, Taint.class, int.class, Taint.class, TaintedShortWithObjTag.class);

    private final int opcode;
    private final String owner;
    private final String name;
    private final String descriptor;
    private final boolean isInterface;
    private final Class<?> returnType;

    /**
     * Constructs a new method.
     *
     * @param opcode         the opcode of the type instruction associated with the method
     * @param owner          the internal name of the method's owner class
     * @param name           the method's name
     * @param returnType     the class of the method's return type
     * @param isInterface    if the method's owner class is an interface
     * @param parameterTypes the types of the parameters of the method
     */
    TaintMethodRecord(int opcode, Class<?> owner, String name, Class<?> returnType, boolean isInterface, Class<?>... parameterTypes) {
        this.opcode = opcode;
        this.owner = Type.getInternalName(owner);
        this.name = name;
        this.isInterface = isInterface;
        this.descriptor = MethodRecord.createDescriptor(returnType, parameterTypes);
        this.returnType = returnType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getOpcode() {
        return opcode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOwner() {
        return owner;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescriptor() {
        return descriptor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isInterface() {
        return isInterface;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getReturnType() {
        return returnType;
    }

    public static TaintMethodRecord getTaintedArrayRecord(int opcode, String arrayReferenceType) {
        switch(opcode) {
            case Opcodes.LASTORE:
                return TAINTED_LONG_ARRAY_SET;
            case Opcodes.DASTORE:
                return TAINTED_DOUBLE_ARRAY_SET;
            case Opcodes.IASTORE:
                return TAINTED_INT_ARRAY_SET;
            case Opcodes.FASTORE:
                return TAINTED_FLOAT_ARRAY_SET;
            case Opcodes.BASTORE:
                return arrayReferenceType.contains("Boolean") ? TAINTED_BOOLEAN_ARRAY_SET : TAINTED_BYTE_ARRAY_SET;
            case Opcodes.CASTORE:
                return TAINTED_CHAR_ARRAY_SET;
            case Opcodes.SASTORE:
                return TAINTED_SHORT_ARRAY_SET;
            case Opcodes.AASTORE:
                return TAINTED_REFERENCE_ARRAY_SET;
            case Opcodes.LALOAD:
                return TAINTED_LONG_ARRAY_GET;
            case Opcodes.DALOAD:
                return TAINTED_DOUBLE_ARRAY_GET;
            case Opcodes.IALOAD:
                return TAINTED_INT_ARRAY_GET;
            case Opcodes.FALOAD:
                return TAINTED_FLOAT_ARRAY_GET;
            case Opcodes.BALOAD:
                return arrayReferenceType.contains("Boolean") ? TAINTED_BOOLEAN_ARRAY_GET : TAINTED_BYTE_ARRAY_GET;
            case Opcodes.CALOAD:
                return TAINTED_CHAR_ARRAY_GET;
            case Opcodes.SALOAD:
                return TAINTED_SHORT_ARRAY_GET;
            case Opcodes.AALOAD:
                return TAINTED_REFERENCE_ARRAY_GET;
            default:
                throw new IllegalArgumentException("Opcode must be an array store or load operation.");
        }
    }
}
