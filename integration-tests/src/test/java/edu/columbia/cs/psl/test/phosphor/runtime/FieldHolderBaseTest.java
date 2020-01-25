package edu.columbia.cs.psl.test.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.test.phosphor.BaseMultiTaintClass;
import org.junit.Rule;
import org.junit.rules.ExternalResource;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedList;

/* Provides classes with fields of various types and standard methods for interacting with them. */
public abstract class FieldHolderBaseTest extends BaseMultiTaintClass {

    public PrimitiveSupplier supplier;
    public LinkedList<Class<?>> primArrayTypes;

    @Rule
    public ExternalResource resource = new ExternalResource() {
        @Override
        protected void before() {
            supplier = new PrimitiveSupplier();
            primArrayTypes = new LinkedList<Class<?>>(
                    Arrays.asList(
                            int[].class,
                            long[].class,
                            boolean[].class,
                            short[].class,
                            double[].class,
                            byte[].class,
                            char[].class,
                            float[].class
                    )
            );
        }
    };

    /* Holds primitive fields. */
    public static class PrimitiveHolder implements Serializable {

        private static final long serialVersionUID = 1785984900982403834L;

        private int i;
        private long j;
        private boolean z;
        private short s;
        private double d;
        private byte b;
        private char c;
        private float f;

        /* Constructs a new PrimitiveHolder with initial values. If taintFields is true then the initial values should be
         * tainted. */
        public PrimitiveHolder(boolean taintFields) {
            if(taintFields) {
                this.i = MultiTainter.taintedInt(5, "int-field");
                this.j = MultiTainter.taintedLong(44, "long-field");
                this.z = MultiTainter.taintedBoolean(true, "bool-field");
                this.s = MultiTainter.taintedShort((short)5, "short-field");
                this.d = MultiTainter.taintedDouble(4.5, "double-field");
                this.b = MultiTainter.taintedByte((byte)4, "byte-field");
                this.c = MultiTainter.taintedChar('w', "char-field");
                this.f = MultiTainter.taintedFloat(3.3f, "float-field");
            } else {
                this.i = 5;
                this.j = 44;
                this.z = true;
                this.s = 5;
                this.d = 4.5;
                this.b = 4;
                this.c = 'w';
                this.f = 3.3f;
            }
        }

        /* Checks that every field for this instance is tainted. */
        public void checkFieldsAreTainted() {
            assertNonNullTaint(MultiTainter.getTaint(i));
            assertNonNullTaint(MultiTainter.getTaint(j));
            assertNonNullTaint(MultiTainter.getTaint(z));
            assertNonNullTaint(MultiTainter.getTaint(s));
            assertNonNullTaint(MultiTainter.getTaint(d));
            assertNonNullTaint(MultiTainter.getTaint(b));
            assertNonNullTaint(MultiTainter.getTaint(c));
            assertNonNullTaint(MultiTainter.getTaint(f));
        }

        /* Checks that every field for this instance is not tainted. */
        public void checkFieldsAreNotTainted() {
            assertNullOrEmpty(MultiTainter.getTaint(i));
            assertNullOrEmpty(MultiTainter.getTaint(j));
            assertNullOrEmpty(MultiTainter.getTaint(z));
            assertNullOrEmpty(MultiTainter.getTaint(s));
            assertNullOrEmpty(MultiTainter.getTaint(d));
            assertNullOrEmpty(MultiTainter.getTaint(b));
            assertNullOrEmpty(MultiTainter.getTaint(c));
            assertNullOrEmpty(MultiTainter.getTaint(f));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PrimitiveHolder that = (PrimitiveHolder) o;

            if (i != that.i) return false;
            if (j != that.j) return false;
            if (z != that.z) return false;
            if (s != that.s) return false;
            if (Double.compare(that.d, d) != 0) return false;
            if (b != that.b) return false;
            if (c != that.c) return false;
            return Float.compare(that.f, f) == 0;
        }

