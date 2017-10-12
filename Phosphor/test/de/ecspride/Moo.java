package de.ecspride;

public class Moo {
	private Foo f = new Foo();
	private boolean flag ;

	public Moo(boolean flag_val) { this.flag = flag_val;}

	public Foo getF() {
		return f;
	}

	public void setF(Foo f) {
		this.f = f;
	}

	public boolean isFlag() {
		return flag;
	}

	public void setFlag(boolean flag) {
		this.flag = flag;
	}


}
