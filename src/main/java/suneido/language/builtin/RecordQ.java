/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import suneido.SuRecord;
import suneido.language.Params;

public class RecordQ {

	@Params("value")
	public static Boolean RecordQ(Object a) {
		return a instanceof SuRecord;
	}

}
