package suneido.language.builtin;

import suneido.SuException;
import suneido.language.FunctionSpec;
import suneido.language.Ops;
import suneido.language.SuFunction1;

public class SystemFunction extends SuFunction1 {

	{ params = FunctionSpec.string; }

	@Override
	public Object call1(Object a) {
		String[] cmd = new String[3];
		getShell(cmd);
		cmd[2] = Ops.toStr(a);
		try {
			ProcessBuilder pb = new ProcessBuilder(cmd);
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
