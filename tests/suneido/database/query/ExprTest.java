package suneido.database.query;

import static org.junit.Assert.assertEquals;
import static suneido.Util.list;

import java.util.Collections;

import org.junit.Test;

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
			};
			for (int i = 0; i < cases.length; i += 2) {
				Expr e = ParseQuery.expr(cases[i]);
				assertEquals(cases[i + 1], e.fold().toString());
			}
	}
}
