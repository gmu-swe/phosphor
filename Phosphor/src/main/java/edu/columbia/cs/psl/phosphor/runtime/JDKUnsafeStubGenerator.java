package edu.columbia.cs.psl.phosphor.runtime;


import org.objectweb.asm.Type;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;

public class JDKUnsafeStubGenerator {
    private static boolean isIntrinsicCandidate(Method m) {
        Annotation[] annotations = m.getAnnotations();
        for (Annotation an : annotations) {
            if (an.toString().contains("IntrinsicCandidate")) {
                return true;
            }
        }
        return false;
    }

    JDKUnsafeStubGenerator(Collection<Method> intrinsics) {
        this.initPatternMethods(intrinsics);
        this.loadTemplates();
    }

    private void loadTemplates() {
        InputStream is = JDKUnsafeStubGenerator.class.getClassLoader().getResourceAsStream("unsafe-patterns");
        Scanner s = new Scanner(is);
        StringBuilder currentTemplate = null;
        IntrinsicMethod currentMethod = null;
        while (s.hasNextLine()) {
            String line = s.nextLine();
            if (line.startsWith("//Template for: ")) {
                if (currentMethod != null) {
                    currentMethod.template = currentTemplate.toString();
                }
                currentTemplate = new StringBuilder();
                String methodKey = line.replace("//Template for: ", "");
                currentMethod = groupedMethods.get(methodKey);
                s.nextLine();
            } else if (currentTemplate != null) {
                currentTemplate.append(line);
                currentTemplate.append('\n');
            }

        }
        if (currentMethod != null) {
            currentMethod.template = currentTemplate.toString();
        }
        s.close();


    }

    private HashMap<String, IntrinsicMethod> groupedMethods = new HashMap<>();

    private void initPatternMethods(Collection<Method> intrinsics) {
        for (Method m : intrinsics) {
            TypePattern patternType = TypePattern.forMethod(m.getName());
            if (patternType != null) {
                String desc = patternType.generalizeDescriptor(m);
                String name = patternType.generalizeName(m);
                String key = name + desc;
                IntrinsicMethod im = groupedMethods.get(key);
                if (im == null) {
                    im = new IntrinsicMethod();
                    im.concreteExample = m;
                    im.genericName = name;
                    im.genericDesc = desc;
                    groupedMethods.put(key, im);
                }
                im.types.add(patternType);
            }
        }
        this.methodPatterns = new ArrayList<>(groupedMethods.values());
    }

    private ArrayList<IntrinsicMethod> methodPatterns = new ArrayList<>();

    private static enum TypePattern {
        REFERENCE("Reference", "Ljava/lang/Object;", "Object"),
        BOOLEAN("Boolean", "Z", "boolean"),
        BYTE("Byte", "B", "byte"),
        LONG("Long", "J", "long"),
        INT("Int", "I", "int"),
        SHORT("Short", "S", "short"),
        DOUBLE("Double", "D", "double"),
        CHAR("Char", "C", "char"),
        FLOAT("Float", "F", "float"),
        PATTERN("$methodType", "LPlaceHolder;", "$sourceType");

        String generalizeName(Method m) {
            return m.getName().replace(nameType, PATTERN.nameType);
        }

        String generalizeDescriptor(Method m) {
            Type[] args = Type.getArgumentTypes(m);
            Type returnType = Type.getReturnType(m);
            if (returnType.getDescriptor().equals(descType)) {
                returnType = Type.getType(PATTERN.descType);
            }
            for (int i = 2; i < args.length; i++) {
                if (args[i].getDescriptor().equals(descType)) {
                    args[i] = Type.getType(PATTERN.descType);
                }
            }
            return Type.getMethodDescriptor(returnType, args);
        }

        static TypePattern forMethod(String methodName) {
            for (TypePattern p : values()) {
                if (methodName.contains(p.nameType)) {
                    return p;
                }
            }
            return null;
        }

        private final String nameType;
        private final String descType;

        private final String sourceType;

        TypePattern(String nameType, String descType, String sourceType) {
            this.nameType = nameType;
            this.descType = descType;
            this.sourceType = sourceType;
        }
    }

