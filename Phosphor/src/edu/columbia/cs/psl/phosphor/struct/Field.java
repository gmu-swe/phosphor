package edu.columbia.cs.psl.phosphor.struct;

import java.util.Objects;

public class Field {
    public boolean isStatic;
    public String name;
    public String owner;
    public String description;

    public Field(boolean isStatic, String owner, String name, String description) {
        this.isStatic = isStatic;
        this.name = name;
        this.owner = owner;
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }
        if(o == null || getClass() != o.getClass()) {
            return false;
        }
        Field field = (Field) o;
        return isStatic == field.isStatic &&
                Objects.equals(name, field.name) &&
                Objects.equals(owner, field.owner) &&
                Objects.equals(description, field.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isStatic, name, owner, description);
    }

    @Override
    public String toString() {
        return "Field{" +
                "isStatic=" + isStatic +
                ", name='" + name + '\'' +
                ", owner='" + owner + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
