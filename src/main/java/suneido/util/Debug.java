package suneido.util;

public class Debug {

	public static void printStackTrace(String msg) {
		System.out.println(msg);
		printStackTrace();
	}

	public static void printStackTrace() {
		try {
			throw new RuntimeException();
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
	}

}
