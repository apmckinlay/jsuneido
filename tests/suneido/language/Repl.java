package suneido.language;

// TestRunner.RunLib('stdlib', quit_on_failure:, exclude: #(CheckCode_Test));;

import java.io.*;
import java.util.Map;

import suneido.*;
import suneido.database.*;

public class Repl {
	static PrintWriter out = new PrintWriter(System.out);

	public static void main(String[] args) throws Exception {
		setup();

		BufferedReader in =
				new BufferedReader(new InputStreamReader(System.in));
		Compiler.eval("JInit()");
		while (true) {
			out.print("> ");
			out.flush();
			String line = in.readLine();
			if ("q".equals(line))
				break;
			try {
				Object f = compile("function () { " + line + " }");
				Object[] locals = new SuValue[0];
				Object result = Ops.call(f, locals);
				if (result != null)
					out.println(" => " + Ops.display(result));
				saveTest(line, result);
			} catch (SuException e) {
				e.printStackTrace();
			}
		}
		out.println("bye");
		out.flush();
	}

	public static void setup() {
		Mmfile mmf = new Mmfile("suneido.db", Mode.OPEN);
		Database.theDB = new Database(mmf, Mode.OPEN);

		Globals.put("Print", new Print());
		Globals.put("Alert", new Alert());
	}

	private static void saveTest(String line, Object result)
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
		CompileGenerator generator = new CompileGenerator("Repl", pw);
		ParseFunction<Object, Generator<Object>> pc =
				new ParseFunction<Object, Generator<Object>>(lexer, generator);
		return (SuValue) pc.parse();
	}

	static class Print extends SuFunction {
		@Override
		public Object call(Object... args) {
			SuContainer c = Args.collectArgs(args, new SuContainer());
			int i = 0;
			for (; i < c.vecSize(); ++i)
				System.out.print((i > 0 ? " " : "") + Ops.toStr(c.get(i)));
			for (Map.Entry<Object, Object> e : c.mapEntrySet())
				System.out.print((i++ > 0 ? " " : "") + e.getKey() + ": "
						+ Ops.toStr(e.getValue()));
			System.out.println();
			return null;
		}
	}

	static class Alert extends SuFunction {
		@Override
		public Object call(Object... args) {
			System.out.println("ALERT: " + Ops.toStr(args[0]));
			return null;
		}
	}

}
