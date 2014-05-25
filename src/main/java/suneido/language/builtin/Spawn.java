/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import java.util.ArrayList;

import suneido.SuException;
import suneido.language.ArgsIterator;
import suneido.language.Ops;

public class Spawn {
	static final int WAIT = 0;
	static final int NOWAIT = 1;

	public static Integer Spawn(Object... a) {
		ArgsIterator iter = new ArgsIterator(a);
		if (! iter.hasNext())
			usage();
		int mode = Ops.toInt(iter.next());
		if (mode != WAIT && mode != NOWAIT)
			throw new RuntimeException("Spawn bad mode");

		ArrayList<String> args = new ArrayList<>();
		while (iter.hasNext())
			args.add(Ops.toStr(iter.next()));
		if (args.size() == 0)
			usage();

		try {
			ProcessBuilder pb = new ProcessBuilder(args);
			pb.inheritIO();
			Process proc = pb.start();
			return mode == NOWAIT ? 0 : proc.waitFor();
		} catch (Throwable e) {
			throw new SuException("Spawn failed", e);
		}
	}

	private static void usage() {
		throw new SuException("usage: Spawn(mode, cmd, arg...)");
	}

}
