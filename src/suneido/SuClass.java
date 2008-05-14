package suneido;

public class SuClass extends SuValue {

	@Override
	public String toString() {
		return "a Suneido class";
	}

	public SuValue invoke(int method, SuValue ... args) {
		return invoke2(method, args);
	}
	public SuValue invoke2(int method, SuValue[] args) {
		if (method == SuSymbol.DEFAULT)
			throw unknown_method(method);
		return invoke2(SuSymbol.DEFAULT, args);
	}
	//TODO massage arguments
	//TODO data members
}
