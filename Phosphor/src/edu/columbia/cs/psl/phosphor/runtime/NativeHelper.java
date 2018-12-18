package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.struct.*;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArray;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArrayWithObjTag;
import org.objectweb.asm.Type;

import java.util.Collection;

public final class NativeHelper {

	public static final TaintedBooleanWithObjTag equals$$PHOSPHORTAGGED(Object o1, Object o2, TaintedBooleanWithObjTag ret) {
		if (o1 instanceof TaintedObjectWithObjTag)
			return ((TaintedObjectWithObjTag) o1).equals$$PHOSPHORTAGGED(o2, ret);
		else {
			if(o2 instanceof MultiDTaintedArrayWithObjTag)
				o2 = MultiDTaintedArray.unboxRaw(o2);
			ret.val = o1.equals(o2);
			ret.taint = null;
			return ret;
		}
	}

	public static final TaintedIntWithObjTag hashCode$$PHOSPHORTAGGED(Object o, TaintedIntWithObjTag ret) {
		if (o instanceof TaintedObjectWithObjTag)
			return ((TaintedObjectWithObjTag) o).hashCode$$PHOSPHORTAGGED(ret);
		else {
			ret.val = o.hashCode();
			ret.taint = null;
			return ret;
		}
	}

	public static final TaintedIntWithObjTag hashCode$$PHOSPHORTAGGED(Object o, ControlTaintTagStack ctrl, TaintedIntWithObjTag ret) {
		ret.val = o.hashCode();
		ret.taint = null;
		return ret;
	}


	public static final TaintedBooleanWithObjTag equals$$PHOSPHORTAGGED(Object o1, Object o2, ControlTaintTagStack ctrl, TaintedBooleanWithObjTag ret) {
		if (o1 instanceof TaintedObjectWithObjCtrlTag)
			return ((TaintedObjectWithObjCtrlTag) o1).equals$$PHOSPHORTAGGED(o2, ctrl, ret);
		else {
			if (o2 instanceof MultiDTaintedArrayWithObjTag)
				o2 = MultiDTaintedArray.unboxRaw(o2);
			ret.val = o1.equals(o2);
			ret.taint = null;
			return ret;
		}
	}

	@SuppressWarnings("rawtypes")
	public static final Collection ensureIsBoxedImplicitTracking(Collection in) {
		if (in != null) {
			Collection tmp = null;
			for (Object o : in) {
				if (o == null)
					break;
				Type t = Type.getType(o.getClass());
				if (t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT) {
					if (tmp == null) {
						try {
							tmp = (Collection) in.getClass().getConstructor().newInstance(null);
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
					tmp.add$$PHOSPHORTAGGED(MultiDTaintedArrayWithObjTag.boxIfNecessary(o), new ControlTaintTagStack(), new TaintedBooleanWithObjTag());
				} else
					break;
			}
			if (tmp != null) {
				in.clear();
				tmp.add$$PHOSPHORTAGGED(tmp, new ControlTaintTagStack(), new TaintedBooleanWithObjTag());
			}
		}
		return in;
	}

	@SuppressWarnings("rawtypes")
	public static final Collection ensureIsBoxedObjTags(Collection in) {
		if (in != null) {
			Collection tmp = null;
			for (Object o : in) {
				if (o == null)
					break;
				Type t = Type.getType(o.getClass());
				if (t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT) {
					if (tmp == null) {
						try {
							tmp = (Collection) in.getClass().getConstructor().newInstance(null);
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
					tmp.add$$PHOSPHORTAGGED(MultiDTaintedArrayWithObjTag.boxIfNecessary(o), new TaintedBooleanWithObjTag());
				} else
					break;
			}
			if (tmp != null) {
				in.clear();
				tmp.add$$PHOSPHORTAGGED(tmp, new TaintedBooleanWithObjTag());
			}
		}
		return in;
	}

	@SuppressWarnings("rawtypes")
	public static final Collection ensureIsBoxed(Collection in) {
		if (in != null) {
			Collection tmp = null;
			for (Object o : in) {
				if (o == null)
					break;
				Type t = Type.getType(o.getClass());
				if (t.getSort() == Type.ARRAY && t.getElementType().getSort() != Type.OBJECT) {
					if (tmp == null) {
						try {
							tmp = (Collection) in.getClass().getConstructor().newInstance(null);
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
					tmp.add$$PHOSPHORTAGGED(MultiDTaintedArrayWithIntTag.boxIfNecessary(o), new TaintedBooleanWithIntTag());
				} else
					break;
			}
			if (tmp != null) {
				in.clear();
				tmp.add$$PHOSPHORTAGGED(tmp, new TaintedBooleanWithIntTag());
			}
		}
		return in;
	}

	public static final Collection ensureIsUnBoxed(Collection in) {
		if (in != null) {
			Collection tmp = null;
			for (Object o : in) {
				if (o != null && MultiDTaintedArrayWithIntTag.isPrimitiveBoxClass(o.getClass()) != null) {
					if (tmp == null) {
						try {
							tmp = (Collection) in.getClass().getConstructor().newInstance(null);
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
					tmp.add$$PHOSPHORTAGGED(MultiDTaintedArrayWithIntTag.unboxRaw(o), new TaintedBooleanWithIntTag());

				} else
					break;
			}
			if (tmp != null) {
				in.clear();
				tmp.add$$PHOSPHORTAGGED(tmp, new TaintedBooleanWithIntTag());
			}
		}
		return in;
	}
	public static final Collection ensureIsUnBoxedObjTags(Collection in) {
		if (in != null) {
			Collection tmp = null;
			for (Object o : in) {
				if (o != null && MultiDTaintedArrayWithObjTag.isPrimitiveBoxClass(o.getClass()) != null) {
					if (tmp == null) {
						try {
							tmp = (Collection) in.getClass().getConstructor().newInstance(null);
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
					tmp.add$$PHOSPHORTAGGED(MultiDTaintedArrayWithObjTag.unboxRaw(o), new TaintedBooleanWithObjTag());

				} else
					break;
			}
			if (tmp != null) {
				in.clear();
				tmp.add$$PHOSPHORTAGGED(tmp, new TaintedBooleanWithObjTag());
			}
		}
		return in;
	}
	public static final Collection ensureIsUnBoxedImplicitTracking(Collection in) {
		if (in != null) {
			Collection tmp = null;
			for (Object o : in) {
				if (o != null && MultiDTaintedArrayWithObjTag.isPrimitiveBoxClass(o.getClass()) != null) {
					if (tmp == null) {
						try {
							tmp = (Collection) in.getClass().getConstructor().newInstance(null);
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
					tmp.add$$PHOSPHORTAGGED(MultiDTaintedArrayWithObjTag.unboxRaw(o), new ControlTaintTagStack(), new TaintedBooleanWithObjTag());

				} else
					break;
			}
			if (tmp != null) {
				in.clear();
				tmp.add$$PHOSPHORTAGGED(tmp, new ControlTaintTagStack(), new TaintedBooleanWithObjTag());
			}
		}
		return in;
	}
}
