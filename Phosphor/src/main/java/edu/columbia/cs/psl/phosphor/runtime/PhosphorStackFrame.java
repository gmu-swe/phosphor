package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.control.ControlFlowStack;
import edu.columbia.cs.psl.phosphor.instrumenter.InvokedViaInstrumentation;
import edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord;
import edu.columbia.cs.psl.phosphor.struct.*;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;

public class PhosphorStackFrame {
    public static final String INTERNAL_NAME = "edu/columbia/cs/psl/phosphor/runtime/PhosphorStackFrame";
    public static final String DESCRIPTOR = "L"+INTERNAL_NAME+";";
    public Taint[] argsPassed = new Taint[100];
    public Object[] wrappedArgs = new Object[100];
    public Object wrappedReturn;
    public Taint returnTaint = Taint.emptyTaint();
    static PhosphorStackFrame frame = new PhosphorStackFrame();
    public ControlFlowStack controlFlowTags;
    public String intendedNextMethodDebug;
    public int intendedNextMethodFast;
    public PhosphorStackFrame prevFrame;
    public boolean needsCleanup;

    public PhosphorStackFrame(){

    }

    public PhosphorStackFrame(PhosphorStackFrame prevFrame) {
        this.needsCleanup = true;
        this.prevFrame = prevFrame;
    }

    public static int hashForDesc(String intendedNextMethodDebug) {
        return intendedNextMethodDebug.hashCode();
    }


    /**
     *
     * @param shouldPop
     */
    @InvokedViaInstrumentation(record = TaintMethodRecord.POP_STACK_FRAME)
    public void popStackFrameIfNeeded(boolean shouldPop){
        if(shouldPop)
            Thread.currentThread().phosphorStackFrame = this.prevFrame;
    }

    @InvokedViaInstrumentation(record = TaintMethodRecord.GET_AND_CLEAR_CLEANUP_FLAG)
    public boolean getAndClearCleanupFlag(){
        boolean ret = this.needsCleanup;
        if(ret)
            this.needsCleanup = false;
        return ret;
    }

    @InvokedViaInstrumentation(record = TaintMethodRecord.PREPARE_FOR_CALL_DEBUG)
    public void prepareForCall(String descToCall){
        this.intendedNextMethodDebug = descToCall;
        if(initialized){
            Thread.currentThread().phosphorStackFrame = this;
        }
    }
    @InvokedViaInstrumentation(record = TaintMethodRecord.PREPARE_FOR_CALL_FAST)
    public void prepareForCall(int hashToCall){
        this.intendedNextMethodFast = hashToCall;
        if(initialized){
            Thread.currentThread().phosphorStackFrame = this;
        }
    }

    @InvokedViaInstrumentation(record = TaintMethodRecord.SET_ARG_WRAPPER)
    public void setArgWrapper(Object val, int idx){
        wrappedArgs[idx] = val;
    }

    @InvokedViaInstrumentation(record = TaintMethodRecord.GET_ARG_WRAPPER_OBJECT)
    public LazyReferenceArrayObjTags getArgWrapper(int idx, Object[] actual) {
        Object ret = wrappedArgs[idx];
        wrappedArgs[idx] = null;
        if (ret == null || !(ret instanceof LazyReferenceArrayObjTags)){
            if(actual != null)
                return new LazyReferenceArrayObjTags(actual);
            return null;
        }
        LazyReferenceArrayObjTags refWrapper = (LazyReferenceArrayObjTags) ret;
        if (refWrapper.val != actual && actual != null) {
            return new LazyReferenceArrayObjTags(actual);
        }
        return (LazyReferenceArrayObjTags) ret;
    }

