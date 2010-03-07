package suneido;

import suneido.language.Ops;

/**
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.</small></p>
 */
public class SuException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	String s;

	public SuException(Object e) {
		this.s = e.toString();
		//printStackTrace();
	}

	public SuException(String s, Throwable e) {
		this.s = s;
		initCause(e);
		//printStackTrace();
	}

	@Override
	public String toString() {
		return s;
	}

	public static final SuException unreachable() {
		return new SuException("should not reach here");
	}

	public static final SuException methodNotFound(Object object, String method) {
		return new SuException("method not found: "
				+ lowerFirst(Ops.typeName(object)) + "." + method
				+ " (" + object + ")");
	}

	public static String lowerFirst(String s) {
		return s.substring(0, 1).toLowerCase() + s.substring(1);
	}

	public static void fatal(String msg) {
		throw new SuException("FATAL " + msg);
	}

	/**
	 * Similar to assert, but always enabled
	 * so it may be used around actual code.
	 * @param expr
	 */
	public static void verify(boolean expr) {
		if (! expr)
			throw new SuException("assertion failed");
	}

	/**
	 * Similar to assert, but always enabled
	 * so it may be used around actual code.
	 * @param expr
	 * @param msg An additional explanatory message.
	 */
	public static void verify(boolean expr, String msg) {
		if (! expr)
			throw new SuException("assertion failed - " + msg);
	}

	public static void verifyEquals(Object expected, Object actual) {
		if (!expected.equals(actual))
			throw new SuException("verify failed: expected " + expected
					+ " got: " + actual);
	}

}
