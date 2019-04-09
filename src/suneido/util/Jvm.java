/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class Jvm {

	public static boolean runWithNewJvm(String cmd) {
		String javaHome = System.getProperty("java.home");
		String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
		String jarPath = JarPath.jarPath();
		ProcessBuilder builder =
				new ProcessBuilder(javaBin, "-ea", "-jar", jarPath, cmd);
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
