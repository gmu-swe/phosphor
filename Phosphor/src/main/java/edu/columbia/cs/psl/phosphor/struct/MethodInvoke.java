package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.Taint;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class MethodInvoke {
    public Constructor c;
    public Taint c_taint;
    public Method m;
    public Taint m_taint;
    public Object o;
    public Taint o_taint;
    public LazyReferenceArrayObjTags a;
    public Taint a_taint;
    public TaintedReferenceWithObjTag prealloc;
}
