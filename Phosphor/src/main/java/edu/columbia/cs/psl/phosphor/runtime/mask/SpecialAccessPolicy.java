package edu.columbia.cs.psl.phosphor.runtime.mask;

public enum SpecialAccessPolicy {
    VOLATILE {
        @Override
        public void putObject(UnsafeAdapter unsafe, Object o, long offset, Object x) {
            unsafe.putObjectVolatile(o, offset, x);
        }

        @Override
        public Object getObject(UnsafeAdapter unsafe, Object o, long offset) {
            return unsafe.getObjectVolatile(o, offset);
        }
    },
    ORDERED {
        @Override
        public void putObject(UnsafeAdapter unsafe, Object o, long offset, Object x) {
            unsafe.putOrderedObject(o, offset, x);
        }

        @Override
        public Object getObject(UnsafeAdapter unsafe, Object o, long offset) {
            throw new UnsupportedOperationException();
        }
    },
    NONE {
        @Override
        public void putObject(UnsafeAdapter unsafe, Object o, long offset, Object x) {
            unsafe.putObject(o, offset, x);
        }

        @Override
        public Object getObject(UnsafeAdapter unsafe, Object o, long offset) {
            return unsafe.getObject(o, offset);
        }
    };

    public abstract void putObject(UnsafeAdapter unsafe, Object o, long offset, Object x);

    public abstract Object getObject(UnsafeAdapter unsafe, Object o, long offset);
}
