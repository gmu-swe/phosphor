package edu.columbia.cs.psl.test.phosphor;
import org.junit.Test;


public class InheritedAutoTaintObjTagITCase extends BaseMultiTaintClass {

    public interface TaintSourceInterface {
        int getSource();
    }

    public interface ChildInterface extends TaintSourceInterface {

    }

    public static class Grandchild implements ChildInterface {
        public int getSource() {
            return 2;
        }
    }

    public static class TaintSourceClass {
        public int getSource() {
            return 7;
        }
    }

    public static class ChildClass extends TaintSourceClass {

    }

    public static class Grandchild2 extends ChildClass {
        @Override
        public int getSource() {
            return 3;
        }
    }

    /* Tests that the source method of a grandchild class of an interface whose source method is marked as a taint source
     * is also treated as a taint source. */
    @Test
    public void testInterfaceSourceMethodInherited() throws Exception {
        Grandchild obj = new Grandchild();
        int taintedInt = obj.getSource();
        assertNonNullTaint(taintedInt);
    }

    /* Tests that the source method of a grandchild class of a class whose source method is marked as a taint source
     * is also treated as a taint source. */
    @Test
    public void testClassSourceMethodInherited() throws Exception {
        Grandchild2 obj = new Grandchild2();
        int taintedInt = obj.getSource();
        assertNonNullTaint(taintedInt);
    }
}
