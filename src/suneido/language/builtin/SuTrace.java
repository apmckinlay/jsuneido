package suneido.language.builtin;

import static suneido.Trace.Type.CONSOLE;
import static suneido.Trace.Type.LOGFILE;
import static suneido.util.Util.array;
import suneido.Trace;
import suneido.language.FunctionSpec;
import suneido.language.Ops;
import suneido.language.SuFunction2;

public class SuTrace extends SuFunction2 {

	{ params = new FunctionSpec(array("flags", "block"), false); }

	@Override
	public Object call2(Object a, Object b) {
		if (Ops.isString(a))
			Trace.println(Ops.toStr(a));
		else {
			int flags = Ops.toInt(a);
			if (0 == (flags & (CONSOLE.bit | LOGFILE.bit)))
				flags |= CONSOLE.bit | LOGFILE.bit;
			Trace.flags = flags;
			if (b != Boolean.FALSE)
				return Ops.call(b);
		}
		return null;
	}

}
