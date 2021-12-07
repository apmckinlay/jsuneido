/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

import static java.lang.System.err;
import static java.lang.System.out;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import suneido.compiler.Compiler;
import suneido.database.immudb.Dbpkg;
import suneido.runtime.Concats;
import suneido.runtime.Except;
import suneido.runtime.Ops;
import suneido.util.Dnum;
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
			Dbpkg.create("suneido.db").close();
		}
		Suneido.openDbms();
		repl2();
	}

	public static void repl2() throws IOException {
		try {
			Compiler.eval("Init.Repl()");
		} catch (SuException e) {
			if (e.getMessage().equals("can't find Init"))
				out.println("WARNING: can't find Init");
			else
				throw e;
		}
		err.println("Built: " + Suneido.built);
		err.println("Press Enter twice (i.e. blank line) to execute, q to quit");

		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		StringBuilder code = new StringBuilder(1024);
		repl: while (true) {
			err.println("~~~");
			code.delete(0,  code.length());
			while (true) {
				String line = in.readLine();
				if (line == null || "q".equals(line))
					break repl;
				if (line.isEmpty())
					break;
				code.append(line).append("\n");
			}
			// Evaluate the code
			try {
				Object result = Compiler.eval(code);
				if (result != null) {
					String type = Ops.typeName(result);
					if (result instanceof Concats)
						type = " <Concat>";
					else if (result instanceof Integer)
						type = " <Integer>";
					else if (result instanceof Dnum)
						type = " <Dnum>";
					else if (result instanceof Except)
						type = " <Except>";
					else if (type.equals("String") ||
							type.equals("Boolean") || type.equals("Date"))
						type = "";
					else
						type = " <" + type + ">";
					out.println(Ops.display(result) + type);
					Thread.sleep(100); // needed for Eclipse to sync output
				}
			} catch (Throwable e) {
				Errlog.error("Repl", e);
			}
		} // repl: while (true)
	}

}
