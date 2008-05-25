package suneido;

public class Suneido {
	public static void main(String args[]) {
		System.out.println("hello world");
	}
	
	/**
	 * Similar to assert, but always enabled
	 * so it may be used around actual code.
	 * @param expr
	 */
	public static void verify(boolean expr) {
		if (! expr)
			throw new SuException(new Throwable().getStackTrace()[1] + 
					" assertion failed");
	}

	/**
	 * Similar to assert, but always enabled
	 * so it may be used around actual code.
	 * @param expr
	 * @param msg An additional explanatory message.
	 */
	public static void verify(boolean expr, String msg) {
		if (! expr)
			throw new SuException(new Throwable().getStackTrace()[1] + 
					" assertion failed - " + msg);
	}
}
