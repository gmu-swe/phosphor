package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.instrumenter.InvokedViaInstrumentation;
import edu.columbia.cs.psl.phosphor.runtime.proxied.InstrumentedJREFieldHelper;
import edu.columbia.cs.psl.phosphor.struct.*;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashSet;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;

import java.lang.reflect.Array;

import static edu.columbia.cs.psl.phosphor.instrumenter.TaintMethodRecord.AUTO_TAINT;

/**
 * This class handles dynamically doing source-based tainting.
 * <p>
 * If you want to replace the *value* dynamically, then: 1. extend this class 2.
 * Set Configuration.autoTainter to be an instance of your new
 * TaintSourceWrapper 3. Override autoTaint..., change the value, then call
 * super.autoTaint... in order to set the taint correctly
 * <p>
 * Example:
 * <p>
 * <p>
 * public TaintedIntWithObjTag autoTaint(TaintedIntWithObjTag ret, String
 * source, int argIdx) { ret.val = 100; //Change value to be 100 instead of
 * whatever it was normally ret = super.autoTaint(ret, source, argIdx); //will
 * set the taint return ret; }
 *
 * @author jon
 */
public class TaintSourceWrapper<T extends AutoTaintLabel> {

    public boolean shouldInstrumentMethodForImplicitLightTracking(String className, String methodName, String methodDescriptor) {
        return className.equals("edu/columbia/cs/psl/test/phosphor/SelectiveLightImplicitObjTagITCase") && methodName.equals("hasImplicitTracking");
    }

    /* For multi-tainting/object tags. */
    @SuppressWarnings({"unchecked", "unused"})
    public void combineTaintsOnArray(Object inputArray, Taint tag) {
        if(inputArray instanceof TaggedArray) {
            TaggedArray array = ((TaggedArray) inputArray);
            if(array.taints == null) {
                array.taints = new Taint[array.getLength()];
            }
            for(int i = 0; i < array.getLength(); i++) {
                if(array.taints[i] == null) {
                    array.taints[i] = tag;
                } else {
                    array.taints[i] = array.taints[i].union(tag);
                }
            }
        } else if(inputArray instanceof Object[]) {
            for(int i = 0; i < ((Object[]) inputArray).length; i++) {
                Object o = ((Object[]) inputArray)[i];
                if(o instanceof TaintedWithObjTag) {
                    Taint existing = (Taint) ((TaintedWithObjTag) o).getPHOSPHOR_TAG();
                    if(existing != null) {
                        ((TaintedWithObjTag) o).setPHOSPHOR_TAG(existing.union(tag));
                    } else {
                        ((TaintedWithObjTag) o).setPHOSPHOR_TAG(tag);
                    }
                }
            }
        }
    }

    /* Called by taintThrough methods. */
    @SuppressWarnings({"unchecked", "unused"})
    public void addTaint(Object obj, Taint<? extends AutoTaintLabel> tag) {
        //TODO make this return Taint
        if(tag == null || obj == null) {
            return;
        }
        if(obj instanceof TaggedArray) {
            combineTaintsOnArray(obj, tag);
        } else if(obj instanceof TaintedWithObjTag) {
            TaintedWithObjTag tainted = (TaintedWithObjTag) obj;
            Taint prevTag = (Taint) tainted.getPHOSPHOR_TAG();
            if (prevTag != null) {
                //prevTag.addDependency(tag);
            } else {
                tainted.setPHOSPHOR_TAG(tag);
            }
        } else if(obj.getClass().isArray()) {
            for(int i = 0; i < Array.getLength(obj); i++) {
                addTaint(Array.get(obj, i), tag);
            }
        }
    }

    public Taint<AutoTaintLabel> generateTaint(String source) {
        StackTraceElement[] st = Thread.currentThread().getStackTrace();
        StackTraceElement[] s = new StackTraceElement[st.length - 3];
        System.arraycopy(st, 3, s, 0, s.length);
        return Taint.withLabel(new AutoTaintLabel(source, s));
    }

    /* Called by sources for the arguments and return value. */
    @InvokedViaInstrumentation(record = AUTO_TAINT)
    public Object autoTaint(Object obj, String baseSource, String actualSource, int argIdx) {
        return autoTaint(obj, generateTaint(baseSource));
    }

    /* Adds the specified tag to the specified object. */
    public Object autoTaint(Object obj, Taint<? extends AutoTaintLabel> tag) {
        if(obj == null) {
            return null;
        } else if(obj instanceof TaggedArray) {
            return autoTaint((TaggedArray) obj, tag);
        } else if(obj instanceof TaintedWithObjTag) {
            return autoTaint((TaintedWithObjTag) obj, tag);
        } else if(obj.getClass().isArray()) {
            for(int i = 0; i < Array.getLength(obj); i++) {
                Array.set(obj, i, autoTaint(Array.get(obj, i), tag));
            }
            return obj;
        } else if(obj instanceof Taint) {
            return tag;
        }
        return obj;
    }

