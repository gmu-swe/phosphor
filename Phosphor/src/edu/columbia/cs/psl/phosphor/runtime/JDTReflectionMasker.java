package edu.columbia.cs.psl.phosphor.runtime;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.jdt.internal.compiler.api.env.IBinaryMethod;

import edu.columbia.cs.psl.phosphor.TaintUtils;

public class JDTReflectionMasker {
	public static IBinaryMethod[] getMethods(IBinaryMethod[] methods)
	{
//		if(methods == null)
//			return null;
//		System.out.println("RBM " + Arrays.toString(methods));
//		ArrayList<IBinaryMethod> ret = new ArrayList<IBinaryMethod>(methods.length);
//		for(IBinaryMethod m : methods)
//		{
//			String s = new String(m.getMethodDescriptor());
//			if(s.endsWith(TaintUtils.METHOD_SUFFIX) || s.endsWith(TaintUtils.METHOD_SUFFIX_UNINST))
//				continue;
//			ret.add(m);
//		}
//		IBinaryMethod[] r = new IBinaryMethod[ret.size()];
//		r = ret.toArray(r);
//		return r;
		throw new UnsupportedOperationException();
	}
}
