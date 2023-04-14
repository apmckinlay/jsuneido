/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

public class OSName {

	public static String OSName() {
		var os = System.getProperty("os.name");
		os = os.toLowerCase();
		if (os.contains("windows"))
			return "windows";
		else if (os.contains("linux"))
			return "linux";
		else if (os.contains("mac"))
			return "macos";
		else
			throw new RuntimeException("unknown OS: " + os);
	}

}
