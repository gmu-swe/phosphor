package edu.columbia.cs.psl.test.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.test.phosphor.BaseMultiTaintClass;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

@RunWith(Theories.class)
public class GetCharsObjTagITCase extends BaseMultiTaintClass {

    @DataPoints
    public static Integer[] intValues() {
        return new Integer[]{-10000, -1000, -100, -10, 0, 10, 100, 1000, 10000};
    }

    @DataPoints
    public static Long[] longValues() {
        return new Long[]{-10000L, -1000L, -100L, -10L, 0L, 10L, 100L, 1000L, 10000L};
    }

    @DataPoints
    public static Boolean[] tainted() {
        return new Boolean[]{Boolean.TRUE, Boolean.FALSE};
    }

    @Theory
    public void testGetCharsInt(Integer value, Boolean tainted) {
        int val = tainted ? MultiTainter.taintedInt(value, "tainted int") : value;
        String result = new StringBuilder().append(val).toString();
        for(char c : result.toCharArray()) {
            if(tainted) {
                assertNonNullTaint(MultiTainter.getTaint(c));
            } else {
                assertNullOrEmpty(MultiTainter.getTaint(c));
            }
        }
    }

    @Theory
    public void testGetCharsLong(Long value, Boolean tainted) {
        long val = tainted ? MultiTainter.taintedLong(value, "tainted long") : value;
        String result = new StringBuilder().append(val).toString();
        for(char c : result.toCharArray()) {
            if(tainted) {
                assertNonNullTaint(MultiTainter.getTaint(c));
            } else {
                assertNullOrEmpty(MultiTainter.getTaint(c));
            }
        }
    }
}
