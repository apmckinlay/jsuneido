/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.compiler;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import suneido.compiler.Compiler;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

public class CompileFile {
	public static void main(String[] args) throws IOException {
//		Repl.setup();
		String src = Files.toString(new File("compilefile.src"), Charsets.US_ASCII);
		Object result = Compiler.compile("Test", src,
				new PrintWriter(System.out), true);
		System.out.println(result);
	}

}
