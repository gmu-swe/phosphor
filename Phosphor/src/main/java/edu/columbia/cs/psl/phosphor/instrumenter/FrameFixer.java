package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.HackyClassWriter;
import edu.columbia.cs.psl.phosphor.instrumenter.asm.OffsetPreservingClassReader;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.commons.OurJSRInlinerAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.*;

public class FrameFixer {

    public static ClassReader fix(ClassReader cr) {
        ClassWriter cw = new HackyClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cr.accept(new ClassVisitor(Configuration.ASM_VERSION, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                                             String[] exceptions) {
                return new OurJSRInlinerAdapter(super.visitMethod(access, name, desc, signature, exceptions),
                        access, name, desc, signature, exceptions);
            }
        }, 0);
        return (Configuration.READ_AND_SAVE_BCI ? new OffsetPreservingClassReader(cw.toByteArray()) :
                new ClassReader(cw.toByteArray()));
    }

    public static boolean shouldFixFrames(ClassNode cn, String className, ClassReader cr) {
        if (cn.version >= 100 || cn.version <= 50 || className.endsWith("$Access4JacksonSerializer")
                || className.endsWith("$Access4JacksonDeSerializer")) {
            return true;
        } else if (Configuration.ALWAYS_CHECK_FOR_FRAMES) {
            cn = new ClassNode();
            cr.accept(cn, 0);
            for (MethodNode mn : cn.methods) {
                if (hasFrames(mn)) {
                    return false;
                } else if (hasJumps(mn)) {
                    // The method has at least one jump but no frames
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasJumps(MethodNode mn) {
        if (!mn.tryCatchBlocks.isEmpty()) {
            return true;
        }
        AbstractInsnNode insn = mn.instructions.getFirst();
        while (insn != null) {
            if (insn instanceof JumpInsnNode
                    || insn instanceof TableSwitchInsnNode
                    || insn instanceof LookupSwitchInsnNode) {
                return true;
            }
            insn = insn.getNext();
        }
        return false;
    }

    private static boolean hasFrames(MethodNode mn) {
        AbstractInsnNode insn = mn.instructions.getFirst();
        while (insn != null) {
            if (insn instanceof FrameNode) {
                return true;
            }
            insn = insn.getNext();
        }
        return false;
    }
}