/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.tools;

import java.io.*;

import suneido.SuException;
import suneido.util.JarPath;

public class DbTools {

	public static File tempfile() {
		try {
			File tmpfile = File.createTempFile("sudb", null, new File("."));
			tmpfile.deleteOnExit();
			return tmpfile;
		} catch (IOException e) {
			throw new SuException("Can't create temp file", e);
		}
	}

	public static void renameWithBackup(File tmpfile, String filename) {
		File file = new File(filename);
		File bakfile = new File(filename + ".bak");
		if (bakfile.exists() && !bakfile.delete())
			throw new SuException("can't delete " + bakfile);
		if (file.exists() && !file.renameTo(bakfile))
			throw new SuException("can't rename " + file + " to " + bakfile);
		if (!tmpfile.renameTo(file)) {
			bakfile.renameTo(file);
			throw new SuException("can't rename " + tmpfile + " to " + file);
		}
	}

	public static boolean runWithNewJvm(String cmd) throws InterruptedException {
		String javaHome = System.getProperty("java.home");
		String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
		String jarPath = JarPath.jarPath();
		//System.out.println(javaBin + " -jar " + jarPath + " " + cmd);
		ProcessBuilder builder = new ProcessBuilder(javaBin,
				"-ea", "-jar", jarPath, cmd);
		try {
			builder.redirectErrorStream(true); // merge stderr into stdout
			Process process = builder.start();
			BufferedReader in = new BufferedReader(
					new InputStreamReader(process.getInputStream()));
			while (true) {
				int c = in.read();
				if (c == -1)
					break;
				System.out.print((char) c);
			}
			return 0 == process.waitFor();
		} catch (IOException e) {
			throw new SuException("unable to runWithNewJvm: " + javaBin +
					" -jar " + jarPath + " " + cmd, e);
		}
	}

	public static void main(String[] args) throws InterruptedException {
		System.out.println("success? " + runWithNewJvm("-load:fred"));
	}

}
