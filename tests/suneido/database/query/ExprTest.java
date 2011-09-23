package suneido.database.query;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static suneido.Suneido.dbpkg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import suneido.database.query.expr.Expr;
import suneido.intfc.database.Record;
import suneido.language.Ops;

public class ExprTest {

	@Before
	public void setQuoting() {
		Ops.default_single_quotes = true;
	}

	@Test
	public void fields() {
		Object cases[] = new Object[] {
				"123", Collections.emptyList(),
				"a", asList("a"),
				"-a", asList("a"),
				"a + b", asList("a", "b"),
				"a ? b : c", asList("a", "b", "c"),
				"a and b and c", asList("a", "b", "c"),
				"a or b or c", asList("a", "b", "c"),
				"f(a, b)", asList("a", "b"),
		};
		for (int i = 0; i < cases.length; i += 2) {
			Expr e = CompileQuery.expr((String) cases[i]);
			assertEquals(cases[i + 1], e.fields());
		}
	}

	@Test
	public void fold_test() {
		fold("a", "a");
		fold("f(a,b)", "f(a,b)");
		fold("f(1 + 2,  3 - 4)", "f(3,-1)");
		fold("123", "123");
		fold("not true", "false");
		fold("12 + 34", "46");
		fold("12 < 34", "true");
		fold("12 >= 34", "false");
		fold("'abc' =~ 'b'", "true");
		fold("3 | 0xa", "11");
		fold("true ? 12 : 34", "12");
		fold("false ? 12 : 34", "34");
		fold("true and 1 < 2 and true", "true");
		fold("true and 1 < 2 and false", "false");
		fold("a and 1 < 2 and true", "a");
		fold("false or 1 > 2 or false", "false");
		fold("false or 1 < 2 or false", "true");
		fold("a or 1 > 2 or false", "a");
		fold("1 + 2 + a in (2,3,4)", "(3 + a) in (2,3,4)");
		fold("1 + 2 in (2,3,4)", "true");
		fold("3 * 4 in (2,3,4)", "false");
	}

	private void fold(String expr, String expected) {
		Expr e = CompileQuery.expr(expr);
		assertEquals(expected, e.fold().toString());
	}

	@Test
	public void isTerm() {
		List<String> fields = asList("a", "b", "c");
		String truecases[] = new String[] { "a = 5", "5 > b", "b in (3,4,5)" };
		for (String s : truecases)
			assertTrue(s, CompileQuery.expr(s).isTerm(fields));
		String falsecases[] = new String[] { "a", "d = 5", "c =~ 'x'" };
		for (String s : falsecases)
			assertFalse(s, CompileQuery.expr(s).isTerm(fields));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void eval() {
		String cases[] = new String[] {
				"a + 10", "11",
				"a + -1", "0",
				"10 - b", "8",
				"b + c", "5",
				"d + c + b + a", "10",
				"d $ a", "'41'",
				"b * c", "6",
				"d / b", "2",
				"d % 3", "1",
				"a + b = c", "true",
				"a = 1", "true",
				"b != 2", "false",
				"a is 2", "false",
				"9 > d", "true",
				"c <= 3", "true",
				"b > 2", "false",
				"b > a", "true",
				"d in (3,4,5)", "true",
				"e < #20081216.152744828", "false",
				"e < #20081216.155544828", "true"
		};
		Header hdr = new Header(asList(asList("a"), asList("a", "b", "c", "d", "e")),
				asList("a", "b", "c", "d", "e"));
		Record key = dbpkg.recordBuilder().add(1).build();
		Record rec = dbpkg.recordBuilder().add(1).add(2).add(3).add(4).
				add(Ops.stringToDate("#20081216.153244828")).build();
		Row row = new Row(key, rec);
		for (int i = 0; i < cases.length; i += 2) {
			Expr e = CompileQuery.expr(cases[i]);
			assertEquals(e.toString(), cases[i + 1],
					Ops.display(e.eval(hdr, row)));
		}
	}

	@Test
	public void rename() {
		String cases[] = new String[] {
				"a = 1", "(a is 1)",
				"y", "yy",
				"a + x < y * b", "((a + xx) < (yy * b))",
				"a and z and b", "(a and zz and b)",
				"z < 6 ? x : a + y", "((zz < 6) ? xx : (a + yy))"
		};
		List<String> from = asList("x", "y", "z");
		List<String> to = asList("xx", "yy", "zz");
		for (int i = 0; i < cases.length; i += 2) {
			Expr e = CompileQuery.expr(cases[i]);
			e = e.rename(from, to);
			assertEquals(e.toString(), cases[i + 1], e.toString());
		}
	}

	@Test
	public void replace() {
		String cases[] = new String[] {
				"a = 1", "(a is 1)",
				"y", "yy",
				"a + x < y * b", "((a + (1 + x)) < (yy * b))",
				"a and z and b", "(a and '' and b)",
				"z < 6 ? x : a + y", "(('' < 6) ? (1 + x) : (a + yy))"
		};
		List<String> from = asList("x", "y", "z");
		List<Expr> to = new ArrayList<Expr>();
		for (String s : asList("1 + x", "yy", "''"))
			to.add(CompileQuery.expr(s));
		for (int i = 0; i < cases.length; i += 2) {
			Expr e = CompileQuery.expr(cases[i]);
			assertEquals(e.toString(), cases[i + 1],
					e.replace(from, to).toString());
		}
	}
}