    @InvokedViaInstrumentation(record = TaintMethodRecord.GET_ARG_WRAPPER_BOOLEAN)
    public LazyBooleanArrayObjTags getArgWrapper(int idx, boolean[] actual) {
        Object ret = wrappedArgs[idx];
        wrappedArgs[idx] = null;
        if (!(ret instanceof LazyBooleanArrayObjTags)){
            if(actual != null)
                return new LazyBooleanArrayObjTags(actual);
            return null;
        }
        LazyBooleanArrayObjTags refWrapper = (LazyBooleanArrayObjTags) ret;
        if (refWrapper.val != actual && actual != null) {
            return new LazyBooleanArrayObjTags(actual);
        }
        return (LazyBooleanArrayObjTags) ret;
    }
    @InvokedViaInstrumentation(record = TaintMethodRecord.GET_ARG_WRAPPER_BYTE)
    public LazyByteArrayObjTags getArgWrapper(int idx, byte[] actual) {
        Object ret = wrappedArgs[idx];
        wrappedArgs[idx] = null;
        if (!(ret instanceof LazyByteArrayObjTags)){
            if(actual != null)
                return new LazyByteArrayObjTags(actual);
            return null;
        }
        LazyByteArrayObjTags refWrapper = (LazyByteArrayObjTags) ret;
        if (refWrapper.val != actual && actual != null) {
            return new LazyByteArrayObjTags(actual);
        }
        return (LazyByteArrayObjTags) ret;
    }
    @InvokedViaInstrumentation(record = TaintMethodRecord.GET_ARG_WRAPPER_CHAR)
    public LazyCharArrayObjTags getArgWrapper(int idx, char[] actual) {
        Object ret = wrappedArgs[idx];
        wrappedArgs[idx] = null;
        if (!(ret instanceof LazyCharArrayObjTags)){
            if(actual != null)
                return new LazyCharArrayObjTags(actual);
            return null;
        }
        LazyCharArrayObjTags refWrapper = (LazyCharArrayObjTags) ret;
        if (refWrapper.val != actual && actual != null) {
            return new LazyCharArrayObjTags(actual);
        }
        return (LazyCharArrayObjTags) ret;
    }
    @InvokedViaInstrumentation(record = TaintMethodRecord.GET_ARG_WRAPPER_FLOAT)
    public LazyFloatArrayObjTags getArgWrapper(int idx, float[] actual) {
        Object ret = wrappedArgs[idx];
        wrappedArgs[idx] = null;
        if (!(ret instanceof LazyFloatArrayObjTags)){
            if(actual != null)
                return new LazyFloatArrayObjTags(actual);
            return null;
        }
        LazyFloatArrayObjTags refWrapper = (LazyFloatArrayObjTags) ret;
        if (refWrapper.val != actual && actual != null) {
            return new LazyFloatArrayObjTags(actual);
        }
        return (LazyFloatArrayObjTags) ret;
    }
    @InvokedViaInstrumentation(record = TaintMethodRecord.GET_ARG_WRAPPER_INT)
    public LazyIntArrayObjTags getArgWrapper(int idx, int[] actual) {
        Object ret = wrappedArgs[idx];
        wrappedArgs[idx] = null;
        if (!(ret instanceof LazyIntArrayObjTags)){
            if(actual != null)
                return new LazyIntArrayObjTags(actual);
            return null;
        }
        LazyIntArrayObjTags refWrapper = (LazyIntArrayObjTags) ret;
        if (refWrapper.val != actual && actual != null) {
            return new LazyIntArrayObjTags(actual);
        }
        return (LazyIntArrayObjTags) ret;
    }
    @InvokedViaInstrumentation(record = TaintMethodRecord.GET_ARG_WRAPPER_SHORT)
    public LazyShortArrayObjTags getArgWrapper(int idx, short[] actual) {
        Object ret = wrappedArgs[idx];
        wrappedArgs[idx] = null;
        if (!(ret instanceof LazyShortArrayObjTags)){
            if(actual != null)
                return new LazyShortArrayObjTags(actual);
            return null;
        }
        LazyShortArrayObjTags refWrapper = (LazyShortArrayObjTags) ret;
        if (refWrapper.val != actual && actual != null) {
            return new LazyShortArrayObjTags(actual);
        }
        return (LazyShortArrayObjTags) ret;
    }
    @InvokedViaInstrumentation(record = TaintMethodRecord.GET_ARG_WRAPPER_LONG)
    public LazyLongArrayObjTags getArgWrapper(int idx, long[] actual) {
        Object ret = wrappedArgs[idx];
        wrappedArgs[idx] = null;
        if (!(ret instanceof LazyLongArrayObjTags)){
            if(actual != null)
                return new LazyLongArrayObjTags(actual);
            return null;
        }
        LazyLongArrayObjTags refWrapper = (LazyLongArrayObjTags) ret;
        if (refWrapper.val != actual && actual != null) {
            return new LazyLongArrayObjTags(actual);
        }
        return (LazyLongArrayObjTags) ret;
    }
    @InvokedViaInstrumentation(record = TaintMethodRecord.GET_ARG_WRAPPER_DOUBLE)
    public LazyDoubleArrayObjTags getArgWrapper(int idx, double[] actual) {
        Object ret = wrappedArgs[idx];
        wrappedArgs[idx] = null;
        if (!(ret instanceof LazyDoubleArrayObjTags)){
            if(actual != null)
                return new LazyDoubleArrayObjTags(actual);
            return null;
        }
        LazyDoubleArrayObjTags refWrapper = (LazyDoubleArrayObjTags) ret;
        if (refWrapper.val != actual && actual != null) {
            return new LazyDoubleArrayObjTags(actual);
        }
        return (LazyDoubleArrayObjTags) ret;
    }


