package edu.columbia.cs.psl.phosphor.instrumenter.analyzer;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.util.Printer;

public class SinkableArrayValue extends BasicValue {
	public static final BasicValue NULL_VALUE = new BasicArrayValue(Type.getType("Lnull;"));

	public boolean flowsToInstMethodCall;
	public HashSet<SinkableArrayValue> reverseDeps;
	public HashSet<SinkableArrayValue> deps;
	private AbstractInsnNode src;
	public AbstractInsnNode oldSrc;
	public AbstractInsnNode sink;
	public boolean isNewArray;
	
	public SinkableArrayValue copyOf;
	public boolean dontPropogateToDeps;
	public boolean flowsToPrim;
	public boolean okToPropogateToDeps;
	
	public boolean[] leaveDupOnAt = new boolean[4];
	
	public LinkedList<SinkableArrayValue> otherDups = new LinkedList<SinkableArrayValue>();
	public boolean isBottomDup;
	public boolean resolved;

	public SinkableArrayValue masterDup;

	public boolean isConstant;

	public SinkableArrayValue disabledFor;
	public void disable()
	{
		if(src != null) {
			this.oldSrc = src;
			src = null;
		}
	}
	public int getLine(){
		AbstractInsnNode aa = getSrc();
		while (aa != null)
		{
			if(aa instanceof LineNumberNode)
				return ((LineNumberNode)aa).line;
			aa = aa.getPrevious();
		}
		return -1;
	}
	public void addDepCopy(SinkableArrayValue d) {
		if (d != null && deps == null)
			deps = new HashSet<SinkableArrayValue>();
		if(deps.add(d) && d.isNewArray)
			isNewArray = true;
		copyOf = d;
		if(d.reverseDeps == null)
			d.reverseDeps = new HashSet<SinkableArrayValue>();
		d.reverseDeps.add(this);
	}
	
	public void addDep(SinkableArrayValue d) {
		if (d != null && deps == null)
			deps = new HashSet<SinkableArrayValue>();
		deps.add(d);
		if(d.reverseDeps == null)
			d.reverseDeps = new HashSet<SinkableArrayValue>();
		d.reverseDeps.add(this);
	}
	
	public HashSet<SinkableArrayValue> getAllDepsFlat()
	{
		HashSet<SinkableArrayValue> ret = new HashSet<SinkableArrayValue>();
		if(deps != null)
		for(SinkableArrayValue r : deps)
		{
			if(ret.add(r))
				r.getAllDepsFlat(ret);
		}
		return ret;
	}
	private void getAllDepsFlat(HashSet<SinkableArrayValue> ret)
	{
		if(deps != null)
		for(SinkableArrayValue r : deps)
		{
			if(ret.add(r))
				r.getAllDepsFlat(ret);
		}
	}

	public SinkableArrayValue getOriginalValue()
	{
		if(masterDup != null)
			return masterDup.getOriginalValue();
		else
		{
			if(isBottomDup)
			{
				for(SinkableArrayValue d : deps)
				{
					if(d.getSrc().getOpcode() == Opcodes.NEWARRAY)
						return d;
				}
				throw new UnsupportedOperationException();
			}
			return this;
		}
	}
	public AbstractInsnNode getOriginalSource()
	{
		if(masterDup != null)
			return masterDup.getOriginalSource();
		else
		{
			if(isBottomDup)
			{
				for(SinkableArrayValue d : deps)
				{
					if(d.getSrc().getOpcode() == Opcodes.NEWARRAY)
						return d.getSrc();
					if(d.isBottomDup || d.masterDup != null)
						return d.getOriginalSource();
				}
				throw new UnsupportedOperationException();
			}
			return getSrc();
		}
	}
	public SinkableArrayValue(Type type) {
		super(type);
//		if(type == null || type.getSort()==Type.OBJECT)
//			throw new IllegalStateException();
	}

	@Override
	public String toString() {
		if(disabledFor != null)
		{
			return "["+(flowsToInstMethodCall ? "T":"F")+ formatDesc()+", disabled for: " + this.disabledFor+"]";
		}
		if (this == NULL_VALUE)
			return "N";
		else
			return (flowsToInstMethodCall ? "T" : "F") + "<"+ formatDesc() + "> "+ (dontPropogateToDeps ? "T" : "F")+ (src != null && src.getOpcode() > 0 ? Printer.OPCODES[src.getOpcode()] : "????") + (src != null ? "@"+getLine():"");
	}

	private String formatDesc() {
		if (getType() == null)
			return "N";
		else if (this == UNINITIALIZED_VALUE) {
			return ".";
		} else if (this == RETURNADDRESS_VALUE) {
			return "A";
		} else if (this == REFERENCE_VALUE) {
			return "R";
		} else {
			return getType().getDescriptor();
		}
	}

	
	public Collection<SinkableArrayValue> tag(AbstractInsnNode sink) {

		LinkedList<SinkableArrayValue> queue = new LinkedList<SinkableArrayValue>();
		queue.add(this);
		LinkedList<SinkableArrayValue> ret = new LinkedList<SinkableArrayValue>();
		LinkedList<SinkableArrayValue> processed = new LinkedList<>();
		while(!queue.isEmpty())
		{
			SinkableArrayValue v = queue.pop();

			while(v.disabledFor != null) //this is where our problem is
			{
				v = v.disabledFor;
			}
			processed.add(v);
			if (!v.flowsToInstMethodCall) {
				v.flowsToInstMethodCall = true;
				ret.add(v);
				v.sink = sink;
//				if(src != null)
//				System.out.println("TAG " + this + " " + Printer.OPCODES[src.getOpcode()] + (src.getOpcode() == Opcodes.ALOAD ? ((VarInsnNode)src).var : ""));
				if (v.deps != null && !v.dontPropogateToDeps)
				{
					queue.addAll(v.deps);
				}
				
			}
		}
		return ret;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (flowsToInstMethodCall ? 1231 : 1237);
		result = prime * result + ((getSrc() == null) ? 0 : getSrc().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj;
		// if (this == obj)
		// return true;
		// if (!super.equals(obj))
		// return false;
		// if (getClass() != obj.getClass())
		// return false;
		// SinkableArrayValue other = (SinkableArrayValue) obj;
		// if (deps == null) {
		// if (other.deps != null)
		// return false;
		// } else if (!deps.equals(other.deps))
		// return false;
		// if (flowsToInstMethodCall != other.flowsToInstMethodCall)
		// return false;
		// if (src == null) {
		// if (other.src != null)
		// return false;
		// } else if (!src.equals(other.src))
		// return false;
		// return true;
	}

	public boolean deepEquals(SinkableArrayValue obj)
	{
		if(this == obj)
			return true;
		if(getType() == null && obj.getType() != null)
			return false;
		if(getType() != null && obj.getType() == null)
			return false;
		if(((getType() == null && obj.getType() == null) ||
				getType().equals(obj.getType())))
		{
			if((src != null && obj.src == null) || (src == null && obj.src != null))
				return false;
			if (dontPropogateToDeps == obj.dontPropogateToDeps && ((src == null && obj.src == null) || src.equals(obj.getSrc()))) {
				if((deps != null && obj.deps == null) || (deps == null && obj.deps != null))
					return false;
				if((deps == null && obj.deps == null) || deps.equals(obj.deps))
				{
					return true;
				}
			}
		}
		return false;
	}
	public AbstractInsnNode getSrc() {
		return src;
	}

	public void setSrc(AbstractInsnNode src) {

		this.src = src;
	}

}
