package suneido.language;

import java.io.*;

import suneido.Repl;

public class CompileFile {
	public static void main(String[] args) throws IOException {
		Repl.setup();

		FileReader f = new FileReader("compilefile.src");
		char buf[] = new char[100000];
		int n = f.read(buf);
		f.close();
		String src = new String(buf, 0, n);
		compile(src);
	}

	public static void compile(String src) {
		Lexer lexer = new Lexer(src);
		PrintWriter pw = new PrintWriter(System.out);
		AstGenerator generator = new AstGenerator();
		ParseConstant<AstNode, Generator<AstNode>> pc =
				new ParseConstant<AstNode, Generator<AstNode>>(lexer, generator);
		AstNode ast = pc.parse();
		System.out.println(ast);
		Object result = new AstCompile("Test", pw).fold(ast);
		System.out.println(result);
	}

}
