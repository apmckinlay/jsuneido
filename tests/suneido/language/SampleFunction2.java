package suneido.language;

import suneido.SuException;
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
		SuValue a = null, b = null, c = null;
		if (a == null)
			throw new SuException("uninitialized variable");
		return a;
	}

	private void f(int... args) {

	}

}