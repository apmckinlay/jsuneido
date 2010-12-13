package suneido.language.builtin;

import static suneido.language.UserDefined.userDefined;
import static suneido.util.Util.array;

import java.util.*;

import suneido.SuException;
import suneido.language.*;
import suneido.util.DateParse;

public class DateClass extends BuiltinClass {

	private static final Object nil = new Object();

	private static final FunctionSpec dateFS =
			new FunctionSpec(array("string", "pattern",
					"year", "month", "day",
					"hour",	"minute", "second", "millisecond"),
					nil, nil, nil, nil, nil, nil, nil, nil, nil);

	@Override
	public Object newInstance(Object[] args) {
		args = Args.massage(dateFS, args);
		if (args[0] != nil && hasFields(args))
			throw new SuException(
					"usage: Date() or Date(string [, pattern]) or "
							+ "Date(year:, month:, day:, hour:, minute:, second:)");
		if (args[0] != nil) {
			Date d;
			if (args[1] == nil)
				d = DateParse.parse(Ops.toStr(args[0]));
			else
				d = DateParse.parse(Ops.toStr(args[0]), Ops.toStr(args[1]));
			return d == null ? false : d;
		} else if (hasFields(args)) {
			return named(args);
		} else
			return new Date();
	}
	private boolean hasFields(Object[] args) {
		for (int i = 2; i <= 8; ++i)
			if (args[i] != nil)
				return true;
		return false;
	}
	private Date named(Object[] args) {
		Calendar c = Calendar.getInstance();
		c.setTime(new Date());
		if (args[2] != nil)
			c.set(Calendar.YEAR, Ops.toInt(args[2]));
		if (args[3] != nil)
			c.set(Calendar.MONTH, Ops.toInt(args[3]) - 1);
		if (args[4] != nil)
			c.set(Calendar.DAY_OF_MONTH, Ops.toInt(args[4]));
		if (args[5] != nil)
			c.set(Calendar.HOUR_OF_DAY, Ops.toInt(args[5]));
		if (args[6] != nil)
			c.set(Calendar.MINUTE, Ops.toInt(args[6]));
		if (args[7] != nil)
			c.set(Calendar.SECOND, Ops.toInt(args[7]));
		if (args[8] != nil)
			c.set(Calendar.MILLISECOND, Ops.toInt(args[8]));
		return c.getTime();
	}

	@Override
	public Object invoke(Object self, String method, Object... args) {
		if (method == "GetLocalGMTBias")
			return GetLocalGMTBias(self, args);
		return userDefined("Dates", self, method, args);
	}

	private static Object GetLocalGMTBias(Object self, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		int offset = TimeZone.getDefault().getOffset(new Date().getTime());
		return -offset / 60000; // convert from ms to minutes
	}

	@Override
	public Object get(Object member) {
		return DateMethods.singleton.get(member);
	}

}
