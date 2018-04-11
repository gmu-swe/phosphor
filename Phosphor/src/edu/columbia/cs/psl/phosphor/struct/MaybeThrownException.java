package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.Taint;

public class MaybeThrownException {
	public Class<? extends Throwable> clazz;
	public Taint tag;

	public MaybeThrownException(Class<? extends Throwable> clazz, Taint tag) {
		this.clazz = clazz;
		this.tag = tag;
	}
}
