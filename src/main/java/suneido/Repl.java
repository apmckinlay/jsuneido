/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import suneido.jsdi.JSDI;
import suneido.language.Compiler;
import suneido.language.Ops;

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
		Compiler.eval("Init()");
		PrintWriter out = new PrintWriter(System.out);
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		out.println("Built: " + WhenBuilt.when());
		if (JSDI.isInitialized()) {
			out.println("JSDI: " + JSDI.getInstance().whenBuilt());
		}
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
				e.printStackTrace(out);
			}
		}
		out.println("bye");
		out.flush();
	}

	public static void main(String[] args) throws Exception {
		Suneido.openDbms();
		Suneido.cmdlineoptions = CommandLineOptions.parse("eta.go");
		repl();
	}

}
