package suneido.language.builtin;

import java.nio.ByteBuffer;

import suneido.language.*;
import suneido.util.Util;

public class Unpack extends SuFunction1 {

	{ params = FunctionSpec.string; }

	@Override
	public Object call1(Object a) {
		String s = Ops.toStr(a);
		ByteBuffer buf = Util.stringToBuffer(s);
		return suneido.language.Pack.unpack(buf);
	}

}
