package edu.columbia.cs.psl.jigsaw.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.PhosphorOption;
import edu.columbia.cs.psl.phosphor.org.apache.commons.cli.CommandLine;
import edu.columbia.cs.psl.phosphor.org.apache.commons.cli.Option;

import java.io.File;
import java.util.Properties;

public class StandaloneJVMInstrumenter {
    public static void main(String[] args) {
        long START = System.currentTimeMillis();
        CommandLine line = PhosphorOption.configure(false, args);
        if(line == null) {
            return;
        }
        String[] remainingArgs = line.getArgs();
        File jvmDir = new File(remainingArgs[0]);
        File instDir = new File(remainingArgs[1]);
        Properties properties = toProperties(line);
        JLinkInvoker.invokeJLink(jvmDir, instDir, properties);
    }

    private static Properties toProperties(CommandLine line){
        Properties ret = new Properties();
        for(Option opt : line.getOptions()){
            String name = opt.getOpt();
            String value = line.getOptionValue(name);
            if(value != null){
                ret.setProperty(name, value);
            }
        }
        return ret;
    }
}
