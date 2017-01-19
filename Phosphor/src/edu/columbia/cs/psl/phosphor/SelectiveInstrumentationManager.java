package edu.columbia.cs.psl.phosphor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

public class SelectiveInstrumentationManager {
	
	public static boolean inited = false;
	public static Set<MethodDescriptor> methodsToInstrument = new HashSet<MethodDescriptor>();
	
//	public static HashMap<String, HashSet<String>> methodsToInstrumentByClass = new HashMap<String, HashSet<String>>();
	public static void populateMethodsToInstrument(String file) {
		if(inited)
			return;
		inited = true;
		FileInputStream fis = null;
		BufferedReader br = null;
		try {
			fis = new FileInputStream(new File(file));
			br = new BufferedReader(new InputStreamReader(fis));
		 
			String line = null;
			while ((line = br.readLine()) != null)
				if(line.length() > 0) {
					MethodDescriptor desc = TaintUtils.getMethodDesc(line);
					methodsToInstrument.add(desc);
//					if(!methodsToInstrumentByClass.containsKey(desc.getOwner()))
//						methodsToInstrumentByClass.put(desc.getOwner(), new HashSet<String>());
//					methodsToInstrumentByClass.get(desc.getOwner()).add(desc.getName()+desc.getDesc());
				}
			
		} catch(IOException ex) {
			ex.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				fis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args) {
		String s = "<java.awt.geom.PathIterator: int currentSegment(float[])>";
		MethodDescriptor desc = TaintUtils.getMethodDesc(s);
		System.out.println(s);
		System.out.println(TaintUtils.getMethodDesc(desc));
		
	}
}