    private static class IntrinsicMethod implements Comparable<IntrinsicMethod> {
        public String genericDesc;
        public String template;

        @Override
        public String toString() {
            return "IntrinsicMethod{" +
                    "genericDesc='" + genericDesc + '\'' +
                    ", genericName='" + genericName + '\'' +
                    ", types=" + types +
                    '}';
        }

        public String genericName;
        public HashSet<TypePattern> types = new HashSet<>();
        public Method concreteExample;

        public void printMethods() {
            if (this.template == null) {
                System.err.println("Missing template for " + genericName + genericDesc);
                return;
            }
            System.out.println("\t//Generated from Phosphor template for " + genericName + genericDesc);
            for (TypePattern type : types) {
                String method = this.template
                        .replace(TypePattern.PATTERN.nameType, type.nameType)
                        .replace(TypePattern.PATTERN.sourceType, type.sourceType)
                        .replace(TypePattern.PATTERN.descType, type.descType);
                System.out.print(method);
            }
            System.out.println();
        }

        @Override
        public int compareTo(IntrinsicMethod o) {
            return this.genericName.compareTo(o.genericDesc);
        }
    }

    public static void main(String[] args) throws ClassNotFoundException {
        Class c = Class.forName("jdk.internal.misc.Unsafe");
        StringBuilder intrinsicCandidates = new StringBuilder();
        HashMap<String, String> methodsByDesc = new HashMap<>();
        ArrayList<Method> intrinsics = new ArrayList<>();
        for (Method m : c.getDeclaredMethods()) {
            if (isIntrinsicCandidate(m) && m.getParameterTypes().length > 1 && m.getParameterTypes()[0] == Object.class) {
                intrinsics.add(m);
                String desc = Type.getMethodDescriptor(m);
                String descSwitch = methodsByDesc.get(desc);
                if (descSwitch == null) {
                    descSwitch = "";
                }
                descSwitch += "case \"" + m.getName() + "\":\n";
                methodsByDesc.put(desc, descSwitch);
            }
        }

        JDKUnsafeStubGenerator generator = new JDKUnsafeStubGenerator(intrinsics);
        //generator.printMissingTemplates();
        Collections.sort(generator.methodPatterns);
        for (IntrinsicMethod method : generator.methodPatterns) {
            method.printMethods();
        }


        //System.out.println(generator.methodPatterns);
        //System.out.println(generator.methodPatterns.size());
        //Print out switch statement to determien what is an intrinsic to wrap
        //System.out.println("switch(desc){");
        //for(String desc : methodsByDesc.keySet()){
        //    System.out.println("case \""+desc+"\":\n\tswitch(name){");
        //    System.out.println(methodsByDesc.get(desc) + "return true;}");
        //}
        //System.out.println("}\nreturn false;");
    }

    private void printMissingTemplates() {
        System.out.println("//Do not change the comments around the templates");
        for (IntrinsicMethod im : methodPatterns) {
            if (im.template != null) {
                continue;
            }
            System.out.println("//Template for: " + im.genericName + im.genericDesc);
            System.out.println("//Will generate: " + im.types.stream().map(t -> im.genericName.replace(TypePattern.PATTERN.nameType, t.nameType)).collect(Collectors.toList()));
            String retString = Type.getReturnType(im.genericDesc).getDescriptor();
            if (retString.equals(TypePattern.PATTERN.descType)) {
                retString = TypePattern.PATTERN.sourceType;
            } else {
                retString = im.concreteExample.getReturnType().getName();
            }
            System.out.print("public static " + retString + " " + im.genericName + "(UnsafeProxy unsafe, ");
            Type[] argTypes = Type.getArgumentTypes(im.genericDesc);
            for (int i = 0; i < argTypes.length; i++) {
                Parameter p = im.concreteExample.getParameters()[i];
                String argTypeToPrint = p.getType().getName();
                if (argTypes[i].getDescriptor().equals(TypePattern.PATTERN.descType)) {
                    argTypeToPrint = TypePattern.PATTERN.sourceType;
                }
                System.out.print(argTypeToPrint + " " + p.getName() + ", ");
            }
            System.out.println("PhosphorStackFrame phosphorStackFrame){");
            System.out.println("}");
        }
    }
}
