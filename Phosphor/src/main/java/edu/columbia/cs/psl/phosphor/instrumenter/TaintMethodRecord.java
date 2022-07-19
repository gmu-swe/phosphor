package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.PreMain;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.control.ControlFlowStack;
import edu.columbia.cs.psl.phosphor.runtime.PhosphorStackFrame;
import edu.columbia.cs.psl.phosphor.runtime.ReflectionMasker;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.runtime.TaintSourceWrapper;
import edu.columbia.cs.psl.phosphor.struct.*;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
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
    COMBINE_TAGS_ON_OBJECT_CONTROL(INVOKESTATIC, Taint.class, "combineTagsOnObject", Void.TYPE, false, Object.class, PhosphorStackFrame.class),
    COMBINE_TAGS(INVOKESTATIC, Taint.class, "combineTags", TAINT_TAG_OBJ_CLASS, false, TAINT_TAG_OBJ_CLASS, TAINT_TAG_OBJ_CLASS),
    COMBINE_TAGS_CONTROL(INVOKESTATIC, Taint.class, "combineTags", TAINT_TAG_OBJ_CLASS, false, TAINT_TAG_OBJ_CLASS, PhosphorStackFrame.class),
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
    REMOVE_EXTRA_STACK_TRACE_ELEMENTS(INVOKESTATIC, ReflectionMasker.class, "removeExtraStackTraceElements", StackTraceElement[].class, false, StackTraceElement[].class, Class.class),
    REMOVE_TAINTED_INTERFACES(INVOKESTATIC, ReflectionMasker.class, "removeTaintedInterfaces", Class[].class, false, Class[].class),
    REMOVE_TAINTED_CONSTRUCTORS(INVOKESTATIC, ReflectionMasker.class, "removeTaintedConstructors", Constructor[].class, false, Constructor[].class),
    REMOVE_TAINTED_METHODS(INVOKESTATIC, ReflectionMasker.class, "removeTaintedMethods", Method[].class, false, Method[].class),
    REMOVE_TAINTED_FIELDS(INVOKESTATIC, ReflectionMasker.class, "removeTaintedFields", Field[].class, false, Field[].class),
    GET_ORIGINAL_CLASS(INVOKESTATIC, ReflectionMasker.class, "getOriginalClass", Class.class, false, Class.class),
    GET_ORIGINAL_CLASS_OBJECT_OUTPUT_STREAM(INVOKESTATIC, ReflectionMasker.class, "getOriginalClassObjectOutputStream", Class.class, false, Object.class),
    UNWRAP_RETURN(INVOKESTATIC, ReflectionMasker.class, "unwrapReturn", Object.class, false, Object.class, PhosphorStackFrame.class),
    FIX_ALL_ARGS(INVOKESTATIC, ReflectionMasker.class, "fixAllArgs", Object[].class, false, Object[].class, Object.class, PhosphorStackFrame.class),
    FIX_ALL_ARGS_CONSTRUCTOR(INVOKESTATIC, ReflectionMasker.class, "fixAllArgsConstructor", Object[].class, false, Object[].class, PhosphorStackFrame.class),
    IS_INSTANCE(INVOKESTATIC, ReflectionMasker.class, "isInstance", Boolean.TYPE, false, Class.class, Object.class, PhosphorStackFrame.class),

    INSTRUMENT_CLASS_BYTES(INVOKESTATIC, PreMain.class, "instrumentClassBytes", byte[].class, false, byte[].class),


    //Phosphor Stack Frame
    START_STACK_FRAME_TRACKING(INVOKESTATIC, PhosphorStackFrame.class, "initialize", Void.TYPE, false),
    PREPARE_FOR_CALL_DEBUG(INVOKEVIRTUAL, PhosphorStackFrame.class, "prepareForCall", Void.TYPE, false, String.class),
    PREPARE_FOR_CALL_FAST(INVOKEVIRTUAL, PhosphorStackFrame.class, "prepareForCall", Void.TYPE, false, int.class),
    PREPARE_FOR_CALL_PREV(INVOKEVIRTUAL, PhosphorStackFrame.class, "prepareForCallPrev", Void.TYPE, false),
    PREPARE_FOR_CALL_REFLECTIVE(INVOKESTATIC, ReflectionMasker.class, "prepareForCall", Void.TYPE, false, Method.class, PhosphorStackFrame.class),
    PREPARE_FOR_CALL_REFLECTIVE_CONSTRUCTOR(INVOKESTATIC, ReflectionMasker.class, "prepareForCall", Void.TYPE, false, Constructor.class, PhosphorStackFrame.class),
    STACK_FRAME_FOR_METHOD_DEBUG(INVOKESTATIC, PhosphorStackFrame.class, "forMethod", PhosphorStackFrame.class, false, String.class),
    STACK_FRAME_FOR_METHOD_FAST(INVOKESTATIC, PhosphorStackFrame.class, "forMethod", PhosphorStackFrame.class, false, int.class),
    GET_AND_CLEAR_CLEANUP_FLAG(INVOKEVIRTUAL, PhosphorStackFrame.class, "getAndClearCleanupFlag", boolean.class, false),
    POP_STACK_FRAME(INVOKEVIRTUAL, PhosphorStackFrame.class, "popStackFrameIfNeeded", Void.TYPE, false, boolean.class),
    SET_ARG_TAINT(INVOKEVIRTUAL, PhosphorStackFrame.class, "setArgTaint", Void.TYPE, false, Taint.class, int.class),
    GET_ARG_TAINT(INVOKEVIRTUAL, PhosphorStackFrame.class, "getArgTaint", Taint.class, false, int.class),
    GET_RETURN_TAINT(INVOKEVIRTUAL, PhosphorStackFrame.class, "getReturnTaint", Taint.class, false),
    SET_RETURN_TAINT(INVOKEVIRTUAL, PhosphorStackFrame.class, "setReturnTaint", Void.TYPE, false, Taint.class),
    SET_ARG_WRAPPER(INVOKEVIRTUAL, PhosphorStackFrame.class, "setArgWrapper", Void.TYPE, false, Object.class, int.class),
    GET_ARG_WRAPPER_GENERIC(INVOKEVIRTUAL, PhosphorStackFrame.class, "getArgWrapper", Object.class, false, int.class, Object.class),
    GET_ARG_WRAPPER_OBJECT(INVOKEVIRTUAL, PhosphorStackFrame.class, "getArgWrapper", LazyReferenceArrayObjTags.class, false, int.class, Object[].class),
    GET_ARG_WRAPPER_BOOLEAN(INVOKEVIRTUAL, PhosphorStackFrame.class, "getArgWrapper", LazyBooleanArrayObjTags.class, false, int.class, boolean[].class),
    GET_ARG_WRAPPER_BYTE(INVOKEVIRTUAL, PhosphorStackFrame.class, "getArgWrapper", LazyByteArrayObjTags.class, false, int.class, byte[].class),
    GET_ARG_WRAPPER_CHAR(INVOKEVIRTUAL, PhosphorStackFrame.class, "getArgWrapper", LazyCharArrayObjTags.class, false, int.class, char[].class),
    GET_ARG_WRAPPER_FLOAT(INVOKEVIRTUAL, PhosphorStackFrame.class, "getArgWrapper", LazyFloatArrayObjTags.class, false, int.class, float[].class),
    GET_ARG_WRAPPER_INT(INVOKEVIRTUAL, PhosphorStackFrame.class, "getArgWrapper", LazyIntArrayObjTags.class, false, int.class, int[].class),
    GET_ARG_WRAPPER_LONG(INVOKEVIRTUAL, PhosphorStackFrame.class, "getArgWrapper", LazyLongArrayObjTags.class, false, int.class, long[].class),
    GET_ARG_WRAPPER_SHORT(INVOKEVIRTUAL, PhosphorStackFrame.class, "getArgWrapper", LazyShortArrayObjTags.class, false, int.class, short[].class),
    GET_ARG_WRAPPER_DOUBLE(INVOKEVIRTUAL, PhosphorStackFrame.class, "getArgWrapper", LazyDoubleArrayObjTags.class, false, int.class, double[].class),


    SET_WRAPPED_RETURN(INVOKEVIRTUAL, PhosphorStackFrame.class, "setWrappedReturn", Void.TYPE, false, Object.class),
    GET_RETURN_WRAPPER_OBJECT(INVOKEVIRTUAL, PhosphorStackFrame.class, "getReturnWrapper", LazyReferenceArrayObjTags.class, false, Object[].class),
    GET_RETURN_WRAPPER_BOOLEAN(INVOKEVIRTUAL, PhosphorStackFrame.class, "getReturnWrapper", LazyBooleanArrayObjTags.class, false, boolean[].class),
    GET_RETURN_WRAPPER_BYTE(INVOKEVIRTUAL, PhosphorStackFrame.class, "getReturnWrapper", LazyByteArrayObjTags.class, false, byte[].class),
    GET_RETURN_WRAPPER_CHAR(INVOKEVIRTUAL, PhosphorStackFrame.class, "getReturnWrapper", LazyCharArrayObjTags.class, false, char[].class),
    GET_RETURN_WRAPPER_FLOAT(INVOKEVIRTUAL, PhosphorStackFrame.class, "getReturnWrapper", LazyFloatArrayObjTags.class, false, float[].class),
    GET_RETURN_WRAPPER_INT(INVOKEVIRTUAL, PhosphorStackFrame.class, "getReturnWrapper", LazyIntArrayObjTags.class, false, int[].class),
    GET_RETURN_WRAPPER_LONG(INVOKEVIRTUAL, PhosphorStackFrame.class, "getReturnWrapper", LazyLongArrayObjTags.class, false, long[].class),
    GET_RETURN_WRAPPER_SHORT(INVOKEVIRTUAL, PhosphorStackFrame.class, "getReturnWrapper", LazyShortArrayObjTags.class, false, short[].class),
    GET_RETURN_WRAPPER_DOUBLE(INVOKEVIRTUAL, PhosphorStackFrame.class, "getReturnWrapper", LazyDoubleArrayObjTags.class, false, double[].class),




    // Tainted array set methods
    TAINTED_BOOLEAN_ARRAY_SET(INVOKEVIRTUAL, LazyBooleanArrayObjTags.class, "set", Void.TYPE, false, int.class, boolean.class, Taint.class, Taint.class, PhosphorStackFrame.class),
    TAINTED_BYTE_ARRAY_SET(INVOKEVIRTUAL, LazyByteArrayObjTags.class, "set", Void.TYPE, false, int.class, byte.class, Taint.class, Taint.class, PhosphorStackFrame.class),
    TAINTED_CHAR_ARRAY_SET(INVOKEVIRTUAL, LazyCharArrayObjTags.class, "set", Void.TYPE, false, int.class, char.class, Taint.class, Taint.class, PhosphorStackFrame.class),
    TAINTED_DOUBLE_ARRAY_SET(INVOKEVIRTUAL, LazyDoubleArrayObjTags.class, "set", Void.TYPE, false, int.class, double.class, Taint.class, Taint.class, PhosphorStackFrame.class),
    TAINTED_FLOAT_ARRAY_SET(INVOKEVIRTUAL, LazyFloatArrayObjTags.class, "set", Void.TYPE, false, int.class, float.class, Taint.class, Taint.class, PhosphorStackFrame.class),
    TAINTED_INT_ARRAY_SET(INVOKEVIRTUAL, LazyIntArrayObjTags.class, "set", Void.TYPE, false, int.class, int.class, Taint.class, Taint.class, PhosphorStackFrame.class),
    TAINTED_LONG_ARRAY_SET(INVOKEVIRTUAL, LazyLongArrayObjTags.class, "set", Void.TYPE, false, int.class, long.class, Taint.class, Taint.class, PhosphorStackFrame.class),
    TAINTED_REFERENCE_ARRAY_SET(INVOKEVIRTUAL, LazyReferenceArrayObjTags.class, "set", Void.TYPE, false, int.class, Object.class, Taint.class, Taint.class, PhosphorStackFrame.class),
    TAINTED_SHORT_ARRAY_SET(INVOKEVIRTUAL, LazyShortArrayObjTags.class, "set", Void.TYPE, false, int.class, short.class, Taint.class, Taint.class, PhosphorStackFrame.class),

    // Tainted array get methods
    TAINTED_BOOLEAN_ARRAY_GET(INVOKEVIRTUAL, LazyBooleanArrayObjTags.class, "get", boolean.class, false, int.class, Taint.class, PhosphorStackFrame.class),
    TAINTED_BYTE_ARRAY_GET(INVOKEVIRTUAL, LazyByteArrayObjTags.class, "get", byte.class, false, int.class, Taint.class, PhosphorStackFrame.class),
    TAINTED_CHAR_ARRAY_GET(INVOKEVIRTUAL, LazyCharArrayObjTags.class, "get", char.class, false, int.class, Taint.class, PhosphorStackFrame.class),
    TAINTED_DOUBLE_ARRAY_GET(INVOKEVIRTUAL, LazyDoubleArrayObjTags.class, "get", double.class, false, int.class, Taint.class, PhosphorStackFrame.class),
    TAINTED_FLOAT_ARRAY_GET(INVOKEVIRTUAL, LazyFloatArrayObjTags.class, "get", float.class, false, int.class, Taint.class, PhosphorStackFrame.class),
    TAINTED_INT_ARRAY_GET(INVOKEVIRTUAL, LazyIntArrayObjTags.class, "get", int.class, false, int.class, Taint.class, PhosphorStackFrame.class),
    TAINTED_LONG_ARRAY_GET(INVOKEVIRTUAL, LazyLongArrayObjTags.class, "get", long.class, false, int.class, Taint.class, PhosphorStackFrame.class),
    TAINTED_REFERENCE_ARRAY_GET(INVOKEVIRTUAL, LazyReferenceArrayObjTags.class, "get", Object.class, false, int.class, Taint.class, PhosphorStackFrame.class),
    TAINTED_SHORT_ARRAY_GET(INVOKEVIRTUAL, LazyShortArrayObjTags.class, "get", short.class, false, int.class, Taint.class, PhosphorStackFrame.class),
    // Methods from TaintSourceWrapper
    AUTO_TAINT(INVOKEVIRTUAL, TaintSourceWrapper.class, "autoTaint", Object.class, false, Object.class, String.class, String.class, int.class);

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

    public static TaintMethodRecord getArgWrapperMethod(Type arrayType) {
        if (arrayType.getDimensions() > 1) {
            return GET_ARG_WRAPPER_OBJECT;
        }
        switch (arrayType.getElementType().getSort()) {
            case Type.OBJECT:
                return GET_ARG_WRAPPER_OBJECT;
            case Type.BOOLEAN:
                return GET_ARG_WRAPPER_BOOLEAN;
            case Type.BYTE:
                return GET_ARG_WRAPPER_BYTE;
            case Type.DOUBLE:
                return GET_ARG_WRAPPER_DOUBLE;
            case Type.FLOAT:
                return GET_ARG_WRAPPER_FLOAT;
            case Type.CHAR:
                return GET_ARG_WRAPPER_CHAR;
            case Type.INT:
                return GET_ARG_WRAPPER_INT;
            case Type.SHORT:
                return GET_ARG_WRAPPER_SHORT;
            case Type.LONG:
                return GET_ARG_WRAPPER_LONG;
            default:
                throw new UnsupportedOperationException();
        }
    }

    public static TaintMethodRecord getReturnWrapperMethod(Type arrayType) {
        if (arrayType.getDimensions() > 1) {
            return GET_RETURN_WRAPPER_OBJECT;
        }
        switch (arrayType.getElementType().getSort()) {
            case Type.OBJECT:
                return GET_RETURN_WRAPPER_OBJECT;
            case Type.BOOLEAN:
                return GET_RETURN_WRAPPER_BOOLEAN;
            case Type.BYTE:
                return GET_RETURN_WRAPPER_BYTE;
            case Type.DOUBLE:
                return GET_RETURN_WRAPPER_DOUBLE;
            case Type.FLOAT:
                return GET_RETURN_WRAPPER_FLOAT;
            case Type.CHAR:
                return GET_RETURN_WRAPPER_CHAR;
            case Type.INT:
                return GET_RETURN_WRAPPER_INT;
            case Type.SHORT:
                return GET_RETURN_WRAPPER_SHORT;
            case Type.LONG:
                return GET_RETURN_WRAPPER_LONG;
            default:
                throw new UnsupportedOperationException();
        }
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
