package edu.columbia.cs.psl.phosphor.control.standard;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.PhosphorInstructionInfo;
import edu.columbia.cs.psl.phosphor.control.ControlFlowAnalyzer;
import edu.columbia.cs.psl.phosphor.control.OpcodesUtil;
import edu.columbia.cs.psl.phosphor.control.standard.ForceControlStore.ForceControlStoreField;
import edu.columbia.cs.psl.phosphor.control.standard.ForceControlStore.ForceControlStoreLocal;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.BasicArrayInterpreter;
import edu.columbia.cs.psl.phosphor.struct.Field;
import edu.columbia.cs.psl.phosphor.struct.SinglyLinkedList;
import edu.columbia.cs.psl.phosphor.struct.harmony.util.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

import java.util.Iterator;

import static edu.columbia.cs.psl.phosphor.control.standard.ExecuteForceControlStore.EXECUTE_FORCE_CONTROL_STORE;
import static edu.columbia.cs.psl.phosphor.org.objectweb.asm.commons.OurLocalVariablesSorter.OBJECT_TYPE;
import static org.objectweb.asm.Type.*;

public class StandardControlFlowAnalyzer implements ControlFlowAnalyzer {

    private final boolean shouldTrackExceptions;
    private final boolean isImplicitLightTracking;
    Map<Integer, AnnotatedInstruction> implicitAnalysisBlocks = new HashMap<>();
    private int numberOfJumps;
    private int numberOfTryCatch;
    private int numberOfThrows;

    public StandardControlFlowAnalyzer(boolean isImplicitLightTracking) {
        this.isImplicitLightTracking = isImplicitLightTracking;
        this.shouldTrackExceptions = Configuration.IMPLICIT_EXCEPTION_FLOW;
    }

