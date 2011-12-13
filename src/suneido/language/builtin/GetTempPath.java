package suneido.language.builtin;

import suneido.language.SuFunction0;

public class GetTempPath extends SuFunction0 {

	@Override
	public Object call0() {
		return System.getProperty("java.io.tmpdir").replace('\\', '/');
	}

	public static void main(String[] args) {
		System.out.println(new GetTempPath().call());
	}

}
