package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.control.ControlFlowStack;
import edu.columbia.cs.psl.phosphor.instrumenter.InvokedViaInstrumentation;
import edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord;
import edu.columbia.cs.psl.phosphor.runtime.proxied.InstrumentedJREFieldHelper;
import edu.columbia.cs.psl.phosphor.struct.*;

public class PhosphorStackFrame {
    public static final String INTERNAL_NAME = "edu/columbia/cs/psl/phosphor/runtime/PhosphorStackFrame";
    public static final String DESCRIPTOR = "L" + INTERNAL_NAME + ";";
    public Taint[] argsPassed = new Taint[10];
    public Object[] wrappedArgs = new Object[10];
    public Object wrappedReturn;
    public Taint returnTaint = Taint.emptyTaint();
    public ControlFlowStack controlFlowTags;
    public String intendedNextMethodDebug;
    public int intendedNextMethodFast;
    public PhosphorStackFrame prevFrame;
    public boolean needsCleanup;

    private PhosphorStackFrame spare;

    public PhosphorStackFrame() {

    }

    public PhosphorStackFrame(PhosphorStackFrame prevFrame) {
        this.needsCleanup = true;
        this.prevFrame = prevFrame;
    }

    public static int hashForDesc(String intendedNextMethodDebug) {
        return intendedNextMethodDebug.hashCode();
    }

    private static void setForThread(PhosphorStackFrame forThread) {
        InstrumentedJREFieldHelper.setphosphorStackFrame(Thread.currentThread(), forThread);
    }

    private void ensureArgsLength(int desired){
        if(desired < wrappedArgs.length){
            return;
        }
        growArgs(desired + 10);
    }

    private void growArgs(int desired){
        Object[] tmpWrapped = this.wrappedArgs;
        this.wrappedArgs = new Object[desired];
        System.arraycopy(tmpWrapped, 0, this.wrappedArgs, 0, tmpWrapped.length);
        Object[] tmpTags = this.argsPassed;
        this.argsPassed = new Taint[desired];
        System.arraycopy(tmpTags, 0, this.argsPassed, 0, tmpTags.length);
    }

    @InvokedViaInstrumentation(record = TaintMethodRecord.POP_STACK_FRAME)
    public void popStackFrameIfNeeded(boolean shouldPop) {
        if (shouldPop) {
            this.needsCleanup = true; //Allow this to be used as a "spare"
            setForThread(this.prevFrame);
        }
    }

    @InvokedViaInstrumentation(record = TaintMethodRecord.GET_AND_CLEAR_CLEANUP_FLAG)
    public boolean getAndClearCleanupFlag() {
        boolean ret = this.needsCleanup;
        if (ret) {
            this.needsCleanup = false;
        }
        return ret;
    }

    @InvokedViaInstrumentation(record = TaintMethodRecord.PREPARE_FOR_CALL_DEBUG)
    public void prepareForCall(String descToCall) {
        this.intendedNextMethodDebug = descToCall;
        if (initialized) {
            setForThread(this);
        }
    }

    @InvokedViaInstrumentation(record = TaintMethodRecord.PREPARE_FOR_CALL_FAST)
    public void prepareForCall(int hashToCall) {
        this.intendedNextMethodFast = hashToCall;
        if (initialized) {
            setForThread(this);
        }
    }

    @InvokedViaInstrumentation(record = TaintMethodRecord.PREPARE_FOR_CALL_PREV)
    public void prepareForCallPrev() {
        if(initialized){
            setForThread(this.prevFrame);
        }
    }

    @InvokedViaInstrumentation(record = TaintMethodRecord.SET_ARG_WRAPPER)
    public void setArgWrapper(Object val, int idx) {
        ensureArgsLength(idx);
        wrappedArgs[idx] = val;
    }

    private Object getAndClearWrappedArgInternal(int idx){
        Object ret = wrappedArgs[idx];
        wrappedArgs[idx] = null;
        return ret;
    }

