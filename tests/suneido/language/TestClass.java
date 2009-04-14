/**
 *
 */
package suneido.language;


class TestClass extends SampleClass {
	@Override
	public Object invoke(String method, Object... args) {
		if (method == "Substr")
			return TestClass.method1(args);
		else if (method == "Size")
			return TestClass.method2(args);
		else
			return super.invoke(method, args);
	}
	public static Object method1(Object[] args) {
		return "";
	}
	public static Object method2(Object[] args) {
		return 0;
	}
}