package suneido.language;

import java.util.Random;

public class SampleFunction extends SuFunction {

	@Override
	public Object call(Object self, Object... args) {
		return null;
	}

	public static void f(int[] a, int b) {
		switch (b + g()) {
		case 0:
			g();
		case 1:
			g();
		default:
			g();
		}
	}

	public static int g() {
		return new Random().nextInt();
	}

}