    @InvokedViaInstrumentation(record = TaintMethodRecord.GET_ARG_WRAPPER_OBJECT)
    public TaggedReferenceArray getArgWrapper(int idx, Object[] actual) {
        Object ret = getAndClearWrappedArgInternal(idx);
        if (ret == null || !(ret instanceof TaggedReferenceArray)) {
            if (actual != null) {
                return new TaggedReferenceArray(actual);
            }
            return null;
        }
        TaggedReferenceArray refWrapper = (TaggedReferenceArray) ret;
        if (refWrapper.val != actual && actual != null) {
            return new TaggedReferenceArray(actual);
        }
        return (TaggedReferenceArray) ret;
    }

    @InvokedViaInstrumentation(record = TaintMethodRecord.GET_ARG_WRAPPER_GENERIC)
    public Object getArgWrapper(int idx, Object actual) {
        if (actual == null || !actual.getClass().isArray()) {
            return actual;
        }
        Object ret = getAndClearWrappedArgInternal(idx);
        if (ret == null) {
            return actual;
        }
        return ret;
    }

    @InvokedViaInstrumentation(record = TaintMethodRecord.GET_ARG_WRAPPER_BOOLEAN)
    public TaggedBooleanArray getArgWrapper(int idx, boolean[] actual) {
        Object ret = getAndClearWrappedArgInternal(idx);
        if (!(ret instanceof TaggedBooleanArray)) {
            if (actual != null) {
                return new TaggedBooleanArray(actual);
            }
            return null;
        }
        TaggedBooleanArray refWrapper = (TaggedBooleanArray) ret;
        if (refWrapper.val != actual && actual != null) {
            return new TaggedBooleanArray(actual);
        }
        return (TaggedBooleanArray) ret;
    }

    @InvokedViaInstrumentation(record = TaintMethodRecord.GET_ARG_WRAPPER_BYTE)
    public TaggedByteArray getArgWrapper(int idx, byte[] actual) {
        Object ret = getAndClearWrappedArgInternal(idx);
        if (!(ret instanceof TaggedByteArray)) {
            if (actual != null) {
                return new TaggedByteArray(actual);
            }
            return null;
        }
        TaggedByteArray refWrapper = (TaggedByteArray) ret;
        if (refWrapper.val != actual && actual != null) {
            return new TaggedByteArray(actual);
        }
        return (TaggedByteArray) ret;
    }

    @InvokedViaInstrumentation(record = TaintMethodRecord.GET_ARG_WRAPPER_CHAR)
    public TaggedCharArray getArgWrapper(int idx, char[] actual) {
        Object ret = getAndClearWrappedArgInternal(idx);
        if (!(ret instanceof TaggedCharArray)) {
            if (actual != null) {
                return new TaggedCharArray(actual);
            }
            return null;
        }
        TaggedCharArray refWrapper = (TaggedCharArray) ret;
        if (refWrapper.val != actual && actual != null) {
            return new TaggedCharArray(actual);
        }
        return (TaggedCharArray) ret;
    }

    @InvokedViaInstrumentation(record = TaintMethodRecord.GET_ARG_WRAPPER_FLOAT)
    public TaggedFloatArray getArgWrapper(int idx, float[] actual) {
        Object ret = getAndClearWrappedArgInternal(idx);
        if (!(ret instanceof TaggedFloatArray)) {
            if (actual != null) {
                return new TaggedFloatArray(actual);
            }
            return null;
        }
        TaggedFloatArray refWrapper = (TaggedFloatArray) ret;
        if (refWrapper.val != actual && actual != null) {
            return new TaggedFloatArray(actual);
        }
        return (TaggedFloatArray) ret;
    }

    @InvokedViaInstrumentation(record = TaintMethodRecord.GET_ARG_WRAPPER_INT)
    public TaggedIntArray getArgWrapper(int idx, int[] actual) {
        Object ret = getAndClearWrappedArgInternal(idx);
        if (!(ret instanceof TaggedIntArray)) {
            if (actual != null) {
                return new TaggedIntArray(actual);
            }
            return null;
        }
        TaggedIntArray refWrapper = (TaggedIntArray) ret;
        if (refWrapper.val != actual && actual != null) {
            return new TaggedIntArray(actual);
        }
        return (TaggedIntArray) ret;
    }

