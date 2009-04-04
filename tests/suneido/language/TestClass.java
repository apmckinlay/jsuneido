/**
 *
 */
package suneido.language;

import suneido.*;

class TestClass extends SampleClass {
	@Override
	public SuValue invoke(String method, SuValue ... args) {
		if (method == "Substr")
			return TestClass.method1(args);
		else if (method == "Size")
			return TestClass.method2(args);
		else
			return super.invoke(method, args);
	}
	public static SuValue method1(SuValue[] args) {
		return SuString.EMPTY;
	}
	public static SuValue method2(SuValue[] args) {
		return SuInteger.ZERO;
	}
	@Override
	public SuClass newInstance() {
		return new TestClass();
	}
}