    /**
     * This method should:
     * BranchStart, BranchEnd signals to the instruction stream
     * <p>
     * For branch not taken:
     * Add FORCE_CONTROL_STORE for:
     * Item at top of stack (add an InsnNode)
     * Local variables
     * Fields of "this"
     * Static fields
     */
    @Override
    public void annotate(String owner, MethodNode methodNode) {
        this.numberOfTryCatch = methodNode.tryCatchBlocks.size();
        countThrows(methodNode.instructions);
        try {
            FlowAnalyzer fa = new FlowAnalyzer((methodNode.access & Opcodes.ACC_STATIC) != 0, new HashMap<>(), methodNode.instructions);
            fa.analyze(owner, methodNode);
            boolean hasJumps = false;
            InsnList instructions = methodNode.instructions;
            HashSet<AnnotatedInstruction> tryCatchHandlers = new HashSet<>();
            if(shouldTrackExceptions && numberOfTryCatch > 0) {
                int exceptionHandlerCount = 1;
                hasJumps = true;
                for(Object o : methodNode.tryCatchBlocks) {
                    TryCatchBlockNode t = (TryCatchBlockNode) o;
                    AnnotatedInstruction startBlock = null;
                    AnnotatedInstruction handlerBlock = null;
                    AnnotatedInstruction endBlock = null;
                    Integer startKey = null;
                    Integer endKey = null;
                    for(Map.Entry<Integer, AnnotatedInstruction> e : implicitAnalysisBlocks.entrySet()) {
                        AnnotatedInstruction b = e.getValue();
                        Integer i = e.getKey();
                        if(b.insn == t.handler) {
                            handlerBlock = b;
                        }
                        if(b.insn == t.start) {
                            startBlock = b;
                            startKey = i;
                        }
                        if(b.insn == t.end) {
                            endBlock = b;
                            endKey = i;
                        }
                        if(startBlock != null && handlerBlock != null && endBlock != null) {
                            break;
                        }
                    }
                    if(startBlock == handlerBlock || endBlock == null) {
                        continue;
                    }
                    //Identify all of the instructions in this try block
                    if(startBlock != null) {
                        for(int i = startKey; i <= endKey; i++) {
                            if(implicitAnalysisBlocks.get(i) != null) {
                                implicitAnalysisBlocks.get(i).coveredByTryBlockFor.add(t.type);
                            }
                        }
                    }
                    handlerBlock.exceptionsHandled.add(t.type);
                    tryCatchHandlers.add(handlerBlock);
                    startBlock.isTryBlockStart = true;
                    startBlock.exceptionsHandled.add(t.type);
                    handlerBlock.onFalseSideOfJumpFrom.add(startBlock);
                    handlerBlock.handlerForRegionStartingAt.add(startBlock);
                    startBlock.successors.add(handlerBlock);
                    startBlock.ex_count = exceptionHandlerCount;
                    startBlock.tryBlockEnd = endBlock;
                    startBlock.handledAt = handlerBlock;
                    exceptionHandlerCount++;
                    for(AnnotatedInstruction suc : startBlock.successors) {
                        if(!suc.onFalseSideOfJumpFrom.contains(startBlock)) {
                            suc.onTrueSideOfJumpFrom.add(startBlock);
                        }
                    }
                }
            }
            for(AnnotatedInstruction b : implicitAnalysisBlocks.values()) {
                if(b.isJump) {
                    hasJumps = true;
                    break;
                }
            }
            if(implicitAnalysisBlocks.size() > 1 && hasJumps) {
                Stack<AnnotatedInstruction> stack = new Stack<>();
                //Fix successors to only point to jumps or labels
                boolean changed = true;
                while(changed) {
                    changed = false;
                    for(AnnotatedInstruction b : implicitAnalysisBlocks.values()) {
                        for(AnnotatedInstruction s : b.successors) {
                            if(s.isInteresting()) {
                                changed |= b.basicBlockStartingSuccessors.add(s);
                            } else {
                                changed |= b.basicBlockStartingSuccessors.addAll(s.basicBlockStartingSuccessors);
                            }
                        }
                    }
                }
                // Post dominator analysis
                HashSet<AnnotatedInstruction> interestingBlocks = new HashSet<>();
                for(AnnotatedInstruction b : implicitAnalysisBlocks.values()) {
                    if(b.isInteresting()) {
                        interestingBlocks.add(b);
                    }
                }
                for(AnnotatedInstruction b : implicitAnalysisBlocks.values()) {
                    if(b.basicBlockStartingSuccessors.isEmpty()) {
                        b.postDominators.add(b);
                    } else {
                        b.postDominators.addAll(interestingBlocks);
                    }
                }
                changed = true;
                while(changed) {
                    changed = false;
                    for(AnnotatedInstruction b : implicitAnalysisBlocks.values()) {
                        if(!b.basicBlockStartingSuccessors.isEmpty() && b.isInteresting()) {
                            Iterator<AnnotatedInstruction> iter = b.basicBlockStartingSuccessors.iterator();
                            AnnotatedInstruction successor = iter.next();
                            HashSet<AnnotatedInstruction> intersectionOfPredecessors = new HashSet<>(successor.postDominators);
                            while(iter.hasNext()) {
                                successor = iter.next();
                                intersectionOfPredecessors.retainAll(successor.postDominators);
                            }
                            intersectionOfPredecessors.add(b);
                            if(!b.postDominators.equals(intersectionOfPredecessors)) {
                                changed = true;
                                b.postDominators = intersectionOfPredecessors;
                            }
                        }
                    }
                }
                // Add in markings for where jumps are resolved
                for(AnnotatedInstruction j : implicitAnalysisBlocks.values()) {
                    if(j.isJump || j.isTryBlockStart) {
                        j.postDominators.remove(j);
                        HashSet<AnnotatedInstruction> visited = new HashSet<>();
                        AnnotatedInstruction min = findImmediatePostDominator(j, visited);
                        if(min != null) {
                            min.resolvedBlocks.add(j);
                            min.resolvedHereBlocks.add(j);
                        } else {
                            // There are no post-dominators of this branch. That means that one leg of the
                            // branch goes to a return. So, we'll say that this gets resolved at each return that
                            // is a successor
                            for(AnnotatedInstruction b : visited) {
                                if(isExitInstruction(b.insn)) {
                                    b.resolvedHereBlocks.add(j);
                                }
                            }
                        }
                    }
                }
                // Propagate forward true-side/false-side to determine which vars are written
                stack.add(implicitAnalysisBlocks.get(0));
                while(!stack.isEmpty()) {
                    AnnotatedInstruction b = stack.pop();
                    if(b.visited) {
                        continue;
                    }
                    b.visited = true;
                    b.onFalseSideOfJumpFrom.removeAll(b.resolvedBlocks);
                    b.onTrueSideOfJumpFrom.removeAll(b.resolvedBlocks);
                    // Propagate markings to successors
                    for(AnnotatedInstruction s : b.successors) {
                        s.onFalseSideOfJumpFrom.addAll(b.onFalseSideOfJumpFrom);
                        s.onTrueSideOfJumpFrom.addAll(b.onTrueSideOfJumpFrom);
                        s.resolvedBlocks.addAll(b.resolvedBlocks);
                        s.onFalseSideOfJumpFrom.remove(s);
                        s.onTrueSideOfJumpFrom.remove(s);
                        stack.add(s);
                    }
                }
                for(AnnotatedInstruction j : implicitAnalysisBlocks.values()) {
                    j.visited = false;

                }
                for(AnnotatedInstruction j : implicitAnalysisBlocks.values()) {
                    if(j.isJump || j.isTryBlockStart) {
                        stack = new Stack<>();
                        stack.addAll(j.successors);
                        Set<AnnotatedInstruction> visited = new HashSet<>();
                        while(!stack.isEmpty()) {
                            AnnotatedInstruction b = stack.pop();
                            if(!visited.add(b)) {
                                continue;
                            }
                            if(b.onTrueSideOfJumpFrom.contains(j)) {
                                j.varsWrittenTrueSide.addAll(b.varsWritten);
                                j.fieldsWrittenTrueSide.addAll(b.fieldsWritten);
                                j.exceptionsThrownTrueSide.addAll(b.exceptionsThrown);
                                stack.addAll(b.successors);
                            } else if(b.onFalseSideOfJumpFrom.contains(j)) {
                                j.varsWrittenFalseSide.addAll(b.varsWritten);
                                j.fieldsWrittenFalseSide.addAll(b.fieldsWritten);
                                j.exceptionsThrownFalseSide.addAll(b.exceptionsThrown);
                                stack.addAll(b.successors);
                            }
                        }
                    }
                }
                Map<AnnotatedInstruction, Integer> jumpIDs = new HashMap<>();
                int jumpID = 0;
                for(AnnotatedInstruction r : implicitAnalysisBlocks.values()) {
                    if(r.isTryBlockStart) {
                        annotateExceptionHandler(instructions, r);
                    } else if(r.isJump) {
                        jumpID = annotateJump(instructions, r, jumpID, jumpIDs);
                    } else if(shouldTrackExceptions && r.insn.getOpcode() >= Opcodes.IRETURN && r.insn.getOpcode() <= Opcodes.RETURN) {
                        //Return statement: check to see how we might have gotten here, and then find which exceptions we might have thrown if we came otherwise
                        HashSet<String> missedExceptions = new HashSet<>();
                        for(AnnotatedInstruction b : r.onFalseSideOfJumpFrom) {
                            HashSet<String> tmp = new HashSet<>(b.exceptionsThrownTrueSide);
                            tmp.removeAll(b.exceptionsThrownFalseSide);
                            missedExceptions.addAll(tmp);
                        }
                        for(AnnotatedInstruction b : r.onTrueSideOfJumpFrom) {
                            HashSet<String> tmp = new HashSet<>(b.exceptionsThrownFalseSide);
                            tmp.removeAll(b.exceptionsThrownTrueSide);
                            missedExceptions.addAll(tmp);
                        }
                        HashSet<String> filtered = new HashSet<>();
                        for(String s : missedExceptions) {
                            if(s == null) {
                                s = "java/lang/Throwable";
                            }
                            if(s.contains("#")) {
                                s = s.substring(0, s.indexOf('#'));
                            }
                            if(filtered.add(s)) {
                                instructions.insertBefore(r.insn, new LdcInsnNode(new UnthrownException(s)));
                            }
                        }
                    } else if(shouldTrackExceptions && (r.insn.getType() == AbstractInsnNode.METHOD_INSN
                            || r.insn.getType() == AbstractInsnNode.INVOKE_DYNAMIC_INSN)) {
                        // Are we in a try handler? If so, after this instruction, we should note that our execution may be contingent on some exception
                        for(String s : r.coveredByTryBlockFor) {
                            if(s == null) {
                                s = "java/lang/Throwable";
                            }
                            instructions.insert(r.insn, new LdcInsnNode(new UnthrownExceptionCheck(s)));
                        }
                    }
                }
                for(AnnotatedInstruction b : implicitAnalysisBlocks.values()) {
                    AbstractInsnNode insn = b.insn;
                    while(insn.getType() == AbstractInsnNode.FRAME || insn.getType() == AbstractInsnNode.LINE || insn.getType() == AbstractInsnNode.LABEL) {
                        insn = insn.getNext();
                    }
                    if(b.resolvedHereBlocks.size() == jumpIDs.size()) {
                        //Everything is resolved
                        BranchEnd end = new BranchEnd(-1);
                        instructions.insertBefore(insn, new LdcInsnNode(end));
                    } else {
                        for(AnnotatedInstruction r : b.resolvedHereBlocks) {
                            if(!r.isTryBlockStart) {
                                if(!b.successors.isEmpty()) {
                                    // For any return or athrow, we'll just bulk pop-all
                                    BranchEnd end = new BranchEnd(jumpIDs.get(r));
                                    instructions.insertBefore(insn, new LdcInsnNode(end));
                                    if(r.isTwoOperandJumpInstruction) {
                                        end = new BranchEnd(jumpIDs.get(r) + 1);
                                        instructions.insertBefore(insn, new LdcInsnNode(end));
                                    }
                                }
                            }
                        }
                    }
                    // In light tracking mode no need to POP off of control at RETURN/THROW, because we don't reuse the obj
                    if(b.successors.isEmpty() && !isImplicitLightTracking) {
                        instructions.insertBefore(b.insn, new LdcInsnNode(EXECUTE_FORCE_CONTROL_STORE));
                        instructions.insertBefore(b.insn, new LdcInsnNode(new BranchEnd(-1)));
                    }
                }
                this.numberOfJumps = jumpID;
            }
        } catch(AnalyzerException e) {
            numberOfJumps = 0;
        }
    }

