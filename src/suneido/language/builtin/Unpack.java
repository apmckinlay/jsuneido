package suneido.language.builtin;

import java.nio.ByteBuffer;

import suneido.language.*;

public class Unpack extends BuiltinFunction {

	private static final FunctionSpec fs = new FunctionSpec("string");

	@Override
	public Object call(Object... args) {
		args = Args.massage(fs, args);
		String s = Ops.toStr(args[0]);
		int n = s.length();
		ByteBuffer buf = ByteBuffer.allocate(n);
		for (int i = 0; i < n; ++i)
			buf.put((byte) s.charAt(i));
		buf.rewind();
		return suneido.language.Pack.unpack(buf);
	}

}
