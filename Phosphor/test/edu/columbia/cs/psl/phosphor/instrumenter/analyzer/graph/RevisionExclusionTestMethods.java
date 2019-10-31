package edu.columbia.cs.psl.phosphor.instrumenter.analyzer.graph;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;

public class RevisionExclusionTestMethods {

    public static MethodNode getMethodNode(String methodName) throws NoSuchMethodException, IOException {
        ClassReader cr = new ClassReader(RevisionExclusionTestMethods.class.getName());
        ClassNode classNode = new ClassNode();
        cr.accept(classNode, 0);
        for(MethodNode mn : classNode.methods) {
            if(mn.name.equals(methodName)) {
                return mn;
            }
        }
        throw new NoSuchMethodException();
    }

    public static void allLocalAssignmentsConstant(boolean condition) {
        int a = 1;
        int b = a + 5;
        long c = b * a;
        c = c * c;
        int d;
        if(condition) {
            d = 9;
        } else {
            d = 9;
        }
        int e = d;
        int f = 6 * 7 + 88;
    }

    public static void allLocalAssignmentsConstant2(boolean condition) {
        int b = 0;
        int a = 77;
        if(condition) {
            b += 1;
        } else {
            b = 1;
        }
        if(condition) {
            a = a * b;
        }
    }

    public static void allLocalAssignmentsExcluded(boolean condition) {
        int c = 7;
        int v = 2;
        int w = 300;
        int x = -4;
        int y = 44;
        int z = 5;
        for(int i = 0; i < 55; i++) {
            for(int j = 0; j < 10; j++) {
                if(condition) {
                    v = (v * v + v + 5 * 77 - 99)/(2 * v);
                    w = c;
                    x = c + x;
                    y = y / c;
                    z = -z;
                }
            }
        }
    }

    public static void noLocalAssignmentsExcluded(int a, int b, int c) {
        int d = b + 7;
        int e = -a;
        int f = 6 + c;
    }

    public static void unableToMergeConstants(boolean condition) {
        int a = -88; // excluded  x = C
        int b;
        if(condition) {
            b = 77;  // excluded x = C
        } else {
            b = 144; // excluded x = C
        }
        b = b * a; // excluded x = x OP C
        int c = b; // not excluded
        int d = a + 77; // excluded x = C
        a = a + b; // not excluded
    }
}
