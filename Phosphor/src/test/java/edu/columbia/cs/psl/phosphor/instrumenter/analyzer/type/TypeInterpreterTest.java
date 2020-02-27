package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.type;

import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.PhosphorOpcodeIgnoringAnalyzer;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.analysis.Analyzer;
import org.junit.Test;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static edu.columbia.cs.psl.phosphor.control.graph.ControlFlowGraphTestUtil.getMethodNode;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertArrayEquals;

public class TypeInterpreterTest {

    private static final String owner = TypeInterpreterTestMethods.class.getName().replace(".", "/");
    private static final Type ownerType = Type.getType(TypeInterpreterTestMethods.class);

    @Test
    public void testMergeStringAndNull() throws Exception {
        MethodNode methodNode = getMethodNode(TypeInterpreterTestMethods.class, "mergeStringAndNull");
        Type[][] expectedLocals = new Type[][]{
                {ownerType, Type.BOOLEAN_TYPE}, // ldc
                {ownerType, Type.BOOLEAN_TYPE}, // astore_2
                {ownerType, Type.BOOLEAN_TYPE, Type.getType(String.class)}, // iload_1
                {ownerType, Type.BOOLEAN_TYPE, Type.getType(String.class)}, // ifeq
                {ownerType, Type.BOOLEAN_TYPE, Type.getType(String.class)}, // aconst_null
                {ownerType, Type.BOOLEAN_TYPE, Type.getType(String.class)}, // astore_2
                {ownerType, Type.BOOLEAN_TYPE, Type.getType(String.class)}, // aload_2
                {ownerType, Type.BOOLEAN_TYPE, Type.getType(String.class)}, // astore_3
                {ownerType, Type.BOOLEAN_TYPE, Type.getType(String.class), Type.getType(String.class)}  //return
        };
        Type[][] expectedStackElements = new Type[][]{
                {}, // ldc
                {Type.getType(String.class)}, // astore_2
                {}, // iload_1
                {Type.BOOLEAN_TYPE}, // ifeq
                {}, // aconst_null
                {TypeValue.nullType}, // astore_2
                {}, // aload_2
                {Type.getType(String.class)}, // astore_3
                {}  //return
        };
        checkFrames(expectedLocals, expectedStackElements, calculateTypeFrames(methodNode));
    }

    @Test
    public void mergeNullAndString() throws Exception {
        MethodNode methodNode = getMethodNode(TypeInterpreterTestMethods.class, "mergeNullAndString");
        Type[][] expectedLocals = new Type[][]{
                {ownerType, Type.BOOLEAN_TYPE}, // aconst_null
                {ownerType, Type.BOOLEAN_TYPE}, // astore_2
                {ownerType, Type.BOOLEAN_TYPE, TypeValue.nullType}, // iload_1
                {ownerType, Type.BOOLEAN_TYPE, TypeValue.nullType}, // ifeq
                {ownerType, Type.BOOLEAN_TYPE, TypeValue.nullType}, // ldc
                {ownerType, Type.BOOLEAN_TYPE, TypeValue.nullType}, // astore_2
                {ownerType, Type.BOOLEAN_TYPE, Type.getType(String.class)}, // aload_2
                {ownerType, Type.BOOLEAN_TYPE, Type.getType(String.class)}, // astore_3
                {ownerType, Type.BOOLEAN_TYPE, Type.getType(String.class), Type.getType(String.class)}  // return
        };
        Type[][] expectedStackElements = new Type[][]{
                {}, // aconst_null
                {TypeValue.nullType}, // astore_2
                {}, // iload_1
                {Type.BOOLEAN_TYPE}, // ifeq
                {}, // ldc
                {Type.getType(String.class)}, // astore_2
                {}, // aload_2
                {Type.getType(String.class)}, // astore_3
                {}  // return
        };
        checkFrames(expectedLocals, expectedStackElements, calculateTypeFrames(methodNode));
    }

