/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

import static suneido.compiler.Token.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import suneido.compiler.Compiler;
import suneido.compiler.Lexer;
import suneido.compiler.Token;
import suneido.runtime.Ops;
import suneido.util.DnumTest;
import suneido.util.Regex;
import suneido.util.Tr;

/** Run portable tests defined in text files */
public class PortTests {
	static class TestDir {
		final static String path = findTestDir(); // lazy initialization

		private static String findTestDir() {
			String file = "ptestdir.txt";
			for (int i = 0; i < 8; i++) {
				try {
					byte[] b = Files.readAllBytes(Paths.get(file));
					return new String(b).trim();
				} catch (Throwable e) {
				}
				file = "../" + file;
			}
			throw new RuntimeException("PortTests could not file ptestdir.txt");
		}
	}

	public static boolean runFile(String file) {
		String src;
		try {
			byte[] b = Files.readAllBytes(Paths.get(TestDir.path, file));
			src = new String(b);
		} catch (Throwable e) {
			throw new RuntimeException("PortTests can't get " + TestDir.path + file);
		}
		return new Parser(src).run();
	}

	private static class Parser {
		Lexer lxr;
		Token tok;

		Parser(String src) {
			lxr = new Lexer(src);
			tok = lxr.next();
		}

		public boolean run() {
			boolean ok = true;
			while (tok != EOF) {
				ok = ok && run1();
			}
			return ok;
		}

		private boolean run1() {
			match(AT, false); // '@'
			String name = lxr.getValue();
			match(IDENTIFIER, true);
			System.out.println(name + ":");
			Test test;
			if (testmap.containsKey(name))
				test = testmap.get(name);
			else {
				System.out.println("\tMISSING: '" + name + "'");
				test = (args) -> false;
			}
			int n = 0;
			boolean ok = true;
			while (tok != EOF && tok != AT) {
				List<String> args = new ArrayList<>();
				while (true) {
					String text = lxr.getValue();
					if (tok == SUB) {
						next(false);
						text = "-" + lxr.getValue();
					}
					args.add(text);
					next(false);
					if (tok == COMMA)
						next(true);
					if (tok == EOF || tok == NEWLINE)
						break;
				}
				if (!test.run(args.toArray(new String[0]))) {
					ok = false;
					System.out.println("\tFAILED: " + args);
				}
				next(true);
				n++;
			}
			if (ok) {
				System.out.printf("\tok (%d)\n", n);
			}
			return ok;
		}

		void match(Token expected, boolean skip) {
			if (tok != expected && lxr.getKeyword() != expected)
				throw new RuntimeException("PortTests syntax error on " + lxr.getValue());
			next(skip);
		}

		void next(boolean skip) {
			while (true) {
				tok = lxr.next();
				switch (tok) {
				case NEWLINE:
					if (!skip)
						return;
				case WHITE:
				case COMMENT:
					continue;
				default:
					return;
				}
			}
		}
	}

	@FunctionalInterface
	public interface Test {
		boolean run(String... args);
	}

	private static HashMap<String, Test> testmap = new HashMap<>();

	public static void addTest(String name, Test test) {
		testmap.put(name, test);
	}

	static Test ptest = (args) -> args[0].equals(args[1]);

	static Test pt_tr_replace =
			(args) -> Tr.tr(args[0], args[1], args[2]).equals(args[3]);

	// pt_match is a ptest for matching
	// simple usage is two arguments, string and pattern
	// an optional third argument can be "false" for matches that should fail
	// or additional arguments can specify \0, \1, ...
	static boolean pt_regex_match(String... args) {
		Regex.Pattern pat = Regex.compile(args[1]);
		Regex.Result result = pat.firstMatch(args[0], 0);
		boolean ok = result != null;
		if (args.length > 2) {
			if (args[2].equals("false"))
				ok = ! ok;
			else if (result != null)
				for (int i = 2; i < args.length; ++i)
					ok = ok && args[i].equals(result.group(args[0], i - 2));
		}
		return ok;
	}

	static boolean pt_execute(String... args) {
		Ops.default_single_quotes = true;
		try {
			String result = Ops.display(Compiler.eval(args[0]));
			String expected = "true";
			if (args.length > 1)
				expected = args[1];
			boolean ok = result.equals(expected);
			if (! ok) {
				System.out.println("for: " + Arrays.toString(args));
				System.out.println("got: " + result);
				System.out.println("expected: " + expected);
			}
			return ok;
		} finally {
			Ops.default_single_quotes = false;
		}
	}

	public static void main(String[] args) {
		//System.out.println("'" + TestDir.path + "'");
		addTest("ptest", ptest);
		runFile("ptest.test");
		addTest("tr_replace", pt_tr_replace);
		runFile("tr.test");
		addTest("regex_match", PortTests::pt_regex_match);
		runFile("regex.test");
		addTest("execute", PortTests::pt_execute);
		runFile("execute.test");
		addTest("dnum_add", DnumTest::pt_dnum_add);
		runFile("dnum.test");
	}

}
