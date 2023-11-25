package edu.columbia.cs.psl.phosphor;


import edu.columbia.cs.psl.phosphor.runtime.NonModifiableClassException;
import edu.columbia.cs.psl.phosphor.runtime.PhosphorStackFrame;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;

import static edu.columbia.cs.psl.phosphor.PhosphorBaseTransformer.INITED;

/**
 * We use a premain in a separate jar for Java9+, in the phosphor-jigsaw-javaagent package
 */
public class Java8PreMain {
    static class PhosphorTransformerBridge implements ClassFileTransformer {
        private PhosphorBaseTransformer transformer;

        public PhosphorTransformerBridge(PhosphorBaseTransformer transformer) {
            this.transformer = transformer;
        }

        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
            return transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer, null);
        }

        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer, PhosphorStackFrame phosphorStackFrame) throws IllegalClassFormatException {
            try {
                if (!INITED) {
                    Configuration.init();
                    INITED = true;
                }
                return transformer.signalAndTransform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer, false);
            } catch (Throwable t) {
                t.printStackTrace();
                throw t;
            }
        }
    }

    public static void premain(String args, Instrumentation instr, PhosphorStackFrame phosphorStackFrame) {
        premain(args, instr);
    }

    public static void premain(String args, Instrumentation instr) {
        if (args == null) {
            args = "java8";
        } else {
            args += ",java8";
        }
        edu.columbia.cs.psl.phosphor.PreMain.premain(args, new InstrumentationHelper() {
            @Override
            public void addTransformer(final PhosphorBaseTransformer transformer) {
                instr.addTransformer(new PhosphorTransformerBridge(transformer));
            }

            @Override
            public Class<?>[] getAllLoadedClasses() {
                return instr.getAllLoadedClasses();
            }

            @Override
            public void addTransformer(PhosphorBaseTransformer transformer, boolean canRedefineClasses) {
                instr.addTransformer(new PhosphorTransformerBridge(transformer), canRedefineClasses);
            }

            @Override
            public void retransformClasses(Class<?> clazz) throws NonModifiableClassException {
                try {
                    instr.retransformClasses(clazz);
                } catch (UnmodifiableClassException e) {
                    throw new NonModifiableClassException(e);
                }
            }
        });
    }
}
