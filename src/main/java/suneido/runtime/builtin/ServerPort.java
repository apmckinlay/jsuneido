/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import suneido.Suneido;

public class ServerPort {

	public static int ServerPort() {
		return Suneido.cmdlineoptions.serverPort;
	}

}
