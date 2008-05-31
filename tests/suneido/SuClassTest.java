package suneido;

import org.junit.Test;
import static org.junit.Assert.*;

public class SuClassTest {
	static class TestClass extends SuClass {
		public SuValue invoke2(SuValue self, int method, SuValue[] args) {
			switch (method) {
			case 1234:
				return TestClass.method1(self, args);
			case 5678:
				return TestClass.method2(self, args);
			default:
				return super.invoke2(self, method, args);
			}
		}
		public static SuValue method1(SuValue self, SuValue[] args) {
			return SuString.EMPTY;
		}
		public static SuValue method2(SuValue self, SuValue[] args) {
			return SuInteger.ZERO;
		}
	}
	
	@Test
	public void test() {
		SuValue c = new TestClass();
		assertEquals(SuString.EMPTY, c.invoke(1234));
		assertEquals(SuInteger.ZERO, c.invoke(5678));		
	}
	
	@Test(expected=SuException.class)
	public void unknown() {
		new TestClass().invoke(9999);
	}
	
	@Test
	public void massage() {
		SuValue[] empty = new SuValue[0];
		SuValue i = SuInteger.from(123);
		SuValue s = new SuString("hello");
		SuSymbol a = SuSymbol.symbol("a");
		SuSymbol x = SuSymbol.symbol("x");
		SuContainer c = new SuContainer();
		c.append(i);
		c.putdata(a, s);
		SuContainer c2 = new SuContainer();
		c2.append(s);
		c2.putdata(x, i);
		SuContainer c3 = new SuContainer();
		c3.append(i);
		c3.append(s);
		c3.putdata(a, s);
		c3.putdata(x, i);
		SuValue[] args1 = { i, SuSymbol.NAMED, a, s };
		SuValue[] args2 = { SuSymbol.EACH, c };
		SuValue[] args3 = { SuSymbol.EACH, c, SuSymbol.EACH, c2 };
		SuValue[] locals1 = { i };
		SuValue[] locals2 = { i, s };
		SuValue[] locals4 = { c };
		SuValue[] locals5 = { c3 };
		SuValue[] locals6 = { s, null };
		
		// function () () => []
		assertArrayEquals(empty, SuClass.massage(0, new SuValue[0]));
		assertArrayEquals(new SuValue[1], SuClass.massage(1, empty));
		// function (@args) () => []
		assertArrayEquals(empty, SuClass.massage(0, empty, SuSymbol.EACHi));
		assertArrayEquals(new SuValue[1], SuClass.massage(1, empty, SuSymbol.EACHi));
		// function (@args) (123, a: "hello") => [[123, a: "hello]]
		assertArrayEquals(locals4, SuClass.massage(1, args1, SuSymbol.EACHi));
		// function (@args) (@(123, a: "hello")) => [[123, a: "hello]]
		assertArrayEquals(locals4, SuClass.massage(1, args2, SuSymbol.EACHi));
		// function (@args) (@(123, a: "hello"), @("hello", x: 123))
		//		=> [[123, "hello", a: "hello", x: 123]]
		assertArrayEquals(locals5, SuClass.massage(1, args3, SuSymbol.EACHi));
		// function (x) (123, a: "hello") => [123]
		assertArrayEquals(locals1, SuClass.massage(1, args1, x.symnum()));
		// function (x, a) (123, a: "hello") => [123]
		assertArrayEquals(locals2, SuClass.massage(2, args1, x.symnum(), a.symnum()));	
		// function (a, x) (123, a: "hello") => ["hello", null]
		assertArrayEquals(locals6, SuClass.massage(2, args1, a.symnum(), x.symnum()));
		// function (x, a) (@(123, a: "hello")) => [123, "hello"]
		assertArrayEquals(locals2, SuClass.massage(2, args2, x.symnum(), a.symnum()));
		// function (x, a) (@(123, a: "hello")) => too many arguments
		try {
			SuClass.massage(1, args3, x.symnum());
			fail();
		} catch (SuException e) { }
	}
}
