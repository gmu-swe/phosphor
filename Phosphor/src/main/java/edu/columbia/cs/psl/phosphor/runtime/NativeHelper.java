package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.control.ControlFlowStack;
import edu.columbia.cs.psl.phosphor.struct.*;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArrayWithObjTag;
import org.objectweb.asm.Type;

import java.util.Collection;

public final class NativeHelper {

    private NativeHelper() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static TaintedBooleanWithObjTag equals$$PHOSPHORTAGGED(Object o1, Taint<?> t1, Object o2, Taint<?> t2, TaintedBooleanWithObjTag ret) {
        if(o1 instanceof TaintedObjectWithObjTag) {
            return ((TaintedObjectWithObjTag) o1).equals$$PHOSPHORTAGGED(t1, o2, t2, ret);
        } else if(o1 instanceof LazyArrayObjTags) {
            return ((LazyArrayObjTags) o1).equals$$PHOSPHORTAGGED(t1, o2, t2, ret);
        } else {
            if(o2 instanceof MultiDTaintedArrayWithObjTag) {
                o2 = MultiDTaintedArray.unboxRaw(o2);
            }
            ret.val = o1.equals(o2);
            ret.taint = Taint.emptyTaint();
            return ret;
        }
    }

    public static TaintedIntWithObjTag hashCode$$PHOSPHORTAGGED(Object o, Taint<?> t, TaintedIntWithObjTag ret) {
        if(o instanceof TaintedObjectWithObjTag) {
            return ((TaintedObjectWithObjTag) o).hashCode$$PHOSPHORTAGGED(t, ret);
        } else if(o instanceof LazyArrayObjTags) {
            return ((LazyArrayObjTags) o).hashCode$$PHOSPHORTAGGED(t, ret);
        } else {
            ret.val = o.hashCode();
            ret.taint = t;
            return ret;
        }
    }

    public static <T> TaintedBooleanWithObjTag equals$$PHOSPHORTAGGED(Object o1, Taint<T> t1, Object o2, Taint<T> t2,
                                                                      ControlFlowStack ctrl, TaintedBooleanWithObjTag ret) {
        if(o1 instanceof TaintedObjectWithObjTag) {
            return ((TaintedObjectWithObjTag) o1).equals$$PHOSPHORTAGGED(t1, o2, t2, ctrl, ret);
        } else if(o1 instanceof LazyArrayObjTags) {
            return ((LazyArrayObjTags) o1).equals$$PHOSPHORTAGGED(t1, o2, t2, ret, ctrl);
        } else {
            if(o2 instanceof MultiDTaintedArrayWithObjTag) {
                o2 = MultiDTaintedArray.unboxRaw(o2);
            }
            ret.val = o1.equals(o2);
            ret.taint = Taint.emptyTaint();
            return ret;
        }
    }

    public static TaintedIntWithObjTag hashCode$$PHOSPHORTAGGED(Object o, Taint<?> t, ControlFlowStack ctrl, TaintedIntWithObjTag ret) {
        if(o instanceof TaintedObjectWithObjTag) {
            return ((TaintedObjectWithObjTag) o).hashCode$$PHOSPHORTAGGED(t, ctrl, ret);
        } else if(o instanceof LazyArrayObjTags) {
            return ((LazyArrayObjTags) o).hashCode$$PHOSPHORTAGGED(t, ret, ctrl);
        } else {
            ret.val = o.hashCode();
            ret.taint = t;
            return ret;
        }
    }

    public static Class<?> getClassOrWrapped(Object in) {
        if(in instanceof LazyReferenceArrayObjTags) {
            return ((LazyReferenceArrayObjTags) in).getUnderlyingClass();
        }
        if(in instanceof LazyArrayObjTags) {
            return ((LazyArrayObjTags) in).getVal().getClass();
        }
        return in.getClass();
    }

    @SuppressWarnings("rawtypes")
    public static Collection ensureIsBoxedImplicitTracking(Collection in) {
        if(in != null) {
            Collection tmp = null;
            for(Object o : in) {
                if(o == null) {
                    break;
                }
                Type t = Type.getType(o.getClass());
                if(t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT) {
                    if(tmp == null) {
                        try {
                            tmp = in.getClass().getConstructor().newInstance(null);
                        } catch(Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                    tmp.add$$PHOSPHORTAGGED(MultiDTaintedArrayWithObjTag.boxIfNecessary(o), Configuration.controlFlowManager.getStack(false), new TaintedBooleanWithObjTag());
                } else {
                    break;
                }
            }
            if(tmp != null) {
                in.clear();
                tmp.add$$PHOSPHORTAGGED(tmp, Configuration.controlFlowManager.getStack(false), new TaintedBooleanWithObjTag());
            }
        }
        return in;
    }

    @SuppressWarnings("rawtypes")
    public static Collection ensureIsBoxedObjTags(Collection in) {
        if(in != null) {
            Collection tmp = null;
            for(Object o : in) {
                if(o == null) {
                    break;
                }
                Type t = Type.getType(o.getClass());
                if(t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT) {
                    if(tmp == null) {
                        try {
                            tmp = in.getClass().getConstructor().newInstance(null);
                        } catch(Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                    tmp.add$$PHOSPHORTAGGED(MultiDTaintedArrayWithObjTag.boxIfNecessary(o), new TaintedBooleanWithObjTag());
                } else {
                    break;
                }
            }
            if(tmp != null) {
                in.clear();
                tmp.add$$PHOSPHORTAGGED(tmp, new TaintedBooleanWithObjTag());
            }
        }
        return in;
    }

    public static Collection ensureIsUnBoxedObjTags(Collection in) {
        if(in != null) {
            Collection tmp = null;
            for(Object o : in) {
                if(o != null && MultiDTaintedArrayWithObjTag.isPrimitiveBoxClass(o.getClass()) != null) {
                    if(tmp == null) {
                        try {
                            tmp = in.getClass().getConstructor().newInstance(null);
                        } catch(Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                    tmp.add$$PHOSPHORTAGGED(MultiDTaintedArrayWithObjTag.unboxRaw(o), new TaintedBooleanWithObjTag());

                } else {
                    break;
                }
            }
            if(tmp != null) {
                in.clear();
                tmp.add$$PHOSPHORTAGGED(tmp, new TaintedBooleanWithObjTag());
            }
        }
        return in;
    }

    public static Collection ensureIsUnBoxedImplicitTracking(Collection in) {
        if(in != null) {
            Collection tmp = null;
            for(Object o : in) {
                if(o != null && MultiDTaintedArrayWithObjTag.isPrimitiveBoxClass(o.getClass()) != null) {
                    if(tmp == null) {
                        try {
                            tmp = in.getClass().getConstructor().newInstance(null);
                        } catch(Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                    tmp.add$$PHOSPHORTAGGED(MultiDTaintedArrayWithObjTag.unboxRaw(o), Configuration.controlFlowManager.getStack(false), new TaintedBooleanWithObjTag());

                } else {
                    break;
                }
            }
            if(tmp != null) {
                in.clear();
                tmp.add$$PHOSPHORTAGGED(tmp, Configuration.controlFlowManager.getStack(false), new TaintedBooleanWithObjTag());
            }
        }
        return in;
    }
}
