package edu.columbia.cs.psl.phosphor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

public class ClassHierarchyCreator extends ClassVisitor {
	
	public ClassHierarchyCreator() {
		super(Opcodes.ASM5);
	}

	// sub to list of super classes
	public static Map<String, Set<String>> map = new HashMap<String, Set<String>>();
			
	@Override
	public void visit(int version, int access, String name, String signature,
			String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);
		if(!map.containsKey(name))
			map.put(name, new HashSet<String>());
		map.get(name).add(superName);
		for(String inter: interfaces) 
			map.get(name).add(inter);
	}
	
	public static Set<String> allSupers(String className) {
		if(!map.containsKey(className))
			return new HashSet<String>();
		
		if(map.get(className).size() == 1 && map.get(className).iterator().next().equals("java/lang/Object"))
			return new HashSet<String>();
		
		Set<String> temp1 = new HashSet<String>();
		Set<String> temp2 = new HashSet<String>();
		Set<String> ret = new HashSet<String>();
		
		for(String s : map.get(className)) {
			if(!s.equals("java/lang/Object")) {
				temp1.add(s);
				ret.add(s);
			}
		}
		
		while(true) {
			for(String s : temp1) {
				if(map.containsKey(s) && !s.equals("java/lang/Object")) {
					Set<String> toAdd = map.get(s);
					for(String willAdd:  toAdd) {
						if(!willAdd.equals("java/lang/Object")) {
							temp2.add(willAdd);
							ret.add(willAdd);
						}
					}
				}
			}
			if(temp2.isEmpty())
				return ret;
			else {
				temp1 = temp2;
				temp2 = new HashSet<String>();
			}
		}
	}
}
