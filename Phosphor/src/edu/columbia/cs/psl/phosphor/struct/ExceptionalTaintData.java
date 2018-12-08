package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.Taint;

public class ExceptionalTaintData {
	public Taint taint;
	public LinkedList<Taint> prevTaints = new LinkedList<>();
	public void push(Taint tag) {
		if(this.taint == tag)
			return;
		prevTaints.addFast(this.taint);
		if(this.taint == null)
		{
			this.taint = new Taint(tag);
		}
		else
		{
			if(!this.taint.contains(tag)) {
				this.taint = this.taint.copy();
				this.taint.addDependency(tag);
			}
		}
	}
	public void pop(int n){

		while(n > 0){
			this.taint = prevTaints.pop();
			n--;
		}
	}
}