    private void annotateExceptionHandler(InsnList instructions, AnnotatedInstruction r) {
        //Need to actually insert this code at every exit from the SCC that is this try-catch block.
        //Find the end of the handler
        //this is any block that succeeds the handler and either: has no successors or has a successor in common with the start block
        LinkedList<AnnotatedInstruction> handlerEndBlock = new LinkedList<>();
        LinkedList<AnnotatedInstruction> toCheck = new LinkedList<>(r.handledAt.successors);
        Set<AnnotatedInstruction> visited = new HashSet<>();
        while(!toCheck.isEmpty()) {
            AnnotatedInstruction e = toCheck.poll();
            if(!visited.add(e)) {
                continue;
            }
            if(e.successors.isEmpty()) {
                handlerEndBlock.add(e);
            } else if(r.postDominators.contains(e)) {
                handlerEndBlock.add(e);
            } else {
                toCheck.addAll(e.successors);
            }
        }

        AbstractInsnNode lastInstructionInTryBlock = r.tryBlockEnd.insn;
        while(lastInstructionInTryBlock.getType() == AbstractInsnNode.FRAME
                || lastInstructionInTryBlock.getType() == AbstractInsnNode.LINE
                || lastInstructionInTryBlock.getType() == AbstractInsnNode.LABEL) {
            lastInstructionInTryBlock = lastInstructionInTryBlock.getNext();
        }
        //Set up the force control store's at the bottom of the try block
        Set<ForceControlStoreLocal> lvsOnlyInHandler = new HashSet<>(r.varsWrittenFalseSide);
        lvsOnlyInHandler.removeAll(r.varsWrittenTrueSide);
        HashSet<Field> fieldsOnlyInHandler = new HashSet<>(r.fieldsWrittenFalseSide);
        fieldsOnlyInHandler.removeAll(r.fieldsWrittenTrueSide);
        for(ForceControlStoreLocal i : lvsOnlyInHandler) {
            instructions.insertBefore(lastInstructionInTryBlock, new LdcInsnNode(i));
        }
        for(Field f : fieldsOnlyInHandler) {
            LdcInsnNode force = new LdcInsnNode(new ForceControlStoreField(f));
            instructions.insertBefore(lastInstructionInTryBlock, force);
        }
        AbstractInsnNode handledAtInsn = r.handledAt.insn;
        Set<String> handledHereAlready = new HashSet<>();
        Set<ForceControlStoreLocal> forceStoreAlready = new HashSet<>();
        while(handledAtInsn.getType() == AbstractInsnNode.FRAME || handledAtInsn.getType() == AbstractInsnNode.LINE
                || handledAtInsn.getType() == AbstractInsnNode.LABEL || handledAtInsn.getOpcode() > 200
                || (handledAtInsn instanceof LdcInsnNode && ((LdcInsnNode) handledAtInsn).cst instanceof PhosphorInstructionInfo)) {
            if(handledAtInsn instanceof LdcInsnNode && ((LdcInsnNode) handledAtInsn).cst instanceof ExceptionHandlerStart) {
                String exceptionType = ((ExceptionHandlerStart) ((LdcInsnNode) handledAtInsn).cst).getExceptionType();
                if(exceptionType != null) {
                    handledHereAlready.add(exceptionType);
                }
            } else if(handledAtInsn instanceof LdcInsnNode && ((LdcInsnNode) handledAtInsn).cst instanceof ForceControlStoreLocal) {
                forceStoreAlready.add((ForceControlStoreLocal) ((LdcInsnNode) handledAtInsn).cst);
            }
            handledAtInsn = handledAtInsn.getNext();
        }

        //Then do all of the force-ctr-stores
        //In the exception handler, force a store of what was written
        Set<ForceControlStoreLocal> diff = new HashSet<>();

        diff.addAll(r.varsWrittenTrueSide);
        diff.removeAll(r.varsWrittenFalseSide);

        HashSet<Field> diffFields = new HashSet<>();
        diffFields.addAll(r.fieldsWrittenTrueSide);
        diffFields.removeAll(r.fieldsWrittenFalseSide);

        for(ForceControlStoreLocal i : diff) {
            if(!forceStoreAlready.contains(i)) {
                instructions.insertBefore(handledAtInsn, new LdcInsnNode(i));
            }
        }
        for(Field f : diffFields) {
            LdcInsnNode force = new LdcInsnNode(new ForceControlStoreField(f));
            instructions.insertBefore(handledAtInsn, force);
        }

        //At the START of the handler, note that it's the start...
        if(handledHereAlready.isEmpty()) {
            instructions.insertBefore(handledAtInsn, new LdcInsnNode(new ExceptionHandlerStart(null)));
        }
        for(String ex : r.exceptionsHandled) {
            if(ex == null) {
                ex = "java/lang/Throwable";
            }
            if(!handledHereAlready.contains(ex)) {
                instructions.insertBefore(handledAtInsn, new LdcInsnNode(new ExceptionHandlerStart(ex)));
            }
            instructions.insertBefore(lastInstructionInTryBlock, new LdcInsnNode(new ExceptionHandlerEnd(ex)));
        }

        //At the END of the handler, remove this exception from the queue
        for(AnnotatedInstruction b : handlerEndBlock) {
            AbstractInsnNode insn = b.insn;
            //Peek backwards to see if we are behind a GOTO
            while(insn != null && insn.getPrevious() != null && mightEndBlock(insn.getPrevious())) {
                insn = insn.getPrevious();
            }
            if(insn.getType() == AbstractInsnNode.LABEL || insn.getType() == AbstractInsnNode.LINE || insn.getType() == AbstractInsnNode.FRAME) {
                insn = b.insn;
            }
            instructions.insertBefore(insn, new LdcInsnNode(new ExceptionHandlerEnd(null)));
        }
    }

