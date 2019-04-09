/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.SuObject;
import suneido.TheDbms;

public class Libraries {

	public static SuObject Libraries() {
		return new SuObject(TheDbms.dbms().libraries());
	}

}
