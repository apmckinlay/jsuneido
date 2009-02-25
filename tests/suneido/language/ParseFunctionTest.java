package suneido.language;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author Andrew McKinlay
 */
public class ParseFunctionTest {

    @Test
    public void test() {
        String[][] cases = new String[][]{
			{ "foo", "foo;" },
			{ "return", "return;" },
			{ "return \n fn", "return; fn;" },
			{ "return fn", "return fn;" },
			{ "forever return", "forever { return; }" },
			{ "forever { forever return }", "forever { forever { return; } }" },
			{ "if (a) b",	"if (a) { b; }" },
			{ "if a \n b",	"if (a) { b; }" },
			{ "if (a) b; else c", "if (a) { b; } else { c; }" },
			{ "if a \n b \n else c", "if (a) { b; } else { c; }" },
			{ "if (a) { b } else c", "if (a) { b; } else { c; }" },
			{ "while a \n b", "while (a) { b; }" },
			{ "while (a) b", "while (a) { b; }" },
			{ "while (a) ;", "while (a) { }" },
			{ "while a \n { b }", "while (a) { b; }" },
			{ "do a while b", "do { a; } while (b);" },
			{ "do { a; } while (b)", "do { a; } while (b);" },
			{ "while (a) if (b) break", "while (a) { if (b) { break; } }" },
			{ "while (a) if (b) continue", "while (a) { if (b) { continue; } }" },
			{ "throw x", "throw x;" },
			{ "try f", "try { f; }" },
			{ "try f catch g", "try { f; } catch { g; }" },
			{ "try f catch(e) g", "try { f; } catch(e) { g; }" },
			{ "try f catch(e, 'p') g", "try { f; } catch(e, 'p') { g; }" },
			{ "switch a \n { }", "switch (a) { }" },
			{ "switch (a) { case b: f }", "switch (a) { case b: f; }" },
			{ "switch a \n { default: f }", "switch (a) { default: f; }" },
			{ "switch (a) { case b: f \n case c,d: g; h; default: i }",
					"switch (a) { case b: f; case c, d: g; h; default: i; }" },
			{ "for (x in ob) f", "for (x in ob) { f; }" },
			{ "for x in ob \n f", "for (x in ob) { f; }" },
			{ "for (x in ob) { f }", "for (x in ob) { f; }" },
			{ "for (;;) f", "for (; ; ) { f; }" },
			{ "for (a; b; c) f", "for (a; b; c) { f; }" },
			{ "for (a,b; c; d,e) f", "for (a, b; c; d, e) { f; }" },
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