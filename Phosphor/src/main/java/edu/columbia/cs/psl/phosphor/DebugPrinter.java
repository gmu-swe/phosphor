package edu.columbia.cs.psl.phosphor;

import edu.columbia.cs.psl.phosphor.control.ControlFlowAnalyzer;
import edu.columbia.cs.psl.phosphor.control.ControlFlowManager;
import edu.columbia.cs.psl.phosphor.control.standard.StandardControlFlowManager;
import edu.columbia.cs.psl.phosphor.instrumenter.PrimitiveArrayAnalyzer;
import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.NeverNullArgAnalyzerAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.PrintWriter;

public class DebugPrinter {

    private DebugPrinter() {
        // Prevents this class from being instantiated
    }

    public static void main(String[] args) throws Exception {
        // Configuration.IMPLICIT_TRACKING = true;
        File clazz = new File(args[0]);
        final ClassReader cr1 = new ClassReader(new FileInputStream(clazz));
        PrintWriter pw = new PrintWriter(new FileWriter("z.txt"));
        ClassWriter cw1 = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {
            @Override
            protected String getCommonSuperClass(String type1, String type2) {
                try {
                    return super.getCommonSuperClass(type1, type2);
                } catch(Exception ex) {
                    // System.err.println("err btwn " + type1 + " " +type2);
                    return "java/lang/Unknown";
                }
            }
        };
        cr1.accept(new ClassVisitor(Configuration.ASM_VERSION, cw1) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                return new JSRInlinerAdapter(super.visitMethod(access, name, desc, signature, exceptions), access, name, desc, signature, exceptions);
            }
        }, ClassReader.EXPAND_FRAMES);
        final ClassReader cr = new ClassReader(cw1.toByteArray());

        TraceClassVisitor tcv = new TraceClassVisitor(new ClassVisitor(Configuration.ASM_VERSION, new ClassWriter(0)) {
            String className;

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                super.visit(version, access, name, signature, superName, interfaces);
                this.className = name;
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                ControlFlowManager controlManager = new StandardControlFlowManager();
                ControlFlowAnalyzer flowAnalyzer = controlManager.createPropagationPolicy(access, cr.getClassName(), name, desc).getFlowAnalyzer();
                mv = new PrimitiveArrayAnalyzer(cr.getClassName(), access, name, desc, signature, exceptions, mv, false, false, flowAnalyzer);
                NeverNullArgAnalyzerAdapter an = new NeverNullArgAnalyzerAdapter(cr.getClassName(), access, name, desc, mv);
                ((PrimitiveArrayAnalyzer) mv).setAnalyzer(an);
                mv = an;
                return mv;
            }
        }, pw);
        cr.accept(tcv, ClassReader.EXPAND_FRAMES);
        pw.flush();
    }
}
