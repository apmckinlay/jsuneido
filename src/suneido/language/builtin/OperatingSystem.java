package suneido.language.builtin;

import suneido.language.SuFunction0;

public class OperatingSystem extends SuFunction0 {

	@Override
	public Object call0() {
		return System.getProperty("os.name");
	}

	public static void main(String[] args) {
		System.out.println("Operating System " + System.getProperty("os.name"));
	}

}