    @InvokedViaInstrumentation(record = TaintMethodRecord.SET_ARG_TAINT)
    public void setArgTaint(Taint tag, int idx){
        argsPassed[idx] = tag;
    }
    @InvokedViaInstrumentation(record = TaintMethodRecord.GET_ARG_TAINT)
    public Taint getArgTaint(int idx){
        return argsPassed[idx];
    }

    @InvokedViaInstrumentation(record = TaintMethodRecord.GET_RETURN_TAINT)
    public Taint getReturnTaint(){
        if(initialized) {
            Thread.currentThread().phosphorStackFrame = this.prevFrame;
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
        PhosphorStackFrame onThread = currentThread.phosphorStackFrame;

        //Base case - we just made a thread, make a new PhosphorStackFrame, will be used by this method and its callees
        if(onThread == null){
            onThread = new PhosphorStackFrame();
            currentThread.phosphorStackFrame = onThread;
            return onThread;
        }
        //Look through any pending calls to see if they are us.
        PhosphorStackFrame ret = onThread;
        if(desc != null && ret.intendedNextMethodDebug != null && !StringUtils.equals(desc, ret.intendedNextMethodDebug)){
                ret = new PhosphorStackFrame();
                ret.needsCleanup = true;
                ret.prevFrame = onThread;
        }
        return ret;
    }
    @InvokedViaInstrumentation(record = TaintMethodRecord.STACK_FRAME_FOR_METHOD_FAST)
    public static PhosphorStackFrame forMethod(int hash) {
        if (!initialized) {
            return disabledFrame;
        }
        Thread currentThread = Thread.currentThread();
        PhosphorStackFrame onThread = currentThread.phosphorStackFrame;

        //Base case - we just made a thread, make a new PhosphorStackFrame, will be used by this method and its callees
        if(onThread == null){
            onThread = new PhosphorStackFrame();
            currentThread.phosphorStackFrame = onThread;
            return onThread;
        }
        PhosphorStackFrame ret = onThread;
        if(ret.intendedNextMethodFast != hash){
            ret = new PhosphorStackFrame(onThread);
        }
        return ret;
    }


    @InvokedViaInstrumentation(record = TaintMethodRecord.SET_WRAPPED_RETURN)
    public void setWrappedReturn(Object wrappedReturn) {
        this.wrappedReturn = wrappedReturn;
    }

    @InvokedViaInstrumentation(record = TaintMethodRecord.GET_RETURN_WRAPPER_OBJECT)
    public LazyReferenceArrayObjTags getReturnWrapper(Object[] unwrapped) {
        if(!(wrappedReturn instanceof LazyReferenceArrayObjTags)){
            if(unwrapped != null)
            {
                if(unwrapped.getClass().getComponentType().isArray()){
                    //Multi-d array
                    return LazyReferenceArrayObjTags.forMultiDArray(unwrapped);
                }
                return new LazyReferenceArrayObjTags(unwrapped);
            }
            return null;
        }
        LazyReferenceArrayObjTags ret = (LazyReferenceArrayObjTags) wrappedReturn;
        this.wrappedReturn = null;
        if(ret == null || (unwrapped != null && ret.val != unwrapped)){
            return new LazyReferenceArrayObjTags(unwrapped);
        }
        return ret;
    }
    @InvokedViaInstrumentation(record = TaintMethodRecord.GET_RETURN_WRAPPER_CHAR)
    public LazyCharArrayObjTags getReturnWrapper(char[] unwrapped) {
        if(!(wrappedReturn instanceof LazyCharArrayObjTags)){
            if(unwrapped != null)
                return new LazyCharArrayObjTags(unwrapped);
            return null;
        }
        LazyCharArrayObjTags ret = (LazyCharArrayObjTags) wrappedReturn;
        this.wrappedReturn = null;
        if(ret == null || (unwrapped != null && ret.val != unwrapped)){
            return new LazyCharArrayObjTags(unwrapped);
        }
        return ret;
    }
    @InvokedViaInstrumentation(record = TaintMethodRecord.GET_RETURN_WRAPPER_SHORT)
    public LazyShortArrayObjTags getReturnWrapper(short[] unwrapped) {
        if(!(wrappedReturn instanceof LazyShortArrayObjTags)){
            if(unwrapped != null)
                return new LazyShortArrayObjTags(unwrapped);
            return null;
        }
        LazyShortArrayObjTags ret = (LazyShortArrayObjTags) wrappedReturn;
        this.wrappedReturn = null;
        if(ret == null || (unwrapped != null && ret.val != unwrapped)){
            return new LazyShortArrayObjTags(unwrapped);
        }
        return ret;
    }
    @InvokedViaInstrumentation(record = TaintMethodRecord.GET_RETURN_WRAPPER_BYTE)
    public LazyByteArrayObjTags getReturnWrapper(byte[] unwrapped) {
        if(!(wrappedReturn instanceof LazyByteArrayObjTags)){
            if(unwrapped != null)
                return new LazyByteArrayObjTags(unwrapped);
            return null;
        }
        LazyByteArrayObjTags ret = (LazyByteArrayObjTags) wrappedReturn;
        this.wrappedReturn = null;
        if(ret == null || (unwrapped != null && ret.val != unwrapped)){
            return new LazyByteArrayObjTags(unwrapped);
        }
        return ret;
    }
    @InvokedViaInstrumentation(record = TaintMethodRecord.GET_RETURN_WRAPPER_BOOLEAN)
    public LazyBooleanArrayObjTags getReturnWrapper(boolean[] unwrapped) {
        if(!(wrappedReturn instanceof LazyBooleanArrayObjTags)){
            if(unwrapped != null)
                return new LazyBooleanArrayObjTags(unwrapped);
            return null;
        }
        LazyBooleanArrayObjTags ret = (LazyBooleanArrayObjTags) wrappedReturn;
        this.wrappedReturn = null;
        if(ret == null || (unwrapped != null && ret.val != unwrapped)){
            return new LazyBooleanArrayObjTags(unwrapped);
        }
        return ret;
    }
    @InvokedViaInstrumentation(record = TaintMethodRecord.GET_RETURN_WRAPPER_LONG)
    public LazyLongArrayObjTags getReturnWrapper(long[] unwrapped) {
        if(!(wrappedReturn instanceof LazyLongArrayObjTags)){
            if(unwrapped != null)
                return new LazyLongArrayObjTags(unwrapped);
            return null;
        }
        LazyLongArrayObjTags ret = (LazyLongArrayObjTags) wrappedReturn;
        this.wrappedReturn = null;
        if(ret == null || (unwrapped != null && ret.val != unwrapped)){
            return new LazyLongArrayObjTags(unwrapped);
        }
        return ret;
    }
    @InvokedViaInstrumentation(record = TaintMethodRecord.GET_RETURN_WRAPPER_INT)
    public LazyIntArrayObjTags getReturnWrapper(int[] unwrapped) {
        if(!(wrappedReturn instanceof LazyIntArrayObjTags)){
            if(unwrapped != null)
                return new LazyIntArrayObjTags(unwrapped);
            return null;
        }
        LazyIntArrayObjTags ret = (LazyIntArrayObjTags) wrappedReturn;
        this.wrappedReturn = null;
        if(ret == null || (unwrapped != null && ret.val != unwrapped)){
            return new LazyIntArrayObjTags(unwrapped);
        }
        return ret;
    }
    @InvokedViaInstrumentation(record = TaintMethodRecord.GET_RETURN_WRAPPER_FLOAT)
    public LazyFloatArrayObjTags getReturnWrapper(float[] unwrapped) {
        if(!(wrappedReturn instanceof LazyFloatArrayObjTags)){
            if(unwrapped != null)
                return new LazyFloatArrayObjTags(unwrapped);
            return null;
        }
        LazyFloatArrayObjTags ret = (LazyFloatArrayObjTags) wrappedReturn;
        this.wrappedReturn = null;
        if(ret == null || (unwrapped != null && ret.val != unwrapped)){
            return new LazyFloatArrayObjTags(unwrapped);
        }
        return ret;
    }
    @InvokedViaInstrumentation(record = TaintMethodRecord.GET_RETURN_WRAPPER_DOUBLE)
    public LazyDoubleArrayObjTags getReturnWrapper(double[] unwrapped) {
        if(!(wrappedReturn instanceof LazyDoubleArrayObjTags)){
            if(unwrapped != null)
                return new LazyDoubleArrayObjTags(unwrapped);
            return null;
        }
        LazyDoubleArrayObjTags ret = (LazyDoubleArrayObjTags) wrappedReturn;
        this.wrappedReturn = null;
        if(ret == null || (unwrapped != null && ret.val != unwrapped)){
            return new LazyDoubleArrayObjTags(unwrapped);
        }
        return ret;
    }
}
