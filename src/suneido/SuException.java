package suneido;

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
	public String toString() {
		return s;
	}
	public static final SuException unreachable() {
		return new SuException("should not reach here");
	}
}