        @Override
        public int hashCode() {
            int result;
            long temp;
            result = i;
            result = 31 * result + (int) (j ^ (j >>> 32));
            result = 31 * result + (z ? 1 : 0);
            result = 31 * result + (int) s;
            temp = Double.doubleToLongBits(d);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            result = 31 * result + (int) b;
            result = 31 * result + (int) c;
            result = 31 * result + (f != +0.0f ? Float.floatToIntBits(f) : 0);
            return result;
        }

        public int getI() {
            return i;
        }

        public long getJ() {
            return j;
        }

        public boolean isZ() {
            return z;
        }

        public short getS() {
            return s;
        }

        public double getD() {
            return d;
        }

        public byte getB() {
            return b;
        }

        public char getC() {
            return c;
        }

        public float getF() {
            return f;
        }

        /* Returns the names of this class' fields without using reflection. */
        public static String[] fieldNames() {
            return new String[]{"i", "j", "z", "s", "d", "b", "c", "f"};
        }

        /* Returns this class' fields without calling getDeclaredFields. */
        public static Field[] fields() throws NoSuchFieldException {
            return getFields(PrimitiveHolder.class, fieldNames());
        }
    }

    /* Holds boxed primitive fields. */
    public static class BoxedPrimitiveHolder implements Serializable {

        private static final long serialVersionUID = 8912602941266021704L;

        private Integer i;
        private Long j;
        private Boolean z;
        private Short s;
        private Double d;
        private Byte b;
        private Character c;
        private Float f;

        /* Constructs a new BoxedPrimitiveHolder with initial values. If taintFields is true then the initial values should be
         * tainted. */
        public BoxedPrimitiveHolder(boolean taintFields) {
            if(taintFields) {
                this.i = MultiTainter.taintedInt(5, "int-field");
                this.j = MultiTainter.taintedLong(44, "long-field");
                this.z = MultiTainter.taintedBoolean(true, "bool-field");
                this.s = MultiTainter.taintedShort((short)5, "short-field");
                this.d = MultiTainter.taintedDouble(4.5, "double-field");
                this.b = MultiTainter.taintedByte((byte)4, "byte-field");
                this.c = MultiTainter.taintedChar('w', "char-field");
                this.f = MultiTainter.taintedFloat(3.3f, "float-field");
            } else {
                this.i = 5;
                this.j = (long)44;
                this.z = true;
                this.s = 5;
                this.d = 4.5;
                this.b = 4;
                this.c = 'w';
                this.f = 3.3f;
            }
        }

        /* Checks that every field for this instance is tainted. */
        public void checkFieldsAreTainted() {
            assertNonNullTaint(MultiTainter.getTaint(i));
            assertNonNullTaint(MultiTainter.getTaint(j));
            assertNonNullTaint(MultiTainter.getTaint(z));
            assertNonNullTaint(MultiTainter.getTaint(s));
            assertNonNullTaint(MultiTainter.getTaint(d));
            assertNonNullTaint(MultiTainter.getTaint(b));
            assertNonNullTaint(MultiTainter.getTaint(c));
            assertNonNullTaint(MultiTainter.getTaint(f));
        }

        /* Checks that every field for this instance is not tainted. */
        public void checkFieldsAreNotTainted() {
            assertNullOrEmpty(MultiTainter.getTaint(i));
            assertNullOrEmpty(MultiTainter.getTaint(j));
            assertNullOrEmpty(MultiTainter.getTaint(z));
            assertNullOrEmpty(MultiTainter.getTaint(s));
            assertNullOrEmpty(MultiTainter.getTaint(d));
            assertNullOrEmpty(MultiTainter.getTaint(b));
            assertNullOrEmpty(MultiTainter.getTaint(c));
            assertNullOrEmpty(MultiTainter.getTaint(f));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BoxedPrimitiveHolder that = (BoxedPrimitiveHolder) o;
            if (i != null ? !i.equals(that.i) : that.i != null) return false;
            if (j != null ? !j.equals(that.j) : that.j != null) return false;
            if (z != null ? !z.equals(that.z) : that.z != null) return false;
            if (s != null ? !s.equals(that.s) : that.s != null) return false;
            if (d != null ? !d.equals(that.d) : that.d != null) return false;
            if (b != null ? !b.equals(that.b) : that.b != null) return false;
            if (c != null ? !c.equals(that.c) : that.c != null) return false;
            return f != null ? f.equals(that.f) : that.f == null;
        }

