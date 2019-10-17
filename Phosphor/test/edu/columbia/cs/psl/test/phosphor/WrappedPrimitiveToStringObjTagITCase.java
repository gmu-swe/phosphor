package edu.columbia.cs.psl.test.phosphor;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WrappedPrimitiveToStringObjTagITCase {

    @Test
    public void testConcatStrWithTaintedLongsThrowsNoException() {
        // Test showing exception thrown of the following code where toSkip and skipped are tainted
        // when no exception should have been thrown.
        // "Chars to skip: " + toSkip + " actual: " + skipped
        // From org.apache.commons.io.IOUtilsTestCase.testSkipFully_Reader$$PHOSPHORTAGGED(IOUtilsTestCase.java:860)
        // Previously threw an ArrayIndexOutOfBounds Exception.

        final long val1 = MultiTainter.taintedLong(1, "Val 1 Taint");
        final long val2 = MultiTainter.taintedLong(2, "Val 2 Taint");

        String taintedStrVal = "Chars to skip: " + val1 + " actual: " + val2;
        // Test is expected not to throw an exception.
    }

    @Test
    public void testLongGetCharsAppliesTags(){
        long[] vals = {Long.MIN_VALUE + 1, Long.MAX_VALUE, 0L, 1L, -1L, 10L, 100L, 1000L};
        //Note: We will have tag loss on Long.MIN_VALUE unless we intercept also some other places, or commit to do this
        //at Long.toString instead, which won't cover Java 7.
        for(long v : vals){
            Object label = "Taint for long " + v;
            long taintedLong = MultiTainter.taintedLong(v, label);
            String str = ""+taintedLong;
            for(int i = 0; i < str.length(); i++){
                assertEquals("Error at char " + i, label, MultiTainter.getTaint(str.charAt(i)));
            }
        }
    }

    @Test
    public void testIntGetCharsAppliesTags(){
        int[] vals = {Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 1, -1, 10, 100, 1000};
        //Note: We will have tag loss on Long.MIN_VALUE unless we intercept also some other places, or commit to do this
        //at Long.toString instead, which won't cover Java 7.
        for(int v : vals){
            Object label = "Taint for int " + v;
            int taintedLong = MultiTainter.taintedInt(v, label);
            String str = ""+taintedLong;
            for(int i = 0; i < str.length(); i++){
                assertEquals("Error at char " + i, label, MultiTainter.getTaint(str.charAt(i)));
            }
        }
    }
}
