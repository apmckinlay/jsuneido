package suneido;

public class Trace {
	public static final int NONE = 0;
	public static final int FUNCTIONS =		1 << 0;
	public static final int STATEMENTS =	1 << 1;
	public static final int OPCODES =		1 << 2;
	public static final int STACK =			1 << 3;
	public static final int LIBLOAD =		1 << 4;
	public static final int SLOWQUERY =		1 << 5;
	public static final int QUERY =			1 << 6;
	public static final int SYMBOL =		1 << 7;
	public static final int ALLINDEX =		1 << 8;
	public static final int TABLE =			1 << 9;
	public static final int SELECT =		1 << 10;
	public static final int TEMPINDEX =		1 << 11;
	public static final int QUERYOPT =		1 << 12;

	public static final int CONSOLE =		1 << 13;
	public static final int LOGFILE =		1 << 14;

	public static final int CLIENTSERVER =	1 << 15;
	public static final int EXCEPTIONS =	1 << 16;
	public static final int GLOBALS =		1 << 17;

	public static int flags = NONE;

	public static void trace(int type, String s) {
		if ((flags & type) != 0)
			System.out.println(s);
	}
}
