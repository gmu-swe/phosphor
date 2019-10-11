package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.cfg;

import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashMap;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashSet;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Map;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.util.Objects;

@SuppressWarnings("unused")
public class ControlFlowGraphTestMethods {

    public static final int ENTRY_NODE_ID = -2;
    public static final int EXIT_NODE_ID = -3;

    // Used to identify which basic block a particular code chunk ended up in
    private int blockID = -1;

    public static MethodNode getMethodNode(String methodName) throws NoSuchMethodException, IOException {
        ClassReader cr = new ClassReader(ControlFlowGraphTestMethods.class.getName());
        ClassNode classNode = new ClassNode();
        cr.accept(classNode, 0);
        for (MethodNode mn : classNode.methods) {
            if (mn.name.equals(methodName)) {
                return mn;
            }
        }
        throw new NoSuchMethodException();
    }

    private static Map<Integer, BasicBlock> createBlockIDBasicBlockMap(Iterable<BasicBlock> basicBlocks) {
        String owner = ControlFlowGraphTestMethods.class.getName().replaceAll("\\.", "/");
        Map<Integer, BasicBlock> blockIDMap = new HashMap<>();
        for (BasicBlock basicBlock : basicBlocks) {
            for (AbstractInsnNode node : basicBlock.instructions) {
                if (node instanceof FieldInsnNode && owner.equals(((FieldInsnNode) node).owner)
                        && "blockID".equals(((FieldInsnNode) node).name)) {
                    for (AbstractInsnNode prev = node.getPrevious(); prev != null; prev = prev.getPrevious()) {
                        int id = -1;
                        if (prev instanceof InsnNode) {
                            switch (prev.getOpcode()) {
                                case Opcodes.ICONST_0:
                                    id = 0;
                                    break;
                                case Opcodes.ICONST_1:
                                    id = 1;
                                    break;
                                case Opcodes.ICONST_2:
                                    id = 2;
                                    break;
                                case Opcodes.ICONST_3:
                                    id = 3;
                                    break;
                                case Opcodes.ICONST_4:
                                    id = 4;
                                    break;
                                case Opcodes.ICONST_5:
                                    id = 5;
                                    break;
                            }
                        } else if (prev instanceof IntInsnNode) {
                            id = ((IntInsnNode) prev).operand;
                        }
                        if (id != -1) {
                            blockIDMap.put(id, basicBlock);
                            break;
                        }
                    }
                }
            }
        }
        return blockIDMap;
    }

    public static Map<Integer, Set<Integer>> createNumberedSuccessorsMap(ControlFlowGraph cfg) {
        Map<Integer, BasicBlock> idBlockMap = createBlockIDBasicBlockMap(cfg.getBasicBlocks());
        Map<BasicBlock, Integer> blockIdMap = new HashMap<>();
        for (Integer key : idBlockMap.keySet()) {
            blockIdMap.put(idBlockMap.get(key), key);
        }
        Map<Integer, Set<Integer>> successors = new HashMap<>();
        for (BasicBlock basicBlock : cfg.getBasicBlocks()) {
            if (blockIdMap.containsKey(basicBlock)) {
                Set<Integer> s = new HashSet<>();
                for (ControlFlowNode node : cfg.getSuccessors(basicBlock)) {
                    if (blockIdMap.containsKey(node)) {
                        s.add(blockIdMap.get(node));
                    } else if (node instanceof EntryPoint) {
                        s.add(ENTRY_NODE_ID);
                    } else if (node instanceof ExitPoint) {
                        s.add(EXIT_NODE_ID);
                    }
                }
                successors.put(blockIdMap.get(basicBlock), s);
            }
        }
        return successors;
    }

    public int basicTableSwitch(int value) {
        blockID = 0;
        int y;
        switch (value) {
            case 1:
                blockID = 1;
                y = 44;
                break;
            case 8:
                blockID = 2;
                y = 88;
                break;
            case 3:
                blockID = 3;
                y = 99;
                break;
            case 4:
                blockID = 4;
                y = -8;
                break;
            case 5:
                blockID = 5;
                y = 220;
                break;
            default:
                blockID = 6;
                y = 0;
        }
        blockID = 7;
        return y * 6;
    }

