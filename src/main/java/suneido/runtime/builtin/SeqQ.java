/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;
import suneido.runtime.Params;
import suneido.runtime.SequenceBase;

public class SeqQ {

	@Params("value")
	public static Boolean SeqQ(Object a) {
		return a instanceof SequenceBase;
	}

}
