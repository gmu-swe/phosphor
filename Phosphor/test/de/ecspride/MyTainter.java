package de.ecspride;

import java.lang.reflect.Field;
import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;


public class MyTainter {

	private MyTainter() {}

	public static void taintObjects(Object obj) throws IllegalArgumentException, IllegalAccessException
	{
		// Tainting the object reference Moo
		MultiTainter.taintedObject(obj, new Taint<String>("tainted_" +  obj.toString()));
		Field[] fields = obj.getClass().getDeclaredFields();
		for (Field field : fields)
		{
			field.setAccessible(true);
			if (field.get(obj).getClass().isPrimitive())
			{
				// Tainting Moo.flag field of primitive type boolean
				field.setBoolean(obj, MultiTainter.taintedBoolean(field.getBoolean(obj), "tainted_" +  obj.toString()));
			}
			else
			{
				// Tainting reference Moo.f of type Foo
				MultiTainter.taintedObject(field.get(obj), new Taint<String>("tainted_" +  obj.toString()));
				Field[] fields_  = field.get(obj).getClass().getDeclaredFields();
				for (Field field_ :  fields_)
				{
					field_.setAccessible(true);
					if (field_.get(field.get(obj)).getClass().isPrimitive())
					{
						// Tainting Foo.x field of primitive type int
						field_.setInt(field.get(obj), MultiTainter.taintedInt(field.getInt(field.get(obj)), "tainted_" +  obj.toString()));
					}
					else
					{
						// Never reached
						MultiTainter.taintedObject(field_.get(field.get(obj)), new Taint<String>("tainted_" +  obj.toString()));
					}
				}
			}
		}
	}


}
