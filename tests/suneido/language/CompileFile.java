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

	public static Object compile(String src) {
		Lexer lexer = new Lexer(src);
		StringWriter sw = new StringWriter();
		CompileGenerator generator =
				new CompileGenerator("Test", new PrintWriter(sw));
		ParseConstant<Object, Generator<Object>> pc =
				new ParseConstant<Object, Generator<Object>>(lexer, generator);
		return pc.parse();
	}

}
