package suneido.language;

import java.io.PrintWriter;
import java.io.StringWriter;

public class TestCompileClass {

	public static void main(String[] args) {
		Object c = compile("Test", "class { f() { 123 } G() { 456 } }");
		Object result = Ops.invoke(c, "G");
		System.out.println(result);
	}

	private static Object compile(String name, String s) {
System.out.println("====== " + s);
		Lexer lexer = new Lexer(s);
		StringWriter sw = new StringWriter();
		CompileGenerator generator =
				new CompileGenerator(name, new PrintWriter(sw));
		ParseConstant<Object, Generator<Object>> pc =
				new ParseConstant<Object, Generator<Object>>(lexer, generator);
		Object c = pc.parse();
		String r = sw.toString();
System.out.println(r);
		return c;
	}

}
