/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import java.util.concurrent.TimeUnit;

import suneido.Suneido;
import suneido.language.Ops;
import suneido.language.Params;

public class Delayed {

	@Params("ms, function")
	public static Object Delayed(Object ms, Object fn) {
		Suneido.schedule(new Run(fn),
				Ops.toInt(ms), TimeUnit.MILLISECONDS);
		return null;
	}

	private static class Run implements Runnable {
		private final Object fn;
		public Run(Object fn) {
			this.fn = fn;
		}
		@Override
		public void run() {
			Ops.call(fn);
		}
	}

	// TODO return a Timer object with a Kill method

}
