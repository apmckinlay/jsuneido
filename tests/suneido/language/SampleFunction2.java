package suneido.language;

import suneido.SuValue;

public class SampleFunction2 extends SuFunction {
	@Override
	public String toString() {
		return "SampleFunction";
	}

	@Override
	public SuValue invoke(SuValue... args) {
		SuValue[] constants = Constants.get("SampleFunction");
		//		System.out.println("hello world");
		Globals.get("X");
		SuValue a = null, b = null, c = null;
		invokeN(SuClass.EACH, a);
		return null;
	}

	private void f(int... args) {

	}

}