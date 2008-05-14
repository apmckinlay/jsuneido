package suneido;

import org.junit.Test;
import static org.junit.Assert.*;

public class SuSymbolTest {
	@Test
	public void test() {
		SuSymbol hello = SuSymbol.symbol("hello");
		assert hello == SuSymbol.symbol("hello");
		assertEquals(new SuString("hello"), hello);
		assert hello == SuSymbol.symbol(hello.symnum());
	}
}