    @InvokedViaInstrumentation(record = TaintMethodRecord.GET_ARG_WRAPPER_SHORT)
    public TaggedShortArray getArgWrapper(int idx, short[] actual) {
        Object ret = getAndClearWrappedArgInternal(idx);
        if (!(ret instanceof TaggedShortArray)) {
            if (actual != null) {
                return new TaggedShortArray(actual);
            }
            return null;
        }
        TaggedShortArray refWrapper = (TaggedShortArray) ret;
        if (refWrapper.val != actual && actual != null) {
            return new TaggedShortArray(actual);
        }
        return (TaggedShortArray) ret;
    }

    @InvokedViaInstrumentation(record = TaintMethodRecord.GET_ARG_WRAPPER_LONG)
    public TaggedLongArray getArgWrapper(int idx, long[] actual) {
        Object ret = getAndClearWrappedArgInternal(idx);
        if (!(ret instanceof TaggedLongArray)) {
            if (actual != null) {
                return new TaggedLongArray(actual);
            }
            return null;
        }
        TaggedLongArray refWrapper = (TaggedLongArray) ret;
        if (refWrapper.val != actual && actual != null) {
            return new TaggedLongArray(actual);
        }
        return (TaggedLongArray) ret;
    }

    @InvokedViaInstrumentation(record = TaintMethodRecord.GET_ARG_WRAPPER_DOUBLE)
    public TaggedDoubleArray getArgWrapper(int idx, double[] actual) {
        Object ret = getAndClearWrappedArgInternal(idx);
        if (!(ret instanceof TaggedDoubleArray)) {
            if (actual != null) {
                return new TaggedDoubleArray(actual);
            }
            return null;
        }
        TaggedDoubleArray refWrapper = (TaggedDoubleArray) ret;
        if (refWrapper.val != actual && actual != null) {
            return new TaggedDoubleArray(actual);
        }
        return (TaggedDoubleArray) ret;
    }


    @InvokedViaInstrumentation(record = TaintMethodRecord.SET_ARG_TAINT)
    public void setArgTaint(Taint tag, int idx) {
        ensureArgsLength(idx);
        argsPassed[idx] = tag;
    }

    @InvokedViaInstrumentation(record = TaintMethodRecord.GET_ARG_TAINT)
    public Taint getArgTaint(int idx) {
        return argsPassed[idx];
    }

    @InvokedViaInstrumentation(record = TaintMethodRecord.GET_RETURN_TAINT)
    public Taint getReturnTaint() {
        if (initialized) {
            setForThread(this.prevFrame);
        }
        Taint ret = this.returnTaint;
        this.returnTaint = Taint.emptyTaint();
        return ret;
    }

    @InvokedViaInstrumentation(record = TaintMethodRecord.SET_RETURN_TAINT)
    public void setReturnTaint(Taint returnTaint) {
        this.returnTaint = returnTaint;
    }

    private static PhosphorStackFrame disabledFrame = new PhosphorStackFrame();

    private static boolean initialized = false;

