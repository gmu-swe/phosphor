package edu.columbia.cs.psl.phosphor.agent;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.InstrumentationAdaptor;
import edu.columbia.cs.psl.phosphor.Phosphor;
import edu.columbia.cs.psl.phosphor.PhosphorBaseTransformer;
import edu.columbia.cs.psl.phosphor.runtime.NonModifiableClassException;
import edu.columbia.cs.psl.phosphor.runtime.PhosphorStackFrame;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;

import static edu.columbia.cs.psl.phosphor.PhosphorBaseTransformer.INITED;

public final class PhosphorAgent {
    private PhosphorAgent() {
        throw new AssertionError("Tried to instantiate static agent class: " + getClass());
    }

    @SuppressWarnings("unused")
    public static void premain(String agentArgs, Instrumentation inst, PhosphorStackFrame frame) {
        premain(agentArgs, inst);
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        InstrumentationAdaptor adaptor = new InstrumentationAdaptor() {
            private final Instrumentation delegate = inst;

            @Override
            public void addTransformer(final PhosphorBaseTransformer transformer) {
                delegate.addTransformer(new PhosphorTransformerBridge(transformer));
            }

            @Override
            public Class<?>[] getAllLoadedClasses() {
                return delegate.getAllLoadedClasses();
            }

            @Override
            public void addTransformer(PhosphorBaseTransformer transformer, boolean canRedefineClasses) {
                delegate.addTransformer(new PhosphorTransformerBridge(transformer), canRedefineClasses);
            }

            @Override
            public void retransformClasses(Class<?> clazz) throws NonModifiableClassException {
                try {
                    delegate.retransformClasses(clazz);
                } catch (UnmodifiableClassException e) {
                    throw new NonModifiableClassException(e);
                }
            }
        };
        Phosphor.initialize(agentArgs, adaptor);
    }

    private static final class PhosphorTransformerBridge implements ClassFileTransformer {
        private final PhosphorBaseTransformer transformer;

        public PhosphorTransformerBridge(PhosphorBaseTransformer transformer) {
            this.transformer = transformer;
        }

        @SuppressWarnings("unused")
        public byte[] transform(
                ClassLoader loader,
                String className,
                Class<?> classBeingRedefined,
                ProtectionDomain protectionDomain,
                byte[] classfileBuffer,
                PhosphorStackFrame frame) {
            return transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
        }

        @Override
        public byte[] transform(
                ClassLoader loader,
                String className,
                Class<?> classBeingRedefined,
                ProtectionDomain protectionDomain,
                byte[] classfileBuffer) {
            try {
                if (!INITED) {
                    Configuration.init();
                    INITED = true;
                }
                return transformer.signalAndTransform(
                        loader, className, classBeingRedefined, protectionDomain, classfileBuffer, false);
            } catch (Throwable t) {
                //noinspection CallToPrintStackTrace
                t.printStackTrace();
                throw t;
            }
        }
    }
}
