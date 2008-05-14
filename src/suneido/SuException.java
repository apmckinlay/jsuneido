package suneido;

public class SuException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	String s;
	public SuException(String s) {
		this.s = s;
	}
	public String toString() {
		return s;
	}
}
