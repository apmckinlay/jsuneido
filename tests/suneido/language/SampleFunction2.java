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
		return null;
	}

}