package edu.columbia.cs.psl.phosphor.struct;

import edu.columbia.cs.psl.phosphor.runtime.BoxedPrimitiveStoreWithObjTags;
import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

import java.io.Serializable;

public abstract class SerializationWrapper implements Serializable {

    private static final long serialVersionUID = -5083451790123224811L;
    protected final Taint tag;

    private SerializationWrapper(Taint tag) {
        this.tag = tag;
    }

    public abstract Object unwrap();

    public static SerializationWrapper wrap(Boolean val) {
        return new BooleanWrapper(val);
    }

    public static SerializationWrapper wrap(Byte val) {
        return new ByteWrapper(val);
    }

    public static SerializationWrapper wrap(Character val) {
        return new CharacterWrapper(val);
    }

    public static SerializationWrapper wrap(Short val) {
        return new ShortWrapper(val);
    }

    private static class BooleanWrapper extends SerializationWrapper {

        private static final long serialVersionUID = 391181930404648158L;
        private final boolean val;

        BooleanWrapper(Boolean val) {
            super(MultiTainter.getTaint(val));
            this.val = val;
        }

        @Override
        public Object unwrap() {
            return BoxedPrimitiveStoreWithObjTags.valueOf(tag, val);
        }
    }

    private static class ByteWrapper extends SerializationWrapper {

        private static final long serialVersionUID = 8430253026877283689L;
        private final byte val;

        ByteWrapper(Byte val) {
            super(MultiTainter.getTaint(val));
            this.val = val;
        }

        @Override
        public Object unwrap() {
            return BoxedPrimitiveStoreWithObjTags.valueOf(tag, val);
        }
    }

    private static class CharacterWrapper extends SerializationWrapper {

        private static final long serialVersionUID = 2766884087956504194L;
        private final char val;

        CharacterWrapper(Character val) {
            super(MultiTainter.getTaint(val));
            this.val = val;
        }

        @Override
        public Object unwrap() {
            return BoxedPrimitiveStoreWithObjTags.valueOf(tag, val);
        }
    }

    private static class ShortWrapper extends SerializationWrapper {

        private static final long serialVersionUID = -2702102915716255295L;
        private final short val;

        ShortWrapper(Short val) {
            super(MultiTainter.getTaint(val));
            this.val = val;
        }

        @Override
        public Object unwrap() {
            return BoxedPrimitiveStoreWithObjTags.valueOf(tag, val);
        }
    }
}
