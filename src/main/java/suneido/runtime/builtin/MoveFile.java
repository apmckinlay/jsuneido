/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import suneido.runtime.Ops;
import suneido.runtime.Params;

public class MoveFile {

	@Params("from, to")
	public static Boolean MoveFile(Object from, Object to) {
		try {
			Files.move(Paths.get(Ops.toStr(from)), Paths.get(Ops.toStr(to)),
					StandardCopyOption.REPLACE_EXISTING);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

}