    private int annotateJump(InsnList instructions, AnnotatedInstruction r, int jumpID, Map<AnnotatedInstruction, Integer> jumpIDs) {
        jumpID++;
        Set<ForceControlStoreLocal> common = new HashSet<>();
        common.addAll(r.varsWrittenFalseSide);
        common.retainAll(r.varsWrittenTrueSide);
        Set<ForceControlStoreLocal> diff = new HashSet<>();
        diff.addAll(r.varsWrittenTrueSide);
        diff.addAll(r.varsWrittenFalseSide);
        diff.removeAll(common);

        HashSet<Field> commonFields = new HashSet<>();
        commonFields.addAll(r.fieldsWrittenTrueSide);
        commonFields.retainAll(r.fieldsWrittenFalseSide);
        HashSet<Field> diffFields = new HashSet<>();
        diffFields.addAll(r.fieldsWrittenFalseSide);
        diffFields.addAll(r.fieldsWrittenTrueSide);
        diffFields.removeAll(common);

        HashSet<String> commonExceptionsThrown = new HashSet<>();
        commonExceptionsThrown.addAll(r.exceptionsThrownFalseSide);
        commonExceptionsThrown.retainAll(r.exceptionsThrownTrueSide);
        HashSet<String> diffExceptions = new HashSet<>();
        diffExceptions.addAll(r.exceptionsThrownTrueSide);
        diffExceptions.addAll(r.exceptionsThrownFalseSide);
        diffExceptions.removeAll(commonExceptionsThrown);

        instructions.insertBefore(r.insn, new LdcInsnNode(new BranchStart(jumpID)));
        jumpIDs.put(r, jumpID);
        if(r.isTwoOperandJumpInstruction) {
            jumpID++;
        }
        for(ForceControlStoreLocal i : diff) {
            instructions.insertBefore(r.insn, new LdcInsnNode(i));
        }
        for(Field f : diffFields) {
            LdcInsnNode force = new LdcInsnNode(new ForceControlStoreField(f));
            instructions.insertBefore(r.insn, force);
        }
        return jumpID;
    }

