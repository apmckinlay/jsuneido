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
            { "a or b or c", "((a OR b) OR c)" },
            { "a and b and c", "((a AND b) AND c)" },
            { "a | b | c", "((a BITOR b) BITOR c)" },
            { "a ^ b ^ c", "((a BITXOR b) BITXOR c)" },
            { "a & b & c", "((a BITAND b) BITAND c)" },
            { "a == b == c", "((a IS b) IS c)" },
            { "a != b != c", "((a ISNT b) ISNT c)" },
            { "a =~ b =~ c", "((a MATCH b) MATCH c)" },
            { "a !~ b !~ c", "((a MATCHNOT b) MATCHNOT c)" },
            { "a < b < c", "((a LT b) LT c)" },
            { "a <= b <= c", "((a LTE b) LTE c)" },
            { "a > b > c", "((a GT b) GT c)" },
            { "a >= b >= c", "((a GTE b) GTE c)" },
            { "a >> b >> c", "((a RSHIFT b) RSHIFT c)" },
            { "a << b << c", "((a LSHIFT b) LSHIFT c)" },
            { "a + b + c", "((a ADD b) ADD c)" },
            { "a - b - c", "((a SUB b) SUB c)" },
            { "a $ b $ c", "((a CAT b) CAT c)" },
            { "a * b * c", "((a MUL b) MUL c)" },
            { "a / b / c", "((a DIV b) DIV c)" },
            { "a % b % c", "((a MOD b) MOD c)" },
			{ "a * b + c * d", "((a MUL b) ADD (c MUL d))" },
			{ "a * (b + c) * d", "((a MUL (b ADD c)) MUL d)" },
			{ "+ - ! ~ x", "(ADD (SUB (NOT (BITNOT x))))" },
			{ "--x", "--(x)" }, { "++x", "++(x)" }, { "x--", "(x)--" }, { "x++", "(x)++" },
			{ "a.b", "a.b" },
			{ "++a.b.c", "++(a.b.c)" },
			{ "a[b]", "a[b]" },
			{ "a[b]++", "(a[b])++" },
			{ ".a.b", "this.a.b" },
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