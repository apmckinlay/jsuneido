/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

public class OperatingSystem {

	public static String OperatingSystem() {
		return System.getProperty("os.name") +
				(System.getProperty("os.arch").contains("64") ? " 64bit" : "");
		// NOTE: os.arch will not have 64 if running 32 bit JVM
	}

}
