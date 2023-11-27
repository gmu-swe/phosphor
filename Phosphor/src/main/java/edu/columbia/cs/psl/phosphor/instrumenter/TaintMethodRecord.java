package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Phosphor;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.control.ControlFlowStack;
import edu.columbia.cs.psl.phosphor.runtime.*;
import edu.columbia.cs.psl.phosphor.struct.*;
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
    // Methods from MultiDArrayUtils
    BOX_IF_NECESSARY(INVOKESTATIC, MultiDArrayUtils.class, "boxIfNecessary", Object.class, false, Object.class),
    // Methods from ReflectionMasker
    REMOVE_EXTRA_STACK_TRACE_ELEMENTS(INVOKESTATIC, ReflectionMasker.class, "removeExtraStackTraceElements", StackTraceElement[].class, false, StackTraceElement[].class, Class.class),
    REMOVE_TAINTED_INTERFACES(INVOKESTATIC, ReflectionMasker.class, "removeTaintedInterfaces", Class[].class, false, Class[].class),
    REMOVE_TAINTED_CONSTRUCTORS(INVOKESTATIC, ReflectionMasker.class, "removeTaintedConstructors", Constructor[].class, false, Constructor[].class),
    REMOVE_TAINTED_METHODS(INVOKESTATIC, ReflectionMasker.class, "removeTaintedMethods", Method[].class, false, Method[].class),
    REMOVE_TAINTED_FIELDS(INVOKESTATIC, ReflectionMasker.class, "removeTaintedFields", Field[].class, false, Field[].class),
    GET_ORIGINAL_CLASS(INVOKESTATIC, ReflectionMasker.class, "getOriginalClass", Class.class, false, Class.class),
    GET_ORIGINAL_CLASS_OBJECT_OUTPUT_STREAM(INVOKESTATIC, ReflectionMasker.class, "getOriginalClassObjectOutputStream", Class.class, false, Object.class),
    UNWRAP_RETURN(INVOKESTATIC, ReflectionMasker.class, "unwrapReturn", Object.class, false, Object.class, PhosphorStackFrame.class),
    PREPARE_FOR_CALL_REFLECTIVE(INVOKESTATIC, ReflectionMasker.class, "prepareForCall", ReflectionMasker.MethodInvocationTuple.class, false, Method.class, Object.class, Object[].class, PhosphorStackFrame.class),
    PREPARE_FOR_CALL_REFLECTIVE_CONSTRUCTOR(INVOKESTATIC, ReflectionMasker.class, "prepareForCall", ReflectionMasker.ConstructorInvocationPair.class, false, Constructor.class, Object[].class, PhosphorStackFrame.class),
    IS_INSTANCE(INVOKESTATIC, ReflectionMasker.class, "isInstance", Boolean.TYPE, false, Class.class, Object.class, PhosphorStackFrame.class),

    INSTRUMENT_CLASS_BYTES(INVOKESTATIC, Phosphor.class, "instrumentClassBytes", byte[].class, false, byte[].class),
    INSTRUMENT_CLASS_BYTES_ANONYMOUS(INVOKESTATIC, Phosphor.class, "instrumentClassBytesAnonymous", byte[].class, false, byte[].class),

    //Phosphor Stack Frame
    START_STACK_FRAME_TRACKING(INVOKESTATIC, PhosphorStackFrame.class, "initialize", Void.TYPE, false),
    PREPARE_FOR_CALL_DEBUG(INVOKEVIRTUAL, PhosphorStackFrame.class, "prepareForCall", Void.TYPE, false, String.class),
    PREPARE_FOR_CALL_FAST(INVOKEVIRTUAL, PhosphorStackFrame.class, "prepareForCall", Void.TYPE, false, int.class),
    PREPARE_FOR_CALL_PATCHED(INVOKEVIRTUAL, PhosphorStackFrame.class, "prepareForCallPatched", Void.TYPE, false, int.class),
    PREPARE_FOR_CALL_PREV(INVOKEVIRTUAL, PhosphorStackFrame.class, "prepareForCallPrev", Void.TYPE, false),
    CHECK_STACK_FRAME_TARGET(INVOKEVIRTUAL, PhosphorStackFrame.class, "checkTarget", Void.TYPE, false, String.class),
    STACK_FRAME_FOR_METHOD_DEBUG(INVOKESTATIC, PhosphorStackFrame.class, "forMethod", PhosphorStackFrame.class, false, String.class),
    STACK_FRAME_FOR_METHOD_FAST(INVOKESTATIC, PhosphorStackFrame.class, "forMethod", PhosphorStackFrame.class, false, int.class),
    GET_AND_CLEAR_CLEANUP_FLAG(INVOKEVIRTUAL, PhosphorStackFrame.class, "getAndClearCleanupFlag", boolean.class, false),
    POP_STACK_FRAME(INVOKEVIRTUAL, PhosphorStackFrame.class, "popStackFrameIfNeeded", Void.TYPE, false, boolean.class),
    SET_ARG_TAINT(INVOKEVIRTUAL, PhosphorStackFrame.class, "setArgTaint", Void.TYPE, false, Taint.class, int.class),
    GET_ARG_TAINT(INVOKEVIRTUAL, PhosphorStackFrame.class, "getArgTaint", Taint.class, false, int.class),
    GET_RETURN_TAINT(INVOKEVIRTUAL, PhosphorStackFrame.class, "getReturnTaint", Taint.class, false),
    SET_RETURN_TAINT(INVOKEVIRTUAL, PhosphorStackFrame.class, "setReturnTaint", Void.TYPE, false, Taint.class),
    SET_ARG_WRAPPER(INVOKEVIRTUAL, PhosphorStackFrame.class, "setArgWrapper", Void.TYPE, false, Object.class, int.class),
    SET_CALLER_CLASS_WRAPPER(INVOKEVIRTUAL, PhosphorStackFrame.class, "setCallerClassWrapper", Void.TYPE, false, Class.class),
    GET_CALLER_CLASS_WRAPPER(INVOKEVIRTUAL, PhosphorStackFrame.class, "getCallerClassWrapper", Class.class, false, Class.class, Class.class),
    GET_ARG_WRAPPER_GENERIC(INVOKEVIRTUAL, PhosphorStackFrame.class, "getArgWrapper", Object.class, false, int.class, Object.class),
    GET_ARG_WRAPPER_OBJECT(INVOKEVIRTUAL, PhosphorStackFrame.class, "getArgWrapper", TaggedReferenceArray.class, false, int.class, Object[].class),
    GET_ARG_WRAPPER_BOOLEAN(INVOKEVIRTUAL, PhosphorStackFrame.class, "getArgWrapper", TaggedBooleanArray.class, false, int.class, boolean[].class),
    GET_ARG_WRAPPER_BYTE(INVOKEVIRTUAL, PhosphorStackFrame.class, "getArgWrapper", TaggedByteArray.class, false, int.class, byte[].class),
    GET_ARG_WRAPPER_CHAR(INVOKEVIRTUAL, PhosphorStackFrame.class, "getArgWrapper", TaggedCharArray.class, false, int.class, char[].class),
    GET_ARG_WRAPPER_FLOAT(INVOKEVIRTUAL, PhosphorStackFrame.class, "getArgWrapper", TaggedFloatArray.class, false, int.class, float[].class),
    GET_ARG_WRAPPER_INT(INVOKEVIRTUAL, PhosphorStackFrame.class, "getArgWrapper", TaggedIntArray.class, false, int.class, int[].class),
    GET_ARG_WRAPPER_LONG(INVOKEVIRTUAL, PhosphorStackFrame.class, "getArgWrapper", TaggedLongArray.class, false, int.class, long[].class),
    GET_ARG_WRAPPER_SHORT(INVOKEVIRTUAL, PhosphorStackFrame.class, "getArgWrapper", TaggedShortArray.class, false, int.class, short[].class),
    GET_ARG_WRAPPER_DOUBLE(INVOKEVIRTUAL, PhosphorStackFrame.class, "getArgWrapper", TaggedDoubleArray.class, false, int.class, double[].class),


    SET_WRAPPED_RETURN(INVOKEVIRTUAL, PhosphorStackFrame.class, "setWrappedReturn", Void.TYPE, false, Object.class),
    GET_RETURN_WRAPPER_OBJECT(INVOKEVIRTUAL, PhosphorStackFrame.class, "getReturnWrapper", TaggedReferenceArray.class, false, Object[].class),
    GET_RETURN_WRAPPER_BOOLEAN(INVOKEVIRTUAL, PhosphorStackFrame.class, "getReturnWrapper", TaggedBooleanArray.class, false, boolean[].class),
    GET_RETURN_WRAPPER_BYTE(INVOKEVIRTUAL, PhosphorStackFrame.class, "getReturnWrapper", TaggedByteArray.class, false, byte[].class),
    GET_RETURN_WRAPPER_CHAR(INVOKEVIRTUAL, PhosphorStackFrame.class, "getReturnWrapper", TaggedCharArray.class, false, char[].class),
    GET_RETURN_WRAPPER_FLOAT(INVOKEVIRTUAL, PhosphorStackFrame.class, "getReturnWrapper", TaggedFloatArray.class, false, float[].class),
    GET_RETURN_WRAPPER_INT(INVOKEVIRTUAL, PhosphorStackFrame.class, "getReturnWrapper", TaggedIntArray.class, false, int[].class),
    GET_RETURN_WRAPPER_LONG(INVOKEVIRTUAL, PhosphorStackFrame.class, "getReturnWrapper", TaggedLongArray.class, false, long[].class),
    GET_RETURN_WRAPPER_SHORT(INVOKEVIRTUAL, PhosphorStackFrame.class, "getReturnWrapper", TaggedShortArray.class, false, short[].class),
    GET_RETURN_WRAPPER_DOUBLE(INVOKEVIRTUAL, PhosphorStackFrame.class, "getReturnWrapper", TaggedDoubleArray.class, false, double[].class),

    // Tainted array set methods
    TAINTED_BOOLEAN_ARRAY_SET(INVOKEVIRTUAL, TaggedBooleanArray.class, "set", Void.TYPE, false, int.class, boolean.class, Taint.class, Taint.class, PhosphorStackFrame.class),
    TAINTED_BYTE_ARRAY_SET(INVOKEVIRTUAL, TaggedByteArray.class, "set", Void.TYPE, false, int.class, byte.class, Taint.class, Taint.class, PhosphorStackFrame.class),
    TAINTED_CHAR_ARRAY_SET(INVOKEVIRTUAL, TaggedCharArray.class, "set", Void.TYPE, false, int.class, char.class, Taint.class, Taint.class, PhosphorStackFrame.class),
    TAINTED_DOUBLE_ARRAY_SET(INVOKEVIRTUAL, TaggedDoubleArray.class, "set", Void.TYPE, false, int.class, double.class, Taint.class, Taint.class, PhosphorStackFrame.class),
    TAINTED_FLOAT_ARRAY_SET(INVOKEVIRTUAL, TaggedFloatArray.class, "set", Void.TYPE, false, int.class, float.class, Taint.class, Taint.class, PhosphorStackFrame.class),
    TAINTED_INT_ARRAY_SET(INVOKEVIRTUAL, TaggedIntArray.class, "set", Void.TYPE, false, int.class, int.class, Taint.class, Taint.class, PhosphorStackFrame.class),
    TAINTED_LONG_ARRAY_SET(INVOKEVIRTUAL, TaggedLongArray.class, "set", Void.TYPE, false, int.class, long.class, Taint.class, Taint.class, PhosphorStackFrame.class),
    TAINTED_REFERENCE_ARRAY_SET(INVOKEVIRTUAL, TaggedReferenceArray.class, "set", Void.TYPE, false, int.class, Object.class, Taint.class, Taint.class, PhosphorStackFrame.class),
    TAINTED_SHORT_ARRAY_SET(INVOKEVIRTUAL, TaggedShortArray.class, "set", Void.TYPE, false, int.class, short.class, Taint.class, Taint.class, PhosphorStackFrame.class),

    // Tainted array get methods
    TAINTED_BOOLEAN_ARRAY_GET(INVOKEVIRTUAL, TaggedBooleanArray.class, "get", boolean.class, false, int.class, Taint.class, PhosphorStackFrame.class),
    TAINTED_BYTE_ARRAY_GET(INVOKEVIRTUAL, TaggedByteArray.class, "get", byte.class, false, int.class, Taint.class, PhosphorStackFrame.class),
    TAINTED_CHAR_ARRAY_GET(INVOKEVIRTUAL, TaggedCharArray.class, "get", char.class, false, int.class, Taint.class, PhosphorStackFrame.class),
    TAINTED_DOUBLE_ARRAY_GET(INVOKEVIRTUAL, TaggedDoubleArray.class, "get", double.class, false, int.class, Taint.class, PhosphorStackFrame.class),
    TAINTED_FLOAT_ARRAY_GET(INVOKEVIRTUAL, TaggedFloatArray.class, "get", float.class, false, int.class, Taint.class, PhosphorStackFrame.class),
    TAINTED_INT_ARRAY_GET(INVOKEVIRTUAL, TaggedIntArray.class, "get", int.class, false, int.class, Taint.class, PhosphorStackFrame.class),
    TAINTED_LONG_ARRAY_GET(INVOKEVIRTUAL, TaggedLongArray.class, "get", long.class, false, int.class, Taint.class, PhosphorStackFrame.class),
    TAINTED_REFERENCE_ARRAY_GET(INVOKEVIRTUAL, TaggedReferenceArray.class, "get", Object.class, false, int.class, Taint.class, PhosphorStackFrame.class),
    TAINTED_SHORT_ARRAY_GET(INVOKEVIRTUAL, TaggedShortArray.class, "get", short.class, false, int.class, Taint.class, PhosphorStackFrame.class),
    // Methods from TaintSourceWrapper
    AUTO_TAINT(INVOKEVIRTUAL, TaintSourceWrapper.class, "autoTaint", Object.class, false, Object.class, String.class, String.class, int.class),
    // TaggedReferenceArray
    TAINTED_REFERENCE_ARRAY_UNWRAP(INVOKESTATIC, TaggedReferenceArray.class, "unwrap", Object.class, false, Object.class);

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
