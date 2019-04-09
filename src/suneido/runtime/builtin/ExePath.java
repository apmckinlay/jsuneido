/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.util.JarPath;

public class ExePath {

	public static String ExePath() {
		return JarPath.jarPath();
	}

}
