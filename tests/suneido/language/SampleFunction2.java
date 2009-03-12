package suneido.language;

import suneido.SuInteger;
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
		args[1] = SuInteger.valueOf(123);
		return null;
	}

}