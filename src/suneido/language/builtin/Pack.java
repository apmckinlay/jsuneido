package suneido.language.builtin;

import java.nio.ByteBuffer;

import suneido.language.*;

/**
 * NOTE: inefficient space-wise - uses one char (2 bytes) per byte
 * 
 * @author Andrew McKinlay
 */
public class Pack extends SuFunction {

	private static final FunctionSpec fs = new FunctionSpec("string");

	@Override
	public Object call(Object... args) {
		args = Args.massage(fs, args);
		int n = suneido.language.Pack.packSize(args[0]);
		ByteBuffer buf = ByteBuffer.allocate(n);
		suneido.language.Pack.pack(args[0], buf);
		StringBuilder sb = new StringBuilder(n);
		for (int i = 0; i < n; ++i)
			sb.append((char) (buf.get(i) & 0xff));
		return sb.toString();
	}

}
