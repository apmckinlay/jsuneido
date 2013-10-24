/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import suneido.SuException;
import suneido.language.Ops;
import suneido.language.Params;

public class SystemFunction {

	@Params("string")
	public static Integer System(Object a) {
		String[] cmd = new String[3];
		getShell(cmd);
		cmd[2] = Ops.toStr(a);
		try {
			ProcessBuilder pb = new ProcessBuilder(cmd);
			pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
			pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
			pb.redirectError(ProcessBuilder.Redirect.INHERIT);
			Process proc = pb.start();
			return proc.waitFor();
		} catch (Throwable e) {
			throw new SuException("System failed", e);
		}
	}

	private static void getShell(String[] cmd) {
		cmd[0] = System.getenv("COMSPEC");
		if (cmd[0] != null) {
			cmd[1] = "/c";
			return;
		}
		cmd[0] = System.getenv("SHELL");
		if (cmd[0] != null) {
			cmd[1] = "-c";
			return;
		}
		throw new SuException("System failed: did not find COMSPEC or SHELL");
	}

}