    @InvokedViaInstrumentation(record = TaintMethodRecord.START_STACK_FRAME_TRACKING)
    public static void initialize() {
        initialized = true;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    @InvokedViaInstrumentation(record = TaintMethodRecord.STACK_FRAME_FOR_METHOD_DEBUG)
    public static PhosphorStackFrame forMethod(String desc) {
        if (!initialized) {
            return disabledFrame;
        }
        Thread currentThread = Thread.currentThread();
        PhosphorStackFrame onThread = InstrumentedJREFieldHelper.getphosphorStackFrame(currentThread);

        //Base case - we just made a thread, make a new PhosphorStackFrame, will be used by this method and its callees
        if (onThread == null) {
            onThread = new PhosphorStackFrame();
            InstrumentedJREFieldHelper.setphosphorStackFrame(currentThread, onThread);
            return onThread;
        }
        //Look through any pending calls to see if they are us.
        PhosphorStackFrame ret = onThread;
        if (desc != null && ret.intendedNextMethodDebug != null && !StringUtils.equals(desc, ret.intendedNextMethodDebug)) {
            PhosphorStackFrame spare = onThread.spare;
            if(spare != null && spare.needsCleanup){
                spare.prevFrame = onThread;
                ret = spare;
            } else {
                ret = new PhosphorStackFrame(onThread);
                if (spare == null) {
                    onThread.spare = ret;
                }
            }
        }
        return ret;
    }

    @InvokedViaInstrumentation(record = TaintMethodRecord.STACK_FRAME_FOR_METHOD_FAST)
    public static PhosphorStackFrame forMethod(int hash) {
        if (!initialized) {
            return disabledFrame;
        }
        Thread currentThread = Thread.currentThread();
        PhosphorStackFrame onThread = InstrumentedJREFieldHelper.getphosphorStackFrame(currentThread);

        //Base case - we just made a thread, make a new PhosphorStackFrame, will be used by this method and its callees
        if (onThread == null) {
            onThread = new PhosphorStackFrame();
            InstrumentedJREFieldHelper.setphosphorStackFrame(currentThread, onThread);
            return onThread;
        }
        PhosphorStackFrame ret = onThread;
        if (ret.intendedNextMethodFast != hash) {
            PhosphorStackFrame spare = onThread.spare;
            if (spare != null && spare.needsCleanup) {
                spare.prevFrame = onThread;
                ret = spare;
            } else {
                ret = new PhosphorStackFrame(onThread);
                if (spare == null) {
                    onThread.spare = ret;
                }
            }
        }
        return ret;
    }


    @InvokedViaInstrumentation(record = TaintMethodRecord.SET_WRAPPED_RETURN)
    public void setWrappedReturn(Object wrappedReturn) {
        this.wrappedReturn = wrappedReturn;
    }

    @InvokedViaInstrumentation(record = TaintMethodRecord.GET_RETURN_WRAPPER_OBJECT)
    public TaggedReferenceArray getReturnWrapper(Object[] unwrapped) {
        if (!(wrappedReturn instanceof TaggedReferenceArray)) {
            if (unwrapped != null) {
                if (unwrapped.getClass().getComponentType().isArray()) {
                    //Multi-d array
                    return TaggedReferenceArray.forMultiDArray(unwrapped);
                }
                return new TaggedReferenceArray(unwrapped);
            }
            return null;
        }
        TaggedReferenceArray ret = (TaggedReferenceArray) wrappedReturn;
        this.wrappedReturn = null;
        if (ret == null || (unwrapped != null && ret.val != unwrapped)) {
            return new TaggedReferenceArray(unwrapped);
        }
        return ret;
    }

    @InvokedViaInstrumentation(record = TaintMethodRecord.GET_RETURN_WRAPPER_CHAR)
    public TaggedCharArray getReturnWrapper(char[] unwrapped) {
        if (!(wrappedReturn instanceof TaggedCharArray)) {
            if (unwrapped != null) {
                return new TaggedCharArray(unwrapped);
            }
            return null;
        }
        TaggedCharArray ret = (TaggedCharArray) wrappedReturn;
        this.wrappedReturn = null;
        if (ret == null || (unwrapped != null && ret.val != unwrapped)) {
            return new TaggedCharArray(unwrapped);
        }
        return ret;
    }

    @InvokedViaInstrumentation(record = TaintMethodRecord.GET_RETURN_WRAPPER_SHORT)
    public TaggedShortArray getReturnWrapper(short[] unwrapped) {
        if (!(wrappedReturn instanceof TaggedShortArray)) {
            if (unwrapped != null) {
                return new TaggedShortArray(unwrapped);
            }
            return null;
        }
        TaggedShortArray ret = (TaggedShortArray) wrappedReturn;
        this.wrappedReturn = null;
        if (ret == null || (unwrapped != null && ret.val != unwrapped)) {
            return new TaggedShortArray(unwrapped);
        }
        return ret;
    }

    @InvokedViaInstrumentation(record = TaintMethodRecord.GET_RETURN_WRAPPER_BYTE)
    public TaggedByteArray getReturnWrapper(byte[] unwrapped) {
        if (!(wrappedReturn instanceof TaggedByteArray)) {
            if (unwrapped != null) {
                return new TaggedByteArray(unwrapped);
            }
            return null;
        }
        TaggedByteArray ret = (TaggedByteArray) wrappedReturn;
        this.wrappedReturn = null;
        if (ret == null || (unwrapped != null && ret.val != unwrapped)) {
            return new TaggedByteArray(unwrapped);
        }
        return ret;
    }

    @InvokedViaInstrumentation(record = TaintMethodRecord.GET_RETURN_WRAPPER_BOOLEAN)
    public TaggedBooleanArray getReturnWrapper(boolean[] unwrapped) {
        if (!(wrappedReturn instanceof TaggedBooleanArray)) {
            if (unwrapped != null) {
                return new TaggedBooleanArray(unwrapped);
            }
            return null;
        }
        TaggedBooleanArray ret = (TaggedBooleanArray) wrappedReturn;
        this.wrappedReturn = null;
        if (ret == null || (unwrapped != null && ret.val != unwrapped)) {
            return new TaggedBooleanArray(unwrapped);
        }
        return ret;
    }

    @InvokedViaInstrumentation(record = TaintMethodRecord.GET_RETURN_WRAPPER_LONG)
    public TaggedLongArray getReturnWrapper(long[] unwrapped) {
        if (!(wrappedReturn instanceof TaggedLongArray)) {
            if (unwrapped != null) {
                return new TaggedLongArray(unwrapped);
            }
            return null;
        }
        TaggedLongArray ret = (TaggedLongArray) wrappedReturn;
        this.wrappedReturn = null;
        if (ret == null || (unwrapped != null && ret.val != unwrapped)) {
            return new TaggedLongArray(unwrapped);
        }
        return ret;
    }

    @InvokedViaInstrumentation(record = TaintMethodRecord.GET_RETURN_WRAPPER_INT)
    public TaggedIntArray getReturnWrapper(int[] unwrapped) {
        if (!(wrappedReturn instanceof TaggedIntArray)) {
            if (unwrapped != null) {
                return new TaggedIntArray(unwrapped);
            }
            return null;
        }
        TaggedIntArray ret = (TaggedIntArray) wrappedReturn;
        this.wrappedReturn = null;
        if (ret == null || (unwrapped != null && ret.val != unwrapped)) {
            return new TaggedIntArray(unwrapped);
        }
        return ret;
    }

    @InvokedViaInstrumentation(record = TaintMethodRecord.GET_RETURN_WRAPPER_FLOAT)
    public TaggedFloatArray getReturnWrapper(float[] unwrapped) {
        if (!(wrappedReturn instanceof TaggedFloatArray)) {
            if (unwrapped != null) {
                return new TaggedFloatArray(unwrapped);
            }
            return null;
        }
        TaggedFloatArray ret = (TaggedFloatArray) wrappedReturn;
        this.wrappedReturn = null;
        if (ret == null || (unwrapped != null && ret.val != unwrapped)) {
            return new TaggedFloatArray(unwrapped);
        }
        return ret;
    }

    @InvokedViaInstrumentation(record = TaintMethodRecord.GET_RETURN_WRAPPER_DOUBLE)
    public TaggedDoubleArray getReturnWrapper(double[] unwrapped) {
        if (!(wrappedReturn instanceof TaggedDoubleArray)) {
            if (unwrapped != null) {
                return new TaggedDoubleArray(unwrapped);
            }
            return null;
        }
        TaggedDoubleArray ret = (TaggedDoubleArray) wrappedReturn;
        this.wrappedReturn = null;
        if (ret == null || (unwrapped != null && ret.val != unwrapped)) {
            return new TaggedDoubleArray(unwrapped);
        }
        return ret;
    }
}