    public int getNumberOfUniqueBranchIDs() {
        return (numberOfJumps + numberOfTryCatch == 0) ? 0 : numberOfJumps + numberOfTryCatch + 2;
    }

    public int getNumberOfTryCatch() {
        return numberOfTryCatch;
    }

    public int getNumberOfThrows() {
        return numberOfThrows;
    }

    private void countThrows(InsnList instructions) {
        Iterator<AbstractInsnNode> itr = instructions.iterator();
        while(itr.hasNext()) {
            if(itr.next().getOpcode() == Opcodes.ATHROW) {
                numberOfThrows++;
            }
        }
    }

    /**
     * Returns the immediate post-dominator of the specified block if one the specified block's post-dominators can be
     * reached from the block along successor edges. If such a post-dominator cannot be found, returns null and
     * ensures that the specified visited set contains all of the blocks reachable from the specified block using
     * successor edges.
     */
    private AnnotatedInstruction findImmediatePostDominator(AnnotatedInstruction block, HashSet<AnnotatedInstruction> visited) {
        SinglyLinkedList<AnnotatedInstruction> queue = new SinglyLinkedList<>();
        queue.enqueue(block);
        visited.add(block);
        while(!queue.isEmpty()) {
            AnnotatedInstruction cur = queue.dequeue();
            if(!cur.equals(block) && block.postDominators.contains(cur)) {
                return cur; // Found a post-dominator, first one found using BFS has shortest path
            } else {
                for(AnnotatedInstruction successor : cur.successors) {
                    if(visited.add(successor)) {
                        queue.enqueue(successor);
                    }
                }
            }
        }
        return null;
    }

