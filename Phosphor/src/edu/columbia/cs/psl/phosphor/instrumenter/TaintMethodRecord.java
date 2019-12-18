package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.runtime.ControlTaintTagStackPool;
import edu.columbia.cs.psl.phosphor.runtime.ReflectionMasker;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.struct.*;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static edu.columbia.cs.psl.phosphor.Configuration.TAINT_TAG_OBJ_CLASS;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

/**
 * Represent some method that is used to ensure that taint tags are correctly propagated. Stores the information needed
 * by ASM method visitors to visit the method.
 */
public enum TaintMethodRecord {

    // Methods from Taint
    COMBINE_TAGS_ON_OBJECT_CONTROL(INVOKESTATIC, Taint.class, "combineTagsOnObject", Void.TYPE, false, Object.class, ControlTaintTagStack.class),
    COMBINE_TAGS_IN_PLACE(INVOKESTATIC, Taint.class, "combineTagsInPlace", Void.TYPE, false, Object.class, Taint.class),
    COMBINE_TAGS(INVOKESTATIC, Taint.class, "combineTags", TAINT_TAG_OBJ_CLASS, false, TAINT_TAG_OBJ_CLASS, TAINT_TAG_OBJ_CLASS),
    COMBINE_TAGS_CONTROL(INVOKESTATIC, Taint.class, "combineTags", TAINT_TAG_OBJ_CLASS, false, TAINT_TAG_OBJ_CLASS, ControlTaintTagStack.class),
    //    COPY_TAINT(INVOKESTATIC, Taint.class, "copyTaint", TAINT_TAG_OBJ_CLASS, false, TAINT_TAG_OBJ_CLASS),
    // Methods from TaintUtils
    GET_TAINT_OBJECT(INVOKESTATIC, TaintUtils.class, "getTaintObj", TAINT_TAG_OBJ_CLASS, false, Object.class),
    GET_TAINT_COPY_SIMPLE(INVOKESTATIC, TaintUtils.class, "getTaintCopySimple", TAINT_TAG_OBJ_CLASS, false, Object.class),
    ENSURE_UNBOXED(INVOKESTATIC, TaintUtils.class, "ensureUnboxed", Object.class, false, Object.class),
    // Methods from ControlTaintTagStack
    CONTROL_STACK_PUSH_TAG_EXCEPTION(INVOKEVIRTUAL, ControlTaintTagStack.class, "push", int[].class, false, TAINT_TAG_OBJ_CLASS, int[].class, int.class, int.class, ExceptionalTaintData.class, boolean.class),
    CONTROL_STACK_PUSH_TAG(INVOKEVIRTUAL, ControlTaintTagStack.class, "push", int[].class, false, TAINT_TAG_OBJ_CLASS, int[].class, int.class, int.class, boolean.class),
    CONTROL_STACK_POP_EXCEPTION(INVOKEVIRTUAL, ControlTaintTagStack.class, "pop", Void.TYPE, false, int[].class, int.class, ExceptionalTaintData.class),
    CONTROL_STACK_POP(INVOKEVIRTUAL, ControlTaintTagStack.class, "pop", Void.TYPE, false, int[].class, int.class),
    CONTROL_STACK_POP_ALL_EXCEPTION(INVOKEVIRTUAL, ControlTaintTagStack.class, "pop", Void.TYPE, false, int[].class, ExceptionalTaintData.class),
    CONTROL_STACK_POP_ALL(INVOKEVIRTUAL, ControlTaintTagStack.class, "pop", Void.TYPE, false, int[].class),
    CONTROL_STACK_ENABLE(INVOKEVIRTUAL, ControlTaintTagStack.class, "enable", Void.TYPE, false),
    CONTROL_STACK_DISABLE(INVOKEVIRTUAL, ControlTaintTagStack.class, "disable", Void.TYPE, false),
    CONTROL_STACK_COPY_TAG(INVOKEVIRTUAL, ControlTaintTagStack.class, "copyTag", TAINT_TAG_OBJ_CLASS, false),
    CONTROL_STACK_COPY_TAG_EXCEPTIONS(INVOKEVIRTUAL, ControlTaintTagStack.class, "copyTagExceptions", TAINT_TAG_OBJ_CLASS, false),
    CONTROL_STACK_EXCEPTION_HANDLER_START(INVOKEVIRTUAL, ControlTaintTagStack.class, "exceptionHandlerStart", EnqueuedTaint.class, false, Throwable.class, EnqueuedTaint.class),
    CONTROL_STACK_EXCEPTION_HANDLER_START_VOID(INVOKEVIRTUAL, ControlTaintTagStack.class, "exceptionHandlerStart", Void.TYPE, false, Class.class),
    CONTROL_STACK_EXCEPTION_HANDLER_END(INVOKEVIRTUAL, ControlTaintTagStack.class, "exceptionHandlerEnd", Void.TYPE, false, EnqueuedTaint.class),
    CONTROL_STACK_TRY_BLOCK_END(INVOKEVIRTUAL, ControlTaintTagStack.class, "tryBlockEnd", Void.TYPE, false, Class.class),
    CONTROL_STACK_APPLY_POSSIBLY_UNTHROWN_EXCEPTION(INVOKEVIRTUAL, ControlTaintTagStack.class, "applyPossiblyUnthrownExceptionToTaint", Void.TYPE, false, Class.class),
    CONTROL_STACK_ADD_UNTHROWN_EXCEPTION(INVOKEVIRTUAL, ControlTaintTagStack.class, "addUnthrownException", Void.TYPE, false, ExceptionalTaintData.class, Class.class),
    CONTROL_STACK_FACTORY(INVOKESTATIC, ControlTaintTagStack.class, "factory", ControlTaintTagStack.class, false),
    CONTROL_STACK_COPY_REVISION_EXCLUDED_TAG(INVOKEVIRTUAL, ControlTaintTagStack.class, "copyRevisionExcludedTag", Taint.class, false),
    // Methods from ControlTaintTagStackPool
    CONTROL_STACK_POOL_INSTANCE(INVOKESTATIC, ControlTaintTagStackPool.class, "instance", ControlTaintTagStack.class, false),
    // Methods from MultiDTaintedArray
    BOX_IF_NECESSARY(INVOKESTATIC, MultiDTaintedArray.class, "boxIfNecessary", Object.class, false, Object.class),
    // Methods from ReflectionMasker
    REMOVE_EXTRA_STACK_TRACE_ELEMENTS(INVOKESTATIC, ReflectionMasker.class, "removeExtraStackTraceElements", StackTraceElement[].class, false, StackTraceElement[].class, Class.class),
    REMOVE_TAINTED_INTERFACES(INVOKESTATIC, ReflectionMasker.class, "removeTaintedInterfaces", Class[].class, false, Class[].class),
    REMOVE_TAINTED_CONSTRUCTORS(INVOKESTATIC, ReflectionMasker.class, "removeTaintedConstructors", Constructor[].class, false, Constructor[].class),
    REMOVE_TAINTED_METHODS(INVOKESTATIC, ReflectionMasker.class, "removeTaintedMethods", Method[].class, false, Method[].class, boolean.class),
    REMOVE_TAINTED_FIELDS(INVOKESTATIC, ReflectionMasker.class, "removeTaintedFields", Field[].class, false, Field[].class),
    GET_ORIGINAL_CLASS(INVOKESTATIC, ReflectionMasker.class, "getOriginalClass", Class.class, false, Class.class),
    GET_ORIGINAL_CLASS_OBJECT_OUTPUT_STREAM(INVOKESTATIC, ReflectionMasker.class, "getOriginalClassObjectOutputStream", Class.class, false, Object.class),
    GET_ORIGINAL_METHOD(INVOKESTATIC, ReflectionMasker.class, "getOriginalMethod", Method.class, false, Method.class),
    GET_ORIGINAL_CONSTRUCTOR(INVOKESTATIC, ReflectionMasker.class, "getOriginalConstructor", Constructor.class, false, Constructor.class),
    FIX_ALL_ARGS_METHOD(INVOKESTATIC, ReflectionMasker.class, "fixAllArgs", MethodInvoke.class, false, Method.class, Object.class, Object[].class),
    FIX_ALL_ARGS_METHOD_CONTROL(INVOKESTATIC, ReflectionMasker.class, "fixAllArgs", MethodInvoke.class, false, Method.class, Object.class, Object[].class, ControlTaintTagStack.class),
    FIX_ALL_ARGS_CONSTRUCTOR(INVOKESTATIC, ReflectionMasker.class, "fixAllArgs", Object[].class, false, Object[].class, Constructor.class),
    FIX_ALL_ARGS_CONSTRUCTOR_CONTROL(INVOKESTATIC, ReflectionMasker.class, "fixAllArgs", Object[].class, false, Object[].class, Constructor.class, ControlTaintTagStack.class),
    GET_DECLARED_METHOD(INVOKESTATIC, ReflectionMasker.class, "getDeclaredMethod", Method.class, false, Class.class, String.class, Class[].class),
    GET_METHOD(INVOKESTATIC, ReflectionMasker.class, "getMethod", Method.class, false, Class.class, String.class, Class[].class),
    ADD_TYPE_PARAMS(INVOKESTATIC, ReflectionMasker.class, "addTypeParams", Class[].class, false, Class.class, Class[].class, boolean.class),
    IS_INSTANCE(INVOKESTATIC, ReflectionMasker.class, "isInstance", TaintedBooleanWithObjTag.class, false, Class.class, Object.class, TaintedBooleanWithObjTag.class);

    private final int opcode;
    private final String owner;
    private final String name;
    private final String descriptor;
    private final boolean isInterface;

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
        Type[] parameters = new Type[parameterTypes.length];
        for(int i = 0; i < parameters.length; i++) {
            parameters[i] = Type.getType(parameterTypes[i]);
        }
        this.descriptor = Type.getMethodDescriptor(Type.getType(returnType), parameters);
    }

    /**
     * @return the opcode of the type instruction associated with this method
     */
    public int getOpcode() {
        return opcode;
    }

    /**
     * @return the internal name of this method's owner class
     */
    public String getOwner() {
        return owner;
    }

    /**
     * @return this method's name
     */
    public String getName() {
        return name;
    }

    /**
     * @return this method's descriptor
     */
    public String getDescriptor() {
        return descriptor;
    }

    /**
     * @return true if this method's owner class is an interface
     */
    public boolean isInterface() {
        return isInterface;
    }

    /**
     * Tells the specified method visitor to visit a method instruction for this method.
     *
     * @param methodVisitor the method visitor that should visit this method
     */
    public void delegateVisit(MethodVisitor methodVisitor) {
        methodVisitor.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }
}
