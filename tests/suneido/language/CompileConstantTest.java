package suneido.language;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suneido.*;


public class CompileConstantTest {

	@Test
	public void constant() {
		assertEquals(SuBoolean.TRUE, compile("true"));
		assertEquals(SuBoolean.FALSE, compile("false"));

		assertEquals(SuInteger.valueOf(123), compile("123"));
		assertEquals(SuInteger.valueOf(-123), compile("-123"));
		assertEquals(SuDecimal.valueOf("12.34"), compile("12.34"));

		assertEquals(SuString.valueOf("hello world"), compile("'hello world'"));
		assertEquals(SuString.valueOf("symbol"), compile("#symbol"));
		assertEquals(SuString.valueOf("a symbol"), compile("#'a symbol'"));
		assertEquals(SuString.valueOf("identifier"), compile("identifier"));

		assertEquals(SuDate.valueOf("20090310"), compile("#20090310"));
		assertEquals(SuDate.valueOf("20090310.1026"), compile("#20090310.1026"));

		SuContainer c = new SuContainer();
		c.append(SuInteger.valueOf(12));
		c.put("ab", SuString.valueOf("cd"));
		assertEquals(c, compile("#(12, ab: cd)"));

		SuContainer cc = new SuContainer();
		cc.append(SuInteger.ZERO);
		cc.append(c);
		assertEquals(cc, compile("#(0, (12, ab: cd))"));
	}

	private SuValue compile(String s) {
		Lexer lexer = new Lexer(s);
		CompileGenerator generator = new CompileGenerator();
		ParseConstant<Object, Generator<Object>> pc =
				new ParseConstant<Object, Generator<Object>>(lexer, generator);
		return (SuValue) pc.parse();
	}
}
