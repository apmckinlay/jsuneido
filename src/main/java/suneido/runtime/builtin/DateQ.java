/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.SuDate;
import suneido.runtime.Params;

public class DateQ {

	@Params("value")
	public static Boolean DateQ(Object a) {
		return a instanceof SuDate;
	}

}
