package suneido.language;

import java.io.*;

import suneido.SuException;
import suneido.SuValue;

public class Repl {
	static PrintWriter out = new PrintWriter(System.out);

	public static void main(String[] args) throws Exception {
		BufferedReader in =
				new BufferedReader(new InputStreamReader(System.in));
		while (true) {
			out.print("> ");
			out.flush();
			String line = in.readLine();
			if ("q".equals(line))
				break;
			try {
				SuValue f = compile("function () { " + line + " }");
				SuValue[] locals = new SuValue[0];
				SuValue result = f.invoke("call", locals);
				if (result != null)
				out.println(" => " + result);
				saveTest(line, result);
			} catch (SuException e) {
				out.println(" !! " + e);
			}
		}
		out.println("bye");
		out.flush();
	}

	private static void saveTest(String line, SuValue result)
			throws FileNotFoundException {
		PrintWriter pw =
				new PrintWriter(new FileOutputStream("repl.txt", true));
		pw.println("test(\"" + line.replace('"', '\'') + "\", \"" + result
				+ "\");");
		pw.close();
	}

	private static SuValue compile(String s) throws FileNotFoundException {
		Lexer lexer = new Lexer(s);
		PrintWriter pw = new PrintWriter(new FileOutputStream("repl.out"));
		CompileGenerator generator = new CompileGenerator(pw);
		ParseFunction<Object, Generator<Object>> pc =
				new ParseFunction<Object, Generator<Object>>(lexer, generator);
		return (SuValue) pc.parse();
	}

}
