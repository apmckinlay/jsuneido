package suneido.database.query;

import static org.junit.Assert.*;
import static suneido.Util.list;

import java.util.*;

import org.junit.Test;

import suneido.SuDate;
import suneido.database.Record;
import suneido.database.query.expr.Expr;

public class ExprTest {
	@Test
	public void fields() {
		Object cases[] = new Object[] {
				"123", Collections.emptyList(),
				"a", list("a"),
				"-a", list("a"),
				"a + b", list("a", "b"),
				"a ? b : c", list("a", "b", "c"),
				"a and b and c", list("a", "b", "c"),
				"a or b or c", list("a", "b", "c"),
				"f(a, b)", list("a", "b"),
		};
		for (int i = 0; i < cases.length; i += 2) {
			Expr e = ParseQuery.expr((String) cases[i]);
			assertEquals(cases[i + 1], e.fields());
		}
	}

	@Test
	public void fold() {
		String cases[] = new String[] {
				"a", "a",
				"f(a,b)", "f(a,b)",
				"f(1 + 2,  3 - 4)", "f(3,-1)",
				"123", "123",
				"not true", "false",
				"12 + 34", "46",
				"12 < 34", "true", "12 >= 34", "false",
				"'abc' =~ 'b'", "true",
				"3 | 0xa", "11",
				"true ? 12 : 34", "12",
				"false ? 12 : 34", "34",
				"true and 1 < 2 and true", "true",
				"true and 1 < 2 and false", "false",
				"a and 1 < 2 and true", "a",
				"false or 1 > 2 or false",
				"false", "false or 1 < 2 or false", "true",
				"a or 1 > 2 or false", "a",
				"1 + 2 + a in (2,3,4)", "(3 + a) in (2,3,4)",
				"1 + 2 in (2,3,4)", "true",
				"3 * 4 in (2,3,4)", "false",
		};
		for (int i = 0; i < cases.length; i += 2) {
			Expr e = ParseQuery.expr(cases[i]);
			assertEquals(cases[i + 1], e.fold().toString());
		}
	}

	@Test
	public void isTerm() {
		List<String> fields = list("a", "b", "c");
		String truecases[] = new String[] { "a = 5", "5 > b", "b in (3,4,5)" };
		for (String s : truecases)
			assertTrue(s, ParseQuery.expr(s).isTerm(fields));
		String falsecases[] = new String[] { "a", "d = 5", "c =~ 'x'" };
		for (String s : falsecases)
			assertFalse(s, ParseQuery.expr(s).isTerm(fields));
	}

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
		Header hdr = new Header(list(list("a"), list("a", "b", "c", "d", "e")),
				list("a", "b", "c", "d", "e"));
		Record key = new Record().add(1);
		Record rec = new Record().add(1).add(2).add(3).add(4).
				add(SuDate.literal("#20081216.153244828"));
		Row row = new Row(key, rec);
		for (int i = 0; i < cases.length; i += 2) {
			Expr e = ParseQuery.expr(cases[i]);
			assertEquals(e.toString(), cases[i + 1],
					e.eval(hdr, row).toString());
		}
	}

	@Test
	public void rename() {
		String cases[] = new String[] {
				"a = 1", "(a = 1)",
				"y", "yy",
				"a + x < y * b", "((a + xx) < (yy * b))",
				"a and z and b", "(a and zz and b)",
				"z < 6 ? x : a + y", "((zz < 6) ? xx : (a + yy))"
		};
		List<String> from = list("x", "y", "z");
		List<String> to = list("xx", "yy", "zz");
		for (int i = 0; i < cases.length; i += 2) {
			Expr e = ParseQuery.expr(cases[i]);
			e = e.rename(from, to);
			assertEquals(e.toString(), cases[i + 1], e.toString());
		}
	}

	@Test
	public void replace() {
		String cases[] = new String[] {
				"a = 1", "(a = 1)",
				"y", "yy",
				"a + x < y * b", "((a + (1 + x)) < (yy * b))",
				"a and z and b", "(a and '' and b)",
				"z < 6 ? x : a + y", "(('' < 6) ? (1 + x) : (a + yy))"
		};
		List<String> from = list("x", "y", "z");
		List<Expr> to = new ArrayList<Expr>();
		for (String s : list("1 + x", "yy", "''"))
			to.add(ParseQuery.expr(s));
		for (int i = 0; i < cases.length; i += 2) {
			Expr e = ParseQuery.expr(cases[i]);
			assertEquals(e.toString(), cases[i + 1],
					e.replace(from, to).toString());
		}
	}
}
