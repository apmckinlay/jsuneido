package suneido;

import org.junit.Test;
import static org.junit.Assert.*;

public class SuSymbolTest {
	@Test
	public void test() {
		SuSymbol hello = SuSymbol.symbol("hello");
		assertSame(hello, SuSymbol.symbol("hello"));
		assertEquals(hello, SuSymbol.symbol("hello"));
		assertEquals(new SuString("hello"), hello);
		assertEquals(hello, new SuString("hello"));
		assertSame(hello, SuSymbol.symbol(hello.symnum()));
		assertFalse(hello.equals(SuSymbol.symbol("Hello")));
	}
}
