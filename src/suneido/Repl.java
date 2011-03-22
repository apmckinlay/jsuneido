/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

/*
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

 BookModel.Create('ETA'); LibTreeModel.Create('configlib'); Wipeout_DemoData()
 Create_DemoData('CAD')

 -agentlib:hprof=cpu=samples,interval=1,depth=6,cutoff=.01
 */

import java.io.*;

import suneido.language.Compiler;
import suneido.language.Ops;

public class Repl {

	public static void repl() throws IOException {
		TheDbms.dbms().size(); // ensure it's open
		Compiler.eval("JInit()");
		PrintWriter out = new PrintWriter(System.out);
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		while (true) {
			out.print("> ");
			out.flush();
			String line = in.readLine();
			if ("q".equals(line))
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
		Suneido.cmdlineoptions = CommandLineOptions.parse("eta.go");
		repl();
	}

}
