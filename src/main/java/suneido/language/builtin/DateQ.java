/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import java.util.Date;

import suneido.language.Params;

public class DateQ {

	@Params("value")
	public static Boolean DateQ(Object a) {
		return a instanceof Date;
	}

}