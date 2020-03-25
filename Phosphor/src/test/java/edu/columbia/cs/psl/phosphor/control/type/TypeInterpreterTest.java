package edu.columbia.cs.psl.phosphor.control.type;

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

import static edu.columbia.cs.psl.phosphor.control.type.TypeInterpreterTestMethods.*;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertArrayEquals;

public class TypeInterpreterTest {

    private static final String owner = TypeInterpreterTestMethods.class.getName().replace(".", "/");

    @Test
    public void testMergeStringAndNull() throws Exception {
        MethodNode methodNode = mergeStringAndNull();
        Type[][] expectedLocals = new Type[][]{
                {Type.INT_TYPE}, // ldc
                {Type.INT_TYPE}, // astore_2
                {Type.INT_TYPE, Type.getType(String.class)}, // iload_1
                {Type.INT_TYPE, Type.getType(String.class)}, // ifeq
                {Type.INT_TYPE, Type.getType(String.class)}, // aconst_null
                {Type.INT_TYPE, Type.getType(String.class)}, // astore_2
                {Type.INT_TYPE, Type.getType(String.class)}, // aload_2
                {Type.INT_TYPE, Type.getType(String.class)}, // astore_3
                {Type.INT_TYPE, Type.getType(String.class), Type.getType(String.class)}  //return
        };
        Type[][] expectedStackElements = new Type[][]{
                {}, // ldc
                {Type.getType(String.class)}, // astore_2
                {}, // iload_1
                {Type.INT_TYPE}, // ifeq
                {}, // aconst_null
                {TypeValue.nullType}, // astore_2
                {}, // aload_2
                {Type.getType(String.class)}, // astore_3
                {}  //return
        };
        checkFrames(expectedLocals, expectedStackElements, calculateFilteredTypeFrames(methodNode));
    }

    @Test
    public void testMergeNullAndString() throws Exception {
        MethodNode methodNode = mergeNullAndString();
        Type[][] expectedLocals = new Type[][]{
                {Type.INT_TYPE}, // aconst_null
                {Type.INT_TYPE}, // astore_2
                {Type.INT_TYPE, TypeValue.nullType}, // iload_1
                {Type.INT_TYPE, TypeValue.nullType}, // ifeq
                {Type.INT_TYPE, TypeValue.nullType}, // ldc
                {Type.INT_TYPE, TypeValue.nullType}, // astore_2
                {Type.INT_TYPE, Type.getType(String.class)}, // aload_2
                {Type.INT_TYPE, Type.getType(String.class)}, // astore_3
                {Type.INT_TYPE, Type.getType(String.class), Type.getType(String.class)}  // return
        };
        Type[][] expectedStackElements = new Type[][]{
                {}, // aconst_null
                {TypeValue.nullType}, // astore_2
                {}, // iload_1
                {Type.INT_TYPE}, // ifeq
                {}, // ldc
                {Type.getType(String.class)}, // astore_2
                {}, // aload_2
                {Type.getType(String.class)}, // astore_3
                {}  // return
        };
        checkFrames(expectedLocals, expectedStackElements, calculateFilteredTypeFrames(methodNode));
    }

    @Test
    public void testMergeNullAndNull() throws Exception {
        MethodNode methodNode = mergeNullAndNull();
        Type[][] expectedLocals = new Type[][]{
                {Type.INT_TYPE}, // aconst_null
                {Type.INT_TYPE}, // astore_2
                {Type.INT_TYPE, TypeValue.nullType}, // aload_2
                {Type.INT_TYPE, TypeValue.nullType}, // astore_3
                {Type.INT_TYPE, TypeValue.nullType, TypeValue.nullType}, // iload_1
                {Type.INT_TYPE, TypeValue.nullType, TypeValue.nullType}, // ifeq
                {Type.INT_TYPE, TypeValue.nullType, TypeValue.nullType}, // aconst_null
                {Type.INT_TYPE, TypeValue.nullType, TypeValue.nullType}, // astore_3
                {Type.INT_TYPE, TypeValue.nullType, TypeValue.nullType}, // aload_3
                {Type.INT_TYPE, TypeValue.nullType, TypeValue.nullType}, // astore_2
                {Type.INT_TYPE, TypeValue.nullType, TypeValue.nullType}, // return
        };
        Type[][] expectedStackElements = new Type[][]{
                {}, // aconst_null
                {TypeValue.nullType}, // astore_2
                {}, // aload_2
                {TypeValue.nullType}, // astore_3
                {}, // iload_1
                {Type.INT_TYPE}, // ifeq
                {}, // aconst_null
                {TypeValue.nullType}, // astore_3
                {}, // aload_3
                {TypeValue.nullType}, // astore_2
                {} // return
        };
        checkFrames(expectedLocals, expectedStackElements, calculateFilteredTypeFrames(methodNode));
    }

    @Test
    public void testImpl_getCellBounds() throws Exception {
        MethodNode mn = TypeInterpreterTestMethods.impl_getCellBounds();
        AbstractInsnNode[] instructions = mn.instructions.toArray();
        Analyzer<TypeValue> analyzer = new PhosphorOpcodeIgnoringAnalyzer<>(new TypeInterpreter("javafx/scene/layout/GridPane", mn));
        Frame<TypeValue>[] frames = analyzer.analyze("javafx/scene/layout/GridPane", mn);
        for(int i = 0; i < frames.length; i++) {
            AbstractInsnNode insn = instructions[i];
            Frame<TypeValue> actualFrame = frames[i];
            if(insn instanceof FrameNode) {
                Frame<TypeValue> expectedFrame = TypeInterpreter.convertFrameNode((FrameNode) insn);
                for(int l = 0; l < expectedFrame.getLocals(); l++) {
                    assertEquals(expectedFrame.getLocal(l), actualFrame.getLocal(l));
                }
                for(int l = expectedFrame.getLocals(); l < actualFrame.getLocals(); l++) {
                    assertEquals(TypeValue.UNINITIALIZED_VALUE, actualFrame.getLocal(l));
                }
                assertEquals(expectedFrame.getStackSize(), actualFrame.getStackSize());
                for(int s = 0; s < expectedFrame.getStackSize(); s++) {
                    assertEquals(expectedFrame.getStack(s), actualFrame.getStack(s));
                }
            }
        }
    }

    private static Frame<TypeValue>[] calculateFilteredTypeFrames(MethodNode methodNode) throws AnalyzerException {
        Analyzer<TypeValue> analyzer = new PhosphorOpcodeIgnoringAnalyzer<>(new TypeInterpreter(TypeInterpreterTest.owner, methodNode));
        Frame<TypeValue>[] typeFrames = analyzer.analyze(TypeInterpreterTest.owner, methodNode);
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