package suneido.language.builtin;

import static suneido.util.Util.array;
import suneido.language.*;

public class Adler32Function extends BuiltinFunction {

	private static FunctionSpec fs =
			new FunctionSpec(array("string", "prev"), 1);

	@Override
	public Object call(Object... args) {
		args = Args.massage(fs, args);
		String s = Ops.toStr(args[0]);
		int prev = Ops.toInt(args[1]);
		return adler32(prev, s);
	}

	private static final long BASE = 65521L; // largest prime smaller than 65536
	private static final int NMAX = 5552;
	// NMAX is the largest n such that 255n(n+1)/2 + (n+1)(BASE-1) <= 2^32-1

	// can't use Java Adler32 because it doesn't handle previous value
	int adler32(long adler, String s) {
	    long s1 = adler & 0xffff;
	    long s2 = (adler >> 16) & 0xffff;

		byte[] buf = s.getBytes();
		int i = 0;
		int len = s.length();
	    while (len > 0)
			{
	        int k = Math.min(len, NMAX);
	        len -= k;
			do
				{
				s1 += buf[i++];
				s2 += s1;
				} while (--k > 0);
	        s1 %= BASE;
	        s2 %= BASE;
			}
	    return (int) ((s2 << 16) | s1);
	}

}
