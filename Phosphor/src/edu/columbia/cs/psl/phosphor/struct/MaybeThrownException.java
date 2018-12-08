package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.Taint;

public class MaybeThrownException {
	public Class<? extends Throwable> clazz;
	public Taint tag;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		MaybeThrownException that = (MaybeThrownException) o;

		if (clazz != null ? !clazz.equals(that.clazz) : that.clazz != null) return false;
		return tag != null ? tag.equals(that.tag) : that.tag == null;
	}

	@Override
	public int hashCode() {
		int result = clazz != null ? clazz.hashCode() : 0;
		result = 31 * result + (tag != null ? tag.hashCode() : 0);
		return result;
	}

	public MaybeThrownException(Class<? extends Throwable> clazz, Taint tag) {
		this.clazz = clazz;
		this.tag = tag;
	}
}
