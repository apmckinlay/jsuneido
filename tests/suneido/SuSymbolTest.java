package suneido;

import org.junit.Test;
import static org.junit.Assert.*;
import static suneido.Symbols.SuSymbol;

public class SuSymbolTest {
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
}
