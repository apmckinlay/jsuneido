/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;
import static suneido.runtime.Args.Special.EACH;
import static suneido.runtime.Args.Special.NAMED;
import static suneido.util.Util.array;

import java.util.ArrayList;

import org.junit.Test;

import suneido.SuContainer;
import suneido.SuRecord;
import suneido.SuValue;
import suneido.runtime.Args;
import suneido.runtime.FunctionSpec;
import suneido.runtime.Numbers;

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
		ias.add(i);
		ias.put(a, s);
		SuContainer sxi = new SuContainer();
		sxi.add(s);
		sxi.put(x, i);
		SuContainer isasxi = new SuContainer();
		isasxi.add(i);
		isasxi.add(s);
		isasxi.put(a, s);
		isasxi.put(x, i);
		SuContainer isax = new SuRecord();
		isax.add(i);
		isax.add(s);
		isax.add(a);
		isax.add(x);

		//	 params				args						resulting locals
		good(f(),					empty,					empty);
		good(f(1),					empty,					new SuValue[1]);
		good(f("@args"),			empty,					array(c));
		good(f(1, "@args"),			empty,					array(c, null));
		good(f("@args"),			array(i, NAMED, a, s),		array(ias));
		good(f("@args"),			array(EACH, ias),			array(ias));
		good(f("@args"),			array(EACH, isax),			array(isax));
		good(f("i","s","a","x"),			array(EACH, isax),			array(i, s, a, x));
		good(f("@args"),			array(EACH, ias, EACH, sxi),	array(isasxi));
		good(f("x"),				array(i, NAMED, a, s),		array(i));
		good(f("x", "a"),				array(i, s),					array(i, s));
		good(f("x", "a"),				array(i, NAMED, a, s),		array(i, s));
		good(f("x", "a"),				array(EACH, ias),			array(i, s));
		good(f(1, "x"),				array(EACH, sxi),			array(i, null));
		good(f(),					array(EACH, c),			empty);
		good(f("x=6"),				empty,					array(i));
		good(f("a", "x=6"), 			array(s), 					array(s, i));
		good(f("a", "x=6"), 			array(i, s), 				array(i, s));
		good(f("a", "y=6", "x=2"),		array(i, NAMED, x, s), 		array(i, i, s));

		bad(f("x", "a"),	array(i, NAMED, x, s)); // missing a
		bad(f("x"),			empty); // too few arguments
		bad(f(),			array(s)); // too many arguments
		bad(f("x"),			array(EACH, ias, EACH, sxi)); // too many arguments
	}
	private static void good(FunctionSpec f, Object[] args, Object[] locals) {
		assertArrayEquals(locals, Args.massage(f, args));
	}
	private static void bad(FunctionSpec f, Object[] args) {
		try {
			Args.massage(f, args);
			fail();
		} catch (Exception e) {
		}
	}

	private static FunctionSpec f(String... params) {
		return f(0, params);
	}

	private static FunctionSpec f(int extra, String... params) {
		boolean atParam = (params.length == 1 && params[0].startsWith("@"));
		if (atParam)
			params[0] = params[0].substring(1, params[0].length());
		if (extra < 1) {
			return new FunctionSpec("", params, defaults(params), atParam, null);
		} else {
			String[] localsNames = new String[extra];
			for (int p = 0; p < extra; ++p) {
				localsNames[p] = "p" + extra;
			}
			return new ArgsArraySpec("", params, defaults(params), atParam,
					null, localsNames);
		}
	}

	private static Object[] defaults(String... params) {
		ArrayList<Object> defaults = new ArrayList<>();
		int j;
		for (int i = 0; i < params.length; ++i)
			if (-1 != (j = params[i].indexOf('='))) {
				String s = params[i].substring(j + 1);
				defaults.add(Character.isDigit(s.charAt(0))
						? Numbers.stringToNumber(s) : s);
				params[i] = params[i].substring(0, j);
			}
		return defaults.toArray(new Object[0]);
	}

}
