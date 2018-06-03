/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import java.io.File;

import suneido.runtime.Ops;
import suneido.runtime.Params;
import suneido.util.Dnum;

public class GetDiskFreeSpace {

	@Params("dir = '.'")
	public static Number GetDiskFreeSpace(Object a) {
		String dir = Ops.toStr(a);
		return Dnum.from(new File(dir).getUsableSpace());
	}

}
