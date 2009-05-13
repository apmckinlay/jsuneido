package suneido.language;

import java.io.PrintWriter;
import java.io.StringWriter;

public class TestCompiler {

	public static Object compile(String src) {
		Lexer lexer = new Lexer(src);
		StringWriter sw = new StringWriter();
		CompileGenerator generator =
				new CompileGenerator("Test", new PrintWriter(sw));
		ParseConstant<Object, Generator<Object>> pc =
				new ParseConstant<Object, Generator<Object>>(lexer, generator);
		return pc.parse();
	}

	private static Object[] noLocals = new Object[0];

	public static Object eval(String s) {
		Object f = compile("function () { " + s + " }");
		return Ops.call(f, noLocals);
	}

}
