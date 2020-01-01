package edu.columbia.cs.psl.phosphor.instrumenter.analyzer;

import edu.columbia.cs.psl.phosphor.struct.harmony.util.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.util.Printer;

public class SinkableArrayValue extends BasicValue {

    public static final BasicValue NULL_VALUE = new BasicArrayValue(Type.getType("Lnull;"));

    public boolean flowsToInstMethodCall;
    public Set<SinkableArrayValue> reverseDeps;
    public Set<SinkableArrayValue> deps;
    public AbstractInsnNode oldSrc;
    public AbstractInsnNode sink;
    public boolean isNewArray;
    public SinkableArrayValue copyOf;
    public boolean doNotPropagateToDeps;
    public boolean flowsToPrim;
    public boolean okToPropagateToDeps;
    public List<SinkableArrayValue> otherDups = new LinkedList<>();
    public boolean isBottomDup;
    public SinkableArrayValue masterDup;
    public boolean isConstant;
    public SinkableArrayValue disabledFor;
    private AbstractInsnNode src;

    public SinkableArrayValue(Type type) {
        super(type);
    }

    public void disable() {
        if (src != null) {
            this.oldSrc = src;
            src = null;
        }
    }

    public int getLine() {
        AbstractInsnNode aa = getSrc();
        while(aa != null) {
            if(aa instanceof LineNumberNode) {
                return ((LineNumberNode) aa).line;
            }
            aa = aa.getPrevious();
        }
        return -1;
    }

    public void addDepCopy(SinkableArrayValue d) {
        if(d != null && deps == null) {
            deps = new HashSet<>();
        }
        if(deps.add(d) && d.isNewArray) {
            isNewArray = true;
        }
        copyOf = d;
        if(d.reverseDeps == null) {
            d.reverseDeps = new HashSet<>();
        }
        d.reverseDeps.add(this);
    }

    public void addDep(SinkableArrayValue d) {
        if(d != null && deps == null) {
            deps = new HashSet<>();
        }
        deps.add(d);
        if(d.reverseDeps == null) {
            d.reverseDeps = new HashSet<>();
        }
        d.reverseDeps.add(this);
    }

    public HashSet<SinkableArrayValue> getAllDepsFlat() {
        HashSet<SinkableArrayValue> ret = new HashSet<>();
        if(deps != null) {
            for(SinkableArrayValue r : deps) {
                if(ret.add(r)) {
                    r.getAllDepsFlat(ret);
                }
            }
        }
        return ret;
    }

    private void getAllDepsFlat(HashSet<SinkableArrayValue> ret) {
        if(deps != null) {
            for(SinkableArrayValue r : deps) {
                if(ret.add(r)) {
                    r.getAllDepsFlat(ret);
                }
            }
        }
    }

    public SinkableArrayValue getOriginalValue() {
        if(masterDup != null) {
            return masterDup.getOriginalValue();
        } else {
            if(isBottomDup) {
                for(SinkableArrayValue d : deps) {
                    if(d.getSrc().getOpcode() == Opcodes.NEWARRAY) {
                        return d;
                    }
                }
                throw new UnsupportedOperationException();
            }
            return this;
        }
    }

    public AbstractInsnNode getOriginalSource() {
        if(masterDup != null) {
            return masterDup.getOriginalSource();
        } else {
            if(isBottomDup) {
                for(SinkableArrayValue d : deps) {
                    if(d.getSrc().getOpcode() == Opcodes.NEWARRAY) {
                        return d.getSrc();
                    }
                    if(d.isBottomDup || d.masterDup != null) {
                        return d.getOriginalSource();
                    }
                }
                throw new UnsupportedOperationException();
            }
            return getSrc();
        }
    }

    @Override
    public String toString() {
        if(disabledFor != null) {
            return "[" + (flowsToInstMethodCall ? "T" : "F") + formatDesc() + ", disabled for: " + this.disabledFor + "]";
        }
        if(this == NULL_VALUE) {
            return "N";
        } else {
            return (flowsToInstMethodCall ? "T" : "F") + "<" + formatDesc() + "> " + (doNotPropagateToDeps ? "T" : "F") + (src != null && src.getOpcode() > 0 ? Printer.OPCODES[src.getOpcode()] : "????") + (src != null ? "@" + getLine() : "");
        }
    }

    private String formatDesc() {
        if(getType() == null) {
            return "N";
        } else if(this == UNINITIALIZED_VALUE) {
            return ".";
        } else if(this == RETURNADDRESS_VALUE) {
            return "A";
        } else if(this == REFERENCE_VALUE) {
            return "R";
        } else {
            return getType().getDescriptor();
        }
    }


    public Collection<SinkableArrayValue> tag(AbstractInsnNode sink) {

        LinkedList<SinkableArrayValue> queue = new LinkedList<>();
        queue.add(this);
        LinkedList<SinkableArrayValue> ret = new LinkedList<>();
        LinkedList<SinkableArrayValue> processed = new LinkedList<>();
        if (this.getType() != null && this.getType().getSort() == Type.ARRAY && this.getType().getDimensions() > 1) {
            return ret;
        }
        while(!queue.isEmpty()) {
            SinkableArrayValue v = queue.poll();

            while(v.disabledFor != null) {
                v = v.disabledFor;
            }
            processed.add(v);
            if(!v.flowsToInstMethodCall) {
                v.flowsToInstMethodCall = true;
                ret.add(v);
                v.sink = sink;
                if(v.deps != null && !v.doNotPropagateToDeps) {
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
    }

    public boolean deepEquals(SinkableArrayValue obj) {
        if(this == obj) {
            return true;
        }
        if(getType() == null && obj.getType() != null) {
            return false;
        }
        if(getType() != null && obj.getType() == null) {
            return false;
        }
        if(((getType() == null && obj.getType() == null) ||
                getType().equals(obj.getType()))) {
            if((src != null && obj.src == null) || (src == null && obj.src != null)) {
                return false;
            }
            if(doNotPropagateToDeps == obj.doNotPropagateToDeps && (src == null || src.equals(obj.getSrc()))) {
                if((deps != null && obj.deps == null) || (deps == null && obj.deps != null)) {
                    return false;
                }
                return deps == null || deps.equals(obj.deps);
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
