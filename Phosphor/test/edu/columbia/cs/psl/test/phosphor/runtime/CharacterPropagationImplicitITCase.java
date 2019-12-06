package edu.columbia.cs.psl.test.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.test.phosphor.BaseMultiTaintClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CharacterPropagationImplicitITCase extends BaseMultiTaintClass {

    @Test
    public void testForDigitPropagates() {
        int digit = MultiTainter.taintedInt(5, "digit");
        int radix = MultiTainter.taintedInt(10, "radix");
        char result = Character.forDigit(digit, radix);
        assertEquals('5', result);
        assertTaintHasOnlyLabels(MultiTainter.getTaint(result), "digit", "radix");
    }
}
