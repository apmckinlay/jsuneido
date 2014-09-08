/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.SuContainer;
import suneido.TheDbms;

public class Libraries {

	public static SuContainer Libraries() {
		return new SuContainer(TheDbms.dbms().libraries());
	}

}
