package suneido;

public class TestOS {
	public static void main(String[] args) {
		System.out.println("running on " +
				System.getProperty("os.name") + " - " +
				System.getProperty("os.version"));
	}
}
