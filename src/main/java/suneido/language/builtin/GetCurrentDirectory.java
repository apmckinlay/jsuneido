/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import java.io.File;
import java.io.IOException;

import suneido.SuException;

public class GetCurrentDirectory {

	public static String GetCurrentDirectory() {
		try {
			return new File(".").getCanonicalPath();
		} catch (IOException e) {
			throw new SuException("GetCurrentDirectory", e);
		}
	}

}
