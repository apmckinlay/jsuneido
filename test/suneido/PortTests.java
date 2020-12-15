/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

import static suneido.compiler.Token.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.common.primitives.Booleans;

import suneido.compiler.CompileTest;
import suneido.compiler.ExecuteTest;
import suneido.compiler.ExecuteTest.Def;
import suneido.compiler.Lexer;
import suneido.compiler.Token;
import suneido.runtime.Builtins;
import suneido.runtime.OpsTest;
import suneido.runtime.PackTest;
import suneido.util.DnumTest;
import suneido.util.RegexTest;
import suneido.util.TrTest;

/**
 * Run tests defined in text files
 * that are portable across Suneido implementations.
 * <p>
 * Currently, there are implementations in jSuneido, gSuneido,
 * and in Suneido (stdlib) itself.
 * <p>
 * Expects tests in ../suneido_tests i.e. a sibling directory
 * <p>
 * Fixtures must be defined in each implementation for each type of test,
 * usually along with the other unit test code.
 * <p>
 * Tests are in a separate repo at github.com/apmckinlay/suneido_tests
 */
public class PortTests {
	final static String testdir = "../suneido_tests/";

	public static boolean runFile(String file) {
		String src;
		try {
			byte[] b = Files.readAllBytes(Paths.get(testdir, file));
			src = new String(b);
		} catch (Throwable e) {
			throw new RuntimeException("PortTests can't get " + testdir + file);
		}
		return new Parser(file, src).run();
	}

	private static class Parser {
		String filename;
		Lexer lxr;
		Token tok;
		String comment;

		Parser(String filename, String src) {
			this.filename = filename;
			lxr = new Lexer(src);
			next(true);
		}

		public boolean run() {
			boolean ok = true;
			while (tok != EOF) {
				ok = run1() && ok;
			}
			return ok;
		}

		private boolean run1() {
			match(AT, false); // '@'
			String name = lxr.getValue();
			match(IDENTIFIER, true);
			System.out.println(filename + ": " + name + ": " + comment);
			Test2 test;
			if (testmap.containsKey(name))
				test = testmap.get(name);
			else {
				System.out.println("\tMISSING TEST FIXTURE");
				test = null;
			}
			int n = 0;
			boolean ok = true;
			while (tok != EOF && tok != AT) {
				List<String> args = new ArrayList<>();
				List<Boolean> str = new ArrayList<>();
				while (true) {
					String text = lxr.getValue();
					if (tok == SUB) {
						next(false);
						text = "-" + lxr.getValue();
					}
					args.add(text);
					str.add(tok == Token.STRING);
					next(false);
					if (tok == COMMA)
						next(true);
					if (tok == EOF || tok == NEWLINE)
						break;
				}
				if (test == null) {
				} else if (!test.run(Booleans.toArray(str),
						args.toArray(new String[0]))) {
					ok = false;
					System.out.println("\tFAILED: " + args);
				} else
					n++;
				next(true);
			}
			if (test != null) {
				System.out.printf("\t%d passed\n", n);
			}
			return ok;
		}

		void match(Token expected, boolean skip) {
			if (tok != expected && lxr.getKeyword() != expected)
				throw new RuntimeException("PortTests syntax error on " +
						lxr.getValue());
			next(skip);
		}

		void next(boolean skip) {
			comment = "";
			boolean nl = false;
			while (true) {
				tok = lxr.nextAll();
				switch (tok) {
				case NEWLINE:
					if (!skip)
						return;
					nl = true;
				case WHITE:
					continue;
				case COMMENT:
					// capture trailing comment on same line
					if (!nl)
						comment = lxr.matched();
					continue;
				default:
					return;
				}
			}
		}
	}

	@FunctionalInterface
	public interface Test2 {
		boolean run(boolean[] str, String... args);
	}

	@FunctionalInterface
	public interface Test {
		boolean run(String... args);
	}

	static class Wrap implements Test2 {
		Test test;
		Wrap(Test test) {
			this.test = test;
		}
		@Override
		public boolean run(boolean[] str, String... args) {
			return test.run(args);
		}
	}

	private static HashMap<String, Test2> testmap = new HashMap<>();

	public static void addTest2(String name, Test2 test) {
		testmap.put(name, test);
	}

	public static void addTest(String name, Test test) {
		testmap.put(name, new Wrap(test));
	}

	public static void skipTest(String name) {
		testmap.put(name,  null);
	}

	public static void main(String[] args) {
		addTest("ptest", a -> a[0].equals(a[1]));
		addTest("tr", TrTest::pt_tr);
		addTest("regex_match", RegexTest::pt_regex_match);
		addTest("regex_replace", RegexTest::pt_regex_replace);
		addTest("dnum_add", DnumTest::pt_dnum_add);
		addTest("dnum_sub", DnumTest::pt_dnum_sub);
		addTest("dnum_mul", DnumTest::pt_dnum_mul);
		addTest("dnum_div", DnumTest::pt_dnum_div);
		addTest("dnum_cmp", DnumTest::pt_dnum_cmp);
		addTest("execute", ExecuteTest::pt_execute);
		addTest2("method", ExecuteTest::pt_method);
		addTest("lang_rangeto", ExecuteTest::pt_lang_rangeto);
		addTest("lang_rangelen", ExecuteTest::pt_lang_rangelen);
		addTest("compile", CompileTest::pt_compile);
		addTest("compare", OpsTest::pt_compare);
		addTest("compare_packed", PackTest::pt_compare_packed);
		Suneido.context.set("Def", Builtins.function(Def.class));

		Suneido.openDbms();
		System.out.println("'" + testdir + "'");
		var ok = true;
		for (String filename : new File(testdir).list())
			if (filename.endsWith(".test"))
				ok = runFile(filename) && ok;
		if (!ok)
			System.out.println("FAILURES");
	}

}
