/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime.builtin;

import java.util.concurrent.TimeUnit;

import suneido.SuObject;
import suneido.Suneido;
import suneido.runtime.Ops;
import suneido.runtime.Params;

public class Scheduled {

	@Params("ms, block")
	public static Object Scheduled(Object ms, Object fn) {
		Suneido.schedule(new Run(fn, SuThread.subSuneido.get()),
				Ops.toInt(ms), TimeUnit.MILLISECONDS);
		return null;
	}

	private static class Run implements Runnable {
		private final Object fn;
		private final SuObject subSuneido;

		public Run(Object fn, SuObject subSuneido) {
			this.fn = fn;
			this.subSuneido = subSuneido;
		}
		@Override
		public void run() {
			try {
				SuThread.subSuneido.set(this.subSuneido);
				Ops.call(fn);
			} finally {
				SuThread.subSuneido.remove();
			}
		}
	}

}
