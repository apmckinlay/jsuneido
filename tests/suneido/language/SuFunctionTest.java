package suneido.language;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suneido.SuString;
import suneido.SuValue;

public class SuFunctionTest {

	@Test
	public void test() {
		SuValue f = new MyFunc();
		SuString s = SuString.valueOf("fred");
		assertEquals(s, f.invokeN(s));
	}

	static class MyFunc extends SuFunction {
		static final FunctionSpec params = new FunctionSpec("",
				new String[] { "value" }, 1, new SuValue[0], 0);

		@Override
		public SuValue invoke(String method, SuValue... args) {
			massage(params, args);
			return args[0];
		}

		@Override
		public String toString() {
			return "MyFunc";
		}

		@Override
		public void setConstants(SuValue[][] c) {
		}
	}
}
