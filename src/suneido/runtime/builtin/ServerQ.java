/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import static suneido.CommandLineOptions.Action.SERVER;
import suneido.Suneido;

public class ServerQ {

	public static boolean ServerQ() {
		return Suneido.cmdlineoptions.action == SERVER;
	}

}