    /* Returns whether the specified instruction triggers a method exit. */
    public static boolean isExitInstruction(AbstractInsnNode instruction) {
        int opcode = instruction.getOpcode();
        return OpcodesUtil.isReturnOpcode(opcode) || opcode == Opcodes.ATHROW;
    }

    public static boolean mightEndBlock(AbstractInsnNode insn) {
        return insn.getType() == AbstractInsnNode.LABEL || isExitInstruction(insn) || insn.getOpcode() == Opcodes.GOTO;
    }

    private class FlowAnalyzer extends Analyzer<BasicValue> {

        protected int[] insnToLabel;
        HashMap<Integer, LinkedList<Integer>> edges = new HashMap<>();
        HashSet<String> visited = new HashSet<>();
        InsnList instructions;

        public FlowAnalyzer(boolean isStatic, HashMap<AbstractInsnNode, String> referenceArraysToCheckCast, InsnList instructions) {
            super(new BasicArrayInterpreter(isStatic, isImplicitLightTracking, referenceArraysToCheckCast));
            this.instructions = instructions;
        }

        protected Frame<BasicValue> newFrame(int nLocals, int nStack) {
            return new Frame<BasicValue>(nLocals, nStack) {
                @Override
                public void execute(AbstractInsnNode insn, Interpreter<BasicValue> interpreter) throws AnalyzerException {
                    if(insn.getOpcode() > 200 || (insn instanceof LdcInsnNode && ((LdcInsnNode) insn).cst instanceof PhosphorInstructionInfo)) {
                        return;
                    }
                    super.execute(insn, interpreter);
                }
            };
        }

        @Override
        public Frame<BasicValue>[] analyze(String owner, MethodNode m) throws AnalyzerException {
            Iterator<AbstractInsnNode> insns = m.instructions.iterator();
            insnToLabel = new int[m.instructions.size()];
            int label = -1;
            boolean isFirst = true;
            while(insns.hasNext()) {
                AbstractInsnNode nextInsn = insns.next();
                int idx = m.instructions.indexOf(nextInsn);
                if(nextInsn instanceof LabelNode) {
                    label++;
                }
                insnToLabel[idx] = (isFirst ? 1 : label);
                isFirst = false;
            }
            Frame<BasicValue>[] ret = super.analyze(owner, m);
            if(shouldTrackExceptions) {
                insns = m.instructions.iterator();
                while(insns.hasNext()) {
                    AbstractInsnNode insn = insns.next();
                    int idx = m.instructions.indexOf(insn);
                    if(insn.getOpcode() == Opcodes.ATHROW) {
                        //Are we in a try/catch block that can catch this?
                        for(TryCatchBlockNode each : m.tryCatchBlocks) {
                            try {
                                Class<?> caught = Class.forName((each.type == null ? "java.lang.Throwable" : each.type.replace('/', '.')), false, getClass().getClassLoader());
                                if(caught == Throwable.class) {
                                    //if catching Throwable, we'll catch this regardless of whether we can load the exception type or not
                                    newControlFlowEdge(idx, m.instructions.indexOf(each.handler));
                                    break;
                                }
                                Class<?> thrown;
                                try {
                                    String onStack = ret[idx].getStack(ret[idx].getStackSize() - 1).toString();
                                    if(!onStack.startsWith("L")) {
                                        continue;
                                    }
                                    thrown = Class.forName(Type.getType(ret[idx].getStack(0).toString()).getClassName(), false, getClass().getClassLoader());
                                } catch(Throwable t) {
                                    continue;
                                }
                                if(caught.isAssignableFrom(thrown)) {
                                    //Found a handler for this thrown
                                    newControlFlowEdge(idx, m.instructions.indexOf(each.handler));
                                    break;
                                }
                            } catch(Throwable t) {
                                //Maybe can't load exception type, that's ok
                            }
                        }
                    }
                }
            }
            return ret;
        }

