import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;

import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.DoubleSupplier;
import java.util.function.UnaryOperator;

public class Foo {
    public static void main(String[] args) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, IOException, ClassNotFoundException {
//        DoubleSupplier supplier = ((DoubleSupplier) Foo::getFloat);
//        double d = supplier.getAsDouble();
//        Constructor<Foo> constructor = Foo.class.getConstructor(new Class[0]);
//        for(int i = 0; i < 10000; i++) {
//            String[] arr = new String[]{"a", "b"};
////            String[] newArr = Arrays.copyOf(arr, 5);
//            String[] newArr = (String[]) Array.newInstance(arr.getClass().getComponentType(), 5);
////            System.out.println(Arrays.toString(newArr));
//            constructor.newInstance();
//            System.out.println(i);
//        }
//        System.out.println(instances);

//        BiFunction<Integer, Integer, Integer> func = (x1, x2) -> x1 + x2;
//
//        Integer result = func.apply(2, 3);
//
//        System.out.println(result); // 5

        //AtomicLong al = new AtomicLong();
        //System.out.println(al.get());
        //
        ////System.out.println(al.getAndIncrement());
        ////System.out.println(al.getAndIncrement());
        ////System.out.println(al.getAndIncrement());
        //long l = 10;
        //System.out.println(Long.toString(l));

        //ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        //ObjectOutputStream outStream = new ObjectOutputStream(byteStream);
        ////outStream.writeInt(10);
        //outStream.writeObject(new int[10]);
        //outStream.close();
        //
        //ObjectInputStream inStream = new ObjectInputStream(new ByteArrayInputStream(byteStream.toByteArray()));
        ////int outI = inStream.readInt();
        //int[] ar = (int[]) inStream.readObject();
        //
        //inStream.close();
        //System.out.println(ar);
        int i = 0;

        String str = "Some tainted data " + (++i);
        System.out.println(str);
    }
}

