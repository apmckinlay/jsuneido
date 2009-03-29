/**
 *
 */
package suneido.language;

import suneido.*;

class TestClass extends SuClass {
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
	static final FunctionSpec noParams =
			new FunctionSpec(new String[0], 0, new SuValue[0], 0);
	@Override
	public SuClass newInstance(SuValue... args) {
		massage(noParams, args);
		return new TestClass();
	}
	@Override
	public String toString() {
		return "TestClass";
	}
	@Override
	public void setConstants(SuValue[][] c) {
	}
}