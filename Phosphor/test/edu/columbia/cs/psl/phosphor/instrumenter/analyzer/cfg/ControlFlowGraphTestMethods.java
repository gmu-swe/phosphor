package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.cfg;

import edu.columbia.cs.psl.phosphor.struct.harmony.util.HashMap;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.Map;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.IOException;

@SuppressWarnings("unused")
public class ControlFlowGraphTestMethods {

    // Used to identify which basic block a particular code chunk ended up in
    private int blockID = -1;

    public static MethodNode getMethodNode(String methodName) throws NoSuchMethodException, IOException {
        ClassReader cr = new ClassReader(ControlFlowGraphTestMethods.class.getName());
        ClassNode classNode = new ClassNode();
        cr.accept(classNode, 0);
        for(MethodNode mn : classNode.methods) {
            if(mn.name.equals(methodName)) {
                return mn;
            }
        }
        throw new NoSuchMethodException();
    }

    public static Map<Integer, BasicBlock> makeBlockIDBasicBlockMap(BasicBlock[] basicBlocks) {
        String owner = ControlFlowGraphTestMethods.class.getName().replaceAll("\\.", "/");
        Map<Integer, BasicBlock> blockIDMap = new HashMap<>();
        for(BasicBlock basicBlock : basicBlocks) {
            for(AbstractInsnNode node : basicBlock.instructions) {
                if(node instanceof FieldInsnNode && owner.equals(((FieldInsnNode) node).owner)
                        && "blockID".equals(((FieldInsnNode) node).name)) {
                    AbstractInsnNode prev = node.getPrevious();
                    int id = -1;
                    if(prev instanceof InsnNode) {
                        switch(prev.getOpcode()) {
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
                    } else if(prev instanceof IntInsnNode) {
                        id = ((IntInsnNode) prev).operand;
                    }
                    if(id != -1) {
                        blockIDMap.put(id, basicBlock);
                    }
                }
            }
        }
        return blockIDMap;
    }

    public int basicTableSwitch(int value) {
        blockID = 0;
        int y;
        switch(value) {
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
        switch(value) {
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
        } catch(NullPointerException e) {
            blockID = 1;
            if(b) {
                blockID = 2;
                return 22;
            }
        }
        blockID = 3;
        return 134;
    }
}
