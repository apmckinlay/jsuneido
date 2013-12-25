package suneido.language.builtin;

import suneido.Suneido;

public class Cmdline {

	public static String Cmdline() {
		return Suneido.cmdlineoptions.remainder;
	}

}