    @Test
    public void mergeNullAndNull() throws Exception {
        MethodNode methodNode = getMethodNode(TypeInterpreterTestMethods.class, "mergeNullAndNull");
        Type[][] expectedLocals = new Type[][]{
                {ownerType, Type.BOOLEAN_TYPE}, // aconst_null
                {ownerType, Type.BOOLEAN_TYPE}, // astore_2
                {ownerType, Type.BOOLEAN_TYPE, TypeValue.nullType}, // aload_2
                {ownerType, Type.BOOLEAN_TYPE, TypeValue.nullType}, // astore_3
                {ownerType, Type.BOOLEAN_TYPE, TypeValue.nullType, TypeValue.nullType}, // iload_1
                {ownerType, Type.BOOLEAN_TYPE, TypeValue.nullType, TypeValue.nullType}, // ifeq
                {ownerType, Type.BOOLEAN_TYPE, TypeValue.nullType, TypeValue.nullType}, // aconst_null
                {ownerType, Type.BOOLEAN_TYPE, TypeValue.nullType, TypeValue.nullType}, // astore_3
                {ownerType, Type.BOOLEAN_TYPE, TypeValue.nullType, TypeValue.nullType}, // aload_3
                {ownerType, Type.BOOLEAN_TYPE, TypeValue.nullType, TypeValue.nullType}, // astore_2
                {ownerType, Type.BOOLEAN_TYPE, TypeValue.nullType, TypeValue.nullType}, // return
        };
        Type[][] expectedStackElements = new Type[][]{
                {}, // aconst_null
                {TypeValue.nullType}, // astore_2
                {}, // aload_2
                {TypeValue.nullType}, // astore_3
                {}, // iload_1
                {Type.BOOLEAN_TYPE}, // ifeq
                {}, // aconst_null
                {TypeValue.nullType}, // astore_3
                {}, // aload_3
                {TypeValue.nullType}, // astore_2
                {} // return
        };
        checkFrames(expectedLocals, expectedStackElements, calculateTypeFrames(methodNode));
    }

    @Test
    public void testSetBooleanTrue() throws Exception {
        MethodNode methodNode = getMethodNode(TypeInterpreterTestMethods.class, "setBooleanTrue");
        Type[][] expectedLocals = new Type[][]{
                {ownerType, Type.BOOLEAN_TYPE}, //     0: iconst_1
                {ownerType, Type.BOOLEAN_TYPE}, //     1: istore_1
                {ownerType, Type.BOOLEAN_TYPE}, //     2: return
        };
        Type[][] expectedStackElements = new Type[][]{
                {}, //     0: iconst_1
                {Type.INT_TYPE}, //     1: istore_1
                {}, //     2: return
        };
        checkFrames(expectedLocals, expectedStackElements, calculateTypeFrames(methodNode));
    }

    @Test
    public void testSetBooleanFalse() throws Exception {
        MethodNode methodNode = getMethodNode(TypeInterpreterTestMethods.class, "setBooleanTrue");
        Type[][] expectedLocals = new Type[][]{
                {ownerType, Type.BOOLEAN_TYPE}, //     0: iconst_0
                {ownerType, Type.BOOLEAN_TYPE}, //     1: istore_1
                {ownerType, Type.BOOLEAN_TYPE}, //     2: return
        };
        Type[][] expectedStackElements = new Type[][]{
                {}, //     0: iconst_0
                {Type.INT_TYPE}, //     1: istore_1
                {}, //     2: return
        };
        checkFrames(expectedLocals, expectedStackElements, calculateTypeFrames(methodNode));
    }

    @Test
    public void testInstanceOf() throws Exception {
        MethodNode methodNode = getMethodNode(TypeInterpreterTestMethods.class, "instanceOf");
        Type[][] expectedLocals = new Type[][]{
                {ownerType, Type.getType(Object.class)}, //     0: aload_1
                {ownerType, Type.getType(Object.class)}, //     1: instanceof
                {ownerType, Type.getType(Object.class)}, //     4: istore_2
                {ownerType, Type.getType(Object.class), Type.BOOLEAN_TYPE}, //     5: return
        };
        Type[][] expectedStackElements = new Type[][]{
                {}, //     0: aload_1
                {Type.getType(Object.class)}, //     1: instanceof
                {Type.BOOLEAN_TYPE}, //     4: istore_2
                {}, //     5: return
        };
        checkFrames(expectedLocals, expectedStackElements, calculateTypeFrames(methodNode));
    }

