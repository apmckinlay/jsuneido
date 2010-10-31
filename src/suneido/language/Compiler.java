package suneido.language;

import java.io.PrintWriter;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class Compiler {

	public static Object compile(String name, String src) {
		return compile(name, src, null);
	}

	public static Object compile(String name, String src, PrintWriter pw) {
		Lexer lexer = new Lexer(src);
		AstGenerator generator = new AstGenerator();
		ParseConstant<AstNode, Generator<AstNode>> pc =
				new ParseConstant<AstNode, Generator<AstNode>>(lexer, generator);
		AstNode ast = pc.parse();
		if (pw != null)
			pw.append(ast.toString() + "\n\n");
		return new AstCompile(name, pw).fold(ast);	}

	private static final Object[] noLocals = new Object[0];

	public static Object eval(String s) {
		Object f = compile("eval", "function () { " + s + " }");
		return Ops.call(f, noLocals);
	}

	public static void main(String[] args) {
		String s = "function () { for (i=0; i<10; ++i) continue }";
		PrintWriter pw = new PrintWriter(System.out);
//		Object x =
		compile("Test", s, pw);
//		System.out.println(x);
	}

}