    @SuppressWarnings("unchecked")
    public TaintedWithObjTag autoTaint(TaintedWithObjTag ret, Taint<? extends AutoTaintLabel> tag) {
        Taint prevTag = (Taint) ret.getPHOSPHOR_TAG();
        if(prevTag != null) {
            ret.setPHOSPHOR_TAG(prevTag.union(tag));
        } else {
            ret.setPHOSPHOR_TAG(tag);
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    public TaggedArray autoTaint(TaggedArray ret, Taint<? extends AutoTaintLabel> tag) {
        Taint[] taintArray = ret.taints;
        if(taintArray != null) {
            for(int i = 0; i < taintArray.length; i++) {
                if(taintArray[i] == null) {
                    taintArray[i] = tag;
                } else {
                    taintArray[i] = taintArray[i].union(tag);
                }
            }
        } else {
            ret.setTaints(tag);
        }
        if(ret instanceof TaggedReferenceArray) {
            for(Object o : ((TaggedReferenceArray) ret).val) {
                autoTaint(o, tag);
            }
        }
        return ret;
    }

    /* Called by sink methods. */
    @SuppressWarnings("unused")
    public void checkTaint(Object[] arguments, Taint[] argTaints, String baseSink, String actualSink) {
        if(arguments != null) {
            for(Object argument : arguments) {
                checkTaint(argument, baseSink, actualSink);
            }
            for(int i = 0; i < argTaints.length; i++){
                Taint t = argTaints[i];
                if(!t.isEmpty()){
                    taintViolation(t, arguments[i], baseSink, actualSink);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void checkTaint(Object obj, String baseSink, String actualSink) {
        if(obj instanceof String) {
            Taint[] taints = getStringValueTaints((String) obj);
            if(taints != null) {
                Set<String> reported = new HashSet<>();
                for(Taint t : taints) {
                    if(t != null) {
                        String _t = new String(t.toString().getBytes());
                        if(reported.add(_t)) {
                            taintViolation(t, obj, baseSink, actualSink);
                        }
                    }
                }
            }
        } else if(obj instanceof TaintedWithObjTag) {
            if(((TaintedWithObjTag) obj).getPHOSPHOR_TAG() != null) {
                taintViolation((Taint<T>) ((TaintedWithObjTag) obj).getPHOSPHOR_TAG(), obj, baseSink, actualSink);
            }
        } else if(obj instanceof TaggedArray) {
            TaggedArray tags = ((TaggedArray) obj);
            if(tags.taints != null) {
                for(Object i : tags.taints) {
                    if(i != null) {
                        taintViolation((Taint<T>) i, obj, baseSink, actualSink);
                    }
                }
            }
            if(obj instanceof TaggedReferenceArray) {
                for(Object each : ((TaggedReferenceArray) obj).val) {
                    checkTaint(each, baseSink, actualSink);
                }
            }
        } else if(obj instanceof Object[]) {
            for(Object o : ((Object[]) obj)) {
                checkTaint(o, baseSink, actualSink);
            }
        }
    }

    public void taintViolation(Taint<T> tag, Object obj, String baseSink, String actualSink) {
        throw new TaintSinkError(tag, obj);
    }

    /* Called just before a sink method returns. */
    public void exitingSink(String baseSink, String actualSink) {

    }

    /* Called after a sink method makes its calls to checkTaint but before the rest of the method body executes. */
    public void enteringSink(String baseSink, String actualSink) {

    }

    public static void setStringTaintTag(String str, Taint tag) {
        // TODO: Previously, I think we were combining  tags on strings, now this is overwriting. Is this the right thing to do now?
        if (str != null) {
            if (Configuration.IS_JAVA_8) {
                TaggedCharArray chars = InstrumentedJREFieldHelper.JAVA_8getvaluePHOSPHOR_WRAPPER(str);
                if(chars == null){
                    chars = new TaggedCharArray(InstrumentedJREFieldHelper.JAVA_8getvalue(str));
                    InstrumentedJREFieldHelper.JAVA_8setvaluePHOSPHOR_WRAPPER(str, chars);
                }
                chars.setTaints(tag);
            } else {
                TaggedByteArray chars = InstrumentedJREFieldHelper.getvaluePHOSPHOR_WRAPPER(str);
                if(chars == null){
                    chars = new TaggedByteArray(InstrumentedJREFieldHelper.getvalue(str));
                    InstrumentedJREFieldHelper.setvaluePHOSPHOR_WRAPPER(str, chars);
                }
                chars.setTaints(tag);
            }
        }
    }

    public static void setStringValueTag(String str, TaggedArray tags) {
        if (str != null) {
            if (Configuration.IS_JAVA_8) {
                InstrumentedJREFieldHelper.JAVA_8setvaluePHOSPHOR_WRAPPER(str, (TaggedCharArray) tags);
            } else {
                InstrumentedJREFieldHelper.setvaluePHOSPHOR_WRAPPER(str, (TaggedByteArray) tags);
            }
        }
    }

    public static TaggedArray getStringValueTag(CharSequence str) {
        if (str == null) {
            return null;
        } else {
            if(str instanceof String){
                if (Configuration.IS_JAVA_8) {
                    return InstrumentedJREFieldHelper.JAVA_8getvaluePHOSPHOR_WRAPPER((String) str);
                } else {
                    return InstrumentedJREFieldHelper.getvaluePHOSPHOR_WRAPPER((String) str);
                }
            } else{
                TaggedCharArray ret = new TaggedCharArray(str.length());
                PhosphorStackFrame frame = PhosphorStackFrame.forMethod(null);
                for(int i = 0; i < str.length(); i++){
                    if(Configuration.DEBUG_STACK_FRAME_WRAPPERS) {
                        frame.prepareForCall("charAt(I)");
                    } else{
                        frame.prepareForCall(PhosphorStackFrame.hashForDesc("charAt(I)"));
                    }
                    char c = str.charAt(i);
                    Taint tag = frame.getReturnTaint();
                    ret.set(i, c, tag);
                }
                return ret;
            }
        }
    }

    public static Taint[] getStringValueTaints(String str) {
        TaggedArray tag = getStringValueTag(str);
        return tag == null? null : tag.taints;
    }
}
