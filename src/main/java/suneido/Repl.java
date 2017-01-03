/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

import java.io.*;

import suneido.compiler.Compiler;
import suneido.jsdi.JSDI;
import suneido.runtime.Ops;
import suneido.util.Errlog;

/*
 -agentlib:hprof=cpu=samples,interval=1,depth=6,cutoff=.01

 CheckLibrary('stdlib')
 CheckLibraries(Libraries())
 TestRunner.Run(libs: #(stdlib), skipTags: #(gui, windows), quit_on_failure:);;
 TestRunner.Run(skipTags: #(gui, windows), quit_on_failure:);;
 */

public class Repl {

	public static void repl() throws IOException {
		if (! new File("suneido.dbd").exists() && ! new File("suneido.dbi").exists()) {
			System.out.println("WARNING: no database found, creating an empty one");
			Suneido.dbpkg.create("suneido.db").close();
		}
		Suneido.openDbms();
		repl2();
	}

	public static void repl2() throws IOException {
		PrintWriter out = new PrintWriter(System.out);
		try {
			Compiler.eval("Init()"); // will not return if JSDI
		} catch (SuException e) {
			if (e.getMessage().equals("can't find Init"))
				out.println("WARNING: can't find Init");
			else
				throw e;
		}
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		out.print("Built: " + WhenBuilt.when());
		if (JSDI.isInitialized())
			out.print(" JSDI: " + JSDI.getInstance().whenBuilt());
		out.print(" debug: " + Suneido.cmdlineoptions.debugModel);
		out.println();

		StringBuilder code = new StringBuilder(1024);
		repl: while (true) {
			out.print("> ");
			out.flush();
			code.delete(0,  code.length());
			// Collect 1 or more lines of input: if a line ends with '\', it
			// indicates to ignore the '\' and continue reading the next line.
			while (true) {
				String line = in.readLine();
				if (line == null || "q".equals(line))
					break repl;
				if (line.isEmpty())
					break;
				if ('\\' == line.charAt(line.length() - 1)) {
					code.append(line, 0, line.length() - 1).append('\n');
				} else {
					code.append(line);
					break;
				}
			}
			// Evaluate the code
			try {
				Object result = Compiler.eval(code);
				if (result != null)
					out.println(" => " + Ops.display(result));
			} catch (Throwable e) {
				Errlog.error("Repl", e);
			}
		} // repl: while (true)
		out.println("bye");
		out.flush();
	}

	public static void main(String[] args) throws Exception {
		Suneido.cmdlineoptions = CommandLineOptions.parse("-nojsdi",  "eta.go");
		repl();
	}

}
