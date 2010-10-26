package suneido.language;

import java.io.IOException;
import java.io.PrintWriter;

public class TestCompile {

	public static void main(String[] args) throws IOException {
//		String s = Files.toString(new File("class.txt"), Charsets.UTF_8);
//		compile("MyClass", s);

//		compile("Test", "class { X: (function () {}) }");
//		Object result = Ops.invoke(c, "func");

//		Object c = compile("Test", "function () { c = class { F() { 123 } }; c.F() }");
//		Object result = Ops.call(c);
//		System.out.println(result);
		compile("Test", "class { F() { b = { super.G() } } }");
//		System.out.println(Ops.call(c));
	}

	private static Object compile(String name, String s) {
		Lexer lexer = new Lexer("function () { (X.F)() }");
		PrintWriter pw = new PrintWriter(System.out);
		AstGenerator generator = new AstGenerator("Test");
		ParseConstant<AstNode, Generator<AstNode>> pc =
				new ParseConstant<AstNode, Generator<AstNode>>(lexer, generator);
		AstNode ast = pc.parse();
		System.out.println(ast);
		Object result = new AstCompile(pw).fold(ast);
		System.out.println(result);
		return result;
	}

}
