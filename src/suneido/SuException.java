/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

import suneido.language.Ops;

// TODO use RuntimeException message and cause instead of s

public class SuException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	Object s; // could be String or Except

	public SuException(Object e) {
		this.s = e;
		//printStackTrace();
	}

	public SuException(String s, Throwable e) {
		this.s = s + " (" + e + ")";
		initCause(e);
		//printStackTrace();
	}

	public Object get() {
		return s;
	}

	@Override
	public String toString() {
		return s.toString();
	}

	public static final SuException unreachable() {
		return new SuException("should not reach here");
	}

	public static final SuException methodNotFound(Object object, String method) {
		return new SuException("method not found: "
				+ Ops.typeName(object) + "." + method
				+ " (" + object + ")");
	}

	public static void fatal(String msg) {
		throw new SuException("FATAL " + msg);
	}

}
