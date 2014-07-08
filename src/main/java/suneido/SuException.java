/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

import suneido.language.Ops;

public class SuException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public SuException(String s) {
		super(s);
	}

	public SuException(String s, Throwable e) {
		super(s + " (" + e + ")", e);
	}

	public SuException(Throwable e, String s) {
		super(s, e);
	}

	public SuException(Throwable e) {
		super(e);
	}

	public Object get() {
		return getMessage();
	}

	@Override
	public String toString() {
		return getMessage();
	}

	public static final SuException methodNotFound(Object object, String method) {
		return new SuException("method not found: "
				+ Ops.typeName(object) + "." + method
				+ " (" + object + ")");
	}
}
