/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.SuRecord;
import suneido.runtime.Params;

public class RecordQ {

	@Params("value")
	public static Boolean RecordQ(Object a) {
		return a instanceof SuRecord;
	}

}