    @Test
    public void testBooleanArray() throws Exception {
        MethodNode methodNode = getMethodNode(TypeInterpreterTestMethods.class, "booleanArray");
        Type[][] expectedLocals = new Type[][]{
                {ownerType, Type.getType(boolean[].class)}, //    0: aload_1
                {ownerType, Type.getType(boolean[].class)}, // 1: iconst_0
                {ownerType, Type.getType(boolean[].class)}, // 2: baload
                {ownerType, Type.getType(boolean[].class)}, // 3: istore_2
                {ownerType, Type.getType(boolean[].class), Type.BOOLEAN_TYPE} // 4: return
        };
        Type[][] expectedStackElements = new Type[][]{
                {}, //    0: aload_1
                {Type.getType(boolean[].class)}, // 1: iconst_0
                {Type.getType(boolean[].class), Type.INT_TYPE}, // 2: baload
                {Type.BOOLEAN_TYPE}, // 3: istore_2
                {} // 4: return
        };
        checkFrames(expectedLocals, expectedStackElements, calculateTypeFrames(methodNode));
    }
    
    private static Frame<TypeValue>[] calculateTypeFrames(MethodNode methodNode) throws AnalyzerException {
        Analyzer<TypeValue> analyzer = new PhosphorOpcodeIgnoringAnalyzer<>(new TypeInterpreter(owner, methodNode));
        Frame<TypeValue>[] typeFrames = analyzer.analyze(owner, methodNode);
        @SuppressWarnings("unchecked")
        Frame<TypeValue>[] temp = (Frame<TypeValue>[]) new Frame[0];
        return filterFrames(methodNode.instructions, typeFrames).toArray(temp);
    }

    private static List<Frame<TypeValue>> filterFrames(InsnList instructions, Frame<TypeValue>[] typeFrames) {
        // Remove frames for LabelNodes, FrameNodes, and LineNumberNodes
        List<Frame<TypeValue>> frameList = new LinkedList<>();
        int i = 0;
        Iterator<AbstractInsnNode> itr = instructions.iterator();
        while(itr.hasNext()) {
            AbstractInsnNode insn = itr.next();
            if(!(insn instanceof LabelNode || insn instanceof FrameNode || insn instanceof LineNumberNode)) {
                frameList.add(typeFrames[i]);
            }
            i++;
        }
        return frameList;
    }

    private static void checkFrames(Type[][] expectedLocals, Type[][] expectedStackElements, Frame<TypeValue>[] frames) {
        assertEquals(expectedLocals.length, expectedStackElements.length);
        assertEquals(expectedLocals.length, frames.length);
        Type[][] actualLocals = new Type[expectedLocals.length][];
        Type[][] actualStackElements = new Type[expectedLocals.length][];
        for(int i = 0; i < frames.length; i++) {
            Frame<TypeValue> actual = frames[i];
            LinkedList<Type> types = new LinkedList<>();
            boolean start = false;
            for(int j = actual.getLocals() - 1; j >= 0; j--) {
                if(start || !TypeValue.UNINITIALIZED_VALUE.equals(actual.getLocal(j))) {
                    start = true;
                    types.push(actual.getLocal(j).getType());
                }
            }
            actualLocals[i] = types.toArray(new Type[0]);
        }
        for(int i = 0; i < frames.length; i++) {
            Frame<TypeValue> actual = frames[i];
            LinkedList<Type> types = new LinkedList<>();
            for(int j = 0; j < actual.getStackSize(); j++) {
                types.add(actual.getStack(j).getType());
            }
            actualStackElements[i] = types.toArray(new Type[0]);
        }
        assertArrayEquals(expectedLocals, actualLocals);
        assertArrayEquals(expectedStackElements, actualStackElements);
    }
}