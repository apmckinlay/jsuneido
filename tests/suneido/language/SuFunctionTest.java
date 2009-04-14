package suneido.language;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SuFunctionTest {

	@Test
	public void test() {
		Object f = new MyFunc();
		String s = "fred";
		assertEquals(s, Ops.invoke(f, "call", s));
	}

	static class MyFunc extends SampleFunction {
		static final FunctionSpec params =
				new FunctionSpec("", new String[] { "value" }, 1);

		@Override
		public Object invoke(String method, Object... args) {
			Args.massage(params, args);
			return args[0];
		}
	}
}
