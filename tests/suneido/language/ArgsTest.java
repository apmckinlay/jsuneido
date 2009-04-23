package suneido.language;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;
import static suneido.language.Args.Special.EACH;
import static suneido.language.Args.Special.NAMED;
import static suneido.util.Util.array;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

import suneido.*;

public class ArgsTest {
	
	@Test
	public void massage() {
		Object[] empty = new Object[0];
		Object i = 6;
		Object s = "hello";
		String a = "a";
		String x = "x";
		SuContainer c = new SuContainer();
		SuContainer ias = new SuContainer();
		ias.append(i);
		ias.put(a, s);
		SuContainer sxi = new SuContainer();
		sxi.append(s);
		sxi.put(x, i);
		SuContainer isasxi = new SuContainer();
		isasxi.append(i);
		isasxi.append(s);
		isasxi.put(a, s);
		isasxi.put(x, i);

		//	 params					args							resulting locals
		good(f(),					empty,							empty);
		good(f(1),					empty,							new SuValue[1]);
		good(f("@args"),			empty,							array(c));
		good(f(1, "@args"),			empty,							array(c, null));
		good(f("@args"),			array(i, NAMED, a, s),			array(ias));
		good(f("@args"),			array(EACH, ias),				array(ias));
		good(f("@args"),			array(EACH, ias, EACH, sxi),	array(isasxi));
		good(f("x"),				array(i, NAMED, a, s),			array(i));
		good(f("x", "a"),			array(i, s),					array(i, s));
		good(f("x", "a"),			array(i, NAMED, a, s),			array(i, s));
		good(f("x", "a"),			array(EACH, ias),				array(i, s));
		good(f(1, "x"),				array(EACH, sxi),				array(i, null));
		good(f(),					array(EACH, c),					empty);
		good(f("x=6"),				empty,							array(i));
		good(f("a", "x=6"), 		array(s), 						array(s, i));
		good(f("a", "x=6"), 		array(i, s), 					array(i, s));
		good(f("a", "y=6", "x=2"),	array(i, NAMED, x, s), 			array(i, i, s));

		bad(f("x", "a"),	array(i, NAMED, x, s)); // missing a
		bad(f("x"),			empty); // too few arguments
		bad(f(),			array(s)); // too many arguments
		bad(f("x"),			array(EACH, ias, EACH, sxi)); // too many arguments
	}
	private void good(FunctionSpec f, Object[] args, Object[] locals) {
		assertArrayEquals(locals, Args.massage(f, args));
	}
	private void bad(FunctionSpec f, Object[] args) {
		try {
			Args.massage(f, args);
			fail();
		} catch (SuException e) {
		}
	}

	private FunctionSpec f(String... params) {
		return f(0, params);
	}

	private FunctionSpec f(int extra, String... params) {
		boolean atParam = (params.length == 1 && params[0].startsWith("@"));
		if (atParam)
			params[0] = params[0].substring(1, params[0].length());
		String[] locals = Arrays.copyOf(params, params.length + extra);
		for (int i = 0; i < extra; ++i)
			locals[params.length + i] = "local" + i;
		Object[] defaults = defaults(locals, params);
		return new FunctionSpec("", locals, params.length, defaults,
				defaults.length, atParam);
	}

	private Object[] defaults(String[] locals, String... params) {
		ArrayList<Object> defaults = new ArrayList<Object>();
		int j;
		for (int i = 0; i < params.length; ++i)
			if (-1 != (j = params[i].indexOf('='))) {
				locals[i] = params[i].substring(0, j);
				String s = params[i].substring(j + 1);
				defaults.add(Character.isDigit(s.charAt(0))
						? Ops.stringToNumber(s) : s);
			}
		return defaults.toArray(new Object[0]);
	}

}
