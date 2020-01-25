package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.Taint;

public class MaybeThrownException<E> {

    private final Class<? extends Throwable> clazz;
    private Taint<E> tag;

    public MaybeThrownException(Class<? extends Throwable> clazz, Taint<E> tag) {
        this.clazz = clazz;
        this.tag = tag;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        } else if(!(o instanceof MaybeThrownException)) {
            return false;
        }
        MaybeThrownException<?> that = (MaybeThrownException<?>) o;
        if(clazz != null ? !clazz.equals(that.clazz) : that.clazz != null) {
            return false;
        }
        return tag != null ? tag.equals(that.tag) : that.tag == null;
    }

    @Override
    public int hashCode() {
        int result = clazz != null ? clazz.hashCode() : 0;
        result = 31 * result + (tag != null ? tag.hashCode() : 0);
        return result;
    }

    public Class<? extends Throwable> getClazz() {
        return clazz;
    }

    public Taint<E> getTag() {
        return tag;
    }

    public void unionTag(Taint<E> tag) {
        this.tag = Taint.combineTags(this.tag, tag);
    }
}
