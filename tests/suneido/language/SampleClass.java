package suneido.language;

import suneido.SuValue;

public class SampleClass extends SuClass {

	@Override
	public SuClass newInstance(SuValue... args) {
		return new SampleClass();
	}

	@Override
	public String toString() {
		return "SampleClass";
	}

	@Override
	public SuValue invoke(String method, SuValue... args) {
		if (method == "call")
			return invoke(args);
		else
			return super.invoke(method, args);
	}

	@Override
	public SuValue invoke(SuValue... args) {
		return null;
	}


	@Override
	public SuValue methodDefault(SuValue[] args) {
		return null;
	}

}