        @Override
        protected boolean newControlFlowExceptionEdge(int insnIndex, int successorIndex) {
            return true;
        }

        @Override
        protected void newControlFlowEdge(int insn, int successor) {
            if(visited.contains(insn + "-" + successor)) {
                return;
            }
            visited.add(insn + "-" + successor);
            if(!edges.containsKey(successor)) {
                edges.put(successor, new LinkedList<>());
            }
            if(!edges.get(successor).contains(insn)) {
                edges.get(successor).add(insn);
            }
            AnnotatedInstruction fromBlock;
            if(!implicitAnalysisBlocks.containsKey(insn)) {
                //insn not added yet
                fromBlock = new AnnotatedInstruction();
                fromBlock.idx = insn;
                fromBlock.insn = instructions.get(insn);
                implicitAnalysisBlocks.put(insn, fromBlock);
            } else {
                fromBlock = implicitAnalysisBlocks.get(insn);
            }

            AbstractInsnNode insnN = instructions.get(insn);
            fromBlock.isJump = (insnN.getType() == AbstractInsnNode.JUMP_INSN && insnN.getOpcode() != Opcodes.GOTO)
                    || insnN.getType() == AbstractInsnNode.LOOKUPSWITCH_INSN || insnN.getType() == AbstractInsnNode.TABLESWITCH_INSN;
            if(fromBlock.isJump && insnN.getType() == AbstractInsnNode.JUMP_INSN) {
                switch(insnN.getOpcode()) {
                    case Opcodes.IF_ICMPEQ:
                    case Opcodes.IF_ICMPNE:
                    case Opcodes.IF_ICMPGE:
                    case Opcodes.IF_ICMPGT:
                    case Opcodes.IF_ICMPLT:
                    case Opcodes.IF_ICMPLE:
                    case Opcodes.IF_ACMPEQ:
                    case Opcodes.IF_ACMPNE:
                        fromBlock.isTwoOperandJumpInstruction = true;
                        break;
                }
            }
            AnnotatedInstruction successorBlock;
            if(implicitAnalysisBlocks.containsKey(successor)) {
                successorBlock = implicitAnalysisBlocks.get(successor);
            } else {
                successorBlock = new AnnotatedInstruction();
                successorBlock.idx = successor;
                successorBlock.insn = instructions.get(successor);
                implicitAnalysisBlocks.put(successor, successorBlock);
                if(successorBlock.insn.getType() == AbstractInsnNode.IINC_INSN) {
                    ForceControlStoreLocal local = new ForceControlStoreLocal(((IincInsnNode) successorBlock.insn).var, INT_TYPE);
                    successorBlock.varsWritten.add(local);
                } else if(successorBlock.insn.getType() == AbstractInsnNode.VAR_INSN) {
                    int index = ((VarInsnNode) successorBlock.insn).var;
                    switch(successorBlock.insn.getOpcode()) {
                        case ISTORE:
                            successorBlock.varsWritten.add(new ForceControlStoreLocal(index, INT_TYPE));
                            break;
                        case ASTORE:
                            successorBlock.varsWritten.add(new ForceControlStoreLocal(index, OBJECT_TYPE));
                            break;
                        case DSTORE:
                            successorBlock.varsWritten.add(new ForceControlStoreLocal(index, DOUBLE_TYPE));
                            break;
                        case LSTORE:
                            successorBlock.varsWritten.add(new ForceControlStoreLocal(index, LONG_TYPE));
                            break;
                        case FSTORE:
                            successorBlock.varsWritten.add(new ForceControlStoreLocal(index, FLOAT_TYPE));
                    }
                } else if(successorBlock.insn.getType() == AbstractInsnNode.FIELD_INSN) {
                    FieldInsnNode fin = (FieldInsnNode) successorBlock.insn;
                    if(fin.getOpcode() == Opcodes.PUTFIELD) {
                        Frame<BasicValue> fr = this.getFrames()[successor];
                        if(fr != null && fr.getStack(fr.getStackSize() - 2) == BasicArrayInterpreter.THIS_VALUE) {
                            successorBlock.fieldsWritten.add(new Field(false, fin.owner, fin.name, fin.desc));
                        }
                    } else if(fin.getOpcode() == Opcodes.PUTSTATIC) {
                        successorBlock.fieldsWritten.add(new Field(true, fin.owner, fin.name, fin.desc));
                    }
                } else if(successorBlock.insn.getType() == AbstractInsnNode.METHOD_INSN) {
                    MethodInsnNode min = (MethodInsnNode) successorBlock.insn;
                    if(min.getOpcode() == INVOKEVIRTUAL || min.getOpcode() == INVOKESPECIAL) {
                        Type[] desc = Type.getArgumentTypes(min.desc);
                        if((desc.length == 1 && (Type.getReturnType(min.desc).getSort() == Type.VOID || min.desc.equals("Ljava/lang/StringBuilder;")))
                                || (desc.length == 2 && Type.getReturnType(min.desc).getSort() == Type.VOID && min.name.startsWith("set"))) {
                            Frame<BasicValue> fr = this.getFrames()[successor];
                            if(fr != null && fr.getStack(fr.getStackSize() - 2) instanceof BasicArrayInterpreter.BasicThisFieldValue) {
                                BasicArrayInterpreter.BasicThisFieldValue vv = (BasicArrayInterpreter.BasicThisFieldValue) fr.getStack(fr.getStackSize() - 2);
                                successorBlock.fieldsWritten.add(vv.getField());
                            }
                        }
                    }
                } else if(successorBlock.insn.getOpcode() == Opcodes.ATHROW) {
                    BasicValue ex = this.getFrames()[successor].getStack(0);
                    if(shouldTrackExceptions && ex != null && ex.getType() != null && (ex.getType().getDescriptor().contains("Exception") || ex.getType().getDescriptor().contains("Error"))) {
                        successorBlock.exceptionsThrown.add(ex.getType().getInternalName() + "#" + successor);
                    }
                }
            }
            fromBlock.successors.add(successorBlock);
            successorBlock.predecessors.add(fromBlock);
            if(fromBlock.isJump) {
                if(fromBlock.covered) {
                    successorBlock.onTrueSideOfJumpFrom.add(fromBlock);
                } else {
                    successorBlock.onFalseSideOfJumpFrom.add(fromBlock);
                    fromBlock.covered = true;
                }
            }
            super.newControlFlowEdge(insn, successor);
        }
    }

