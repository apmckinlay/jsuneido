package suneido.language.builtin;

import java.nio.ByteBuffer;

import suneido.language.*;

public class Unpack extends BuiltinFunction {

	@Override
	public Object call(Object... args) {
		args = Args.massage(FunctionSpec.string, args);
		String s = Ops.toStr(args[0]);
		int n = s.length();
		ByteBuffer buf = ByteBuffer.allocate(n);
		for (int i = 0; i < n; ++i)
			buf.put((byte) s.charAt(i));
		buf.rewind();
		return suneido.language.Pack.unpack(buf);
	}

}