        @Override
        public int hashCode() {
            int result = i != null ? i.hashCode() : 0;
            result = 31 * result + (j != null ? j.hashCode() : 0);
            result = 31 * result + (z != null ? z.hashCode() : 0);
            result = 31 * result + (s != null ? s.hashCode() : 0);
            result = 31 * result + (d != null ? d.hashCode() : 0);
            result = 31 * result + (b != null ? b.hashCode() : 0);
            result = 31 * result + (c != null ? c.hashCode() : 0);
            result = 31 * result + (f != null ? f.hashCode() : 0);
            return result;
        }

        /* Returns the names of this class' fields without using reflection. */
        public static String[] fieldNames() {
            return new String[]{"i", "j", "z", "s", "d", "b", "c", "f"};
        }

        /* Returns this class' fields without calling getDeclaredFields. */
        public static Field[] fields() throws NoSuchFieldException {
            return getFields(BoxedPrimitiveHolder.class, fieldNames());
        }
    }

    /* Holds primitive array fields. */
    public static class PrimitiveArrayHolder implements Serializable {

        private static final long serialVersionUID = 2944796967332409895L;

        private int[] ia;
        private long[] ja;
        private boolean[] za;
        private short[] sa;
        private double[] da;
        private byte[] ba;
        private char[] ca;
        private float[] fa;

        /* Constructs a new PrimitiveArrayHolder with initial values. If taintFields is true then the initial values should
         * be tainted. */
        public PrimitiveArrayHolder(boolean taintFields) {
            if(taintFields) {
                this.ia = new int[]{MultiTainter.taintedInt(5, "int-ar--field")};
                this.ja  = new long[]{MultiTainter.taintedLong(44, "long-arr-field")};
                this.za = new boolean[]{MultiTainter.taintedBoolean(true, "bool-arr-field")};
                this.sa = new short[]{MultiTainter.taintedShort((short)5, "short-arr-field")};
                this.da = new double[]{MultiTainter.taintedDouble(4.5, "double-arr-field")};
                this.ba = new byte[]{MultiTainter.taintedByte((byte)4, "byte-arr-field")};
                this.ca = new char[]{MultiTainter.taintedChar('w', "char-arr-field")};
                this.fa = new float[]{MultiTainter.taintedFloat(3.3f, "float-arr-field")};
            } else {
                this.ia = new int[]{5};
                this.ja  = new long[]{44};
                this.za = new boolean[]{true};
                this.sa = new short[]{5};
                this.da = new double[]{4.5};
                this.ba = new byte[]{4};
                this.ca = new char[]{'w'};
                this.fa = new float[]{3.3f};
            }
        }

        /* Checks that field of this instance are tainted. */
        public void checkFieldsAreTainted() {
            assertNonNullTaint(MultiTainter.getMergedTaint(ia));
            assertNonNullTaint(MultiTainter.getMergedTaint(ja));
            assertNonNullTaint(MultiTainter.getMergedTaint(za));
            assertNonNullTaint(MultiTainter.getMergedTaint(sa));
            assertNonNullTaint(MultiTainter.getMergedTaint(da));
            assertNonNullTaint(MultiTainter.getMergedTaint(ba));
            assertNonNullTaint(MultiTainter.getMergedTaint(ca));
            assertNonNullTaint(MultiTainter.getMergedTaint(fa));
        }

