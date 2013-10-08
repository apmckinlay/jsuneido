/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import suneido.language.Ops;
import suneido.language.Params;

public class MoveFile {

	@Params("from, to")
	public static Boolean MoveFile(Object from, Object to) {
		try {
			Files.move(Paths.get(Ops.toStr(from)), Paths.get(Ops.toStr(to)));
			return true;
		} catch (IOException e) {
			return false;
		}
	}

}
