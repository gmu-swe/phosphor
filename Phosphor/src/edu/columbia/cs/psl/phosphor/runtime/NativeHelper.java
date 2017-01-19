package edu.columbia.cs.psl.phosphor.runtime;

import java.util.Collection;

import org.objectweb.asm.Type;

import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;
import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArrayWithObjTag;

public final class NativeHelper {

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
