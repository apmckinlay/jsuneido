/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import org.junit.Test;

public class ParseTest {

	@Test
	public void test() {
		code("123",
			"(NUMBER=123)");
		code("foo",
			"(IDENTIFIER=foo)");
		code("a ? b : c",
			"(Q_MARK (IDENTIFIER=a) (IDENTIFIER=b) (IDENTIFIER=c))");
		code("a or b or c",
			"(OR (IDENTIFIER=a) (IDENTIFIER=b) (IDENTIFIER=c))");
		code("a and b and c",
			"(AND (IDENTIFIER=a) (IDENTIFIER=b) (IDENTIFIER=c))");
		code("a | b | c",
			"(BINARYOP (BITOR) (BINARYOP (BITOR) (IDENTIFIER=a) (IDENTIFIER=b)) (IDENTIFIER=c))");
		code("a ^ b ^ c",
			"(BINARYOP (BITXOR) (BINARYOP (BITXOR) (IDENTIFIER=a) (IDENTIFIER=b)) (IDENTIFIER=c))");
		code("a & b & c",
			"(BINARYOP (BITAND) (BINARYOP (BITAND) (IDENTIFIER=a) (IDENTIFIER=b)) (IDENTIFIER=c))");
		code("a == b == c",
			"(BINARYOP (IS) (BINARYOP (IS) (IDENTIFIER=a) (IDENTIFIER=b)) (IDENTIFIER=c))");
		code("a != b != c",
			"(BINARYOP (ISNT) (BINARYOP (ISNT) (IDENTIFIER=a) (IDENTIFIER=b)) (IDENTIFIER=c))");
		code("a =~ b =~ c",
			"(BINARYOP (MATCH) (BINARYOP (MATCH) (IDENTIFIER=a) (IDENTIFIER=b)) (IDENTIFIER=c))");
		code("a !~ b !~ c",
			"(BINARYOP (MATCHNOT) (BINARYOP (MATCHNOT) (IDENTIFIER=a) (IDENTIFIER=b)) (IDENTIFIER=c))");
		code("a < b < c",
			"(BINARYOP (LT) (BINARYOP (LT) (IDENTIFIER=a) (IDENTIFIER=b)) (IDENTIFIER=c))");
		code("a <= b <= c",
			"(BINARYOP (LTE) (BINARYOP (LTE) (IDENTIFIER=a) (IDENTIFIER=b)) (IDENTIFIER=c))");
		code("a > b > c",
			"(BINARYOP (GT) (BINARYOP (GT) (IDENTIFIER=a) (IDENTIFIER=b)) (IDENTIFIER=c))");
		code("a >= b >= c",
			"(BINARYOP (GTE) (BINARYOP (GTE) (IDENTIFIER=a) (IDENTIFIER=b)) (IDENTIFIER=c))");
		code("a >> b >> c",
			"(BINARYOP (RSHIFT) (BINARYOP (RSHIFT) (IDENTIFIER=a) (IDENTIFIER=b)) (IDENTIFIER=c))");
		code("a << b << c",
			"(BINARYOP (LSHIFT) (BINARYOP (LSHIFT) (IDENTIFIER=a) (IDENTIFIER=b)) (IDENTIFIER=c))");
		code("a + b + c",
			"(BINARYOP (ADD) (BINARYOP (ADD) (IDENTIFIER=a) (IDENTIFIER=b)) (IDENTIFIER=c))");
		code("a - b - c",
			"(BINARYOP (SUB) (BINARYOP (SUB) (IDENTIFIER=a) (IDENTIFIER=b)) (IDENTIFIER=c))");
		code("a $ b $ c",
			"(BINARYOP (CAT) (BINARYOP (CAT) (IDENTIFIER=a) (IDENTIFIER=b)) (IDENTIFIER=c))");
		code("a * b * c",
			"(BINARYOP (MUL) (BINARYOP (MUL) (IDENTIFIER=a) (IDENTIFIER=b)) (IDENTIFIER=c))");
		code("a / b / c",
			"(BINARYOP (DIV) (BINARYOP (DIV) (IDENTIFIER=a) (IDENTIFIER=b)) (IDENTIFIER=c))");
		code("a % b % c",
			"(BINARYOP (MOD) (BINARYOP (MOD) (IDENTIFIER=a) (IDENTIFIER=b)) (IDENTIFIER=c))");
		code("a * b + c * d",
			"(BINARYOP (ADD) (BINARYOP (MUL) (IDENTIFIER=a) (IDENTIFIER=b)) (BINARYOP (MUL) (IDENTIFIER=c) (IDENTIFIER=d)))");
		code("a * (b + c) * d",
			"(BINARYOP (MUL) (BINARYOP (MUL) (IDENTIFIER=a) (RVALUE (BINARYOP (ADD) (IDENTIFIER=b) (IDENTIFIER=c)))) (IDENTIFIER=d))");
		code("+ - ! ~ x",
			"(ADD (SUB (NOT (BITNOT (IDENTIFIER=x)))))");
		code("--x",
			"(PREINCDEC (DEC) (IDENTIFIER=x))");
		code("++x",
			"(PREINCDEC (INC) (IDENTIFIER=x))");
		code("x--",
			"(POSTINCDEC (DEC) (IDENTIFIER=x))");
		code("x++",
			"(POSTINCDEC (INC) (IDENTIFIER=x))");
		code("a.b",
			"(MEMBER=b (IDENTIFIER=a))");
		code("a[b]",
			"(SUBSCRIPT (IDENTIFIER=a) (IDENTIFIER=b))");
		code(".a.b",
			"(MEMBER=b (MEMBER=a (SELFREF)))");
		code("++a.b.c",
			"(PREINCDEC (INC) (MEMBER=c (MEMBER=b (IDENTIFIER=a))))");
		code("a[b]++",
			"(POSTINCDEC (INC) (SUBSCRIPT (IDENTIFIER=a) (IDENTIFIER=b)))");
		code("a = b + c",
			"(EQ (IDENTIFIER=a) (BINARYOP (ADD) (IDENTIFIER=b) (IDENTIFIER=c)))");
		code("a =\n b",
			"(EQ (IDENTIFIER=a) (IDENTIFIER=b))");
		code("a.b += c",
			"(ASSIGNOP (ADDEQ) (MEMBER=b (IDENTIFIER=a)) (IDENTIFIER=c))");
		code("f(a, b)",
			"(CALL (IDENTIFIER=f) (LIST (ARG null (IDENTIFIER=a)) (ARG null (IDENTIFIER=b))))");
		code("f.g(a + b, c)",
			"(CALL (MEMBER=g (IDENTIFIER=f)) (LIST " +
				"(ARG null (BINARYOP (ADD) (IDENTIFIER=a) (IDENTIFIER=b))) " +
				"(ARG null (IDENTIFIER=c))))");
		code("f(@x)",
			"(CALL (IDENTIFIER=f) (AT=0 (IDENTIFIER=x)))");
		code("f(@+1 x)",
			"(CALL (IDENTIFIER=f) (AT=1 (IDENTIFIER=x)))");
		code("return { x }",
			"(RETURN (BLOCK (LIST) (LIST (IDENTIFIER=x)) null))");
		code("return {|a,b| x }",
			"(RETURN (BLOCK (LIST (IDENTIFIER=a null) (IDENTIFIER=b null)) (LIST (IDENTIFIER=x)) null))");
		code("return {|@a| x }",
			"(RETURN (BLOCK (LIST (IDENTIFIER=@a null)) (LIST (IDENTIFIER=x)) null))");
		code("b = { x }",
			"(EQ (IDENTIFIER=b) (BLOCK (LIST) (LIST (IDENTIFIER=x)) null))");
		code("f(a) { x }",
			"(CALL (IDENTIFIER=f) (LIST (ARG null (IDENTIFIER=a)) " +
				"(ARG (STRING=block) (BLOCK (LIST) (LIST (IDENTIFIER=x)) null))))");
		code("function () { f(a)\n { x } }",
			"(FUNCTION (LIST) (LIST (CALL (IDENTIFIER=f) (LIST " +
				"(ARG null (IDENTIFIER=a)) " +
				"(ARG (STRING=block) (BLOCK (LIST) (LIST (IDENTIFIER=x)) null))))))");
		code("f { x }",
			"(CALL (IDENTIFIER=f) (LIST (ARG (STRING=block) (BLOCK (LIST) (LIST (IDENTIFIER=x)) null))))");
		code("new c",
			"(NEW (IDENTIFIER=c) (LIST))");
		code("new c(a, b)",
			"(NEW (IDENTIFIER=c) (LIST (ARG null (IDENTIFIER=a)) (ARG null (IDENTIFIER=b))))");
		code("new a.c",
			"(NEW (MEMBER=c (IDENTIFIER=a)) (LIST))");
		code("f(a, k: b)",
			"(CALL (IDENTIFIER=f) (LIST (ARG null (IDENTIFIER=a)) (ARG (STRING=k) (IDENTIFIER=b))))");
		code("f = function () { }",
			"(EQ (IDENTIFIER=f) (FUNCTION (LIST) (LIST (NIL))))");
		code("c = class { }",
			"(EQ (IDENTIFIER=c) (CLASS null (LIST)))");
		code("c = Base { }",
			"(EQ (IDENTIFIER=c) (CLASS (STRING=Base) (LIST)))");
		code("c = C\n {\n T: 'a'\n N()\n { } }",
			"(EQ (IDENTIFIER=c) (CLASS (STRING=C) (LIST (MEMBER (STRING=T) (STRING=a)) (MEMBER (STRING=N) (METHOD (LIST) (LIST (NIL)))))))");
		code("O('v'\n #(H F #()))",
			"(CALL (IDENTIFIER=O) (LIST (ARG null (STRING=v)) (ARG null (OBJECT (MEMBER null (STRING=H)) (MEMBER null (STRING=F)) (MEMBER null (OBJECT))))))");
		code("f(u:)",
			"(CALL (IDENTIFIER=f) (LIST (ARG (STRING=u) (TRUE))))");
		code("x = [a,b]",
			"(EQ (IDENTIFIER=x) (CALL (IDENTIFIER=Record) (LIST (ARG null (IDENTIFIER=a)) (ARG null (IDENTIFIER=b)))))");
		code(".x = class\n { }",
			"(EQ (MEMBER=x (SELFREF)) (CLASS null (LIST)))");
		code(".x.f().\n g()",
			"(CALL (MEMBER=g (CALL (MEMBER=f (MEMBER=x (SELFREF))) (LIST))) (LIST))");
		code("100.Times() { }",
			"(CALL (MEMBER=Times (NUMBER=100)) (LIST (ARG (STRING=block) (BLOCK (LIST) (LIST (NIL)) null))))");
		code("100.Times()\n { }",
			"(CALL (MEMBER=Times (NUMBER=100)) (LIST (ARG (STRING=block) (BLOCK (LIST) (LIST (NIL)) null))))");
		code("100.Times { }",
			"(CALL (MEMBER=Times (NUMBER=100)) (LIST (ARG (STRING=block) (BLOCK (LIST) (LIST (NIL)) null))))");
		code("100.Times\n { }",
			"(CALL (MEMBER=Times (NUMBER=100)) (LIST (ARG (STRING=block) (BLOCK (LIST) (LIST (NIL)) null))))");

		code("123 + 456",
			"(BINARYOP (ADD) (NUMBER=123) (NUMBER=456))");
		code("s = 'fred'",
			"(EQ (IDENTIFIER=s) (STRING=fred))");
		code("foo",
			"(IDENTIFIER=foo)");
		code("return",
			"(RETURN null)");
		code("return \n fn",
			"(RETURN null) (IDENTIFIER=fn)");
		code("return fn",
			"(RETURN (IDENTIFIER=fn))");
		code("forever return",
			"(FOREVER (RETURN null))");
		code("forever { forever return }",
			"(FOREVER (LIST (FOREVER (RETURN null))))");
		code("if (a) b",
			"(IF (IDENTIFIER=a) (IDENTIFIER=b) null)");
		code("if a \n b",
			"(IF (IDENTIFIER=a) (IDENTIFIER=b) null)");
		code("if f()\n { b }",
			"(IF (CALL (IDENTIFIER=f) (LIST)) (LIST (IDENTIFIER=b)) null)");
		code("if (a) b; else c",
			"(IF (IDENTIFIER=a) (IDENTIFIER=b) (IDENTIFIER=c))");
		code("if a \n b \n else c",
			"(IF (IDENTIFIER=a) (IDENTIFIER=b) (IDENTIFIER=c))");
		code("if (a) { b } else c",
			"(IF (IDENTIFIER=a) (LIST (IDENTIFIER=b)) (IDENTIFIER=c))");
		code("while a \n b",
			"(WHILE (IDENTIFIER=a) (IDENTIFIER=b))");
		code("while (a) b",
			"(WHILE (IDENTIFIER=a) (IDENTIFIER=b))");
		code("while (a) ;",
			"(WHILE (IDENTIFIER=a) null)");
		code("while (a) { b }",
				"(WHILE (IDENTIFIER=a) (LIST (IDENTIFIER=b)))");
		code("while a \n { b }",
				"(WHILE (IDENTIFIER=a) (LIST (IDENTIFIER=b)))");
		code("do a while b",
			"(DO (IDENTIFIER=a) (IDENTIFIER=b))");
		code("do { a; } while (b)",
			"(DO (LIST (IDENTIFIER=a)) (IDENTIFIER=b))");
		code("while (a) if (b) break",
			"(WHILE (IDENTIFIER=a) (IF (IDENTIFIER=b) (BREAK) null))");
		code("while (a) if (b) continue",
			"(WHILE (IDENTIFIER=a) (IF (IDENTIFIER=b) (CONTINUE) null))");
		code("throw x",
			"(THROW (IDENTIFIER=x))");
		code("try f",
			"(TRY (IDENTIFIER=f) null)");
		code("try f catch g",
			"(TRY (IDENTIFIER=f) (CATCH null (IDENTIFIER=g)))");
		code("try f catch(e) g",
			"(TRY (IDENTIFIER=f) (CATCH=e null (IDENTIFIER=g)))");
		code("try f catch(e, 'p') g",
			"(TRY (IDENTIFIER=f) (CATCH=e (STRING=p) (IDENTIFIER=g)))");
		code("switch a \n { }",
			"(SWITCH (RVALUE (IDENTIFIER=a)) (LIST "
			+ "(CASE (LIST) (LIST (THROW (STRING=unhandled switch case))))))");
		code("switch (a) { case b: f }",
			"(SWITCH (RVALUE (IDENTIFIER=a)) (LIST "
			+ "(CASE (LIST (IDENTIFIER=b)) (LIST (IDENTIFIER=f))) "
			+ "(CASE (LIST) (LIST (THROW (STRING=unhandled switch case))))))");
		code("switch a \n { default: f }",
			"(SWITCH (RVALUE (IDENTIFIER=a)) (LIST (CASE (LIST) (LIST (IDENTIFIER=f)))))");
		code("switch (a) { case b: f \n case c,d: g; h; default: i }",
			"(SWITCH (RVALUE (IDENTIFIER=a)) (LIST (CASE (LIST (IDENTIFIER=b)) (LIST (IDENTIFIER=f))) (CASE (LIST (IDENTIFIER=c) (IDENTIFIER=d)) (LIST (IDENTIFIER=g) (IDENTIFIER=h))) (CASE (LIST) (LIST (IDENTIFIER=i)))))");
		code("for (x in ob) f",
			"(FOR_IN=x (IDENTIFIER=ob) (IDENTIFIER=f))");
		code("for x in ob \n f",
			"(FOR_IN=x (IDENTIFIER=ob) (IDENTIFIER=f))");
		code("for (x in ob) { f }",
			"(FOR_IN=x (IDENTIFIER=ob) (LIST (IDENTIFIER=f)))");
		code("for (;;) f",
			"(FOR null null null (IDENTIFIER=f))");
		code("for (a; b; c) f",
			"(FOR (LIST (IDENTIFIER=a)) (IDENTIFIER=b) (LIST (IDENTIFIER=c)) (IDENTIFIER=f))");
		code("for (a,b; c; d,e) f",
			"(FOR (LIST (IDENTIFIER=a) (IDENTIFIER=b)) (IDENTIFIER=c) (LIST (IDENTIFIER=d) (IDENTIFIER=e)) (IDENTIFIER=f))");
		code("if x is \n y \n z",
			"(IF (BINARYOP (IS) (IDENTIFIER=x) (IDENTIFIER=y)) (IDENTIFIER=z) null)");
		code("return x \n ? y \n : z",
			"(RETURN (Q_MARK (IDENTIFIER=x) (IDENTIFIER=y) (IDENTIFIER=z)))");
		code("return .x.f().\n g()",
			"(RETURN (CALL (MEMBER=g (CALL (MEMBER=f (MEMBER=x (SELFREF))) (LIST))) (LIST)))");
		code("args.Each {|x, y|\n z }",
			"(CALL (MEMBER=Each (IDENTIFIER=args)) (LIST (ARG (STRING=block) (BLOCK (LIST (IDENTIFIER=x null) (IDENTIFIER=y null)) (LIST (IDENTIFIER=z)) null))))");
		code("args.Each()\n {|x, y|\n z }",
			"(CALL (MEMBER=Each (IDENTIFIER=args)) (LIST (ARG (STRING=block) (BLOCK (LIST (IDENTIFIER=x null) (IDENTIFIER=y null)) (LIST (IDENTIFIER=z)) null))))");
		code("args.Each\n {|x, y|\n z }",
			"(CALL (MEMBER=Each (IDENTIFIER=args)) (LIST (ARG (STRING=block) (BLOCK (LIST (IDENTIFIER=x null) (IDENTIFIER=y null)) (LIST (IDENTIFIER=z)) null))))");
		code("s[from .. to]",
			"(SUBSCRIPT (IDENTIFIER=s) (RANGETO (IDENTIFIER=from) (IDENTIFIER=to)))");
		code("a in (b, c)",
			"(IN (IDENTIFIER=a) (LIST (IDENTIFIER=b) (IDENTIFIER=c)))");
	}

	private static void code(String code, String expected) {
		ParseConstantTest.constant("function () { " + code + "}",
				"(FUNCTION (LIST) (LIST " + expected + "))");
	}

}