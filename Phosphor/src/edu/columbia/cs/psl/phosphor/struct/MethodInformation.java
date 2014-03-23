package edu.columbia.cs.psl.phosphor.struct;

import java.io.Serializable;
import java.util.HashSet;

public class MethodInformation implements Serializable {

	private static final long serialVersionUID = -6012894290125318919L;

	@Override
	public String toString() {
		return owner+"."+name+desc;
	}
	public MethodInformation(String owner, String name, String desc) {
		this.owner = owner;
		this.name = name;
		this.desc = desc;
	}
	String owner;
	String name;
	String desc;
	HashSet<MethodInformation> methodsCalled = new HashSet<MethodInformation>();
	boolean callsTaintedMethods;
	boolean doesNotCallTaintedMethods;
	boolean taintCallExplorationInProgress;
	
	public String getOwner() {
		return owner;
	}
	public void setOwner(String owner) {
		this.owner = owner;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDesc() {
		return desc;
	}
	public void setDesc(String desc) {
		this.desc = desc;
	}
	public HashSet<MethodInformation> getMethodsCalled() {
		return methodsCalled;
	}
	public void setMethodsCalled(HashSet<MethodInformation> methodsCalled) {
		this.methodsCalled = methodsCalled;
	}
	public boolean callsTaintSourceMethods() {
		return callsTaintedMethods;
	}
	public void setCallsTaintedMethods(boolean callsTaintedMethods) {
		this.callsTaintedMethods = callsTaintedMethods;
	}
	public boolean doesNotCallTaintSourceMethods() {
		return doesNotCallTaintedMethods;
	}
	public void setDoesNotCallTaintedMethods(boolean doesNotCallTaintedMethods) {
		this.doesNotCallTaintedMethods = doesNotCallTaintedMethods;
	}
	public boolean isTaintCallExplorationInProgress() {
		return taintCallExplorationInProgress;
	}
	public void setTaintCallExplorationInProgress(boolean taintCallExplorationInProgress) {
		this.taintCallExplorationInProgress = taintCallExplorationInProgress;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((desc == null) ? 0 : desc.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((owner == null) ? 0 : owner.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MethodInformation other = (MethodInformation) obj;
		if (desc == null) {
			if (other.desc != null)
				return false;
		} else if (!desc.equals(other.desc))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (owner == null) {
			if (other.owner != null)
				return false;
		} else if (!owner.equals(other.owner))
			return false;
		return true;
	}
	boolean isPure = true;
	public boolean isPure() {
		return isPure;
	}
	public void setPure(boolean isPure) {
		this.isPure = isPure;
	}
	boolean visited;
	public void setVisited(boolean b) {
		this.visited =b;
	}
	public boolean isVisited() {
		return visited;
	}
	boolean calculated;
	public boolean isCalculated() {
		return calculated;
	}
	public void setCalculated(boolean calculated) {
		this.calculated = calculated;
	}
}
