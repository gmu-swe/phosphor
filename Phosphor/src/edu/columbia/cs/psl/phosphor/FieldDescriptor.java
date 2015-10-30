package edu.columbia.cs.psl.phosphor;

public class FieldDescriptor {
	
	private String name;
	private String owner;
	private String desc;
	
	public FieldDescriptor(String name, String owner, String desc) {
		super();
		this.name = name;
		this.owner = owner;
		this.desc = desc;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getOwner() {
		return owner;
	}
	public void setOwner(String owner) {
		this.owner = owner;
	}
	public String getDesc() {
		return desc;
	}
	public void setDesc(String desc) {
		this.desc = desc;
	}
	
	@Override
	public String toString() {
		return "name: " + name + "& owner: " + owner + "& desc: " + desc;
	}
	
	@Override
	public int hashCode() {
		return name.length() + owner.length() + desc.length();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof FieldDescriptor) {
			FieldDescriptor mdesc = (FieldDescriptor)obj;
			return mdesc.getDesc().equals(desc) && mdesc.getName().equals(name) && mdesc.getOwner().equals(owner);
		}
		return false;
	}
}
