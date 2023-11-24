package edu.gmu.swe.phosphor.jlink;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.OptionsUtil;
import edu.columbia.cs.psl.phosphor.PhosphorOption;
import edu.columbia.cs.psl.phosphor.PreMain;
import edu.columbia.cs.psl.phosphor.instrumenter.TaintTrackingClassVisitor;

import java.io.File;
import java.util.Properties;

public class PhosphorInstrumentation implements Instrumentation {
    private PreMain.PCLoggingTransformer transformer;

    @Override
    public void configure(File inputDirectory, File outputDirectory, Properties options) {
        String[] arguments = OptionsUtil.createPhosphorMainArguments(inputDirectory, outputDirectory, options);
        PhosphorOption.configure(false, arguments);
        Configuration.init();
        TaintTrackingClassVisitor.IS_RUNTIME_INST = false;
        transformer = new PreMain.PCLoggingTransformer();
    }

    @Override
    public File[] getClassPathElements() {
        return new File[] {InstrumentUtil.getClassPathElement(PreMain.class)};
    }

    @Override
    public byte[] apply(byte[] classFileBuffer) {
        transformer.transform(null, null, null, null,
                classFileBuffer, false);
        return null;
    }
}
