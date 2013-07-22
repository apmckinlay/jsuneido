/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

public class GetTempPath {

	public static String GetTempPath() {
		return System.getProperty("java.io.tmpdir").replace('\\', '/');
	}

}
