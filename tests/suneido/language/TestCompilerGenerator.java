package suneido.language;

import java.io.PrintWriter;
import java.io.StringWriter;

import suneido.SuValue;

public class TestCompilerGenerator {

	private static final StringWriter sw = new StringWriter();

	public static void main(String[] args) throws Exception {
		SuValue f = compile("function () { f = function () { 123 }; f() }");
		System.out.println(sw);
		Object[] locals = new Object[] { 12, null };
		Object result = Ops.invoke(f, "call", locals);
		System.out.println("result: " + result);
	}

    private static SuValue compile(String s) {
		Lexer lexer = new Lexer(s);
		CompileGenerator generator =
				new CompileGenerator("Test", new PrintWriter(sw));
		ParseFunction<Object, Generator<Object>> pc =
				new ParseFunction<Object, Generator<Object>>(lexer, generator);
		return (SuValue) pc.parse();
	}

}
