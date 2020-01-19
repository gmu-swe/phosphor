package edu.columbia.cs.psl.phosphor.control;

public final class LocalVariable {

    private final int index;
    private final String typeInternalName;

    public LocalVariable(int index, String typeInternalName) {
        if(typeInternalName == null) {
            throw new NullPointerException();
        }
        this.index = index;
        this.typeInternalName = typeInternalName;
    }

    public int getIndex() {
        return index;
    }

    public String getTypeInternalName() {
        return typeInternalName;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        } else if(!(o instanceof LocalVariable)) {
            return false;
        }
        LocalVariable that = (LocalVariable) o;
        if(index != that.index) {
            return false;
        }
        return typeInternalName.equals(that.typeInternalName);
    }

    @Override
    public int hashCode() {
        int result = index;
        result = 31 * result + typeInternalName.hashCode();
        return result;
    }
}
