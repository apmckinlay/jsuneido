package suneido.language;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class Compiler {

	public static Object compile(String name, String src) {
		Lexer lexer = new Lexer(src);
		AstGenerator generator = new AstGenerator();
		ParseConstant<AstNode, Generator<AstNode>> pc =
				new ParseConstant<AstNode, Generator<AstNode>>(lexer, generator);
		AstNode ast = pc.parse();
		return new AstCompile(name).fold(ast);	}

	private static final Object[] noLocals = new Object[0];

	public static Object eval(String s) {
		Object f = compile("eval", "function () { " + s + " }");
		return Ops.call(f, noLocals);
	}

}
