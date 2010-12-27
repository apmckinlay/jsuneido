package suneido.language.builtin;

import static suneido.util.Util.array;
import suneido.language.*;

public class Trace extends SuFunction2 {

	{ params = new FunctionSpec(array("flags", "block"), false); }

	@Override
	public Object call2(Object a, Object b) {
		int flags = Ops.toInt(a);
		suneido.Trace.flags = flags;
		if (b != Boolean.FALSE)
			return Ops.call(b);
		return suneido.Trace.flags;
	}

}
