package suneido.database.query;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static suneido.Suneido.dbpkg;

import java.util.Collections;
import java.util.List;

import org.junit.After;
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

	@After
	public void restoreQuoting() {
		Ops.default_single_quotes = false;
	}

	@Test
	public void fields() {
		List<String> emptyList = Collections.emptyList();
		fields("123", emptyList);
		fields("a", asList("a"));
		fields("-a", asList("a"));
		fields("a + b", asList("a", "b"));
		fields("a ? b : c", asList("a", "b", "c"));
		fields("a and b and c", asList("a", "b", "c"));
		fields("a or b or c", asList("a", "b", "c"));
		fields("f(a, b)", asList("a", "b"));
	}
	private static void fields(String expr, List<String> expected) {
		Expr e = CompileQuery.expr(expr);
		assertEquals(expected, e.fields());
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
		fold("a in ()", "false");
		fold("!(a in ())", "true");
	}
	private static void fold(String expr, String expected) {
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

	private Header hdr;
	private Row row;

	@Test
	public void eval() {
		hdr = new Header(asList(asList("a"), asList("a", "b", "c", "d", "e")),
				asList("a", "b", "c", "d", "e"));
		Record key = dbpkg.recordBuilder().add(1).build();
		Record rec = dbpkg.recordBuilder().add(1).add(2).add(3).add(4).
				add(Ops.stringToDate("#20081216.153244828")).build();
		row = new Row(key, rec);
		eval("a + 10", "11");
		eval("a + -1", "0");
		eval("10 - b", "8");
		eval("b + c", "5");
		eval("d + c + b + a", "10");
		eval("d $ a", "'41'");
		eval("b * c", "6");
		eval("d / b", "2");
		eval("d % 3", "1");
		eval("a + b = c", "true");
		eval("a = 1", "true");
		eval("b != 2", "false");
		eval("a is 2", "false");
		eval("9 > d", "true");
		eval("c <= 3", "true");
		eval("b > 2", "false");
		eval("b > a", "true");
		eval("d in (3,4,5)", "true");
		eval("e < #20081216.152744828", "false");
		eval("e < #20081216.155544828", "true");
	}
	private void eval(String expr, String result) {
		Expr e = CompileQuery.expr(expr);
		assertEquals(e.toString(), result, Ops.display(e.eval(hdr, row)));
	}

	private final List<String> from = asList("x", "y", "z");
	private final List<String> to = asList("xx", "yy", "zz");

	@Test
	public void rename() {
		rename("a = 1", "(a is 1)");
		rename("y", "yy");
		rename("a + x < y * b", "((a + xx) < (yy * b))");
		rename("a and z and b", "(a and zz and b)");
		rename("z < 6 ? x : a + y", "((zz < 6) ? xx : (a + yy))");
	}
	private void rename(String expr, String expected) {
		Expr e = CompileQuery.expr(expr);
		e = e.rename(from, to);
		assertEquals(e.toString(), expected, e.toString());
	}

	private final List<Expr> eto = asList(
			CompileQuery.expr("1 + x"),
			CompileQuery.expr("yy"),
			CompileQuery.expr("''"));

	@Test
	public void replace() {
		replace("a = 1", "(a is 1)");
		replace("y", "yy");
		replace("a + x < y * b", "((a + (1 + x)) < (yy * b))");
		replace("a and z and b", "(a and '' and b)");
		replace("z < 6 ? x : a + y", "((6 > '') ? (1 + x) : (a + yy))");
	}
	private void replace(String expr, String expected) {
		Expr e = CompileQuery.expr(expr);
		assertEquals(e.toString(), expected, e.replace(from, eto).toString());
	}

}
