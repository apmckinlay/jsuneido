package suneido.language.builtin;

import java.nio.ByteBuffer;

import suneido.language.*;
import suneido.util.ByteBuffers;

public class Unpack extends SuFunction1 {

	{ params = FunctionSpec.string; }

	@Override
	public Object call1(Object a) {
		String s = Ops.toStr(a);
		ByteBuffer buf = ByteBuffers.stringToBuffer(s);
		return suneido.language.Pack.unpack(buf);
	}

}
