package suneido;

import org.junit.Test;
import static org.junit.Assert.*;
import static suneido.Symbols.SuSymbol;

public class SuClassTest {
	static class TestClass extends SuClass {
		@Override
		public SuValue invoke(SuValue self, int method, SuValue ... args) {
			switch (method) {
			case Symbols.SUBSTR:
				return TestClass.method1(self, args);
			case Symbols.SIZE:
				return TestClass.method2(self, args);
			default:
				return super.invoke(self, method, args);
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
		assertEquals(SuString.EMPTY, c.invoke(Symbols.SUBSTR));
		assertEquals(SuInteger.ZERO, c.invoke(Symbols.SIZE));		
	}
	
	@Test(expected=SuException.class)
	public void unknown() {
		new TestClass().invoke(Symbols.CALLi);
	}
	
	@Test
	public void massage() {
		SuValue[] empty = new SuValue[0];
		SuValue i = SuInteger.from(123);
		SuValue s = new SuString("hello");
		SuSymbol a = Symbols.symbol("a");
		SuSymbol x = Symbols.symbol("x");
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
		SuValue[] args1 = { i, Symbols.NAMED, a, s };
		SuValue[] args2 = { Symbols.EACH, c };
		SuValue[] args3 = { Symbols.EACH, c, Symbols.EACH, c2 };
		SuValue[] locals1 = { i };
		SuValue[] locals2 = { i, s };
		SuValue[] locals4 = { c };
		SuValue[] locals5 = { c3 };
		SuValue[] locals6 = { s, null };
		
		// function () () => []
		assertArrayEquals(empty, SuClass.massage(0, new SuValue[0]));
		assertArrayEquals(new SuValue[1], SuClass.massage(1, empty));
		// function (@args) () => []
		assertArrayEquals(empty, SuClass.massage(0, empty, Symbols.EACHi));
		assertArrayEquals(new SuValue[1], SuClass.massage(1, empty, Symbols.EACHi));
		// function (@args) (123, a: "hello") => [[123, a: "hello]]
		assertArrayEquals(locals4, SuClass.massage(1, args1, Symbols.EACHi));
		// function (@args) (@(123, a: "hello")) => [[123, a: "hello]]
		assertArrayEquals(locals4, SuClass.massage(1, args2, Symbols.EACHi));
		// function (@args) (@(123, a: "hello"), @("hello", x: 123))
		//		=> [[123, "hello", a: "hello", x: 123]]
		assertArrayEquals(locals5, SuClass.massage(1, args3, Symbols.EACHi));
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
	
	@Test
	public void test_new() {
		DefaultClass dc = new DefaultClass();
		SuValue instance = dc.invoke(Symbols.INSTANTIATE);
		assertEquals(SuString.EMPTY, instance.invoke(Symbols.SUBSTR));
		assertArrayEquals(new SuValue[] { Symbols.symbol(Symbols.SUBSTR) }, dc.args);
		instance.invoke(Symbols.SUBSTR, SuInteger.ONE);
		assertArrayEquals(new SuValue[] { Symbols.symbol(Symbols.SUBSTR), SuInteger.ONE }, dc.args);
	}
	static class DefaultClass extends SuClass {
		public SuValue[] args;
		@Override
		public SuValue methodDefault(SuValue self, SuValue[] args) {
			this.args = args;
			return SuString.EMPTY;
		}
	}
	
	@Test
	public void test_inheritance() {
		Globals.set(Globals.num("DefaultClass"), new DefaultClass());
		SuValue subClass = new SubClass();
		SuValue instance = subClass.invoke(Symbols.INSTANTIATE);
		assertTrue(instance instanceof SuInstance);
		assertEquals(subClass, ((SuInstance) instance).myclass);
		assertEquals(SuInteger.from(99), instance.invoke(Symbols.SIZE));
	}
	static class SubClass extends SuClass {
		final static int parent = Globals.num("DefaultClass"); // global index
		@Override
		public SuValue invoke(SuValue self, int method, SuValue ... args) {
			switch (method) {
			case Symbols.SIZE :
				return SuInteger.from(99);
			default :
				return Globals.get(parent).invoke(self, method, args);
			}
		}
	}
}
