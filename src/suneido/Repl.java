package suneido;

/*
 TestRunner.Run(#(stdlib), skipTags: #(gui, windows), quit_on_failure:)
 TestRunner.Run(#(Accountinglib), skipTags: #(windows), quit_on_failure:)
 TestRunner.Run(#(etalib), skipTags: #(windows), quit_on_failure:)
 TestRunner.Run(#(ticketlib), skipTags: #(windows), quit_on_failure:)
 TestRunner.Run(#(joblib), skipTags: #(windows), quit_on_failure:)
 TestRunner.Run(#(prlib), skipTags: #(windows), quit_on_failure:)
 TestRunner.Run(#(prcadlib), skipTags: #(windows), quit_on_failure:)
 TestRunner.Run(#(etaprlib), skipTags: #(windows), quit_on_failure:)
 TestRunner.Run(#(invenlib), skipTags: #(windows), quit_on_failure:)
 TestRunner.Run(#(wolib), skipTags: #(windows), quit_on_failure:)
 TestRunner.Run(#(polib), skipTags: #(windows), quit_on_failure:)
 TestRunner.Run(skipTags: #(gui, windows), quit_on_failure:);;

 BookModel.Create('ETA'); LibTreeModel.Create('configlib'); Wipeout_DemoData()
 Create_DemoData('CAD')

 -agentlib:hprof=cpu=samples,interval=1,depth=6,cutoff=.01
 */

import java.io.*;
import java.util.Map;

import suneido.database.Database;
import suneido.language.*;
import suneido.language.Compiler;

public class Repl {
	static PrintWriter out = new PrintWriter(System.out);

	public static void main(String[] args) throws Exception {
		setup();

		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		while (true) {
			out.print("> ");
			out.flush();
			String line = in.readLine();
			if ("q".equals(line))
				break;
			try {
				Object f = compile("function () { " + line + "\n}");
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
		if (Suneido.cmdlineoptions == null)
			Suneido.cmdlineoptions = CommandLineOptions.parse(new String[0]);

		Database.open_theDB();

		Globals.builtin("Print", new Print());

		Compiler.eval("Use('Accountinglib')");
		Compiler.eval("Use('etalib')");
		Compiler.eval("Use('ticketlib')");
		Compiler.eval("Use('joblib')");
		Compiler.eval("Use('prlib')");
		Compiler.eval("Use('prcadlib')");
		Compiler.eval("Use('etaprlib')");
		Compiler.eval("Use('invenlib')");
		Compiler.eval("Use('wolib')");
		Compiler.eval("Use('polib')");
		Compiler.eval("Use('configlib')");
		Compiler.eval("Use('demobookoptions')");
		Compiler.eval("JInit()");
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

	static class Print extends BuiltinFunction {
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

}
