/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

import java.io.*;

import suneido.compiler.Compiler;
import suneido.debug.CallstackProvider;
import suneido.jsdi.JSDI;
import suneido.runtime.Ops;

/*
 -agentlib:hprof=cpu=samples,interval=1,depth=6,cutoff=.01

 CheckLibraries(Libraries())
 CheckLibrary('stdlib')

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
		out.println("Built: " + WhenBuilt.when());
		if (JSDI.isInitialized())
			out.println("JSDI: " + JSDI.getInstance().whenBuilt());
		while (true) {
			out.print("> ");
			out.flush();
			String line = in.readLine();
			if (line == null || "q".equals(line))
				break;
			try {
				Object result = Compiler.eval(line);
				if (result != null)
					out.println(" => " + Ops.display(result));
			} catch (Throwable e) {
				if (e instanceof CallstackProvider) {
					((CallstackProvider)e).printCallstack(out);
				} else {
					e.printStackTrace(out);
				}
			}
		}
		out.println("bye");
		out.flush();
	}

	public static void main(String[] args) throws Exception {
		Suneido.cmdlineoptions = CommandLineOptions.parse("-nojsdi",  "eta.go");
		repl();
	}

}
