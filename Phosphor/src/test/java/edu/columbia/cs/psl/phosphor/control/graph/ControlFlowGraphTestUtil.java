package edu.columbia.cs.psl.phosphor.control.graph;

import edu.columbia.cs.psl.phosphor.struct.harmony.util.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.util.Iterator;
import java.util.function.Predicate;

public class ControlFlowGraphTestUtil {

    public static final int ENTRY_NODE_ID = -2;
    public static final int EXIT_NODE_ID = -3;

    public static MethodNode getMethodNode(Class<?> clazz, String methodName) throws NoSuchMethodException, IOException {
        ClassReader cr = new ClassReader(clazz.getName());
        ClassNode classNode = new ClassNode();
        cr.accept(classNode, ClassReader.EXPAND_FRAMES);
        for(MethodNode mn : classNode.methods) {
            if(mn.name.equals(methodName)) {
                return mn;
            }
        }
        throw new NoSuchMethodException();
    }

    public static Map<BasicBlock, Integer> createBasicBlockIDMap(Class<?> clazz, MethodNode mn,
                                                                 Iterable<BasicBlock> basicBlocks) {
        String owner = clazz.getName().replaceAll("\\.", "/");
        Map<BasicBlock, Integer> blockIDMap = new HashMap<>();
        for(BasicBlock basicBlock : basicBlocks) {
            if(basicBlock instanceof EntryPoint) {
                blockIDMap.put(basicBlock, ENTRY_NODE_ID);
            } else if(basicBlock instanceof ExitPoint) {
                blockIDMap.put(basicBlock, EXIT_NODE_ID);
            } else {
                blockCheck:
                for(AbstractInsnNode node : getInstructions(mn, basicBlock)) {
                    if(node instanceof FieldInsnNode && owner.equals(((FieldInsnNode) node).owner)
                            && "blockID".equals(((FieldInsnNode) node).name)) {
                        for(AbstractInsnNode prev = node.getPrevious(); prev != null; prev = prev.getPrevious()) {
                            if(prev instanceof InsnNode) {
                                switch(prev.getOpcode()) {
                                    case Opcodes.ICONST_0:
                                        blockIDMap.put(basicBlock, 0);
                                        break blockCheck;
                                    case Opcodes.ICONST_1:
                                        blockIDMap.put(basicBlock, 1);
                                        break blockCheck;
                                    case Opcodes.ICONST_2:
                                        blockIDMap.put(basicBlock, 2);
                                        break blockCheck;
                                    case Opcodes.ICONST_3:
                                        blockIDMap.put(basicBlock, 3);
                                        break blockCheck;
                                    case Opcodes.ICONST_4:
                                        blockIDMap.put(basicBlock, 4);
                                        break blockCheck;
                                    case Opcodes.ICONST_5:
                                        blockIDMap.put(basicBlock, 5);
                                        break blockCheck;
                                }
                            } else if(prev instanceof IntInsnNode) {
                                blockIDMap.put(basicBlock, ((IntInsnNode) prev).operand);
                                break blockCheck;
                            }
                        }
                    }
                }
            }
        }
        return blockIDMap;
    }

    public static List<AbstractInsnNode> getInstructions(MethodNode mn, BasicBlock basicBlock) {
        List<AbstractInsnNode> instructions = new LinkedList<>();
        AbstractInsnNode node = basicBlock.getFirstInsn();
        if(mn.instructions.contains(node)) {
            instructions.add(node);
            do {
                node = node.getNext();
                if(node != null) {
                    instructions.add(node);
                }
            } while(node != null && node != basicBlock.getLastInsn());
        }
        return instructions;
    }

    public static Set<Integer> convertBasicBlocksToIDs(Map<BasicBlock, Integer> blockIDMap, Set<BasicBlock> targetSet) {
        Set<Integer> convertedSet = new HashSet<>();
        for(BasicBlock el : targetSet) {
            if(blockIDMap.containsKey(el)) {
                convertedSet.add(blockIDMap.get(el));
            }
        }
        return convertedSet;
    }

    public static Map<Integer, Set<Integer>> convertBasicBlocksToIDs(Map<BasicBlock, Integer> blockIDMap, Map<BasicBlock, Set<BasicBlock>> targetMap) {
        Map<Integer, Set<Integer>> convertedMap = new HashMap<>();
        for(BasicBlock key : targetMap.keySet()) {
            if(blockIDMap.containsKey(key)) {
                convertedMap.put(blockIDMap.get(key), convertBasicBlocksToIDs(blockIDMap, targetMap.get(key)));
            }
        }
        return convertedMap;
    }

    public static Map<Integer, Set<Integer>> calculateSuccessors(Class<?> clazz, MethodNode mn, boolean addExceptionalEdges,
                                                                 Map<AbstractInsnNode, String> explicitlyThrownExceptionTypes) {
        FlowGraph<BasicBlock> cfg = new BaseControlFlowGraphCreator(addExceptionalEdges).createControlFlowGraph(mn, explicitlyThrownExceptionTypes);
        Map<BasicBlock, Integer> blockIDMap = createBasicBlockIDMap(clazz, mn, cfg.getVertices());
        return convertBasicBlocksToIDs(blockIDMap, cfg.getSuccessors());
    }

    public static Map<Integer, Set<Integer>> calculateSuccessors(Class<?> clazz, String methodName) throws NoSuchMethodException, IOException {
        MethodNode mn = getMethodNode(clazz, methodName);
        FlowGraph<BasicBlock> cfg = new BaseControlFlowGraphCreator().createControlFlowGraph(mn);
        Map<BasicBlock, Integer> blockIDMap = createBasicBlockIDMap(clazz, mn, cfg.getVertices());
        return convertBasicBlocksToIDs(blockIDMap, cfg.getSuccessors());
    }

    public static Map<Integer, Set<Integer>> calculateLoops(Class<?> clazz, String methodName) throws NoSuchMethodException, IOException {
        MethodNode mn = getMethodNode(clazz, methodName);
        FlowGraph<BasicBlock> cfg = new BaseControlFlowGraphCreator().createControlFlowGraph(mn);
        Map<BasicBlock, Integer> blockIDMap = createBasicBlockIDMap(clazz, mn, cfg.getVertices());
        Map<BasicBlock, Set<BasicBlock>> loops = new HashMap<>();
        for(FlowGraph.NaturalLoop<BasicBlock> loop : cfg.getNaturalLoops()) {
            loops.put(loop.getHeader(), loop.getVertices());
        }
        return convertBasicBlocksToIDs(blockIDMap, loops);
    }

    public static List<AbstractInsnNode> filterInstructions(MethodNode mn, Predicate<AbstractInsnNode> predicate) {
        List<AbstractInsnNode> filtered = new LinkedList<>();
        Iterator<AbstractInsnNode> itr = mn.instructions.iterator();
        while(itr.hasNext()) {
            AbstractInsnNode insn = itr.next();
            if(predicate.test(insn)) {
                filtered.add(insn);
            }
        }
        return filtered;
    }
}
