package suneido;

import org.junit.Test;
import static org.junit.Assert.*;
import static suneido.Symbols.SuSymbol;

public class SymbolsTest {
	@Test
	public void test() {
		SuSymbol hello = Symbols.symbol("hello");
		assertSame(hello, Symbols.symbol("hello"));
		assertEquals(hello, Symbols.symbol("hello"));
		assertEquals(new SuString("hello"), hello);
		assertEquals(hello, new SuString("hello"));
		assertSame(hello, Symbols.symbol(hello.symnum()));
		assertFalse(hello.equals(Symbols.symbol("Hello")));
	}
	
	@Test
	public void call() {
		SuString s = new SuString("hello world");
		SuSymbol substr = Symbols.symbol("Substr");
		assertEquals(Symbols.SUBSTR, substr.symnum());
		assertEquals(new SuString("world"), 
				substr.invoke(Symbols.CALLi, s, SuInteger.from(6)));
	}
}
