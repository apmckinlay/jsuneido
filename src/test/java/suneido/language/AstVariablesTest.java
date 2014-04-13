/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;

import java.util.Set;

import org.junit.Test;

import com.google.common.collect.ImmutableSet;

public class AstVariablesTest {

	@Test
	public void main() {
		test("x + y", "x", "y");
		test("for (x in F()) G()", "x");
		test(".f()", "this");
		test("try F() catch (x) G()", "x");
		test("f = function (a) { b }", "f");
		test("++x", "x");
		test("function (a, b) { }", "a", "b");
		test("function (@args) { }", "args");
		test("function (_a, .b, .C, ._d, ._E) { }", "a", "b", "c", "d", "e");
	}

	private static void test(String code, String... vars) {
		Set<String> expected = ImmutableSet.copyOf(vars);
		if (! code.startsWith("function"))
			code = "function () { " + code + "\n}";
		AstNode ast = Compiler.parse(code);
		assertEquals(expected, AstVariables.vars(ast));
	}

	@Test
	public void paramToName() {
		assertThat(AstVariables.paramToName("@name"), equalTo("name"));
		assertThat(AstVariables.paramToName(".name"), equalTo("name"));
		assertThat(AstVariables.paramToName(".Name"), equalTo("name"));
		assertThat(AstVariables.paramToName("_name"), equalTo("name"));
		assertThat(AstVariables.paramToName("._name"), equalTo("name"));
		assertThat(AstVariables.paramToName("._Name"), equalTo("name"));
		assertThat(AstVariables.paramToName("Name"), equalTo("Name"));
	}

}