        /* Checks that the fields of this instance are not tainted. */
        public void checkFieldsAreNotTainted() {
            assertNullOrEmpty(MultiTainter.getMergedTaint(ia));
            assertNullOrEmpty(MultiTainter.getMergedTaint(ja));
            assertNullOrEmpty(MultiTainter.getMergedTaint(za));
            assertNullOrEmpty(MultiTainter.getMergedTaint(sa));
            assertNullOrEmpty(MultiTainter.getMergedTaint(da));
            assertNullOrEmpty(MultiTainter.getMergedTaint(ba));
            assertNullOrEmpty(MultiTainter.getMergedTaint(ca));
            assertNullOrEmpty(MultiTainter.getMergedTaint(fa));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PrimitiveArrayHolder that = (PrimitiveArrayHolder) o;
            if (!Arrays.equals(ia, that.ia)) return false;
            if (!Arrays.equals(ja, that.ja)) return false;
            if (!Arrays.equals(za, that.za)) return false;
            if (!Arrays.equals(sa, that.sa)) return false;
            if (!Arrays.equals(da, that.da)) return false;
            if (!Arrays.equals(ba, that.ba)) return false;
            if (!Arrays.equals(ca, that.ca)) return false;
            return Arrays.equals(fa, that.fa);
        }

        @Override
        public int hashCode() {
            int result = Arrays.hashCode(ia);
            result = 31 * result + Arrays.hashCode(ja);
            result = 31 * result + Arrays.hashCode(za);
            result = 31 * result + Arrays.hashCode(sa);
            result = 31 * result + Arrays.hashCode(da);
            result = 31 * result + Arrays.hashCode(ba);
            result = 31 * result + Arrays.hashCode(ca);
            result = 31 * result + Arrays.hashCode(fa);
            return result;
        }

        public int[] getIa() {
            return ia;
        }

        public long[] getJa() {
            return ja;
        }

        public boolean[] getZa() {
            return za;
        }

        public short[] getSa() {
            return sa;
        }

        public double[] getDa() {
            return da;
        }

        public byte[] getBa() {
            return ba;
        }

        public char[] getCa() {
            return ca;
        }

        public float[] getFa() {
            return fa;
        }

        /* Returns the names of this class' fields without using reflection. */
        public static String[] fieldNames() {
            return new String[]{"ia", "ja", "za", "sa", "da", "ba", "ca", "fa"};
        }

        @Override
        public String toString() {
            return "PrimitiveArrayHolder{" +
                    "ia=" + Arrays.toString(ia) +
                    ", ja=" + Arrays.toString(ja) +
                    ", za=" + Arrays.toString(za) +
                    ", sa=" + Arrays.toString(sa) +
                    ", da=" + Arrays.toString(da) +
                    ", ba=" + Arrays.toString(ba) +
                    ", ca=" + Arrays.toString(ca) +
                    ", fa=" + Arrays.toString(fa) +
                    '}';
        }

        /* Returns this class' fields without calling getDeclaredFields. */
        public static Field[] fields() throws NoSuchFieldException {
            return getFields(PrimitiveArrayHolder.class, fieldNames());
        }
    }

    /* Hold 2D primitive array fields. */
    public static class Primitive2DArrayHolder implements Serializable {

        private static final long serialVersionUID = 1814117877155387001L;

        private int[][] iaa;
        private long[][] jaa;
        private boolean[][] zaa;
        private short[][] saa;
        private double[][] daa;
        private byte[][] baa;
        private char[][] caa;
        private float[][] faa;

