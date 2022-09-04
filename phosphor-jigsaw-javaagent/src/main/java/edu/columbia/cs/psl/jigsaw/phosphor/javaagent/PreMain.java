package edu.columbia.cs.psl.jigsaw.phosphor.javaagent;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.InstrumentationHelper;
import edu.columbia.cs.psl.phosphor.PhosphorBaseTransformer;
import edu.columbia.cs.psl.phosphor.runtime.NonModifiableClassException;
import edu.columbia.cs.psl.phosphor.runtime.PhosphorStackFrame;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;

import static edu.columbia.cs.psl.phosphor.PhosphorBaseTransformer.INITED;

public class PreMain {
    static class PhosphorTransformerBridge implements ClassFileTransformer {
        private PhosphorBaseTransformer transformer;

        public PhosphorTransformerBridge(PhosphorBaseTransformer transformer) {
            this.transformer = transformer;
        }

        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer, PhosphorStackFrame stackFrame) throws IllegalClassFormatException {
            return transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
        }

        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
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

    public static void premain(String args, final Instrumentation instr, final PhosphorStackFrame stackFrame) {
        premain(args, instr);
    }

    public static void premain(String args, final Instrumentation instr) {
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
