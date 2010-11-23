/**
 *
 */
package suneido.language;

import java.util.HashMap;
import java.util.Map;

class TestClass {

	public static SuClass instance() {
		Map<String, Object> methods = new HashMap<String, Object>();
		methods.put("Substr", new Test_Substr());
		methods.put("Size", new Test_Size());
		return new SuClass("TestClass", null, methods);
	}

	private static class Test_Substr extends SuMethod {
		@Override
		public Object eval(Object self, Object... args) {
			return "";
		}
	}

	private static class Test_Size extends SuMethod {
		@Override
		public Object eval(Object self, Object... args) {
			return 0;
		}
	}

}