    private static class AnnotatedInstruction {
        AnnotatedInstruction handledAt;
        Set<AnnotatedInstruction> postDominators = new HashSet<>();
        int idx;
        Set<AnnotatedInstruction> basicBlockStartingSuccessors = new HashSet<>();
        Set<AnnotatedInstruction> successors = new HashSet<>();
        Set<AnnotatedInstruction> predecessors = new HashSet<>();
        AbstractInsnNode insn;
        boolean covered;
        boolean visited;
        boolean isTryBlockStart;
        boolean isJump;
        boolean isTwoOperandJumpInstruction;
        int ex_count;
        Set<String> exceptionsHandled = new HashSet<>();
        Set<String> coveredByTryBlockFor = new HashSet<>();
        Set<AnnotatedInstruction> handlerForRegionStartingAt = new HashSet<>();
        AnnotatedInstruction tryBlockEnd;
        Set<AnnotatedInstruction> resolvedHereBlocks = new HashSet<>();
        Set<AnnotatedInstruction> resolvedBlocks = new HashSet<>();
        Set<AnnotatedInstruction> onFalseSideOfJumpFrom = new HashSet<>();
        Set<AnnotatedInstruction> onTrueSideOfJumpFrom = new HashSet<>();
        Set<ForceControlStoreLocal> varsWritten = new HashSet<>();
        Set<Field> fieldsWritten = new HashSet<>();
        Set<String> exceptionsThrown = new HashSet<>();
        Set<String> exceptionsThrownTrueSide = new HashSet<>();
        Set<String> exceptionsThrownFalseSide = new HashSet<>();
        Set<ForceControlStoreLocal> varsWrittenTrueSide = new HashSet<>();
        Set<ForceControlStoreLocal> varsWrittenFalseSide = new HashSet<>();
        Set<Field> fieldsWrittenTrueSide = new HashSet<>();
        Set<Field> fieldsWrittenFalseSide = new HashSet<>();

        boolean isInteresting() {
            return isJump || isTryBlockStart || insn instanceof LabelNode;
        }

        @Override
        public String toString() {
            return "" + idx;
        }
    }
}
