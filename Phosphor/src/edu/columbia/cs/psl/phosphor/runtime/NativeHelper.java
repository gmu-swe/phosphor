package edu.columbia.cs.psl.phosphor.runtime;

import java.util.Collection;

import edu.columbia.cs.psl.phosphor.Configuration;
import org.objectweb.asm.Type;
import edu.columbia.cs.psl.phosphor.struct.ControlTaintTagStack;
import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.TaintedBooleanWithObjTag;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArrayWithIntTag;
import edu.columbia.cs.psl.phosphor.struct.multid.MultiDTaintedArrayWithObjTag;

public final class NativeHelper {

	@SuppressWarnings("rawtypes")
	public static final Collection ensureIsBoxedImplicitTracking(Collection in) {
		return ensureIsBoxedInternal(in, 0);
	}

	@SuppressWarnings("rawtypes")
	public static final Collection ensureIsBoxedObjTags(Collection in) {
		return ensureIsBoxedInternal(in, 1);
	}

	@SuppressWarnings("rawtypes")
	public static final Collection ensureIsBoxed(Collection in) {
		return ensureIsBoxedInternal(in, 2);
	}

	@SuppressWarnings("rawtypes")
	private static final Collection ensureIsBoxedInternal(Collection in, int which) {
		if (in != null) {
			Collection tmp = null;
			for (Object o : in) {
				if (o == null)
					break;
				Type t = Type.getType(o.getClass());
				if (t.getSort() == Type.ARRAY
						&& t.getElementType().getSort() != Type.OBJECT) {
					if (tmp == null) {
						try {
							tmp = (Collection) in.getClass()
												.getConstructor().newInstance(null);
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
					switch (which) {
					case 0:
						tmp.add$$PHOSPHORTAGGED(MultiDTaintedArrayWithObjTag.boxIfNecessary(o),
								new ControlTaintTagStack(), new TaintedBooleanWithObjTag());
						break;
					case 1:
						tmp.add$$PHOSPHORTAGGED(MultiDTaintedArrayWithObjTag.boxIfNecessary(o),
								new TaintedBooleanWithObjTag());
						break;
					case 2:
						tmp.add$$PHOSPHORTAGGED(MultiDTaintedArrayWithIntTag.boxIfNecessary(o),
								new TaintedBooleanWithIntTag());
						break;
					}
				} else
					break;
			}
			if (tmp != null) {
				in.clear();
				switch (which) {
				case 0:
					tmp.add$$PHOSPHORTAGGED(tmp,
							new ControlTaintTagStack(),
							new TaintedBooleanWithObjTag());
					break;
				case 1:
					tmp.add$$PHOSPHORTAGGED(tmp,
							new TaintedBooleanWithObjTag());
					break;
				case 2:
					tmp.add$$PHOSPHORTAGGED(tmp,
							new TaintedBooleanWithIntTag());
					break;
				}
			}
		}
		return in;
	}

	public static final Collection ensureIsUnBoxed(Collection in) {
		return ensureIsUnBoxedInternal(in, 2);
	}

	public static final Collection ensureIsUnBoxedObjTags(Collection in) {
		return ensureIsUnBoxedInternal(in, 1);
	}

	public static final Collection ensureIsUnBoxedImplicitTracking(Collection in) {
		return ensureIsUnBoxedInternal(in, 0);
	}

	private static final Collection ensureIsUnBoxedInternal(Collection in, int which) {

		boolean isNull;

		if (in != null) {
			Collection tmp = null;
			for (Object o : in) {

				isNull = (which < 2)
					? MultiDTaintedArrayWithObjTag.isPrimitiveBoxClass(o.getClass()) != null
					: MultiDTaintedArrayWithIntTag.isPrimitiveBoxClass(o.getClass()) != null;

				if (o != null && isNull) {
					if (tmp == null) {
						try {
							tmp = (Collection) in.getClass()
								.getConstructor().newInstance(null);
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}

					switch (which) {
					case 0:
						tmp.add$$PHOSPHORTAGGED(MultiDTaintedArrayWithObjTag.unboxRaw(o),
								new ControlTaintTagStack(), new TaintedBooleanWithObjTag());
						break;
					case 1:
						tmp.add$$PHOSPHORTAGGED(MultiDTaintedArrayWithObjTag.unboxRaw(o),
								new TaintedBooleanWithObjTag());
						break;
					case 2:
						tmp.add$$PHOSPHORTAGGED(MultiDTaintedArrayWithIntTag.unboxRaw(o),
								new TaintedBooleanWithIntTag());
						break;
					}

				} else
					break;
			}
			if (tmp != null) {
				in.clear();
				switch (which) {
				case 0:
					tmp.add$$PHOSPHORTAGGED(tmp,
							new ControlTaintTagStack(),
							new TaintedBooleanWithObjTag());
					break;
				case 1:
					tmp.add$$PHOSPHORTAGGED(tmp,
							new TaintedBooleanWithObjTag());
					break;
				case 2:
					tmp.add$$PHOSPHORTAGGED(tmp,
							new TaintedBooleanWithIntTag());
					break;
				}
			}
		}
		return in;
	}
}
