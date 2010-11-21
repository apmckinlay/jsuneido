package suneido.language.builtin;

import suneido.language.*;

public class ObjectQ extends BuiltinFunction1 {

	@Override
	public Object call1(Object a) {
		return a instanceof SuInstance ||
				null != Ops.toContainer(a);
	}

}
