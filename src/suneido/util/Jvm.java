/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import suneido.Suneido;

public class Jvm {

	public static boolean runWithNewJvm(String cmd) {
		String javaHome = System.getProperty("java.home");
		String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
		String jarPath = JarPath.jarPath();
		var args = new ArrayList<String>();
		args.add(javaBin);
		args.add("-ea");
		args.add("-jar");
		args.add(jarPath);
		args.add(cmd);
		if (Suneido.cmdlineoptions.asof != null) {
			args.add("-asof");
			args.add(Suneido.cmdlineoptions.asof);
		}
		ProcessBuilder builder = new ProcessBuilder(args);
		try {
			builder.redirectErrorStream(true); // merge stderr into stdout
			Process process = builder.start();
			printProcessOutput(process);
			return 0 == process.waitFor();
		} catch (IOException e) {
			throw new RuntimeException("unable to runWithNewJvm: " + javaBin +
					" -jar " + jarPath + " " + cmd, e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("runWithNewJvm interrupted", e);
		}
	}

	private static void printProcessOutput(Process process) throws IOException {
		BufferedReader in = new BufferedReader(
				new InputStreamReader(process.getInputStream()));
		while (true) {
			int c = in.read();
			if (c == -1)
				break;
			System.out.print((char) c);
		}
	}

}
