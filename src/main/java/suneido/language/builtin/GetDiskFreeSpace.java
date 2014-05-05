/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import java.io.File;
import java.math.BigDecimal;

import suneido.language.Ops;
import suneido.language.Params;

public class GetDiskFreeSpace {

	@Params("dir = '.'")
	public static Number GetDiskFreeSpace(Object a) {
		String dir = Ops.toStr(a);
		return BigDecimal.valueOf(new File(dir).getUsableSpace());
	}

}
