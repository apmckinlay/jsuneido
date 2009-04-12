package suneido.language;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;

import org.junit.Test;

import suneido.SuContainer;


public class CompileConstantTest {

	@Test
	public void constant() {
		assertEquals(Boolean.TRUE, compile("true"));
		assertEquals(Boolean.FALSE, compile("false"));

		assertEquals(123, compile("123"));
		assertEquals(-123, compile("-123"));
		assertEquals(new BigDecimal("12.34"), compile("12.34"));

		assertEquals("hello world", compile("'hello world'"));
		assertEquals("symbol", compile("#symbol"));
		assertEquals("a symbol", compile("#'a symbol'"));
		assertEquals("identifier", compile("identifier"));

		assertEquals(Ops.stringToDate("20090310"), compile("#20090310"));
		assertEquals(Ops.stringToDate("20090310.1026"),
				compile("#20090310.1026"));

		SuContainer c = new SuContainer();
		c.append(12);
		c.put("ab", "cd");
		assertEquals(c, compile("#(12, ab: cd)"));

		SuContainer cc = new SuContainer();
		cc.append(0);
		cc.append(c);
		assertEquals(cc, compile("#(0, (12, ab: cd))"));
	}

	private Object compile(String s) {
		Lexer lexer = new Lexer(s);
		CompileGenerator generator = new CompileGenerator("Test");
		ParseConstant<Object, Generator<Object>> pc =
				new ParseConstant<Object, Generator<Object>>(lexer, generator);
		return pc.parse();
	}
}
