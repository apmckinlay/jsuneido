package suneido.language.builtin;

import java.io.File;

import suneido.language.*;

public class CreateDirectory extends SuFunction1 {
	{ params = FunctionSpec.string; }

	@Override
	public Object call1(Object a) {
		String path = Ops.toStr(a);
		return new File(path).mkdir();
	}

}
