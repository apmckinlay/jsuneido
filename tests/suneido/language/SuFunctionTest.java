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
		assertEquals(s, f.invoke(s));
	}

	static class MyFunc extends SuFunction {

		@Override
		public SuValue invoke(SuValue... args) {
			massage(args, "value");
			return args[0];
		}

		@Override
		public String toString() {
			return "MyFunc";
		}

	}
}
