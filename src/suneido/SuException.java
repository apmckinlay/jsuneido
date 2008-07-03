package suneido;

/**
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved. Licensed under GPLv2.</small></p>
 */
public class SuException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	String s;

	public SuException(String s) {
		this.s = s;
	}

	public SuException(String s, Throwable e) {
		this.s = s;
		initCause(e);
	}

	@Override
	public String toString() {
		return s;
	}

	public static final SuException unreachable() {
		return new SuException("should not reach here");
	}
}
