package suneido.language.builtin;

import static suneido.util.Util.array;

import java.util.Calendar;
import java.util.Date;

import suneido.language.*;

public class DateClass extends BuiltinClass {

	private static Object nil = new Object();

	private static final FunctionSpec dateFS =
			new FunctionSpec(array("string", "year", "month", "day", "hour",
					"minute", "second", "millisecond"), Boolean.FALSE, nil,
					nil, nil, nil, nil, nil, nil);

	@Override
	public Object newInstance(Object[] args) {
		args = Args.massage(dateFS, args);
		if (args[0] != Boolean.FALSE) {
			Date d = Ops.stringToDate(Ops.toStr(args[0]));
			return d == null ? false : d;
		} else {
			for (int i = 1; i <= 7; ++i)
				if (args[i] != nil)
					return named(args);
		}
		return new Date();
		// TODO parse english dates
	}

	private Object named(Object[] args) {
		Calendar c = Calendar.getInstance();
		c.setTime(new Date());
		if (args[1] != nil)
			c.set(Calendar.YEAR, Ops.toInt(args[1]));
		if (args[2] != nil)
			c.set(Calendar.MONTH, Ops.toInt(args[2]) - 1);
		if (args[3] != nil)
			c.set(Calendar.DAY_OF_MONTH, Ops.toInt(args[3]));
		if (args[4] != nil)
			c.set(Calendar.HOUR_OF_DAY, Ops.toInt(args[4]));
		if (args[5] != nil)
			c.set(Calendar.MINUTE, Ops.toInt(args[5]));
		if (args[6] != nil)
			c.set(Calendar.SECOND, Ops.toInt(args[6]));
		if (args[7] != nil)
			c.set(Calendar.MILLISECOND, Ops.toInt(args[7]));
		return c.getTime();
	}

}