        /* Constructs a new PrimitiveArrayHolder with initial values. If taintFields is true then the initial values should
         * be tainted. */
        public Primitive2DArrayHolder(boolean taintFields) {
            if(taintFields) {
                this.iaa = new int[][]{{MultiTainter.taintedInt(5, "int-ar--field")}};
                this.jaa  = new long[][]{{MultiTainter.taintedLong(44, "long-arr-field")}};
                this.zaa = new boolean[][]{{MultiTainter.taintedBoolean(true, "bool-arr-field")}};
                this.saa = new short[][]{{MultiTainter.taintedShort((short)5, "short-arr-field")}};
                this.daa = new double[][]{{MultiTainter.taintedDouble(4.5, "double-arr-field")}};
                this.baa = new byte[][]{{MultiTainter.taintedByte((byte)4, "byte-arr-field")}};
                this.caa = new char[][]{{MultiTainter.taintedChar('w', "char-arr-field")}};
                this.faa = new float[][]{{MultiTainter.taintedFloat(3.3f, "float-arr-field")}};
            } else {
                this.iaa = new int[][]{{5}};
                this.jaa  = new long[][]{{44}};
                this.zaa = new boolean[][]{{true}};
                this.saa = new short[][]{{5}};
                this.daa = new double[][]{{4.5}};
                this.baa = new byte[][]{{4}};
                this.caa = new char[][]{{'w'}};
                this.faa = new float[][]{{3.3f}};
            }
        }

        /* Checks that field of this instance are tainted. */
        public void checkFieldsAreTainted() {
            assertNonNullTaint(MultiTainter.getMergedTaint(iaa));
            assertNonNullTaint(MultiTainter.getMergedTaint(jaa));
            assertNonNullTaint(MultiTainter.getMergedTaint(zaa));
            assertNonNullTaint(MultiTainter.getMergedTaint(saa));
            assertNonNullTaint(MultiTainter.getMergedTaint(daa));
            assertNonNullTaint(MultiTainter.getMergedTaint(baa));
            assertNonNullTaint(MultiTainter.getMergedTaint(caa));
            assertNonNullTaint(MultiTainter.getMergedTaint(faa));
        }

        /* Checks that the fields of this instance are not tainted. */
        public void checkFieldsAreNotTainted() {
            assertNullOrEmpty(MultiTainter.getMergedTaint(iaa));
            assertNullOrEmpty(MultiTainter.getMergedTaint(jaa));
            assertNullOrEmpty(MultiTainter.getMergedTaint(zaa));
            assertNullOrEmpty(MultiTainter.getMergedTaint(saa));
            assertNullOrEmpty(MultiTainter.getMergedTaint(daa));
            assertNullOrEmpty(MultiTainter.getMergedTaint(baa));
            assertNullOrEmpty(MultiTainter.getMergedTaint(caa));
            assertNullOrEmpty(MultiTainter.getMergedTaint(faa));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Primitive2DArrayHolder that = (Primitive2DArrayHolder) o;
            if (!Arrays.deepEquals(iaa, that.iaa)) return false;
            if (!Arrays.deepEquals(jaa, that.jaa)) return false;
            if (!Arrays.deepEquals(zaa, that.zaa)) return false;
            if (!Arrays.deepEquals(saa, that.saa)) return false;
            if (!Arrays.deepEquals(daa, that.daa)) return false;
            if (!Arrays.deepEquals(baa, that.baa)) return false;
            if (!Arrays.deepEquals(caa, that.caa)) return false;
            return Arrays.deepEquals(faa, that.faa);
        }

        @Override
        public int hashCode() {
            int result = Arrays.deepHashCode(iaa);
            result = 31 * result + Arrays.deepHashCode(jaa);
            result = 31 * result + Arrays.deepHashCode(zaa);
            result = 31 * result + Arrays.deepHashCode(saa);
            result = 31 * result + Arrays.deepHashCode(daa);
            result = 31 * result + Arrays.deepHashCode(baa);
            result = 31 * result + Arrays.deepHashCode(caa);
            result = 31 * result + Arrays.deepHashCode(faa);
            return result;
        }

        /* Returns the names of this class' fields without using reflection. */
        public static String[] fieldNames() {
            return new String[]{"iaa", "jaa", "zaa", "saa", "daa", "baa", "caa", "faa"};
        }

        /* Returns this class' fields without calling getDeclaredFields. */
        public static Field[] fields() throws NoSuchFieldException {
            return getFields(Primitive2DArrayHolder.class, fieldNames());
        }
    }

