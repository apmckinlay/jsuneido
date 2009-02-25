package suneido.language;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author Andrew McKinlay
 */
public class ParseExpressionTest {

    @Test
    public void test() {
        String[][] cases = new String[][]{
            { "123", "n(123)" },
			{ "foo", "foo" },
            { "a ? b : c", "(a ? b : c)" },
            { "a or b or c", "a b OR c OR" },
            { "a and b and c", "a b AND c AND" },
            { "a | b | c", "a b BITOR c BITOR" },
            { "a ^ b ^ c", "a b BITXOR c BITXOR" },
            { "a & b & c", "a b BITAND c BITAND" },
            { "a == b == c", "a b IS c IS" },
            { "a != b != c", "a b ISNT c ISNT" },
            { "a =~ b =~ c", "a b MATCH c MATCH" },
            { "a !~ b !~ c", "a b MATCHNOT c MATCHNOT" },
            { "a < b < c", "a b LT c LT" },
            { "a <= b <= c", "a b LTE c LTE" },
            { "a > b > c", "a b GT c GT" },
            { "a >= b >= c", "a b GTE c GTE" },
            { "a >> b >> c", "a b RSHIFT c RSHIFT" },
            { "a << b << c", "a b LSHIFT c LSHIFT" },
            { "a + b + c", "a b ADD c ADD" },
            { "a - b - c", "a b SUB c SUB" },
            { "a $ b $ c", "a b CAT c CAT" },
            { "a * b * c", "a b MUL c MUL" },
            { "a / b / c", "a b DIV c DIV" },
            { "a % b % c", "a b MOD c MOD" },
			{ "a * b + c * d", "a b MUL c d MUL ADD" },
			{ "a * (b + c) * d", "a b c ADD MUL d MUL" },
			{ "+ - ! ~ x", "x BITNOT NOT uSUB uADD" },
			{ "--x", "preDEC(x)" },
			{ "++x", "preINC(x)" },
			{ "x--", "postDEC(x)" },
			{ "x++", "postINC(x)" },
			{ "a.b", "a .b" },
			{ "a[b]", "a b []" },
			{ ".a.b", "this .a .b" },
			{ "++a.b.c", "preINC(a .b .c)" },
			{ "a[b]++", "postINC(a b [])" },
			{ "a = b + c", "b c ADD EQ(a)" },
			{ "a.b += c", "c ADDEQ(a .b)" },
			{ "f(a, b)", "f(a, b)" },
			{ "f.g(a + b, c)", "f .g(a b ADD, c)" },
			{ "f(@x)", "f(@x)" },
			{ "f(@+1 x)", "f(@+1 x)" },
			{ "{ x }", "{ x; }" },
			{ "b = { x }", "{ x; } EQ(b)" },
			{ "{|a,b| x }", "{|a, b| x; }" },
			{ "{|@a| x }", "{|@a| x; }" },
			{ "f(a) { x }", "f(a, { x; })" },
			{ "f { x }", "f({ x; })" },
			{ "new c", "new c" },
			{ "new c(a, b)", "new c(a, b)" },
			{ "new a.c", "new a .c" },
			{ "f(a, k: b)", "f(a, k: b)" },
			{ "f = function () { }", "function () { } EQ(f)" },
			{ "c = class { }", "class { } EQ(c)" },
        };
        for (String[] c : cases) {
            System.out.println(c[0]);
            assertEquals(c[1], parse(c[0]));
        }
    }

    private String parse(String s) {
        Lexer lexer = new Lexer(s);
        StringGenerator generator = new StringGenerator();
        ParseExpression<String> pc = new ParseExpression<String>(lexer, generator);
        String result = pc.expression();
        pc.checkEof();
        return result;
    }
}