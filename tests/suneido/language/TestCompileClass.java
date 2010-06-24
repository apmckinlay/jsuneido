package suneido.language;

import java.io.*;

public class TestCompileClass {

	public static void main(String[] args) throws IOException {
//		String s = Files.toString(new File("class.txt"), Charsets.UTF_8);
//		compile("MyClass", s);

//		Object c =
			compile("Test", "function () { try { try throw 'x' catch (e) return e } return 'y' }");
//		Object result = Ops.invoke(c, "func");
//		System.out.println(result);

//		compile("Test", "class { X: (function () {}) }");
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