    /* Class containing Object fields used to store primitive arrays. */
    public static class PrimitiveArrayObjHolder implements Serializable {

        private static final long serialVersionUID = -975399692415868060L;

        public Object ia;
        public Object ja;
        public Object za;
        public Object sa;
        public Object da;
        public Object ba;
        public Object ca;
        public Object fa;

        /* Constructs a new PrimitiveArrayObjHolder with initial values. If taintFields is true then the initial values
         *  should be tainted. */
        public PrimitiveArrayObjHolder(boolean taintFields) {
            if(taintFields) {
                this.ia = new int[]{MultiTainter.taintedInt(5, "int-ar--field")};
                this.ja  = new long[]{MultiTainter.taintedLong(44, "long-arr-field")};
                this.za = new boolean[]{MultiTainter.taintedBoolean(true, "bool-arr-field")};
                this.sa = new short[]{MultiTainter.taintedShort((short)5, "short-arr-field")};
                this.da = new double[]{MultiTainter.taintedDouble(4.5, "double-arr-field")};
                this.ba = new byte[]{MultiTainter.taintedByte((byte)4, "byte-arr-field")};
                this.ca = new char[]{MultiTainter.taintedChar('w', "char-arr-field")};
                this.fa = new float[]{MultiTainter.taintedFloat(3.3f, "float-arr-field")};
            } else {
                this.ia = new int[]{5};
                this.ja  = new long[]{44};
                this.za = new boolean[]{true};
                this.sa = new short[]{5};
                this.da = new double[]{4.5};
                this.ba = new byte[]{4};
                this.ca = new char[]{'w'};
                this.fa = new float[]{3.3f};
            }
        }

        /* Checks that field of this instance are tainted. */
        public void checkFieldsAreTainted() {
            assertNonNullTaint(MultiTainter.getMergedTaint(ia));
            assertNonNullTaint(MultiTainter.getMergedTaint(ja));
            assertNonNullTaint(MultiTainter.getMergedTaint(za));
            assertNonNullTaint(MultiTainter.getMergedTaint(sa));
            assertNonNullTaint(MultiTainter.getMergedTaint(da));
            assertNonNullTaint(MultiTainter.getMergedTaint(ba));
            assertNonNullTaint(MultiTainter.getMergedTaint(ca));
            assertNonNullTaint(MultiTainter.getMergedTaint(fa));
        }

        /* Checks that the fields of this instance are not tainted. */
        public void checkFieldsAreNotTainted() {
            assertNullOrEmpty(MultiTainter.getMergedTaint(ia));
            assertNullOrEmpty(MultiTainter.getMergedTaint(ja));
            assertNullOrEmpty(MultiTainter.getMergedTaint(za));
            assertNullOrEmpty(MultiTainter.getMergedTaint(sa));
            assertNullOrEmpty(MultiTainter.getMergedTaint(da));
            assertNullOrEmpty(MultiTainter.getMergedTaint(ba));
            assertNullOrEmpty(MultiTainter.getMergedTaint(ca));
            assertNullOrEmpty(MultiTainter.getMergedTaint(fa));
        }

        /* Returns the names of this class' fields without using reflection. */
        public static String[] fieldNames() {
            return new String[]{"ia", "ja", "za", "sa", "da", "ba", "ca", "fa"};
        }

        /* Returns this class' fields without calling getDeclaredFields. */
        public static Field[] fields() throws NoSuchFieldException {
            return getFields(PrimitiveArrayObjHolder.class, fieldNames());
        }
    }

    /* Returns the specified class' fields without calling getDeclaredFields. */
    private static Field[] getFields(Class<?> clazz, String[] names) throws NoSuchFieldException {
        Field[] fields = new Field[names.length];
        for (int i = 0; i < fields.length; i++) {
            fields[i] = clazz.getDeclaredField(names[i]);
            fields[i].setAccessible(true);
        }
        return fields;
    }
}
