package suneido;

public class Trace {
	public static final int NONE = 0;
	public static final int CLIENTSERVER = 1 << 15;

	public static int flags = NONE;

	public static void trace(int type, String s) {
		if ((flags & type) != 0)
			System.out.println(s);
	}
}
