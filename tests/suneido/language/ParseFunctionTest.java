package suneido.language;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * @author Andrew McKinlay
 */
public class ParseFunctionTest {

    @Test
    public void test() {
        String[][] cases = new String[][]{
			{ "foo", "foo;" },
			{ "a * b + c * d", "((a MUL b) ADD (c MUL d));" },
            { "return", "return;" },
			{ "forever return", "forever { return; }" },
			{ "forever { forever return }", "forever { forever { return; } }" },
        };
        for (String[] c : cases) {
            System.out.println(c[0]);
            assertEquals("function () { " + c[1] + " }",
					parse("function () { " + c[0] + " }"));
        }
    }

    private String parse(String s) {
        Lexer lexer = new Lexer(s);
        StringGenerator generator = new StringGenerator();
        ParseFunction<String> pc = new ParseFunction<String>(lexer, generator);
        String result = pc.function();
        pc.checkEof();
        return result;
    }
}