package suneido.language.builtin;

import suneido.language.BuiltinFunction0;

public class OperatingSystem extends BuiltinFunction0 {

	@Override
	public Object call0() {
		return System.getProperty("os.name");
	}

	public static void main(String[] args) {
		System.out.println("Operating System " + System.getProperty("os.name"));
	}

}
