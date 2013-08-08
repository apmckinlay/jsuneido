/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import java.util.Date;

import suneido.TheDbms;

public class Timestamp {

	public static Date Timestamp() {
		return TheDbms.dbms().timestamp();
	}

}
