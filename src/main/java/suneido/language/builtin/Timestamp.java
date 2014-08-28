/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import suneido.SuDate;
import suneido.TheDbms;

public class Timestamp {

	public static SuDate Timestamp() {
		return TheDbms.dbms().timestamp();
	}

}