    public int basicLookupSwitch(int value) {
        blockID = 0;
        int y;
        switch (value) {
            case 1:
                blockID = 1;
                y = 44;
                break;
            case 11:
                blockID = 2;
                y = 99;
                break;
            case 33333:
                blockID = 3;
                y = -8;
                break;
            case 77:
                blockID = 4;
                y = 220;
                break;
            case -9:
                blockID = 5;
                y = 12;
                break;
            default:
                blockID = 6;
                y = 0;
        }
        blockID = 7;
        return y * 6;
    }

    public int tryCatchWithIf(int[] a, boolean b) {
        try {
            blockID = 0;
            a[0] = 7;
        } catch (NullPointerException e) {
            blockID = 1;
            if (b) {
                blockID = 2;
                return 22;
            }
        }
        blockID = 3;
        return 134;
    }

    public void multipleReturnLoop(int[] a, int[] b) {
        blockID = 0;
        int x = 0;
        for (int i = 0; i < (blockID = 1); i++) {
            blockID = 2;
            if (a[i] == '%') {
                blockID = 3;
                if (!Objects.equals(a.length, i)) {
                    blockID = 4;
                    return;
                }
                blockID = 5;
                b[x++] = a[++i] + a[++i];
            } else {
                blockID = 6;
                b[x++] = a[i];
            }
            blockID = 7;
        }
        blockID = 8;
    }

    public void ifElseIntoWhileLoop(boolean b) {
        blockID = 0;
        int i;
        if (b) {
            blockID = 1;
            i = 3;
        } else {
            blockID = 2;
            i = 7;
        }
        while (i > (blockID = 3)) {
            blockID = 4;
            System.out.println(i);
            i--;
        }
        blockID = 5;
    }

    public int forLoopWithReturn(Integer[] a) {
        blockID = 0;
        int count = 0;
        for (int i = 0; i < (blockID = 1); i++) {
            blockID = 2;
            if (a[i] == null) {
                blockID = 3;
                count = -1;
                return count;
            }
            blockID = 4;
            count += a[i];
        }
        blockID = 5;
        return count;
    }

    public int forLoopWithBreak(Integer[] a) {
        blockID = 0;
        int count = 0;
        for (int i = 0; i < (blockID = 1); i++) {
            blockID = 2;
            if (a[i] == null) {
                blockID = 3;
                break;
            }
            blockID = 4;
            count += a[i];
        }
        blockID = 5;
        return count;
    }

    public int forLoopWithOr(Integer[] a) {
        blockID = 0;
        int count = 0;
        int extra = 13;
        for (int i = 0; i < (blockID = 1) || extra-- > (blockID = 2); i++) {
            blockID = 3;
            count += a[i];
        }
        blockID = 4;
        return count;
    }

    public int whileTrue(int a) {
        blockID = 0;
        int count = 0;
        while (true) {
            count += a;
            if (count > (blockID = 1)) {
                blockID = 4;
                break;
            }
            blockID = 2;
        }
        blockID = 3;
        return count;
    }

    public void nestedLoopsMultipleExits(Integer[] a) {
        blockID = 0;
        for (int i = 0; i < (blockID = 1); i++) {
            blockID = 2;
            if (a[i] == null) {
                blockID = 3;
                throw new IllegalArgumentException();
            }
            blockID = 4;
            for (int j = 0; j < (blockID = 5); j++) {
                blockID = 6;
                if (a[i] == j) {
                    blockID = 8;
                    throw new RuntimeException();
                }
                blockID = 7;
            }
            blockID = 9;
        }
        blockID = 10;
    }

    public int multipleTryBlocks(int[] a) {
        blockID = 0;
        int i = 0;
        try {
            while(i < (blockID = 1)) {
                blockID = 2;
                a[i++] = i;
            }
            blockID = 6;
        } catch(NullPointerException e) {
            blockID = 3;
            i = 7;
        } catch(IndexOutOfBoundsException e) {
            blockID = 4;
            if(i < 2) {
                blockID = 5;
                return i;
            }
        }
        blockID = 7;
        try {
            a[7] = 22;
        } catch(Exception e) {
            blockID = 8;
            i++;
        }
        blockID = 9;
        return i;
    }

    public boolean labeledBreak(int[][] a) {
        blockID = 0;
        boolean foundIt = false;
        int i, j;
        search:
        for(i = 0; i < (blockID = 1); i++) {
            blockID = 2;
            for(j = 0; j < (blockID = 3); j++) {
                blockID = 4;
                if(a[i][j] == 9) {
                    blockID = 5;
                    foundIt = true;
                    break search;
                }
                blockID = 6;
            }
            blockID = 7;
        }
        blockID = 8;
        return foundIt;
    }
}