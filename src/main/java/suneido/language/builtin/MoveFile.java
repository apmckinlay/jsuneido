/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import java.io.File;

import suneido.language.Ops;
import suneido.language.Params;

public class MoveFile {

	@Params("from, to")
	public static Boolean MoveFile(Object from, Object to) {
		return new File(Ops.toStr(from)).renameTo(new File(Ops.toStr(to)));
	}

}
