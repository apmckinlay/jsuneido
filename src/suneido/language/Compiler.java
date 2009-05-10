package suneido.language;

public class Compiler {

	public static Object compile(String name, String src) {
		Lexer lexer = new Lexer(src);
		CompileGenerator generator = new CompileGenerator(name);
		ParseConstant<Object, Generator<Object>> pc =
				new ParseConstant<Object, Generator<Object>>(lexer, generator);
		return pc.parse();
	}

	private static Object[] noLocals = new Object[0];
	public static Object eval(String s) {
		Object f = compile("eval", "function () { " + s + " }");
		return Ops.call(f, noLocals);
	}

}
